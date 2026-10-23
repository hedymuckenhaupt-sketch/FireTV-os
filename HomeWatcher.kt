package de.muckenhaupt.chillzimmer

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

/**
 * Fire OS lässt keinen fremden Standard-Launcher zu. Dieser Dienst merkt,
 * wenn die Amazon-Startseite erscheint (z. B. nach der Home-Taste), und
 * holt stattdessen die Chillzimmer-Startseite nach vorn.
 */
class HomeWatcher : AccessibilityService() {

    companion object {
        const val AMAZON_LAUNCHER = "com.amazon.tv.launcher"
        @Volatile var allowAmazonHome = false
    }

    private var lastJump = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (event.packageName?.toString() != AMAZON_LAUNCHER || allowAmazonHome) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastJump < 800) return
        lastJump = now

        startActivity(
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        )
    }

    override fun onInterrupt() {}
}
