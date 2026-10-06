package uz.ishvaqtim.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Profil jadvali bilan ishlaydi: o'qish, saqlash, o'chirish. */
class ProfileDao(context: Context) {

    private val database = AppDatabase.getInstance(context)

    suspend fun get(): ProfileEntity? = withContext(Dispatchers.IO) {
        database.readableDatabase
            .query("profile", null, "id = 1", null, null, null, null)
            .use { c ->
                if (c.moveToFirst()) {
                    ProfileEntity(
                        id = c.getInt(c.getColumnIndexOrThrow("id")),
                        firstName = c.getString(c.getColumnIndexOrThrow("firstName")),
                        lastName = c.getString(c.getColumnIndexOrThrow("lastName")),
                        scheduleType = c.getString(c.getColumnIndexOrThrow("scheduleType")),
                        scheduleStart = c.getString(c.getColumnIndexOrThrow("scheduleStart")),
                        workStart = c.getString(c.getColumnIndexOrThrow("workStart")),
                        workEnd = c.getString(c.getColumnIndexOrThrow("workEnd")),
                        breakMinutes = c.getInt(c.getColumnIndexOrThrow("breakMinutes")),
                        monthlySalary = c.getDouble(c.getColumnIndexOrThrow("monthlySalary")),
                        cardHash = c.getString(c.getColumnIndexOrThrow("cardHash"))
                    )
                } else {
                    null
                }
            }
    }

    suspend fun save(profile: ProfileEntity) {
        withContext(Dispatchers.IO) {
            val values = ContentValues()
            values.put("id", profile.id)
            values.put("firstName", profile.firstName)
            values.put("lastName", profile.lastName)
            values.put("scheduleType", profile.scheduleType)
            values.put("scheduleStart", profile.scheduleStart)
            values.put("workStart", profile.workStart)
            values.put("workEnd", profile.workEnd)
            values.put("breakMinutes", profile.breakMinutes)
            values.put("monthlySalary", profile.monthlySalary)
            values.put("cardHash", profile.cardHash)
            database.writableDatabase.insertWithOnConflict(
                "profile", null, values, SQLiteDatabase.CONFLICT_REPLACE
            )
        }
    }

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            database.writableDatabase.delete("profile", null, null)
        }
    }
}
