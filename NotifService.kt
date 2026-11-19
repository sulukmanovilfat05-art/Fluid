package com.example.fluidnotif

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotifService : NotificationListenerService() {

    private val skipCategories = setOf(
        Notification.CATEGORY_TRANSPORT, Notification.CATEGORY_PROGRESS,
        Notification.CATEGORY_SERVICE, Notification.CATEGORY_SYSTEM,
        Notification.CATEGORY_STATUS
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val n = sbn.notification
        val st = Style(getSharedPreferences("fluid", Context.MODE_PRIVATE))

        if (sbn.packageName in st.blocked) return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (st.skipOngoing && (sbn.isOngoing || n.category in skipCategories)) return

        val ex = n.extras
        val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (ex.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: ex.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        if (title.isEmpty() && text.isEmpty()) return

        val pm = packageManager
        val pkg = sbn.packageName
        val appName = try {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) { pkg }

        val acts = n.actions.orEmpty()
        val replyAct = acts.firstOrNull { !it.remoteInputs.isNullOrEmpty() }
        val readAct = acts.firstOrNull {
            it.semanticAction == Notification.Action.SEMANTIC_ACTION_MARK_AS_READ ||
                it.title?.contains("read", ignoreCase = true) == true
        }
        val ri = replyAct?.remoteInputs?.firstOrNull()

        val data = NotifData(
            appName = appName,
            icon = { try { pm.getApplicationIcon(pkg) } catch (e: Exception) { null } },
            title = title,
            text = text,
            canReply = ri != null,
            onOpen = {
                try { n.contentIntent?.send() } catch (_: Exception) {}
                cancelNotification(sbn.key)
            },
            onRead = {
                try { readAct?.actionIntent?.send() } catch (_: Exception) {}
                cancelNotification(sbn.key)
            },
            onReply = { reply ->
                try {
                    if (ri != null) {
                        val i = Intent()
                        val b = Bundle().apply { putCharSequence(ri.resultKey, reply) }
                        RemoteInput.addResultsToIntent(arrayOf(ri), i, b)
                        replyAct.actionIntent.send(applicationContext, 0, i)
                    }
                } catch (_: Exception) {}
                cancelNotification(sbn.key)
            }
        )
        OverlayController.show(applicationContext, data)
    }
}
