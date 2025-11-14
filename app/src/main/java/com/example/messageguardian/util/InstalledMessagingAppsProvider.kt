package com.example.messageguardian.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import java.util.LinkedHashSet

class InstalledMessagingAppsProvider(private val context: Context) {
    fun query(): List<MessagingAppInfo> {
        val pm = context.packageManager
        val messagingPackages = LinkedHashSet<String>()

        val smsIntent = Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("smsto:") }
        messagingPackages += pm.querySafely(smsIntent).map { it.activityInfo.packageName }

        val messagingCategory = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MESSAGING)
        }
        messagingPackages += pm.querySafely(messagingCategory).map { it.activityInfo.packageName }

        if (messagingPackages.isEmpty()) {
            pm.getInstalledApplicationsCompat()
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .sortedBy { pm.getApplicationLabel(it).toString() }
                .forEach { messagingPackages += it.packageName }
        }

        return messagingPackages.mapNotNull { packageName ->
            try {
                val info = pm.getApplicationInfo(packageName, 0)
                MessagingAppInfo(packageName, pm.getApplicationLabel(info).toString())
            } catch (error: Exception) {
                null
            }
        }.sortedBy { it.appName }
    }

    companion object {
        fun from(context: Context) = InstalledMessagingAppsProvider(context.applicationContext)
    }

    private fun PackageManager.querySafely(intent: Intent): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION")
            queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
    }

    private fun PackageManager.getInstalledApplicationsCompat() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        getInstalledApplications(0)
    }
}
