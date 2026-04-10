package com.gkim.im.android.feature.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.GlassCard
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.PillAction
import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.data.remote.im.UserSearchResultDto
import com.gkim.im.android.data.repository.AppContainer
import kotlinx.coroutines.launch

@Composable
fun UserSearchRoute(container: AppContainer, onBack: () -> Unit) {
    val appLanguage = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<UserSearchResultDto>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AetherColors.Surface)
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("user-search-screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = appLanguage.pick("← Back", "← 返回"),
                style = MaterialTheme.typography.bodyLarge,
                color = AetherColors.Primary,
                modifier = Modifier.clickable(onClick = onBack).testTag("user-search-back"),
            )
            Text(
                text = appLanguage.pick("Find People", "搜索用户"),
                style = MaterialTheme.typography.headlineMedium,
                color = AetherColors.OnSurface,
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = { newQuery ->
                query = newQuery
                if (newQuery.trim().length >= 2) {
                    isSearching = true
                    val token = container.sessionStore.token ?: return@OutlinedTextField
                    val baseUrl = container.sessionStore.baseUrl ?: "http://127.0.0.1:18080/"
                    scope.launch {
                        try {
                            results = container.imBackendClient.searchUsers(baseUrl, token, newQuery.trim())
                        } catch (_: Exception) { }
                        isSearching = false
                    }
                } else {
                    results = emptyList()
                }
            },
            label = { Text(appLanguage.pick("Username or display name", "用户名或显示名称")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("user-search-input"),
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.testTag("user-search-results")) {
            items(results, key = { it.id }) { user ->
                SearchResultCard(user = user, container = container, scope = scope)
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    user: UserSearchResultDto,
    container: AppContainer,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val appLanguage = LocalAppLanguage.current
    var status by remember(user.id) { mutableStateOf(user.contactStatus) }

    GlassCard(modifier = Modifier.testTag("search-result-${user.id}")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(AetherColors.Secondary.copy(alpha = 0.14f), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = user.avatarText, color = AetherColors.Secondary, style = MaterialTheme.typography.titleMedium)
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = user.displayName, style = MaterialTheme.typography.titleMedium, color = AetherColors.OnSurface)
                    Text(text = "@${user.username}", style = MaterialTheme.typography.bodyMedium, color = AetherColors.OnSurfaceVariant)
                }
            }
            when (status) {
                "contact" -> Text(
                    text = appLanguage.pick("Contact", "已是好友"),
                    style = MaterialTheme.typography.labelLarge,
                    color = AetherColors.OnSurfaceVariant,
                )
                "pending_sent" -> Text(
                    text = appLanguage.pick("Pending", "已发送"),
                    style = MaterialTheme.typography.labelLarge,
                    color = AetherColors.OnSurfaceVariant,
                )
                "pending_received" -> Text(
                    text = appLanguage.pick("Received", "已收到"),
                    style = MaterialTheme.typography.labelLarge,
                    color = AetherColors.Primary,
                )
                else -> PillAction(
                    label = appLanguage.pick("Add", "添加"),
                    onClick = {
                        val token = container.sessionStore.token ?: return@PillAction
                        val baseUrl = container.sessionStore.baseUrl ?: "http://127.0.0.1:18080/"
                        scope.launch {
                            try {
                                container.imBackendClient.sendFriendRequest(baseUrl, token, user.id)
                                status = "pending_sent"
                            } catch (_: Exception) { }
                        }
                    },
                )
            }
        }
    }
}
