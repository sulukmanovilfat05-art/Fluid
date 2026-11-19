package com.example.fluidnotif

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class SettingsActivity : Activity() {

    private lateinit var sp: SharedPreferences
    private lateinit var stage: FrameLayout
    private val h = Handler(Looper.getMainLooper())
    private var panel: NotifPanel? = null
    private val playRun = Runnable { play() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sp = getSharedPreferences("fluid", Context.MODE_PRIVATE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF2F2F5.toInt())
        }

        root.addView(TextView(this).apply {
            text = "←   Fluid Notifications"
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dpI(18f), dpI(40f), dpI(18f), dpI(8f))
            setOnClickListener { finish() }
        })

        // Live preview: a gradient "phone" with a camera dot.
        val preview = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFFFFB27A.toInt(), 0xFFA9DCEB.toInt(), 0xFFB5F0D0.toInt())
            ).apply { cornerRadius = dpF(28f) }
            setOnClickListener { schedulePlay(0) }
        }
        val dot = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF000000.toInt())
            }
        }
        preview.addView(dot, FrameLayout.LayoutParams(dpI(14f), dpI(14f), Gravity.TOP or Gravity.CENTER_HORIZONTAL)
            .apply { topMargin = dpI(10f) })
        stage = FrameLayout(this)
        preview.addView(stage, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(preview, LinearLayout.LayoutParams(MATCH, dpI(250f)).apply {
            setMargins(dpI(14f), dpI(6f), dpI(14f), dpI(10f))
        })

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpI(18f), 0, dpI(18f), dpI(40f))
        }
        buildOptions(col)
        root.addView(ScrollView(this).apply { addView(col) }, LinearLayout.LayoutParams(MATCH, 0, 1f))

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        schedulePlay(350)
    }

    override fun onPause() {
        super.onPause()
        h.removeCallbacksAndMessages(null)
        releasePanel()
    }

    private fun releasePanel() {
        panel?.let {
            it.release()
            stage.removeView(it)
        }
        panel = null
    }

    private fun schedulePlay(ms: Long) {
        h.removeCallbacks(playRun)
        h.postDelayed(playRun, ms)
    }

    private fun play() {
        releasePanel()
        val w = stage.width
        if (w == 0) { schedulePlay(100); return }
        val st = Style(sp)
        val m = dpI(st.marginDp)
        val cardTop = (dpI(34f) + dpI(st.topGapDp)).coerceAtLeast(0)
        val cx = (w - 2 * m) / 2f
        val cy = dpF(17f)
        val start = RectF(
            cx - dpF(10f), cy - dpF(10f) - cardTop,
            cx + dpF(10f), cy + dpF(10f) - cardTop
        )
        val p = NotifPanel(this, Fakes.scenarios[0].toData(this, toasts = false), st, start, cardTop)
        p.onClosed = {
            if (panel === p) {
                stage.removeView(p)
                panel = null
                h.postDelayed(playRun, 900)
            }
        }
        stage.addView(p, FrameLayout.LayoutParams(MATCH, WRAP).apply { setMargins(m, 0, m, 0) })
        panel = p
        p.open(haptic = false)
    }

    // ---- Settings UI generated from Opts.list ----
    private fun buildOptions(col: LinearLayout) {
        var lastSection = ""
        for (o in Opts.list) {
            if (o.section != lastSection) {
                lastSection = o.section
                col.addView(TextView(this).apply {
                    text = o.section.uppercase()
                    textSize = 12f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(0xFF5B6CFF.toInt())
                    setPadding(0, dpI(22f), 0, dpI(6f))
                })
            }
            when (o.type) {
                T.SWITCH -> col.addView(Switch(this).apply {
                    text = o.title
                    textSize = 15f
                    isChecked = Opts.b(sp, o.key)
                    setPadding(0, dpI(10f), 0, dpI(10f))
                    setOnCheckedChangeListener { _, v ->
                        sp.edit().putInt(o.key, if (v) 1 else 0).apply()
                        schedulePlay(250)
                    }
                })
                T.SLIDER -> {
                    val label = TextView(this).apply { textSize = 15f; setPadding(0, dpI(8f), 0, 0) }
                    fun refresh(v: Int) { label.text = "${o.title}: $v${o.unit}" }
                    val cur = Opts.i(sp, o.key)
                    refresh(cur)
                    val bar = SeekBar(this).apply {
                        max = o.max - o.min
                        progress = cur - o.min
                        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                                val v = p + o.min
                                refresh(v)
                                if (fromUser) {
                                    sp.edit().putInt(o.key, v).apply()
                                    schedulePlay(300)
                                }
                            }
                            override fun onStartTrackingTouch(s: SeekBar) {}
                            override fun onStopTrackingTouch(s: SeekBar) {}
                        })
                    }
                    col.addView(label)
                    col.addView(bar)
                }
                T.CHOICE -> {
                    val tv = TextView(this).apply {
                        textSize = 15f
                        setPadding(0, dpI(12f), 0, dpI(12f))
                    }
                    fun refresh() {
                        tv.text = "${o.title}: ${o.choices[Opts.i(sp, o.key).coerceIn(0, o.choices.size - 1)]}  ▾"
                    }
                    refresh()
                    tv.setOnClickListener {
                        AlertDialog.Builder(this)
                            .setTitle(o.title)
                            .setItems(o.choices.toTypedArray()) { _, which ->
                                sp.edit().putInt(o.key, which).apply()
                                refresh()
                                schedulePlay(250)
                            }.show()
                    }
                    col.addView(tv)
                }
            }
        }

        col.addView(TextView(this).apply {
            text = "APPS"
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFF5B6CFF.toInt())
            setPadding(0, dpI(22f), 0, dpI(6f))
        })
        col.addView(Button(this).apply {
            text = "Choose apps to ignore…"
            isAllCaps = false
            setOnClickListener { pickApps() }
        })
        col.addView(Button(this).apply {
            text = "Reset all settings"
            isAllCaps = false
            setOnClickListener {
                sp.edit().clear().apply()
                recreate()
            }
        })
        col.addView(Button(this).apply {
            text = "Open test lab"
            isAllCaps = false
            setOnClickListener { startActivity(Intent(this@SettingsActivity, TestActivity::class.java)) }
        })
    }

    private fun pickApps() {
        val pm = packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcher, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }
        val blocked = (sp.getStringSet("blocked", emptySet()) ?: emptySet()).toMutableSet()
        val checked = BooleanArray(apps.size) { apps[it].first in blocked }
        AlertDialog.Builder(this)
            .setTitle("Ignore these apps")
            .setMultiChoiceItems(apps.map { it.second }.toTypedArray(), checked) { _, i, c ->
                if (c) blocked.add(apps[i].first) else blocked.remove(apps[i].first)
            }
            .setPositiveButton("Save") { _, _ ->
                sp.edit().putStringSet("blocked", HashSet(blocked)).apply()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
