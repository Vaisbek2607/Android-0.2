package uz.ishvaqtim.app.sync

import android.content.Context
import uz.ishvaqtim.app.data.PendingAttendanceDao
import uz.ishvaqtim.app.data.PendingScanDao
import uz.ishvaqtim.app.remote.SupabaseClient
import java.io.IOException
import java.time.LocalDateTime

/**
 * Karta skani va Kirish/Chiqish tanlovini Supabase'ga yuboradigan markaziy joy.
 * Internet yo'q bo'lsa, tegishli navbat jadvaliga yozib, keyinroq SyncWorker orqali yuboradi.
 */
object AttendanceProcessor {

    /** Karta hali hech kimga bog'lanmagan bo'lsa chaqiriladi (pairing uchun). */
    suspend fun handleUnpairedCard(context: Context, cardHash: String): String {
        val tappedAt = LocalDateTime.now()
        return try {
            SupabaseClient.insertNfcScan(cardHash)
            "✅ Karta topilmadi (ulanmagan). Skan yuborildi, Telegram ilovada ro'yxatdan o'ting."
        } catch (e: IOException) {
            PendingScanDao(context).insert(cardHash, tappedAt)
            SyncScheduler.scheduleSync(context)
            "📥 Internet yo'q. Navbatga qo'yildi, internet qaytganda avtomatik yuboriladi."
        } catch (e: Exception) {
            "❌ Xato: ${e.message}"
        }
    }

    /** Foydalanuvchi Kirish yoki Chiqishni TANLAGANDAN keyin chaqiriladi. */
    suspend fun confirmAttendance(
        context: Context,
        profileId: Long,
        workDate: String,
        checkIn: String?,
        checkOut: String?
    ): String {
        return try {
            SupabaseClient.upsertAttendance(profileId, workDate, checkIn, checkOut)
            if (checkIn != null) "✅ Kirish qayd etildi: $checkIn" else "✅ Chiqish qayd etildi: $checkOut"
        } catch (e: IOException) {
            PendingAttendanceDao(context).insert(profileId, workDate, checkIn, checkOut)
            SyncScheduler.scheduleSync(context)
            "📥 Internet yo'q. Tanlovingiz navbatga qo'yildi, internet qaytganda avtomatik yuboriladi."
        } catch (e: Exception) {
            "❌ Xato: ${e.message}"
        }
    }
}
