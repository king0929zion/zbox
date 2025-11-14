package com.example.messageguardian

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.animateItemPlacement
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.messageguardian.data.SavedMessage
import com.example.messageguardian.ui.theme.MessageGuardianTheme
import com.example.messageguardian.viewmodel.MessagesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MessageGuardianTheme {
                val vm: MessagesViewModel = viewModel(factory = MessagesViewModel.factory(applicationContext))
                val uiState by vm.uiState.collectAsStateWithLifecycle()
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    vm.updateNotificationAccess(isNotificationAccessGranted(context))
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            vm.updateNotificationAccess(isNotificationAccessGranted(context))
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                MessageGuardianScreen(
                    state = uiState,
                    onToggleApp = vm::toggleApp,
                    onOpenNotificationAccess = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

private fun isNotificationAccessGranted(context: android.content.Context): Boolean {
    val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
    return enabledPackages.contains(context.packageName)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageGuardianScreen(
    state: MessagesViewModel.MessagesUiState,
    onToggleApp: (String) -> Unit,
    onOpenNotificationAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "消息工具箱", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(
                            text = "守住每一条对话",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                NotificationAccessCard(
                    granted = state.isNotificationAccessGranted,
                    onOpenSettings = onOpenNotificationAccess
                )
            }
            item {
                MonitoredAppsSection(
                    apps = state.monitoredApps,
                    loading = state.isLoadingApps,
                    onToggleApp = onToggleApp
                )
            }
            item {
                SectionHeader(text = "消息记录")
            }
            if (state.messages.isEmpty()) {
                item {
                    EmptyMessageIllustration()
                }
            } else {
                items(state.messages, key = { it.id }) { message ->
                    MessageBubbleCard(
                        message = message,
                        modifier = Modifier.animateItemPlacement(animationSpec = spring(stiffness = Spring.StiffnessLow))
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun NotificationAccessCard(granted: Boolean, onOpenSettings: () -> Unit) {
    val iconTint by animateColorAsState(
        targetValue = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        label = "iconTint"
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Outlined.Notifications, contentDescription = null, tint = iconTint)
            }
            Spacer(modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (granted) "通知监听已开启" else "需要开启通知监听",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (granted) "可以自动记录被撤回的消息" else "前往系统设置授予权限，才能捕捉消息",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onOpenSettings) {
                Text(text = if (granted) "管理" else "去开启")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonitoredAppsSection(
    apps: List<MessagesViewModel.MonitoredAppUi>,
    loading: Boolean,
    onToggleApp: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(16.dp)
    ) {
        Text(text = "监听应用", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        if (loading) {
            Text(text = "正在获取可选应用…", style = MaterialTheme.typography.bodySmall)
            return@Column
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            apps.forEach { monitoredApp ->
                FilterChip(
                    selected = monitoredApp.isSelected,
                    onClick = { onToggleApp(monitoredApp.info.packageName) },
                    label = { Text(monitoredApp.info.appName) },
                    leadingIcon = if (monitoredApp.isSelected) {
                        { Icon(imageVector = Icons.Outlined.Check, contentDescription = null) }
                    } else null
                )
            }
        }
        if (apps.isEmpty()) {
            Text(
                text = "暂未检测到支持的消息类应用，可稍后再试。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun MessageBubbleCard(message: SavedMessage, modifier: Modifier = Modifier) {
    val bubbleColor = if (message.isRevoked) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    }
    val borderColor = if (message.isRevoked) MaterialTheme.colorScheme.error else Color.Transparent
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "${message.appLabel} · ${message.sender ?: "匿名"}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(22.dp))
                .border(1.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(bubbleColor, bubbleColor.copy(alpha = 0.6f))
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = message.messageText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Outlined.Message, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AnimatedVisibility(visible = message.isRevoked, enter = fadeIn(), exit = fadeOut()) {
                        AssistChip(onClick = {}, enabled = false, label = { Text("已撤回") })
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMessageIllustration() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Message,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(54.dp)
        )
        Text(text = "还没有记录", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "开启通知监听并选择要守护的应用，新的消息会以气泡形式出现在这里。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
