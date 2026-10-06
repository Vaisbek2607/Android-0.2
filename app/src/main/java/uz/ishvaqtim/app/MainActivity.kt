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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import uz.ishvaqtim.app.ui.HomeScreen
import uz.ishvaqtim.app.ui.RegisterScreen

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            MaterialTheme {
                Scaffold { inner ->
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
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val adapter = nfcAdapter

        vm.nfcStatus = when {
            adapter == null -> "Bu telefonda NFC yo'q"
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
