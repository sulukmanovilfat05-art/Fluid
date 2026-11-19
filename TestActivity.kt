package com.example.fluidnotif

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class TestActivity : Activity() {

    private val h = Handler(Looper.getMainLooper())
    private lateinit var delay: EditText
    private lateinit var fApp: EditText
    private lateinit var fTitle: EditText
    private lateinit var fText: EditText
    private lateinit var fReply: Switch
    private var notifId = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpI(18f), dpI(44f), dpI(18f), dpI(32f))
        }

        fun header(s: String) = col.addView(TextView(this).apply {
            text = s
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFF5B6CFF.toInt())
            setPadding(0, dpI(18f), 0, dpI(6f))
        })
        fun btn(label: String, onClick: () -> Unit) = col.addView(Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { onClick() }
        }, LinearLayout.LayoutParams(MATCH, WRAP))
        fun field(hint: String, init: String): EditText {
            val e = EditText(this).apply { this.hint = hint; setText(init); textSize = 15f }
            col.addView(e, LinearLayout.LayoutParams(MATCH, WRAP))
            return e
        }

        col.addView(TextView(this).apply {
            text = "←   Test lab"
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setOnClickListener { finish() }
        })
        col.addView(TextView(this).apply {
            text = "Fake notifications to try your settings. The overlay is shown directly " +
                "(needs only \"Display over other apps\"). Use the delay to switch to the home screen first."
            textSize = 13f
            setPadding(0, dpI(8f), 0, 0)
        })

        header("DELAY BEFORE SHOWING (SECONDS)")
        delay = field("seconds", "3").apply { inputType = InputType.TYPE_CLASS_NUMBER }

        header("QUICK SCENARIOS")
        Fakes.scenarios.forEach { f -> btn("${f.app}: ${f.title}") { fire(f) } }

        header("CUSTOM")
        fApp = field("App name", "Messages")
        fTitle = field("Title", "Sam")
        fText = field("Message", "Are we still on for lunch today?")
        fReply = Switch(this).apply { text = "Can reply"; isChecked = true; setPadding(0, dpI(8f), 0, dpI(8f)) }
        col.addView(fReply)
        btn("Show custom fake overlay") { fire(custom()) }

        header("MORE")
        btn("Burst: 3 notifications in a row") {
            Fakes.scenarios.take(3).forEachIndexed { i, f -> fire(f, extraMs = i * 2500L) }
        }
        btn("Send as REAL notification (full pipeline)") { sendReal(custom()) }
        btn("Dismiss current overlay") { OverlayController.dismiss() }

        setContentView(ScrollView(this).apply { addView(col) })
    }

    private fun custom() = Fake(
        fApp.text.toString().ifBlank { "App" },
        fTitle.text.toString(),
        fText.text.toString(),
        0xFF8E5BFF.toInt(),
        fReply.isChecked
    )

    private fun delayMs(): Long = (delay.text.toString().toLongOrNull() ?: 0L) * 1000L

    private fun fire(f: Fake, extraMs: Long = 0L) {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Grant \"Display over other apps\" first", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
            return
        }
        val total = delayMs() + extraMs
        if (total >= 1000 && extraMs == 0L) {
            Toast.makeText(this, "Showing in ${total / 1000} s — go to the home screen", Toast.LENGTH_SHORT).show()
        }
        val data = f.toData(this)
        h.postDelayed({ OverlayController.show(applicationContext, data) }, total)
    }

    private fun sendReal(f: Fake) {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            return
        }
        val total = delayMs()
        Toast.makeText(this, "Real notification in ${total / 1000} s", Toast.LENGTH_SHORT).show()
        h.postDelayed({
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel("test", "Test", NotificationManager.IMPORTANCE_DEFAULT)
            )
            val pi = PendingIntent.getBroadcast(
                this, 0, Intent("com.example.fluidnotif.NOOP").setPackage(packageName),
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val builder = Notification.Builder(this, "test")
                .setSmallIcon(R.drawable.ic_check)
                .setContentTitle(f.title)
                .setContentText(f.text)
            if (f.canReply) {
                val ri = RemoteInput.Builder("reply").setLabel("Reply").build()
                builder.addAction(
                    Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_check), "Reply", pi)
                        .addRemoteInput(ri)
                        .setSemanticAction(Notification.Action.SEMANTIC_ACTION_REPLY).build()
                )
            }
            builder.addAction(
                Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_check), "Mark as read", pi)
                    .setSemanticAction(Notification.Action.SEMANTIC_ACTION_MARK_AS_READ).build()
            )
            nm.notify(notifId++, builder.build())
        }, total)
    }
}
