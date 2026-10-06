package uz.ishvaqtim.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uz.ishvaqtim.app.MainViewModel
import uz.ishvaqtim.app.data.ProfileEntity
import uz.ishvaqtim.app.domain.SalaryCalculator
import uz.ishvaqtim.app.domain.ScheduleType
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

@Composable
fun HomeScreen(profile: ProfileEntity, vm: MainViewModel) {
    val type = ScheduleType.fromLabel(profile.scheduleType) ?: ScheduleType.TWO_TWO
    val scheduleStart = LocalDate.parse(profile.scheduleStart)
    val workStart = LocalTime.parse(profile.workStart)
    val workEnd = LocalTime.parse(profile.workEnd)
    val month = YearMonth.now()

    val workDays = SalaryCalculator.scheduledDaysInMonth(month, type, scheduleStart)
    val plannedHours = SalaryCalculator.plannedHoursInMonth(
        month, type, scheduleStart, workStart, workEnd, profile.breakMinutes
    )
    val rate = SalaryCalculator.hourlyRate(profile.monthlySalary, plannedHours)

    val scanned = vm.scannedCardHash
    val cardText = when {
        scanned == null -> "Tekshirish uchun kartani tekkizing"
        scanned == profile.cardHash -> "✅ Bu sizning kartangiz"
        else -> "❌ Bu boshqa karta"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "${profile.firstName} ${profile.lastName}",
            style = MaterialTheme.typography.headlineMedium
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Profil", style = MaterialTheme.typography.titleMedium)
                Text("Grafik: ${profile.scheduleType} (${profile.scheduleStart} dan)")
                Text("Ish vaqti: ${profile.workStart} - ${profile.workEnd}")
                Text("Tushlik: ${profile.breakMinutes} daqiqa")
                Text("Oylik maosh: ${formatMoney(profile.monthlySalary)}")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Shu oy: $month", style = MaterialTheme.typography.titleMedium)
                Text("Rejalashtirilgan ish kunlari: $workDays")
                Text("Rejalashtirilgan soat: ${formatHours(plannedHours)}")
                Text("Soatlik stavka: ${formatMoney(rate)}")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("NFC karta", style = MaterialTheme.typography.titleMedium)
                Text(vm.nfcStatus)
                Text(cardText)
                if (vm.scannedCardHash != null) {
                    Text(vm.supabaseStatus)
                }
            }
        }

        OutlinedButton(
            onClick = { vm.deleteProfile() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Profilni o'chirish (test uchun)")
        }
    }
}
