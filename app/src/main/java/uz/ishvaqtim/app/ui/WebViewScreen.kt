package uz.ishvaqtim.app.ui

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private const val MINI_APP_URL = "https://ishvaqtim.vercel.app"

/**
 * Tayyor Mini App sahifasini (kalendar, statistika, sozlamalar) ilova ICHIDA ko'rsatadi.
 * Telegram o'rniga, sahifaga "AndroidApp" nomli ko'prik orqali profil ID'si beriladi -
 * shu orqali sahifa Telegram'siz ham o'z profilini topadi.
 */
class WebAppBridge(private val profileId: Long?) {
    @JavascriptInterface
    fun getProfileId(): String = profileId?.toString() ?: "null"
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(profileId: Long) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                addJavascriptInterface(WebAppBridge(profileId), "AndroidApp")
                loadUrl(MINI_APP_URL)
            }
        }
    )
}
