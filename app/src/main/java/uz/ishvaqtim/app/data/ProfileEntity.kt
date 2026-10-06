package uz.ishvaqtim.app.data

/** Foydalanuvchi profili. Bazada faqat bitta qator (id = 1) bo'ladi. */
data class ProfileEntity(
    val id: Int = 1,
    val firstName: String,
    val lastName: String,
    val scheduleType: String,   // "2/2", "5/2", "6/1"
    val scheduleStart: String,  // "2026-10-01"
    val workStart: String,      // "08:00"
    val workEnd: String,        // "20:00"
    val breakMinutes: Int,
    val monthlySalary: Double,
    val cardHash: String        // karta UID ning SHA-256 xeshi (UID ning o'zi saqlanmaydi)
)
