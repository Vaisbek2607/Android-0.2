package uz.ishvaqtim.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Lokal SQLite bazasi (Android ning o'rnatilgan SQLite si). Jadvallar shu yerda yaratiladi. */
class AppDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, "ish_vaqtim.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE profile (" +
                "id INTEGER PRIMARY KEY, " +
                "firstName TEXT NOT NULL, " +
                "lastName TEXT NOT NULL, " +
                "scheduleType TEXT NOT NULL, " +
                "scheduleStart TEXT NOT NULL, " +
                "workStart TEXT NOT NULL, " +
                "workEnd TEXT NOT NULL, " +
                "breakMinutes INTEGER NOT NULL, " +
                "monthlySalary REAL NOT NULL, " +
                "cardHash TEXT NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Hozircha o'zgartirishlar yo'q. Keyingi bosqichlarda jadvallar shu yerda yangilanadi.
    }

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: AppDatabase(context.applicationContext).also { instance = it }
            }
    }
}
