package com.example.fluidnotif

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationManagerCompat

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpI(20f), dpI(48f), dpI(20f), dpI(24f))
        }
        col.addView(TextView(this).apply {
            text = "Fluid Notifications"
            textSize = 28f
            setTypeface(typeface, Typeface.BOLD)
        })
        col.addView(TextView(this).apply {
            text = "Glass notifications that grow out of the camera."
            textSize = 14f
            setPadding(0, dpI(6f), 0, dpI(16f))
        })

        status = TextView(this).apply {
            textSize = 15f
            setPadding(0, 0, 0, dpI(12f))
        }
        col.addView(status)

        fun btn(label: String, onClick: () -> Unit) {
            col.addView(Button(this).apply {
                text = label
                isAllCaps = false
                gravity = Gravity.CENTER
                setOnClickListener { onClick() }
            }, LinearLayout.LayoutParams(MATCH, WRAP))
        }

        btn("1. Notification access") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        btn("2. Display over other apps") {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }
        btn("3. Battery: set to Unrestricted") {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        col.addView(TextView(this).apply { setPadding(0, dpI(10f), 0, 0) })
        btn("Settings & live preview") { startActivity(Intent(this, SettingsActivity::class.java)) }
        btn("Test lab (fake notifications)") { startActivity(Intent(this, TestActivity::class.java)) }

        col.addView(TextView(this).apply {
            text = "Tip: on Samsung turn off the system pop-up so you don't see two " +
                "(Settings → Notifications → Notification pop-up style → Brief)."
            textSize = 13f
            setPadding(0, dpI(16f), 0, 0)
        })

        setContentView(ScrollView(this).apply { addView(col) })
    }

    override fun onResume() {
        super.onResume()
        val access = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        val overlay = Settings.canDrawOverlays(this)
        status.text = (if (access) "✓" else "✗") + " Notification access\n" +
            (if (overlay) "✓" else "✗") + " Display over other apps"
    }
}
