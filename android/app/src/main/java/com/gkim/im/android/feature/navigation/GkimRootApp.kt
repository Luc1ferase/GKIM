package com.gkim.im.android.feature.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.PeopleAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gkim.im.android.BuildConfig
import com.gkim.im.android.GkimApplication
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.ContentPolicyCopy
import com.gkim.im.android.core.designsystem.GkimTheme
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.core.media.MediaPickerControllerFactory
import com.gkim.im.android.core.model.AppThemeMode
import com.gkim.im.android.data.remote.im.resolveImHttpEndpoint
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.feature.auth.LoginRoute
import com.gkim.im.android.feature.auth.RegisterRoute
import com.gkim.im.android.feature.bootstrap.BootstrapAcknowledgmentDecision
import com.gkim.im.android.feature.bootstrap.BootstrapAcknowledgmentGate
import com.gkim.im.android.feature.bootstrap.BootstrapAcknowledgmentSnapshot
import com.gkim.im.android.feature.chat.ChatRoute
import com.gkim.im.android.feature.contacts.ContactsRoute
import com.gkim.im.android.feature.messages.MessagesRoute
import com.gkim.im.android.feature.settings.ContentPolicyAcknowledgmentRoute
import kotlinx.coroutines.flow.first
import com.gkim.im.android.feature.qr.QrResultRoutePattern
import com.gkim.im.android.feature.qr.QrScanResultRoute
import com.gkim.im.android.feature.qr.QrScanRoute
import com.gkim.im.android.feature.qr.QrScanRoutePath
import com.gkim.im.android.feature.qr.QrScannerControllerFactory
import com.gkim.im.android.feature.qr.qrResultRoute
import com.gkim.im.android.feature.settings.SettingsRoute
import com.gkim.im.android.feature.shared.AppScaffold
import com.gkim.im.android.feature.social.UserSearchRoute
import com.gkim.im.android.feature.tavern.CharacterDetailRoute
import com.gkim.im.android.feature.tavern.CharacterEditorRoute
import com.gkim.im.android.feature.tavern.TavernRoute

private data class RootDestination(val route: String, val label: String, val icon: @Composable () -> Unit)
enum class RootAuthStart {
    Authenticated,
    Unauthenticated,
}

private enum class RootAuthState {
    Loading,
    Authenticated,
    Unauthenticated,
    RequiresAcknowledgment,
}

@Composable
fun GkimRootApp(
    container: AppContainer? = null,
    navController: NavHostController? = null,
    mediaPickerControllerFactory: MediaPickerControllerFactory? = null,
    qrScannerControllerFactory: QrScannerControllerFactory? = null,
    initialAuthStart: RootAuthStart = RootAuthStart.Unauthenticated,
) {
    val resolvedContainer = container ?: (LocalContext.current.applicationContext as GkimApplication).container
    val resolvedNavController = navController ?: rememberNavController()
    val appLanguage by resolvedContainer.preferencesStore.appLanguage.collectAsStateWithLifecycle(initialValue = com.gkim.im.android.core.model.AppLanguage.Chinese)
    val appThemeMode by resolvedContainer.preferencesStore.appThemeMode.collectAsStateWithLifecycle(initialValue = AppThemeMode.Light)
    var authState by rememberSaveable { mutableStateOf(RootAuthState.Loading) }

    LaunchedEffect(initialAuthStart) {
        authState = when (initialAuthStart) {
            RootAuthStart.Authenticated -> RootAuthState.Authenticated
            RootAuthStart.Unauthenticated -> {
                val storedToken = resolvedContainer.sessionStore.token
                if (storedToken.isNullOrBlank()) {
                    RootAuthState.Unauthenticated
                } else {
                    val endpoint = resolvedContainer.resolveImHttpEndpoint()
                    try {
                        resolvedContainer.imBackendClient.loadBootstrap(endpoint.baseUrl, storedToken)
                        resolvedContainer.sessionStore.baseUrl = endpoint.baseUrl
                        RootAuthState.Authenticated
                    } catch (_: Exception) {
                        resolvedContainer.sessionStore.clear()
                        RootAuthState.Unauthenticated
                    }
                }
            }
        }
    }

    LaunchedEffect(authState) {
        if (authState != RootAuthState.Authenticated) return@LaunchedEffect
        val backendSnapshot = try {
            val baseUrl = resolvedContainer.sessionStore.baseUrl.orEmpty()
            val token = resolvedContainer.sessionStore.token.orEmpty()
            if (baseUrl.isBlank() || token.isBlank()) {
                BootstrapAcknowledgmentSnapshot.Unknown
            } else {
                val dto = resolvedContainer.imBackendClient.getContentPolicyAcknowledgment(baseUrl, token)
                BootstrapAcknowledgmentSnapshot.Known(accepted = dto.accepted, version = dto.version)
            }
        } catch (_: Exception) {
            BootstrapAcknowledgmentSnapshot.Unknown
        }
        val localMillis = resolvedContainer.preferencesStore.contentPolicyAcknowledgedAtMillis.first()
        val localVersion = resolvedContainer.preferencesStore.contentPolicyAcknowledgedVersion.first()
        val decision = BootstrapAcknowledgmentGate.decide(
            isDebugBuild = BuildConfig.DEBUG,
            backendSnapshot = backendSnapshot,
            localAcceptedAtMillis = localMillis,
            localAcceptedVersion = localVersion,
            currentVersion = ContentPolicyCopy.currentVersion,
        )
        if (decision == BootstrapAcknowledgmentDecision.RequireAcknowledgment) {
            authState = RootAuthState.RequiresAcknowledgment
        }
    }

    GkimTheme(darkTheme = appThemeMode != AppThemeMode.Light) {
        CompositionLocalProvider(LocalAppLanguage provides appLanguage) {
            when (authState) {
                RootAuthState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("root-auth-loading"),
                )

                RootAuthState.RequiresAcknowledgment -> {
                    ContentPolicyAcknowledgmentRoute(
                        container = resolvedContainer,
                        onAccepted = { authState = RootAuthState.Authenticated },
                        onBack = { authState = RootAuthState.Authenticated },
                    )
                }

                RootAuthState.Unauthenticated -> {
                    val authNavController = rememberNavController()
                    NavHost(navController = authNavController, startDestination = "welcome") {
                        composable("welcome") {
                            WelcomeRoute(
                                onLogin = { authNavController.navigate("login") },
                                onRegister = { authNavController.navigate("register") },
                            )
                        }
                        composable("login") {
                            LoginRoute(
                                container = resolvedContainer,
                                onLoggedIn = { authState = RootAuthState.Authenticated },
                                onBack = { authNavController.popBackStack() },
                            )
                        }
                        composable("register") {
                            RegisterRoute(
                                container = resolvedContainer,
                                onRegistered = { authState = RootAuthState.Authenticated },
                                onBack = { authNavController.popBackStack() },
                            )
                        }
                    }
                }

                RootAuthState.Authenticated -> {
                    AppScaffold(
                        modifier = Modifier.testTag("gkim-theme-${appThemeMode.name}"),
                        bottomBar = { RootBottomBar(resolvedNavController) },
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                        ) {
                            NavHost(navController = resolvedNavController, startDestination = "messages") {
                                composable("messages") { MessagesRoute(resolvedNavController, resolvedContainer) }
                                composable("contacts") { ContactsRoute(resolvedNavController, resolvedContainer) }
                                composable("space") { TavernRoute(resolvedNavController, resolvedContainer) }
                                composable("tavern/detail/{characterId}") { backStackEntry ->
                                    CharacterDetailRoute(
                                        navController = resolvedNavController,
                                        container = resolvedContainer,
                                        characterId = backStackEntry.arguments?.getString("characterId").orEmpty(),
                                    )
                                }
                                composable("tavern/editor?mode={mode}&id={id}") { backStackEntry ->
                                    CharacterEditorRoute(
                                        navController = resolvedNavController,
                                        container = resolvedContainer,
                                        mode = backStackEntry.arguments?.getString("mode").orEmpty(),
                                        characterId = backStackEntry.arguments?.getString("id")?.takeIf { it.isNotBlank() },
                                    )
                                }
                                composable("tavern/import-preview") {
                                    com.gkim.im.android.feature.tavern.ImportCardPreviewRoute(
                                        navController = resolvedNavController,
                                        container = resolvedContainer,
                                    )
                                }
                                composable("chat/{conversationId}") { backStackEntry ->
                                    ChatRoute(
                                        navController = resolvedNavController,
                                        container = resolvedContainer,
                                        conversationId = backStackEntry.arguments?.getString("conversationId").orEmpty(),
                                        mediaPickerControllerFactory = mediaPickerControllerFactory,
                                    )
                                }
                                composable("settings?worldinfoLorebookId={worldinfoLorebookId}") { backStackEntry ->
                                    SettingsRoute(
                                        navController = resolvedNavController,
                                        container = resolvedContainer,
                                        initialWorldInfoLorebookId = backStackEntry.arguments
                                            ?.getString("worldinfoLorebookId")
                                            ?.takeIf { it.isNotBlank() },
                                    )
                                }
                                composable("user-search") {
                                    UserSearchRoute(
                                        container = resolvedContainer,
                                        onBack = { resolvedNavController.popBackStack() },
                                    )
                                }
                                composable(QrScanRoutePath) {
                                    QrScanRoute(
                                        onBack = { resolvedNavController.popBackStack() },
                                        onPayloadScanned = { payload ->
                                            resolvedNavController.navigate(qrResultRoute(payload))
                                        },
                                        qrScannerControllerFactory = qrScannerControllerFactory,
                                    )
                                }
                                composable(QrResultRoutePattern) { backStackEntry ->
                                    QrScanResultRoute(
                                        payload = Uri.decode(backStackEntry.arguments?.getString("payload").orEmpty()),
                                        onBack = { resolvedNavController.popBackStack() },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RootBottomBar(navController: NavHostController) {
    val appLanguage = LocalAppLanguage.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val primaryDestinations = listOf(
        RootDestination("messages", appLanguage.pick("Messages", "消息")) { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null) },
        RootDestination("contacts", appLanguage.pick("Contacts", "联系人")) { Icon(Icons.Outlined.PeopleAlt, contentDescription = null) },
        RootDestination("space", appLanguage.pick("Tavern", "酒馆")) { Icon(Icons.Outlined.AutoAwesome, contentDescription = null) },
    )
    val showBottomBar = primaryDestinations.any { it.route == currentRoute }
    if (!showBottomBar) return

    NavigationBar(containerColor = AetherColors.SurfaceContainerLow, modifier = Modifier.testTag("bottom-nav")) {
        primaryDestinations.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = destination.icon,
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AetherColors.Primary,
                    selectedTextColor = AetherColors.Primary,
                    indicatorColor = AetherColors.SurfaceContainerHigh,
                    unselectedIconColor = AetherColors.OnSurfaceVariant,
                    unselectedTextColor = AetherColors.OnSurfaceVariant,
                ),
            )
        }
    }
}
