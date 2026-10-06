package uz.ishvaqtim.app.domain

import java.time.LocalTime

/** NFC skan natijasida nima qilish kerakligi. */
sealed interface NfcDecision {
    data class RecordEntry(val time: LocalTime) : NfcDecision
    data class RecordExit(val time: LocalTime) : NfcDecision
    data object AlreadyEntered : NfcDecision
    data object AlreadyExited : NfcDecision
    data object EntryRequired : NfcDecision
    data object OutsideWindow : NfcDecision
}

/**
 * NFC vaqt qoidalari:
 *  07:30-08:30 -> Kirish 08:00
 *  18:00-19:29 -> Chiqish 19:00
 *  19:30 va keyin -> Chiqish 20:00
 *  boshqa vaqt -> OutsideWindow
 */
object NfcRules {

    private val ENTRY_TIME = LocalTime.of(8, 0)
    private val EXIT_TIME_NORMAL = LocalTime.of(19, 0)
    private val EXIT_TIME_LATE = LocalTime.of(20, 0)

    private const val ENTRY_FROM = 7 * 60 + 30   // 07:30
    private const val ENTRY_TO = 8 * 60 + 30     // 08:30 (shu daqiqa ham kiradi)
    private const val EXIT_FROM = 18 * 60        // 18:00
    private const val EXIT_LATE_FROM = 19 * 60 + 30 // 19:30

    fun decide(now: LocalTime, todayEntry: LocalTime?, todayExit: LocalTime?): NfcDecision {
        val minutes = now.hour * 60 + now.minute
        return when {
            minutes in ENTRY_FROM..ENTRY_TO ->
                if (todayEntry != null) NfcDecision.AlreadyEntered
                else NfcDecision.RecordEntry(ENTRY_TIME)

            minutes in EXIT_FROM until EXIT_LATE_FROM ->
                exitDecision(todayEntry, todayExit, EXIT_TIME_NORMAL)

            minutes >= EXIT_LATE_FROM ->
                exitDecision(todayEntry, todayExit, EXIT_TIME_LATE)

            else -> NfcDecision.OutsideWindow
        }
    }

    private fun exitDecision(
        todayEntry: LocalTime?,
        todayExit: LocalTime?,
        exitTime: LocalTime
    ): NfcDecision = when {
        todayExit != null -> NfcDecision.AlreadyExited
        todayEntry == null -> NfcDecision.EntryRequired
        else -> NfcDecision.RecordExit(exitTime)
    }
}
