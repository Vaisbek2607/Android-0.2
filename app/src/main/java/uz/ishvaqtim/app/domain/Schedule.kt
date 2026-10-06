package uz.ishvaqtim.app.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Ish grafigi turlari.
 * workDays  - siklda nechta kun ishlanadi
 * cycleDays - sikl necha kundan iborat
 * (web: 2/2 -> farq % 4 < 2, 5/2 -> farq % 7 < 5, 6/1 -> farq % 7 < 6)
 */
enum class ScheduleType(
    val label: String,
    private val workDays: Int,
    private val cycleDays: Int
) {
    TWO_TWO("2/2", 2, 4),
    FIVE_TWO("5/2", 5, 7),
    SIX_ONE("6/1", 6, 7);

    /** [date] rejalashtirilgan ish kunimi? Boshlanish sanasidan oldingi kunlar ish kuni emas. */
    fun isWorkDay(startDate: LocalDate, date: LocalDate): Boolean {
        val diff = ChronoUnit.DAYS.between(startDate, date)
        if (diff < 0) return false
        return diff % cycleDays < workDays
    }

    companion object {
        fun fromLabel(label: String): ScheduleType? =
            entries.firstOrNull { it.label == label }
    }
}
