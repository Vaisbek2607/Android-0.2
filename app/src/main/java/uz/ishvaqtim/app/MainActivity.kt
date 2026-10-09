package uz.ishvaqtim.app

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uz.ishvaqtim.app.sync.SyncScheduler
import uz.ishvaqtim.app.ui.HomeScreen
import uz.ishvaqtim.app.ui.RegisterScreen

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        vm.hasNfc = nfcAdapter != null

        // Ilova ochilganda, agar oldingi safar navbatga qo'yilgan ma'lumotlar bo'lsa,
        // ularni yuborishga urinib ko'ramiz (internet bo'lsa, darhol bajariladi).
        SyncScheduler.scheduleSync(this)

        setContent {
            MaterialTheme {
                Scaffold(
                    bottomBar = {
                        if (!vm.hasNfc) {
                            ManualScanBar(vm)
                        }
                    }
                ) { inner ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(inner)
                    ) {
                        val profile = vm.profile
                        if (vm.loading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (profile == null) {
                            RegisterScreen(vm)
                        } else {
                            HomeScreen(profile, vm)
                        }

                        val choice = vm.pendingChoice
                        if (choice != null) {
                            AlertDialog(
                                onDismissRequest = { vm.cancelChoice() },
                                title = { Text("Karta tekkizildi") },
                                text = {
                                    Column {
                                        Text("Bugungi holat:")
                                        Text("Kirish: ${choice.existingCheckIn ?: "—"}")
                                        Text("Chiqish: ${choice.existingCheckOut ?: "—"}")
                                    }
                                },
                                confirmButton = {
                                    Button(onClick = { vm.confirmEntry() }) {
                                        Text("✅ Kirish")
                                    }
                                },
                                dismissButton = {
                                    Column(horizontalAlignment = Alignment.End) {
                                        OutlinedButton(onClick = { vm.confirmExit() }) {
                                            Text("🚪 Chiqish")
                                        }
                                        TextButton(
                                            onClick = { vm.cancelChoice() },
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Text("Bekor qilish")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val adapter = nfcAdapter

        vm.nfcStatus = when {
            adapter == null -> "Bu telefonda NFC yo'q. Pastdagi tugmadan foydalaning."
            !adapter.isEnabled -> "NFC o'chirilgan. Telefon sozlamalarida yoqing"
            else -> "NFC yoqilgan"
        }
        if (adapter == null || !adapter.isEnabled) return

        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        try {
            adapter.enableForegroundDispatch(this, pendingIntent, null, null)
        } catch (e: Exception) {
            vm.nfcStatus = "NFC xatosi: ${e.message}"
        }
    }

    override fun onPause() {
        try {
            nfcAdapter?.disableForegroundDispatch(this)
        } catch (e: Exception) {
            // e'tibor bermaymiz
        }
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag = extractTag(intent)
        if (tag != null) {
            vm.onCardScanned(tag.id)
        }
    }

    @Suppress("DEPRECATION")
    private fun extractTag(intent: Intent): Tag? =
        if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
}

/**
 * NFC yo'q telefonlar uchun pastki panel.
 * Tabel raqam hali kiritilmagan bo'lsa - kiritish maydoni, aks holda - bitta "Belgilash" tugmasi.
 */
@androidx.compose.runtime.Composable
private fun ManualScanBar(vm: MainViewModel) {
    val savedId = vm.manualCardId

    if (savedId == null) {
        var input by remember { mutableStateOf("") }

        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text("Bu telefonda NFC yo'q. O'zingiz tanlagan tabel raqamingizni kiriting:")
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(
                onClick = { vm.submitManualCardId(input) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("Tasdiqlash")
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Button(
                onClick = { vm.triggerManualScan() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📌 Kirish/Chiqishni belgilash")
            }
        }
    }
}
