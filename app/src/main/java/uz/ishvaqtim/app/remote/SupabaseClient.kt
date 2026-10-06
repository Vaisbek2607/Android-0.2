package uz.ishvaqtim.app.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Supabase bazasi bilan oddiy aloqa.
 * Supabase "PostgREST" degan tayyor server orqali ishlaydi: biz jadval nomini
 * manzilga qo'shib, oddiy HTTP so'rov yuboramiz (maxsus kutubxona shart emas).
 *
 * ANON_KEY - bu "ochiq" kalit, faqat ruxsat etilgan amallarni bajaradi
 * (hozircha jadval qoidalari "hammaga ruxsat" qilib qo'yilgan, keyinroq qattiqlashtiramiz).
 */
object SupabaseClient {

    private const val PROJECT_URL = "https://hkyyrsgnoonsihbwydrs.supabase.co"
    private const val ANON_KEY = "sb_publishable_kdlfcTZhiuEXhZCwWPApeg__SFyVNsX"

    private const val TAG = "SupabaseClient"

    /** Karta skanini "nfc_scans" jadvaliga yozadi (hali hech kimga bog'lanmagan karta uchun). */
    suspend fun insertNfcScan(cardHash: String): Boolean = withContext(Dispatchers.IO) {
        val body = JSONObject().apply { put("card_hash", cardHash) }
        postJson("/rest/v1/nfc_scans", body) != null
    }

    /** Karta kodi bo'yicha profilni qidiradi. Topilmasa null. */
    suspend fun findProfileByCardHash(cardHash: String): JSONObject? = withContext(Dispatchers.IO) {
        val path = "/rest/v1/profiles?card_hash=eq.${encode(cardHash)}&limit=1"
        val rows = getJsonArray(path) ?: return@withContext null
        if (rows.length() > 0) rows.getJSONObject(0) else null
    }

    /** Shu profil uchun, shu kunga tegishli attendance qatorini qidiradi. Topilmasa null. */
    suspend fun findAttendance(profileId: Long, workDate: String): JSONObject? = withContext(Dispatchers.IO) {
        val path = "/rest/v1/attendance?profile_id=eq.$profileId&work_date=eq.$workDate&limit=1"
        val rows = getJsonArray(path) ?: return@withContext null
        if (rows.length() > 0) rows.getJSONObject(0) else null
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
    ): Boolean = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("profile_id", profileId)
            put("work_date", workDate)
            if (checkIn != null) put("check_in", checkIn)
            if (checkOut != null) put("check_out", checkOut)
            put("source", "nfc")
        }
        val path = "/rest/v1/attendance?on_conflict=profile_id,work_date"
        postJson(path, body, merge = true) != null
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun getJsonArray(path: String): JSONArray? {
        return try {
            val url = URL(PROJECT_URL + path)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("apikey", ANON_KEY)
            connection.setRequestProperty("Authorization", "Bearer $ANON_KEY")

            val code = connection.responseCode
            if (code !in 200..299) {
                Log.w(TAG, "GET $path -> $code")
                connection.disconnect()
                return null
            }

            val text = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            connection.disconnect()
            JSONArray(text)
        } catch (e: Exception) {
            Log.w(TAG, "GET xatosi: ${e.message}")
            null
        }
    }

    /** merge=true bo'lsa, Supabase'ga "mavjud bo'lsa yangila" deb aytamiz (upsert). */
    private fun postJson(path: String, body: JSONObject, merge: Boolean = false): JSONObject? {
        return try {
            val url = URL(PROJECT_URL + path)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("apikey", ANON_KEY)
            connection.setRequestProperty("Authorization", "Bearer $ANON_KEY")
            connection.setRequestProperty("Content-Type", "application/json")
            val prefer = if (merge) "resolution=merge-duplicates,return=minimal" else "return=minimal"
            connection.setRequestProperty("Prefer", prefer)

            OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }

            val code = connection.responseCode
            connection.disconnect()

            if (code in 200..299) JSONObject() else {
                Log.w(TAG, "POST $path -> $code")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "POST xatosi: ${e.message}")
            null
        }
    }
}
