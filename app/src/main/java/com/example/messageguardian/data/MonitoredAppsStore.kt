package com.example.messageguardian.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.monitoredAppsDataStore by preferencesDataStore(name = "monitored_apps")

class MonitoredAppsStore(private val context: Context) {
    private val monitoredKey = stringSetPreferencesKey("selected_packages")

    val monitoredPackages: Flow<Set<String>> = context.monitoredAppsDataStore.data
        .map { it[monitoredKey] ?: emptySet() }

    suspend fun togglePackage(packageName: String, enabled: Boolean) {
        withContext(Dispatchers.IO) {
            context.monitoredAppsDataStore.edit { prefs ->
                val current = prefs[monitoredKey]?.toMutableSet() ?: mutableSetOf()
                if (enabled) {
                    current.add(packageName)
                } else {
                    current.remove(packageName)
                }
                prefs[monitoredKey] = current
            }
        }
    }

    suspend fun setPackages(packages: Set<String>) {
        withContext(Dispatchers.IO) {
            context.monitoredAppsDataStore.edit { prefs ->
                prefs[monitoredKey] = packages
            }
        }
    }

    suspend fun getSelectedPackages(): Set<String> = withContext(Dispatchers.IO) {
        context.monitoredAppsDataStore.data.map { prefs ->
            prefs[monitoredKey] ?: emptySet()
        }.first()
    }

    companion object {
        fun from(context: Context): MonitoredAppsStore = MonitoredAppsStore(context.applicationContext)
    }
}
