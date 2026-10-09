package uz.ishvaqtim.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uz.ishvaqtim.app.MainViewModel
import uz.ishvaqtim.app.data.ProfileEntity
import uz.ishvaqtim.app.domain.ScheduleType
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(vm: MainViewModel) {
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var scheduleLabel by rememberSaveable { mutableStateOf(ScheduleType.TWO_TWO.label) }
    var startDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var workStart by rememberSaveable { mutableStateOf("08:00") }
    var workEnd by rememberSaveable { mutableStateOf("20:00") }
    var breakText by rememberSaveable { mutableStateOf("60") }
    var salaryText by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Ro'yxatdan o'tish", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("Ism") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Familiya") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Ish grafigi", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScheduleType.entries.forEach { type ->
                FilterChip(
                    selected = scheduleLabel == type.label,
                    onClick = { scheduleLabel = type.label },
                    label = { Text(type.label) }
                )
            }
        }

        OutlinedTextField(
            value = startDate,
            onValueChange = { startDate = it },
            label = { Text("Grafik boshlanish sanasi (2026-10-01)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = workStart,
                onValueChange = { workStart = it },
                label = { Text("Boshlanish") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = workEnd,
                onValueChange = { workEnd = it },
                label = { Text("Tugash") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = breakText,
            onValueChange = { breakText = it },
            label = { Text("Tushlik tanaffusi (daqiqa)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = salaryText,
            onValueChange = { salaryText = it },
            label = { Text("Oylik maosh") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("NFC karta", style = MaterialTheme.typography.titleMedium)
                Text(vm.nfcStatus)
                Text(
                    when {
                        vm.scannedCardHash != null -> "✅ Karta o'qildi"
                        vm.hasNfc -> "Ish kartangizni telefonning orqa tomoniga tekkizing"
                        else -> "Bu telefonda NFC yo'q. Pastdagi maydonga tabel raqamingizni kiriting va Tasdiqlang"
                    }
                )
                if (vm.scannedCardHash != null) {
                    Text(vm.supabaseStatus)
                }
            }
        }

        val message = error
        if (message != null) {
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                val date = runCatching { LocalDate.parse(startDate.trim()) }.getOrNull()
                val start = runCatching { LocalTime.parse(workStart.trim()) }.getOrNull()
                val end = runCatching { LocalTime.parse(workEnd.trim()) }.getOrNull()
                val breakMin = breakText.trim().toIntOrNull()
                val salary = salaryText.trim().toDoubleOrNull()
                val cardHash = vm.scannedCardHash

                val problem = when {
                    firstName.isBlank() || lastName.isBlank() -> "Ism va familiyani kiriting"
                    date == null -> "Sana noto'g'ri. Format: 2026-10-01"
                    start == null || end == null -> "Vaqt noto'g'ri. Format: 08:00"
                    breakMin == null || breakMin < 0 -> "Tanaffus daqiqada bo'lsin (masalan 60)"
                    salary == null || salary <= 0.0 -> "Oylik maoshni kiriting"
                    cardHash == null -> "Avval kartani telefonga tekkizing"
                    else -> null
                }

                error = problem
                if (problem == null) {
                    // problem == null bo'lsa, yuqoridagi hamma qiymatlar to'g'ri ekani tekshirilgan
                    vm.saveProfile(
                        ProfileEntity(
                            firstName = firstName.trim(),
                            lastName = lastName.trim(),
                            scheduleType = scheduleLabel,
                            scheduleStart = date!!.toString(),
                            workStart = start!!.toString(),
                            workEnd = end!!.toString(),
                            breakMinutes = breakMin!!,
                            monthlySalary = salary!!,
                            cardHash = cardHash!!
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Saqlash")
        }
    }
}
