package uz.ishvaqtim.app.data

/** Foydalanuvchi Kirish/Chiqishni allaqachon tanlagan, lekin internet yo'qligi sababli yuborilmagan yozuv. */
data class PendingAttendanceEntity(
    val id: Long,
    val profileId: Long,
    val workDate: String,
    val checkIn: String?,
    val checkOut: String?
)
