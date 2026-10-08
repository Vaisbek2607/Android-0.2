package uz.ishvaqtim.app.data

/** Internet yo'qligi sababli hali Supabase'ga yuborilmagan karta skani. */
data class PendingScanEntity(
    val id: Long,
    val cardHash: String,
    val tappedAt: String,   // LocalDateTime.toString() ko'rinishida, masalan "2026-10-08T08:03:12"
    val createdAt: String
)
