package uz.ishvaqtim.app.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/** Soat va maosh hisoblari. Web app.js dagi formulalar bilan bir xil. */
object SalaryCalculator {

    /** Ishlangan soat = (chiqish - kirish - tanaffus). Tungi smena (chiqish < kirish) ham ishlaydi. */
    fun workedHours(start: LocalTime?, end: LocalTime?, breakMinutes: Int): Double {
        if (start == null || end == null) return 0.0
        val startTotal = start.hour * 60 + start.minute
        var endTotal = end.hour * 60 + end.minute
        if (endTotal < startTotal) endTotal += 24 * 60
        val minutes = (endTotal - startTotal - breakMinutes).coerceAtLeast(0)
        return minutes / 60.0
    }

    /** Oydagi rejalashtirilgan ish kunlari soni. */
    fun scheduledDaysInMonth(
        month: YearMonth,
        type: ScheduleType,
        scheduleStart: LocalDate
    ): Int = (1..month.lengthOfMonth()).count { day ->
        type.isWorkDay(scheduleStart, month.atDay(day))
    }

    /** Rejalashtirilgan oylik soatlar = ish kunlari x bir kunlik soat. */
    fun plannedHoursInMonth(
        month: YearMonth,
        type: ScheduleType,
        scheduleStart: LocalDate,
        workStart: LocalTime,
        workEnd: LocalTime,
        breakMinutes: Int
    ): Double {
        val days = scheduledDaysInMonth(month, type, scheduleStart)
        val daily = workedHours(workStart, workEnd, breakMinutes)
        return days * daily
    }

    /** Soatlik stavka = oylik maosh / rejalashtirilgan soatlar. Soat 0 bo'lsa, 0 qaytadi. */
    fun hourlyRate(monthlySalary: Double, plannedHours: Double): Double {
        if (monthlySalary <= 0.0 || plannedHours <= 0.0) return 0.0
        return monthlySalary / plannedHours
    }

    /** Ishlangan pul. Qo'shimcha ish ham oddiy stavkada (koeffitsientsiz). */
    fun pay(hours: Double, hourlyRate: Double): Double = hours * hourlyRate
}
