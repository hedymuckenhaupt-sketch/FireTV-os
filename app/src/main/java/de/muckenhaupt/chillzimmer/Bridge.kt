package de.muckenhaupt.chillzimmer

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.util.Base64
import android.webkit.JavascriptInterface
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/** Schnittstelle zwischen Oberfläche (JavaScript) und Fire OS. */
class Bridge(private val act: MainActivity) {

    private val pm: PackageManager = act.packageManager
    private val prefs = act.getSharedPreferences("launcher", Context.MODE_PRIVATE)
    private val imgCache = HashMap<String, String>()
    @Volatile private var ready = false

    @JavascriptInterface
    fun requestApps() {
        ready = true
        scan()
    }

    fun rescanIfReady() {
        if (ready) scan()
    }

    private fun scan() {
        Thread {
            val json = try { buildAppsJson() } catch (e: Exception) { "[]" }
            act.pushApps(json)
        }.start()
    }

    @Synchronized
    private fun buildAppsJson(): String {
        val arr = JSONArray()
        val seen = HashSet<String>()
        val categories = listOf("android.intent.category.LEANBACK_LAUNCHER", Intent.CATEGORY_LAUNCHER)
        for (cat in categories) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(cat)
            for (ri in pm.queryIntentActivities(query, 0)) {
                val ai = ri.activityInfo
                val pkg = ai.packageName
                if (pkg == act.packageName || !seen.add(pkg)) continue
                val updated = try { pm.getPackageInfo(pkg, 0).lastUpdateTime } catch (e: Exception) { 0L }
                val img = imgCache.getOrPut("$pkg@$updated") {
                    try { encode(ri.loadIcon(pm)) } catch (e: Exception) { "" }
                }
                arr.put(
                    JSONObject()
                        .put("pkg", pkg)
                        .put("name", ri.loadLabel(pm).toString())
                        .put("img", img)
                )
            }
        }
        return arr.toString()
    }

    private fun encode(d: Drawable): String {
        val w = 128
        val h = 128
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, w, h)
        d.draw(Canvas(bmp))
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        bmp.recycle()
        return "data:image/png;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    @JavascriptInterface
    fun launch(pkg: String) {
        val intent = pm.getLeanbackLaunchIntentForPackage(pkg)
            ?: pm.getLaunchIntentForPackage(pkg)
            ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        start(intent)
    }

    @JavascriptInterface
    fun openSettings() {
        start(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    @JavascriptInterface
    fun openAmazonHome() {
        HomeWatcher.allowAmazonHome = true
        val home = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .setPackage(HomeWatcher.AMAZON_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        act.runOnUiThread {
            try {
                act.startActivity(home)
            } catch (e: Exception) {
                pm.getLaunchIntentForPackage(HomeWatcher.AMAZON_LAUNCHER)?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try { act.startActivity(it) } catch (_: Exception) {}
                }
            }
        }
    }

    /** Einfache Ablage für Favoriten und Einstellungen */
    @JavascriptInterface
    fun getValue(key: String): String? = prefs.getString(key, null)

    @JavascriptInterface
    fun setValue(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    private fun start(intent: Intent) {
        act.runOnUiThread {
            try { act.startActivity(intent) } catch (_: Exception) {}
        }
    }
}
