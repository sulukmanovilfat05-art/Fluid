package com.example.fluidnotif

import android.content.SharedPreferences

enum class T { SWITCH, SLIDER, CHOICE }

data class Opt(
    val key: String,
    val title: String,
    val type: T,
    val def: Int,
    val min: Int = 0,
    val max: Int = 1,
    val unit: String = "",
    val choices: List<String> = emptyList(),
    val section: String = ""
)

/** Single source of truth for every setting: the settings screen is generated from this list. */
object Opts {
    val EDGE_COLORS = listOf(
        "Purple" to 0xFFA98BFF.toInt(),
        "Cyan" to 0xFF4FD8FF.toInt(),
        "Pink" to 0xFFFF7AC8.toInt(),
        "Orange" to 0xFFFFB066.toInt(),
        "Green" to 0xFF6BE3A0.toInt(),
        "White" to 0xFFFFFFFF.toInt()
    )
    private val colorNames = EDGE_COLORS.map { it.first }

    private fun sl(sec: String, key: String, title: String, def: Int, min: Int, max: Int, unit: String = "") =
        Opt(key, title, T.SLIDER, def, min, max, unit, section = sec)

    private fun sw(sec: String, key: String, title: String, def: Int) =
        Opt(key, title, T.SWITCH, def, section = sec)

    private fun ch(sec: String, key: String, title: String, def: Int, choices: List<String>) =
        Opt(key, title, T.CHOICE, def, choices = choices, section = sec)

    val list = listOf(
        // Animation
        sl("Animation", "open_stiffness", "Open speed (spring stiffness)", 320, 100, 1000),
        sl("Animation", "open_damping", "Open bounce (lower = bouncier)", 72, 20, 100, "%"),
        sl("Animation", "close_stiffness", "Close speed", 420, 100, 1000),
        sl("Animation", "stay_s", "Stay on screen", 6, 1, 20, " s"),
        sl("Animation", "btn_delay", "Buttons appear after", 140, 0, 600, " ms"),
        sl("Animation", "text_blur", "Text blur while appearing", 18, 0, 40, " dp"),
        sw("Animation", "haptic", "Haptic tick when it appears", 1),
        // Look
        sw("Look", "dark", "Dark glass", 0),
        sl("Look", "glass_alpha", "Glass opacity", 88, 30, 100, "%"),
        sl("Look", "corner", "Corner radius", 28, 8, 44, " dp"),
        sl("Look", "margin", "Side margin", 8, 0, 32, " dp"),
        sl("Look", "top_gap", "Gap below camera", 6, -12, 40, " dp"),
        sl("Look", "icon_size", "Icon size", 40, 24, 60, " dp"),
        sl("Look", "text_scale", "Text size", 100, 80, 140, "%"),
        sw("Look", "edge_on", "Glowing edge", 1),
        sl("Look", "edge_w", "Edge thickness (tenths of dp)", 15, 5, 50),
        sl("Look", "glow", "Edge glow", 18, 0, 100, "%"),
        ch("Look", "edge_c1", "Edge colour 1", 0, colorNames),
        ch("Look", "edge_c2", "Edge colour 2", 1, colorNames),
        // Content
        sw("Content", "hide", "Hide message text (\"Message hidden\")", 0),
        sw("Content", "buttons", "Show action buttons", 1),
        sw("Content", "btn_reply", "Reply button", 1),
        sw("Content", "btn_read", "Mark as read button", 1),
        // Behaviour
        sw("Behaviour", "skip_ongoing", "Ignore ongoing (music, calls, timers)", 1),
        sw("Behaviour", "blur", "Real background blur (experimental)", 0),
        sl("Behaviour", "blur_r", "Blur strength", 28, 5, 60, " dp")
    )

    private val defaults = list.associate { it.key to it.def }

    fun i(sp: SharedPreferences, key: String): Int = sp.getInt(key, defaults.getValue(key))
    fun b(sp: SharedPreferences, key: String): Boolean = i(sp, key) != 0
}

/** Snapshot of all settings, read once per notification. */
class Style(private val sp: SharedPreferences) {
    private fun i(k: String) = Opts.i(sp, k)
    private fun b(k: String) = Opts.b(sp, k)

    val openStiffness = i("open_stiffness").toFloat()
    val openDamping = i("open_damping") / 100f
    val closeStiffness = i("close_stiffness").toFloat()
    val stayMs = i("stay_s") * 1000L
    val btnDelay = i("btn_delay").toLong()
    val textBlurDp = i("text_blur").toFloat()
    val haptic = b("haptic")

    val dark = b("dark")
    val glassAlpha = i("glass_alpha") * 255 / 100
    val cornerDp = i("corner").toFloat()
    val marginDp = i("margin").toFloat()
    val topGapDp = i("top_gap").toFloat()
    val iconDp = i("icon_size").toFloat()
    val textScale = i("text_scale") / 100f
    val edgeOn = b("edge_on")
    val edgeWDp = i("edge_w") / 10f
    val glowAlpha = i("glow") * 255 / 100
    val c1 = Opts.EDGE_COLORS[i("edge_c1").coerceIn(0, Opts.EDGE_COLORS.size - 1)].second
    val c2 = Opts.EDGE_COLORS[i("edge_c2").coerceIn(0, Opts.EDGE_COLORS.size - 1)].second

    val hide = b("hide")
    val buttons = b("buttons")
    val btnReply = b("btn_reply")
    val btnRead = b("btn_read")

    val skipOngoing = b("skip_ongoing")
    val blur = b("blur")
    val blurDp = i("blur_r").toFloat()
    val blocked: Set<String> = sp.getStringSet("blocked", emptySet()) ?: emptySet()
}
