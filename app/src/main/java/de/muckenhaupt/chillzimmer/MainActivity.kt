package de.muckenhaupt.chillzimmer

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Zeigt die Startseite (assets/index.html) im Vollbild und reicht die
 * Tasten der Fire-TV-Fernbedienung an die Oberfläche weiter.
 */
class MainActivity : Activity() {

    lateinit var web: WebView
    private lateinit var bridge: Bridge

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.setBackgroundColor(Color.parseColor("#17162B"))
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.isFocusable = true
        web.webViewClient = WebViewClient()
        bridge = Bridge(this)
        web.addJavascriptInterface(bridge, "Launcher")
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
        web.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        // Zurück im Launcher: Home-Taste wieder auf uns umleiten
        HomeWatcher.allowAmazonHome = false
        bridge.rescanIfReady()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val key = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> "up"
            KeyEvent.KEYCODE_DPAD_DOWN -> "down"
            KeyEvent.KEYCODE_DPAD_LEFT -> "left"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "right"
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> "ok"
            KeyEvent.KEYCODE_MENU -> "menu"
            KeyEvent.KEYCODE_BACK -> "back"
            else -> null
        } ?: return super.dispatchKeyEvent(event)

        if (event.action == KeyEvent.ACTION_DOWN) {
            web.evaluateJavascript("window.onRemote && window.onRemote('$key')", null)
        }
        return true
    }

    fun pushApps(json: String) = runOnUiThread {
        web.evaluateJavascript("window.onApps && window.onApps($json)", null)
    }
}
