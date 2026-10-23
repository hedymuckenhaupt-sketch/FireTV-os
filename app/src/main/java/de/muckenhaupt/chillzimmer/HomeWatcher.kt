package de.muckenhaupt.chillzimmer

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.ImageView

/**
 * Fire OS lässt keinen fremden Standard-Launcher zu und gibt die HOME-Taste
 * nicht an Bedienungshilfen weiter. Nach HOME sperrt Android fremde
 * App-Starts außerdem für 5 Sekunden.
 *
 * Lösung, sobald die Amazon-Startseite erscheint:
 *  1. Sofort-Abdeckung: ein Vollbild mit dem Chillzimmer-Hintergrund
 *     verdeckt die Amazon-Seite.
 *  2. Klick-Trick: Der Dienst klickt auf der Amazon-Startseite die
 *     Chillzimmer-Kachel an. Den Start führt dann die Amazon-Startseite
 *     selbst aus – dafür gilt die 5-Sekunden-Sperre nicht.
 *  3. Reserve: normaler Start (kommt nach spätestens ~5 Sekunden).
 */
class HomeWatcher : AccessibilityService() {

    companion object {
        const val TAG = "ChillzimmerHome"
        const val AMAZON_LAUNCHER = "com.amazon.tv.launcher"
        const val LABEL = "Chillzimmer"
        @Volatile var allowAmazonHome = false
        @Volatile private var instance: HomeWatcher? = null

        /** Wird von MainActivity.onResume aufgerufen. */
        fun onLauncherVisible() {
            val s = instance ?: return
            s.mainHandler.post { s.launcherVisible() }
        }
    }

    internal val mainHandler = Handler(Looper.getMainLooper())
    private var cover: View? = null
    private var coverBitmap: Bitmap? = null
    private var bitmapLoaded = false
    private var lastTrigger = 0L
    private var clickAttempts = 0
    private var clickDone = false

    private val hideRunnable = Runnable { hideCover() }

    private val clickRunnable = object : Runnable {
        override fun run() {
            if (clickDone || clickAttempts >= 10) return
            clickAttempts++
            if (tryClickTile()) {
                clickDone = true
                Log.i(TAG, "chillzimmer: Kachel geklickt (Versuch $clickAttempts)")
            } else {
                mainHandler.postDelayed(this, 200)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "chillzimmer: Dienst verbunden")
    }

    override fun onDestroy() {
        hideCover()
        instance = null
        super.onDestroy()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_HOME) return false
        if (event.action == KeyEvent.ACTION_UP) {
            Log.i(TAG, "chillzimmer: HOME-Taste abgefangen")
            allowAmazonHome = false
            startMain()
        }
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (event.packageName?.toString() != AMAZON_LAUNCHER || allowAmazonHome) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastTrigger < 1500) return
        lastTrigger = now
        Log.i(TAG, "chillzimmer: Amazon-Startseite erkannt")

        showCover()
        clickAttempts = 0
        clickDone = false
        mainHandler.removeCallbacks(clickRunnable)
        mainHandler.post(clickRunnable)
        startMain()
    }

    // ---------- Klick-Trick ----------

    private fun tryClickTile(): Boolean {
        val root = rootInActiveWindow ?: return false
        if (root.packageName?.toString() != AMAZON_LAUNCHER) return false
        val hits = root.findAccessibilityNodeInfosByText(LABEL)
        for (hit in hits) {
            var n: AccessibilityNodeInfo? = hit
            while (n != null) {
                if (n.isClickable && n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                n = n.parent
            }
            if (hit.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        }
        return false
    }

    // ---------- Reserve-Start ----------

    private fun startMain() {
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

    // ---------- Sofort-Abdeckung ----------

    private fun showCover() {
        if (cover != null) return
        try {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            val frame = FrameLayout(this)
            frame.setBackgroundColor(Color.parseColor("#17162B"))
            loadBitmap()?.let { bmp ->
                val iv = ImageView(this)
                iv.scaleType = ImageView.ScaleType.CENTER_CROP
                iv.setImageBitmap(bmp)
                frame.addView(
                    iv,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            }
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.OPAQUE
            )
            wm.addView(frame, lp)
            cover = frame
            mainHandler.removeCallbacks(hideRunnable)
            mainHandler.postDelayed(hideRunnable, 8000)
        } catch (e: Exception) {
            Log.e(TAG, "chillzimmer: Abdeckung fehlgeschlagen", e)
        }
    }

    private fun loadBitmap(): Bitmap? {
        if (!bitmapLoaded) {
            bitmapLoaded = true
            coverBitmap = try {
                assets.open("hintergrund.jpg").use { BitmapFactory.decodeStream(it) }
            } catch (e: Exception) {
                null
            }
        }
        return coverBitmap
    }

    private fun hideCover() {
        mainHandler.removeCallbacks(hideRunnable)
        val v = cover ?: return
        cover = null
        try {
            (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(v)
        } catch (e: Exception) {
        }
    }

    internal fun launcherVisible() {
        clickDone = true
        mainHandler.removeCallbacks(clickRunnable)
        mainHandler.removeCallbacks(hideRunnable)
        mainHandler.postDelayed(hideRunnable, 300)
    }

    override fun onInterrupt() {}
}
