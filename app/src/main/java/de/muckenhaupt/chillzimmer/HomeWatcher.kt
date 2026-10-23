package de.muckenhaupt.chillzimmer

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Fire OS lässt keinen fremden Standard-Launcher zu. Dieser Dienst fängt
 * die HOME-Taste direkt ab und öffnet die Chillzimmer-Startseite.
 * Zusätzlich (Fallback): Erscheint trotzdem die Amazon-Startseite,
 * wird Chillzimmer nach vorn geholt.
 */
class HomeWatcher : AccessibilityService() {

    companion object {
        const val TAG = "ChillzimmerHome"
        const val AMAZON_LAUNCHER = "com.amazon.tv.launcher"
        @Volatile var allowAmazonHome = false
    }

    private var lastJump = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "chillzimmer: Dienst verbunden")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_HOME) return false
        if (event.action == KeyEvent.ACTION_UP) {
            Log.i(TAG, "chillzimmer: HOME-Taste abgefangen")
            allowAmazonHome = false
            jump(force = true)
        }
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (event.packageName?.toString() != AMAZON_LAUNCHER || allowAmazonHome) return
        Log.i(TAG, "chillzimmer: Amazon-Startseite erkannt")
        jump(force = false)
    }

    private fun jump(force: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastJump < 800) return
        lastJump = now
        try {
            startActivity(
                Intent(this, MainActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "chillzimmer: Start fehlgeschlagen", e)
        }
    }

    override fun onInterrupt() {}
}
