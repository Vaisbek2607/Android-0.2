package uz.ishvaqtim.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uz.ishvaqtim.app.data.ProfileDao
import uz.ishvaqtim.app.data.ProfileEntity
import uz.ishvaqtim.app.domain.NfcDecision
import uz.ishvaqtim.app.domain.NfcRules
import uz.ishvaqtim.app.nfc.CardHasher
import uz.ishvaqtim.app.remote.SupabaseClient
import java.time.LocalDate
import java.time.LocalTime

/** Ekran holatini saqlaydi: profil, oxirgi skan qilingan karta, NFC holati. */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = ProfileDao(app)

    var loading by mutableStateOf(true)
        private set

    var profile by mutableStateOf<ProfileEntity?>(null)
        private set

    var scannedCardHash by mutableStateOf<String?>(null)
        private set

    var nfcStatus by mutableStateOf("")

    /** Supabase bilan bog'liq oxirgi amal natijasi (foydalanuvchiga ko'rsatiladi). */
    var supabaseStatus by mutableStateOf("")

    init {
        viewModelScope.launch {
            profile = dao.get()
            loading = false
        }
    }

    /**
     * Karta tekkizilganda chaqiriladi.
     * 1) Avval bu karta Supabase'da biror profilga bog'langanmi, tekshiradi.
     * 2) Bog'langan bo'lsa - NFC vaqt qoidalari bo'yicha Kirish/Chiqish yozadi.
     * 3) Bog'lanmagan bo'lsa - "nfc_scans" ga yozadi (Mini App'da ro'yxatdan o'tish uchun).
     */
    fun onCardScanned(uid: ByteArray) {
        val hash = CardHasher.hash(uid)
        scannedCardHash = hash
        supabaseStatus = "Tekshirilmoqda..."

        viewModelScope.launch {
            val remoteProfile = SupabaseClient.findProfileByCardHash(hash)

            if (remoteProfile == null) {
                // Karta hali hech kimga bog'lanmagan - pairing uchun yozamiz
                val ok = SupabaseClient.insertNfcScan(hash)
                supabaseStatus = if (ok) {
                    "✅ Karta topilmadi (ulanmagan). Skan yuborildi, Telegram ilovada ro'yxatdan o'ting."
                } else {
                    "⚠️ Yuborilmadi (internet yoki server xatosi)"
                }
                return@launch
            }

            val profileId = remoteProfile.getLong("id")
            val today = LocalDate.now().toString()

            val todayAttendance = SupabaseClient.findAttendance(profileId, today)
            val existingCheckIn = todayAttendance
                ?.optString("check_in", null)
                ?.let { LocalTime.parse(it) }
            val existingCheckOut = todayAttendance
                ?.optString("check_out", null)
                ?.let { LocalTime.parse(it) }

            val decision = NfcRules.decide(LocalTime.now(), existingCheckIn, existingCheckOut)

            supabaseStatus = when (decision) {
                is NfcDecision.RecordEntry -> {
                    val ok = SupabaseClient.upsertAttendance(profileId, today, decision.time.toString(), null)
                    if (ok) "✅ Kirish qayd etildi: ${decision.time}" else "⚠️ Yozib bo'lmadi (internet xatosi)"
                }

                is NfcDecision.RecordExit -> {
                    val ok = SupabaseClient.upsertAttendance(profileId, today, null, decision.time.toString())
                    if (ok) "✅ Chiqish qayd etildi: ${decision.time}" else "⚠️ Yozib bo'lmadi (internet xatosi)"
                }

                NfcDecision.AlreadyEntered -> "ℹ️ Kirish allaqachon qayd etilgan"
                NfcDecision.AlreadyExited -> "ℹ️ Chiqish allaqachon qayd etilgan"
                NfcDecision.EntryRequired -> "⚠️ Avval Kirish qayd etilishi kerak"
                NfcDecision.OutsideWindow -> "⚠️ Bu vaqtda NFC qabul qilinmaydi"
            }
        }
    }

    fun saveProfile(newProfile: ProfileEntity) {
        viewModelScope.launch {
            dao.save(newProfile)
            profile = newProfile
        }
    }

    fun deleteProfile() {
        viewModelScope.launch {
            dao.clear()
            profile = null
        }
    }
}
