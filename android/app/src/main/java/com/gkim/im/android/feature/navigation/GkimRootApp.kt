package com.gkim.im.android.feature.navigation

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
import com.gkim.im.android.GkimApplication
import com.gkim.im.android.core.media.MediaPickerControllerFactory
import com.gkim.im.android.core.designsystem.AetherColors
import com.gkim.im.android.core.designsystem.GkimTheme
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.core.model.AppThemeMode
import com.gkim.im.android.data.repository.AppContainer
import com.gkim.im.android.feature.chat.ChatRoute
import com.gkim.im.android.feature.auth.LoginRoute
import com.gkim.im.android.feature.auth.RegisterRoute
import com.gkim.im.android.feature.contacts.ContactsRoute
import com.gkim.im.android.feature.messages.MessagesRoute
import com.gkim.im.android.feature.settings.SettingsRoute
import com.gkim.im.android.feature.shared.AppScaffold
import com.gkim.im.android.feature.social.UserSearchRoute
import com.gkim.im.android.feature.space.SpaceRoute

private data class RootDestination(val route: String, val label: String, val icon: @Composable () -> Unit)
enum class RootAuthStart {
    Authenticated,
    Unauthenticated,
}

private enum class RootAuthState {
    Loading,
    Authenticated,
    Unauthenticated,
}

@Composable
fun GkimRootApp(
    container: AppContainer? = null,
    navController: NavHostController? = null,
    mediaPickerControllerFactory: MediaPickerControllerFactory? = null,
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
                    val baseUrl = resolvedContainer.sessionStore.baseUrl ?: "http://127.0.0.1:18080/"
                    try {
                        resolvedContainer.imBackendClient.loadBootstrap(baseUrl, storedToken)
                        RootAuthState.Authenticated
                    } catch (_: Exception) {
                        resolvedContainer.sessionStore.clear()
                        RootAuthState.Unauthenticated
                    }
                }
            }
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
                                composable("space") { SpaceRoute(resolvedNavController, resolvedContainer) }
                                composable("chat/{conversationId}") { backStackEntry ->
                                    ChatRoute(
                                        navController = resolvedNavController,
                                        container = resolvedContainer,
                                        conversationId = backStackEntry.arguments?.getString("conversationId").orEmpty(),
                                        mediaPickerControllerFactory = mediaPickerControllerFactory,
                                    )
                                }
                                composable("settings") { SettingsRoute(resolvedNavController, resolvedContainer) }
                                composable("user-search") {
                                    UserSearchRoute(
                                        container = resolvedContainer,
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
        RootDestination("space", appLanguage.pick("Space", "空间")) { Icon(Icons.Outlined.AutoAwesome, contentDescription = null) },
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
