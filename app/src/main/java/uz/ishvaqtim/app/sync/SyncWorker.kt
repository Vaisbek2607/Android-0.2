package uz.ishvaqtim.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import uz.ishvaqtim.app.data.PendingAttendanceDao
import uz.ishvaqtim.app.data.PendingScanDao
import uz.ishvaqtim.app.remote.SupabaseClient
import java.io.IOException

/**
 * WorkManager shu ishni "internet mavjud bo'lganda" ishga tushiradi.
 * Ikkala navbatni ham (ulanmagan skanlar va tanlangan Kirish/Chiqish) qayta ishlaydi.
 */
class SyncWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val scanDao = PendingScanDao(applicationContext)
        val attendanceDao = PendingAttendanceDao(applicationContext)

        var allSucceeded = true

        for (scan in scanDao.listAll()) {
            try {
                SupabaseClient.insertNfcScan(scan.cardHash)
                scanDao.delete(scan.id)
            } catch (e: IOException) {
                allSucceeded = false
            } catch (e: Exception) {
                scanDao.delete(scan.id) // doimiy xato - qayta urinish foyda bermaydi
            }
        }

        for (item in attendanceDao.listAll()) {
            try {
                SupabaseClient.upsertAttendance(item.profileId, item.workDate, item.checkIn, item.checkOut)
                attendanceDao.delete(item.id)
            } catch (e: IOException) {
                allSucceeded = false
            } catch (e: Exception) {
                attendanceDao.delete(item.id)
            }
        }

        return if (allSucceeded) Result.success() else Result.retry()
    }
}
