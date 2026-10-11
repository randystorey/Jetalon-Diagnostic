package com.randy.jetalondiagnostic

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.graphics.Typeface
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private lateinit var log: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 20, 28, 20)
        }
        root.addView(TextView(this).apply {
            text = "Jetalon GT6 Diagnostic v1"
            textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Safe launcher only — does not root, flash, or change hidden settings automatically."
            textSize = 15f
        })

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun button(label: String, weight: Float = 1f, action: () -> Unit): Button = Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight).apply { setMargins(4,8,4,8) }
        }
        row.addView(button("Developer Options") { launchAction(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, "Developer Options") })
        row.addView(button("Wireless Debugging") { launchWirelessDebug() })
        row.addView(button("Android Settings") { launchAction(Settings.ACTION_SETTINGS, "Android Settings") })
        row.addView(button("Device Info") { launchAction(Settings.ACTION_DEVICE_INFO_SETTINGS, "Device Info") })
        root.addView(row)

        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row2.addView(button("USB Settings") { launchExplicitCandidates("USB Settings", listOf(
            "com.android.settings.Settings\$UsbDetailsActivity",
            "com.android.settings.Settings\$UsbModeChooserActivity"
        )) })
        row2.addView(button("Try Settings package") { launchSettingsPackage() })
        row2.addView(button("Refresh diagnostics") { showDiagnostics() })
        root.addView(row2)

        log = TextView(this).apply {
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            setPadding(8,8,8,8)
        }
        val scroll = ScrollView(this).apply { addView(log) }
        root.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        showDiagnostics()
    }

    private fun launchAction(action: String, label: String) {
        try {
            startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            append("OPENED $label via $action")
        } catch (e: Exception) {
            append("FAILED $label: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun launchWirelessDebug() {
        val candidates = listOf(
            "com.android.settings.Settings\$AdbWirelessSettingsActivity",
            "com.android.settings.Settings\$WirelessDebuggingActivity",
            "com.android.settings.development.WirelessDebuggingActivity"
        )
        if (launchExplicitCandidates("Wireless Debugging", candidates)) return
        // Public fallback: Developer Options is where Wireless debugging normally lives on Android 11+.
        launchAction(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, "Developer Options fallback")
    }

    private fun launchExplicitCandidates(label: String, classes: List<String>): Boolean {
        for (cls in classes) {
            try {
                val i = Intent().setComponent(ComponentName("com.android.settings", cls)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
                append("OPENED $label: $cls")
                return true
            } catch (e: Exception) {
                append("Not available: $cls (${e.javaClass.simpleName})")
            }
        }
        append("No $label activity opened.")
        return false
    }

    private fun launchSettingsPackage() {
        val i = packageManager.getLaunchIntentForPackage("com.android.settings")
        if (i == null) append("com.android.settings has no launcher activity")
        else try { startActivity(i); append("OPENED com.android.settings launcher") }
        catch (e: Exception) { append("FAILED settings package: ${e.message}") }
    }

    private fun showDiagnostics() {
        val devEnabled = try { Settings.Global.getInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) } catch (_: Exception) { -1 }
        val adbEnabled = try { Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0) } catch (_: Exception) { -1 }
        log.text = buildString {
            appendLine("=== DEVICE ===")
            appendLine("Manufacturer: ${Build.MANUFACTURER}")
            appendLine("Brand: ${Build.BRAND}")
            appendLine("Model: ${Build.MODEL}")
            appendLine("Device: ${Build.DEVICE}")
            appendLine("Product: ${Build.PRODUCT}")
            appendLine("Board: ${Build.BOARD}")
            appendLine("Hardware: ${Build.HARDWARE}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Build type: ${Build.TYPE}")
            appendLine("Build tags: ${Build.TAGS}")
            appendLine("Fingerprint: ${Build.FINGERPRINT}")
            appendLine()
            appendLine("=== READ-ONLY GLOBAL STATE ===")
            appendLine("development_settings_enabled=$devEnabled")
            appendLine("adb_enabled=$adbEnabled")
            appendLine()
            appendLine("Try Developer Options first. If it opens, look for Wireless debugging or USB debugging. Do not change unrelated settings.")
        }
    }

    private fun append(s: String) { log.append("\n$s") }
}
