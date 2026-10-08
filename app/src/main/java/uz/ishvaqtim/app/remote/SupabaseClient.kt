package uz.ishvaqtim.app.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Server javobi xato bo'lsa (masalan, noto'g'ri so'rov), bu bilan bildiriladi. Internet xatosi EMAS. */
class SupabaseServerException(val code: Int, message: String) : Exception(message)

/**
 * Supabase bazasi bilan oddiy aloqa (PostgREST orqali).
 *
 * MUHIM: bu yerdagi funksiyalar xatoni "yutib yubormaydi" - ular xato bo'lsa shu xatoni
 * yuqoriga uzatadi (throw qiladi). Shunisi muhim, chunki chaqiruvchi tomon:
 *   - IOException bo'lsa -> internet yo'q, demak navbatga qo'yish kerak
 *   - SupabaseServerException bo'lsa -> internet bor, lekin server rad etdi (navbatga qo'yish foydasiz)
 * ikkisini bir-biridan FARQLASHI kerak.
 */
object SupabaseClient {

    private const val PROJECT_URL = "https://hkyyrsgnoonsihbwydrs.supabase.co"
    private const val ANON_KEY = "sb_publishable_kdlfcT6hiuEXhZCwWPApeg__SFyVNsX"

    /** Karta skanini "nfc_scans" jadvaliga yozadi (hali hech kimga bog'lanmagan karta uchun). */
    suspend fun insertNfcScan(cardHash: String) {
        val body = JSONObject().apply { put("card_hash", cardHash) }
        postJson("/rest/v1/nfc_scans", body)
    }

    /** Karta kodi bo'yicha profilni qidiradi. Topilmasa null. */
    suspend fun findProfileByCardHash(cardHash: String): JSONObject? {
        val path = "/rest/v1/profiles?card_hash=eq.${encode(cardHash)}&limit=1"
        val rows = getJsonArray(path)
        return if (rows.length() > 0) rows.getJSONObject(0) else null
    }

    /** Shu profil uchun, shu kunga tegishli attendance qatorini qidiradi. Topilmasa null. */
    suspend fun findAttendance(profileId: Long, workDate: String): JSONObject? {
        val path = "/rest/v1/attendance?profile_id=eq.$profileId&work_date=eq.$workDate&limit=1"
        val rows = getJsonArray(path)
        return if (rows.length() > 0) rows.getJSONObject(0) else null
    }

    /**
     * Kirish yoki chiqish vaqtini yozadi/yangilaydi.
     * (profile_id, work_date) juftligi bo'yicha jadvalda "unique" cheklov bor,
     * shuning uchun bu amal avtomatik ravishda yoki yangi qator yaratadi, yoki borini yangilaydi.
     */
    suspend fun upsertAttendance(
        profileId: Long,
        workDate: String,
        checkIn: String?,
        checkOut: String?
    ) {
        val body = JSONObject().apply {
            put("profile_id", profileId)
            put("work_date", workDate)
            if (checkIn != null) put("check_in", checkIn)
            if (checkOut != null) put("check_out", checkOut)
            put("source", "nfc")
        }
        val path = "/rest/v1/attendance?on_conflict=profile_id,work_date"
        postJson(path, body, merge = true)
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    /** @throws IOException internet/ulanish muammosi bo'lsa. @throws SupabaseServerException server rad etsa. */
    private suspend fun getJsonArray(path: String): JSONArray = withContext(Dispatchers.IO) {
        val connection = (URL(PROJECT_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("apikey", ANON_KEY)
            setRequestProperty("Authorization", "Bearer $ANON_KEY")
        }

        // Diqqat: connection.responseCode o'zi IOException tashlashi mumkin (masalan internet yo'q bo'lsa) -
        // buni ataylab ushlamaymiz, chaqiruvchiga shu xato ko'rinishicha boradi.
        val code = connection.responseCode

        if (code !in 200..299) {
            connection.disconnect()
            throw SupabaseServerException(code, "GET $path -> $code")
        }

        val text = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
        connection.disconnect()
        JSONArray(text)
    }

    /** merge=true bo'lsa, Supabase'ga "mavjud bo'lsa yangila" deb aytamiz (upsert). */
    private suspend fun postJson(path: String, body: JSONObject, merge: Boolean = false) {
        withContext(Dispatchers.IO) {
            val connection = (URL(PROJECT_URL + path).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("apikey", ANON_KEY)
                setRequestProperty("Authorization", "Bearer $ANON_KEY")
                setRequestProperty("Content-Type", "application/json")
                val prefer = if (merge) "resolution=merge-duplicates,return=minimal" else "return=minimal"
                setRequestProperty("Prefer", prefer)
            }

            OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }

            val code = connection.responseCode
            connection.disconnect()

            if (code !in 200..299) {
                throw SupabaseServerException(code, "POST $path -> $code")
            }
        }
    }
}
