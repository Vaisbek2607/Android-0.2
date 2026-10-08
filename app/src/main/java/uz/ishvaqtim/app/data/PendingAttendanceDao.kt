package uz.ishvaqtim.app.data

import android.content.ContentValues
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class PendingAttendanceDao(context: Context) {

    private val database = AppDatabase.getInstance(context)

    suspend fun insert(profileId: Long, workDate: String, checkIn: String?, checkOut: String?) {
        withContext(Dispatchers.IO) {
            val values = ContentValues()
            values.put("profileId", profileId)
            values.put("workDate", workDate)
            values.put("checkIn", checkIn)
            values.put("checkOut", checkOut)
            values.put("createdAt", LocalDateTime.now().toString())
            database.writableDatabase.insert("pending_attendance", null, values)
        }
    }

    suspend fun listAll(): List<PendingAttendanceEntity> = withContext(Dispatchers.IO) {
        val result = mutableListOf<PendingAttendanceEntity>()
        database.readableDatabase
            .query("pending_attendance", null, null, null, null, null, "id ASC")
            .use { c ->
                while (c.moveToNext()) {
                    result.add(
                        PendingAttendanceEntity(
                            id = c.getLong(c.getColumnIndexOrThrow("id")),
                            profileId = c.getLong(c.getColumnIndexOrThrow("profileId")),
                            workDate = c.getString(c.getColumnIndexOrThrow("workDate")),
                            checkIn = c.getStringOrNull("checkIn"),
                            checkOut = c.getStringOrNull("checkOut")
                        )
                    )
                }
            }
        result
    }

    suspend fun delete(id: Long) {
        withContext(Dispatchers.IO) {
            database.writableDatabase.delete("pending_attendance", "id = ?", arrayOf(id.toString()))
        }
    }

    private fun android.database.Cursor.getStringOrNull(column: String): String? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getString(index)
    }
}
