package com.example.fluidnotif

import android.content.Context
import android.widget.Toast

/** Fake notification scenarios for the test lab and the settings preview. */
class Fake(
    val app: String,
    val title: String,
    val text: String,
    val color: Int,
    val canReply: Boolean = true
) {
    fun toData(ctx: Context, toasts: Boolean = true): NotifData {
        val app = ctx.applicationContext
        fun t(s: String) { if (toasts) Toast.makeText(app, s, Toast.LENGTH_SHORT).show() }
        val letter = this.app.firstOrNull()?.uppercase() ?: "?"
        return NotifData(
            appName = this.app,
            icon = { LetterIcon(color, letter) },
            title = title,
            text = text,
            canReply = canReply,
            onOpen = { t("Opened: $title") },
            onRead = { t("Marked as read") },
            onReply = { t("Sent: $it") }
        )
    }
}

object Fakes {
    val scenarios = listOf(
        Fake("Messages", "Emily", "Yup, it was the little bakery on the corner. Want to come along on Saturday?", 0xFF5B6CFF.toInt()),
        Fake("Chat Business", "Chat Business", "New message from a customer", 0xFF2BB673.toInt()),
        Fake("Mail", "Alex Rivera", "Quarterly report: please review the attached numbers before Friday's meeting and send me your comments when you can.", 0xFFE5483D.toInt(), false),
        Fake("Calendar", "Meeting in 10 minutes", "Design sync · Room 4B", 0xFF2F80ED.toInt(), false),
        Fake("Delivery", "Courier is nearby", "Your order will arrive in about 5 minutes", 0xFFF2994A.toInt(), false),
        Fake("Bank", "Payment received", "+ \$240.00 from Sam", 0xFF27AE60.toInt(), false)
    )
}
