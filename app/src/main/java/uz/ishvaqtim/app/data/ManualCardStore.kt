package uz.ishvaqtim.app.data

import android.content.Context

/**
 * NFC yo'q telefonlar uchun: foydalanuvchi o'zi kiritgan "tabel raqam"ni
 * shu yerda (SharedPreferences, kichik lokal sozlama) saqlaymiz.
 * Shu bilan har safar qayta yozish shart bo'lmaydi - faqat bir marta kiritiladi.
 */
object ManualCardStore {

    private const val PREFS_NAME = "ish_vaqtim_prefs"
    private const val KEY_MANUAL_CARD_ID = "manual_card_id"

    fun get(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_MANUAL_CARD_ID, null)
    }

    fun set(context: Context, value: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MANUAL_CARD_ID, value).apply()
    }
}
