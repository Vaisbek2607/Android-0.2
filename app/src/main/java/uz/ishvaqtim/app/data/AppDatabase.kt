package uz.ishvaqtim.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DB_VERSION = 3

/** Lokal SQLite bazasi (Android ning o'rnatilgan SQLite si). Jadvallar shu yerda yaratiladi. */
class AppDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, "ish_vaqtim.db", null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        createProfileTable(db)
        createPendingScansTable(db)
        createPendingAttendanceTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createPendingScansTable(db)
        }
        if (oldVersion < 3) {
            createPendingAttendanceTable(db)
        }
    }

    private fun createProfileTable(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS profile (" +
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

    /** Hali ulanmagan kartaning skani, internet yo'q paytda shu yerda kutadi. */
    private fun createPendingScansTable(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS pending_scans (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "cardHash TEXT NOT NULL, " +
                "tappedAt TEXT NOT NULL, " +
                "createdAt TEXT NOT NULL)"
        )
    }

    /** Foydalanuvchi Kirish/Chiqishni TANLAGANDAN keyin, internet yo'qligi sababli yuborilmay qolgan qaror. */
    private fun createPendingAttendanceTable(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS pending_attendance (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "profileId INTEGER NOT NULL, " +
                "workDate TEXT NOT NULL, " +
                "checkIn TEXT, " +
                "checkOut TEXT, " +
                "createdAt TEXT NOT NULL)"
        )
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
