package uz.ishvaqtim.app.data

import android.content.ContentValues
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/** "pending_scans" jadvali bilan ishlaydi: qo'shish, ro'yxatini olish, o'chirish. */
class PendingScanDao(context: Context) {

    private val database = AppDatabase.getInstance(context)

    suspend fun insert(cardHash: String, tappedAt: LocalDateTime) {
        withContext(Dispatchers.IO) {
            val values = ContentValues()
            values.put("cardHash", cardHash)
            values.put("tappedAt", tappedAt.toString())
            values.put("createdAt", LocalDateTime.now().toString())
            database.writableDatabase.insert("pending_scans", null, values)
        }
    }

    suspend fun listAll(): List<PendingScanEntity> = withContext(Dispatchers.IO) {
        val result = mutableListOf<PendingScanEntity>()
        database.readableDatabase
            .query("pending_scans", null, null, null, null, null, "id ASC")
            .use { c ->
                while (c.moveToNext()) {
                    result.add(
                        PendingScanEntity(
                            id = c.getLong(c.getColumnIndexOrThrow("id")),
                            cardHash = c.getString(c.getColumnIndexOrThrow("cardHash")),
                            tappedAt = c.getString(c.getColumnIndexOrThrow("tappedAt")),
                            createdAt = c.getString(c.getColumnIndexOrThrow("createdAt"))
                        )
                    )
                }
            }
        result
    }

    suspend fun delete(id: Long) {
        withContext(Dispatchers.IO) {
            database.writableDatabase.delete("pending_scans", "id = ?", arrayOf(id.toString()))
        }
    }

    suspend fun count(): Int = withContext(Dispatchers.IO) {
        database.readableDatabase.query("pending_scans", null, null, null, null, null, null)
            .use { it.count }
    }
}
