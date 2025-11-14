package com.example.messageguardian.service

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.messageguardian.data.MonitoredAppsStore
import com.example.messageguardian.data.SavedMessage
import com.example.messageguardian.data.SavedMessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationCaptureService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository by lazy { SavedMessageRepository.from(this) }
    private val appStore by lazy { MonitoredAppsStore.from(this) }
    private val labelCache = mutableMapOf<String, String>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        serviceScope.launch {
            val monitored = runCatching { appStore.getSelectedPackages() }.getOrDefault(emptySet())
            if (monitored.isEmpty() || !monitored.contains(sbn.packageName)) return@launch

            val notification = sbn.notification ?: return@launch
            val extras = notification.extras ?: return@launch
            val text = extractMessageText(extras) ?: return@launch

            val message = SavedMessage(
                packageName = sbn.packageName,
                appLabel = resolveAppLabel(sbn.packageName),
                conversation = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
                sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                messageText = text,
                timestamp = sbn.postTime,
                notificationKey = sbn.key,
                conversationId = extras.getString(Notification.EXTRA_TEMPLATE)
            )
            repository.persist(message)
            repository.pruneOlderThan(30)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?, reason: Int) {
        super.onNotificationRemoved(sbn, rankingMap, reason)
        val key = sbn?.key ?: return
        serviceScope.launch {
            repository.markRevoked(key)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun extractMessageText(extras: Bundle): String? {
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.mapNotNull { it?.toString() }
            ?.filter { it.isNotBlank() }
            ?.joinToString(separator = "\n")
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        return when {
            !lines.isNullOrBlank() -> lines
            !bigText.isNullOrBlank() -> bigText
            !text.isNullOrBlank() -> text
            else -> null
        }
    }

    private suspend fun resolveAppLabel(packageName: String): String = withContext(Dispatchers.IO) {
        labelCache[packageName]?.let { return@withContext it }
        val pm = packageManager
        val result = runCatching {
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        }.getOrDefault(packageName)
        labelCache[packageName] = result
        result
    }
}
