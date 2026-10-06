package uz.ishvaqtim.app.ui

import java.util.Locale

/** 5400000.0 -> "5 400 000" */
fun formatMoney(value: Double): String =
    String.format(Locale.US, "%,.0f", value).replace(',', ' ')

/** 176.0 -> "176.0" (nuqta bilan, telefon tilidan qat'i nazar) */
fun formatHours(value: Double): String =
    String.format(Locale.US, "%.1f", value)
