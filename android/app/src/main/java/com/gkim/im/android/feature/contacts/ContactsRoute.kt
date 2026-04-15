package com.gkim.im.android.feature.contacts

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.GlassCard
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.PillAction
import com.gkim.im.android.core.designsystem.PrimaryShellHeader
import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.core.model.Contact
import com.gkim.im.android.core.model.ContactSortMode
import com.gkim.im.android.data.remote.im.FriendRequestViewDto
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.data.repository.ContactsRepository
import com.gkim.im.android.data.repository.MessagingRepository
import com.gkim.im.android.data.remote.im.resolveImHttpEndpoint
import com.gkim.im.android.feature.shared.simpleViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val sortMode: ContactSortMode = ContactSortMode.Nickname,
)

internal class ContactsViewModel(
    private val contactsRepository: ContactsRepository,
    private val messagingRepository: MessagingRepository,
) : ViewModel() {
    val uiState = combine(contactsRepository.sortedContacts, contactsRepository.sortMode) { contacts, sortMode ->
        ContactsUiState(contacts = contacts, sortMode = sortMode)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ContactsUiState(
            contacts = contactsRepository.sortedContacts.value,
            sortMode = contactsRepository.sortMode.value,
        ),
    )

    fun setSortMode(mode: ContactSortMode) {
        contactsRepository.setSortMode(mode)
    }

    fun openContact(contact: Contact): String = messagingRepository.ensureConversation(contact).id
}

@Composable
fun ContactsRoute(navController: NavHostController, container: AppContainer) {
    val viewModel = viewModel<ContactsViewModel>(factory = simpleViewModelFactory {
        ContactsViewModel(container.contactsRepository, container.messagingRepository)
    })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ContactsScreen(
        uiState = uiState,
        onSortModeSelected = viewModel::setSortMode,
        onOpenContact = { contact -> navController.navigate("chat/${viewModel.openContact(contact)}") },
        onOpenSearch = { navController.navigate("user-search") },
        container = container,
    )
}

@Composable
private fun ContactsScreen(
    uiState: ContactsUiState,
    onSortModeSelected: (ContactSortMode) -> Unit,
    onOpenContact: (Contact) -> Unit,
    onOpenSearch: () -> Unit,
    container: AppContainer,
) {
    val appLanguage = LocalAppLanguage.current
    val sortOptions = listOf(
        ContactSortMode.Nickname to appLanguage.pick("Nickname A-Z", "昵称 A-Z"),
        ContactSortMode.AddedAscending to appLanguage.pick("Added earliest", "最早添加"),
        ContactSortMode.AddedDescending to appLanguage.pick("Added latest", "最近添加"),
    )
    val selectedSortLabel = sortOptions.first { it.first == uiState.sortMode }.second
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var pendingRequests by remember { mutableStateOf<List<FriendRequestViewDto>>(emptyList()) }

    LaunchedEffect(Unit) {
        val token = container.sessionStore.token ?: return@LaunchedEffect
        val endpoint = container.resolveImHttpEndpoint()
        try {
            pendingRequests = container.imBackendClient.listFriendRequests(endpoint.baseUrl, token)
        } catch (_: Exception) { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AetherColors.Surface)
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("contacts-screen"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        PrimaryShellHeader(title = appLanguage.pick("Contacts", "联系人")) {
            Box {
                Text(
                    text = "$selectedSortLabel ▾",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.OnSurface,
                    modifier = Modifier
                        .testTag("contact-sort-dropdown")
                        .background(AetherColors.SurfaceContainerHigh, CircleShape)
                        .clickable { isSortMenuExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
                DropdownMenu(
                    expanded = isSortMenuExpanded,
                    onDismissRequest = { isSortMenuExpanded = false },
                ) {
                    sortOptions.forEach { (mode, label) ->
                        DropdownMenuItem(
                            modifier = Modifier.testTag("contact-sort-option-${mode.name}"),
                            text = { Text(text = label) },
                            onClick = {
                                onSortModeSelected(mode)
                                isSortMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }

        PillAction(label = appLanguage.pick("Find People", "搜索用户"), onClick = onOpenSearch)

        if (pendingRequests.isNotEmpty()) {
            Text(
                text = appLanguage.pick("Friend Requests (${pendingRequests.size})", "好友请求 (${pendingRequests.size})"),
                style = MaterialTheme.typography.labelLarge,
                color = AetherColors.Primary,
                modifier = Modifier.testTag("contacts-friend-requests-header"),
            )
            pendingRequests.forEach { request ->
                GlassCard(modifier = Modifier.testTag("friend-request-${request.id}")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(text = request.fromUser.displayName, style = MaterialTheme.typography.titleMedium, color = AetherColors.OnSurface)
                            Text(text = request.fromUser.avatarText, style = MaterialTheme.typography.bodyMedium, color = AetherColors.OnSurfaceVariant)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PillAction(label = appLanguage.pick("Accept", "接受")) {
                                val token = container.sessionStore.token ?: return@PillAction
                                scope.launch {
                                    val endpoint = container.resolveImHttpEndpoint()
                                    try {
                                        container.imBackendClient.acceptFriendRequest(endpoint.baseUrl, token, request.id)
                                        container.messagingRepository.refreshBootstrap()
                                        pendingRequests = pendingRequests.filter { it.id != request.id }
                                    } catch (_: Exception) { }
                                }
                            }
                            PillAction(label = appLanguage.pick("Reject", "拒绝")) {
                                val token = container.sessionStore.token ?: return@PillAction
                                scope.launch {
                                    val endpoint = container.resolveImHttpEndpoint()
                                    try {
                                        container.imBackendClient.rejectFriendRequest(endpoint.baseUrl, token, request.id)
                                        container.messagingRepository.refreshBootstrap()
                                        pendingRequests = pendingRequests.filter { it.id != request.id }
                                    } catch (_: Exception) { }
                                }
                            }
                        }
                    }
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("contacts-list"),
        ) {
            items(uiState.contacts, key = { it.id }) { contact ->
                ContactRow(contact = contact, onClick = { onOpenContact(contact) })
            }
        }
    }
}

@Composable
private fun ContactRow(contact: Contact, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.testTag("contact-row-${contact.id}").clickable(onClick = onClick)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(AetherColors.Secondary.copy(alpha = 0.14f), CircleShape)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = contact.avatarText, color = AetherColors.Secondary, style = MaterialTheme.typography.titleLarge)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = contact.nickname, style = MaterialTheme.typography.titleLarge, color = AetherColors.OnSurface)
                Text(text = contact.title, style = MaterialTheme.typography.bodyLarge, color = AetherColors.OnSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .background(if (contact.isOnline) AetherColors.Success else AetherColors.OutlineVariant, CircleShape)
                    .padding(6.dp)
            )
        }
    }
}

