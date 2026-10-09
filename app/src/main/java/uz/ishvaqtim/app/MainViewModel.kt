package uz.ishvaqtim.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uz.ishvaqtim.app.data.ManualCardStore
import uz.ishvaqtim.app.data.ProfileDao
import uz.ishvaqtim.app.data.ProfileEntity
import uz.ishvaqtim.app.nfc.CardHasher
import uz.ishvaqtim.app.remote.SupabaseClient
import uz.ishvaqtim.app.sync.AttendanceProcessor
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime

/** Karta ro'yxatdan o'tgan bo'lsa, foydalanuvchi Kirish/Chiqishni tanlashi uchun kerakli ma'lumot. */
data class PendingChoice(
    val profileId: Long,
    val workDate: String,
    val existingCheckIn: String?,
    val existingCheckOut: String?
)

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

    /** MainActivity NFC modulini tekshirib, shu yerga yozadi. Telefonda NFC yo'q bo'lsa - false. */
    var hasNfc by mutableStateOf(true)

    /** NFC yo'q telefonlar uchun: foydalanuvchi o'zi kiritgan "tabel raqam" (bir marta kiritiladi, keyin eslab qolinadi). */
    var manualCardId by mutableStateOf<String?>(null)
        private set

    /** Supabase bilan bog'liq oxirgi amal natijasi (foydalanuvchiga ko'rsatiladi). */
    var supabaseStatus by mutableStateOf("")

    /** Karta ro'yxatdan o'tgan bo'lsa, shu to'ldiriladi - ekranda tanlov oynasi chiqishiga sabab bo'ladi. */
    var pendingChoice by mutableStateOf<PendingChoice?>(null)
        private set

    init {
        viewModelScope.launch {
            profile = dao.get()
            loading = false
        }
        manualCardId = ManualCardStore.get(app)
    }

    /** Haqiqiy NFC karta tekkizilganda chaqiriladi. */
    fun onCardScanned(uid: ByteArray) {
        processIdentifier(CardHasher.hash(uid))
    }

    /**
     * NFC yo'q telefonda: foydalanuvchi birinchi marta tabel raqamini kiritganda chaqiriladi.
     * Shu raqam saqlanadi va shu zahoti "tekkizilgandek" ishlov beriladi.
     */
    fun submitManualCardId(rawId: String) {
        val trimmed = rawId.trim()
        if (trimmed.isEmpty()) return

        ManualCardStore.set(getApplication(), trimmed)
        manualCardId = trimmed
        processIdentifier(CardHasher.hash(trimmed.toByteArray()))
    }

    /** NFC yo'q telefonda: kundalik "Belgilash" tugmasi bosilganda (tabel raqam allaqachon saqlangan). */
    fun triggerManualScan() {
        val id = manualCardId ?: return
        processIdentifier(CardHasher.hash(id.toByteArray()))
    }

    /**
     * Markaziy mantiq: karta (yoki tabel raqam) kodi bo'yicha profilni tekshiradi.
     * Endi vaqt oynasi tekshirilmaydi - istalgan vaqtda qabul qilinadi.
     * Karta ro'yxatdan o'tgan bo'lsa, Kirish/Chiqish tanlovi uchun oyna chiqadi ("pendingChoice").
     */
    private fun processIdentifier(hash: String) {
        scannedCardHash = hash
        supabaseStatus = "Tekshirilmoqda..."

        viewModelScope.launch {
            try {
                val remoteProfile = SupabaseClient.findProfileByCardHash(hash)

                if (remoteProfile == null) {
                    supabaseStatus = AttendanceProcessor.handleUnpairedCard(getApplication(), hash)
                    return@launch
                }

                val profileId = remoteProfile.getLong("id")
                val today = LocalDate.now().toString()
                val existing = SupabaseClient.findAttendance(profileId, today)

                pendingChoice = PendingChoice(
                    profileId = profileId,
                    workDate = today,
                    existingCheckIn = existing?.optString("check_in", null),
                    existingCheckOut = existing?.optString("check_out", null)
                )
                supabaseStatus = "Kirish yoki Chiqishni tanlang"
            } catch (e: IOException) {
                supabaseStatus = "⚠️ Internet yo'q. Internet qaytganda qayta urinib ko'ring."
            } catch (e: Exception) {
                supabaseStatus = "❌ Xato: ${e.message}"
            }
        }
    }

    /** Vaqtni eng yaqin SOATGA yaxlitlaydi: daqiqa 30 dan kam bo'lsa pastga, aks holda yuqoriga. */
    private fun roundToNearestHour(time: LocalTime): LocalTime {
        val hour = if (time.minute >= 30) (time.hour + 1) % 24 else time.hour
        return LocalTime.of(hour, 0)
    }

    /** "✅ Kirish" tugmasi bosilganda. */
    fun confirmEntry() {
        val choice = pendingChoice ?: return
        pendingChoice = null
        val time = roundToNearestHour(LocalTime.now()).toString()

        viewModelScope.launch {
            supabaseStatus = AttendanceProcessor.confirmAttendance(
                getApplication(), choice.profileId, choice.workDate, time, null
            )
        }
    }

    /** "🚪 Chiqish" tugmasi bosilganda. */
    fun confirmExit() {
        val choice = pendingChoice ?: return
        pendingChoice = null
        val time = roundToNearestHour(LocalTime.now()).toString()

        viewModelScope.launch {
            supabaseStatus = AttendanceProcessor.confirmAttendance(
                getApplication(), choice.profileId, choice.workDate, null, time
            )
        }
    }

    /** "Bekor qilish" tugmasi bosilganda - hech narsa yuborilmaydi. */
    fun cancelChoice() {
        pendingChoice = null
        supabaseStatus = "Bekor qilindi"
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
