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
import uz.ishvaqtim.app.nfc.CardHasher
import uz.ishvaqtim.app.remote.SupabaseClient
import uz.ishvaqtim.app.sync.AttendanceProcessor
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
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
    }

    /**
     * Karta tekkizilganda chaqiriladi. Endi vaqt oynasi tekshirilmaydi - istalgan vaqtda qabul qilinadi.
     * Karta ro'yxatdan o'tgan bo'lsa, Kirish/Chiqish tanlovi uchun oyna chiqadi ("pendingChoice").
     */
    fun onCardScanned(uid: ByteArray) {
        val hash = CardHasher.hash(uid)
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
                supabaseStatus = "⚠️ Internet yo'q. Internet qaytganda kartani qayta tekkizing."
            } catch (e: Exception) {
                supabaseStatus = "❌ Xato: ${e.message}"
            }
        }
    }

    /** "✅ Kirish" tugmasi bosilganda. */
    fun confirmEntry() {
        val choice = pendingChoice ?: return
        pendingChoice = null
        val time = LocalTime.now().toString()

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
        val time = LocalTime.now().toString()

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
