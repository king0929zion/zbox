package com.example.messageguardian.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.messageguardian.data.MonitoredAppsStore
import com.example.messageguardian.data.SavedMessage
import com.example.messageguardian.data.SavedMessageRepository
import com.example.messageguardian.util.InstalledMessagingAppsProvider
import com.example.messageguardian.util.MessagingAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MessagesViewModel(
    private val repository: SavedMessageRepository,
    private val appsStore: MonitoredAppsStore,
    private val appsProvider: InstalledMessagingAppsProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.savedMessages.collect { saved ->
                _uiState.update { it.copy(messages = saved) }
            }
        }

        viewModelScope.launch {
            appsStore.monitoredPackages.collect { selected ->
                _uiState.update { state ->
                    val refreshedApps = if (state.monitoredApps.isEmpty()) {
                        state.monitoredApps
                    } else {
                        state.monitoredApps.map { app ->
                            app.copy(isSelected = selected.contains(app.info.packageName))
                        }
                    }
                    state.copy(selectedPackages = selected, monitoredApps = refreshedApps)
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val appList = runCatching { appsProvider.query() }
                .getOrDefault(emptyList())
            _uiState.update { state ->
                state.copy(
                    monitoredApps = appList.map { appInfo ->
                        MonitoredAppUi(info = appInfo, isSelected = state.selectedPackages.contains(appInfo.packageName))
                    },
                    isLoadingApps = false
                )
            }
        }
    }

    fun toggleApp(packageName: String) {
        viewModelScope.launch {
            val current = uiState.value.selectedPackages
            appsStore.togglePackage(packageName, !current.contains(packageName))
        }
    }

    fun updateNotificationAccess(granted: Boolean) {
        _uiState.update { it.copy(isNotificationAccessGranted = granted) }
    }

    data class MessagesUiState(
        val monitoredApps: List<MonitoredAppUi> = emptyList(),
        val selectedPackages: Set<String> = emptySet(),
        val messages: List<SavedMessage> = emptyList(),
        val isNotificationAccessGranted: Boolean = false,
        val isLoadingApps: Boolean = true
    )

    data class MonitoredAppUi(
        val info: MessagingAppInfo,
        val isSelected: Boolean
    )

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val repository = SavedMessageRepository.from(appContext)
                    val appsStore = MonitoredAppsStore.from(appContext)
                    val provider = InstalledMessagingAppsProvider.from(appContext)
                    @Suppress("UNCHECKED_CAST")
                    return MessagesViewModel(repository, appsStore, provider) as T
                }
            }
        }
    }
}
