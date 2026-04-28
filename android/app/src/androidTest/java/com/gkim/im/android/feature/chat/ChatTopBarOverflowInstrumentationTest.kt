package com.gkim.im.android.feature.chat

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.Conversation
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * §1.2 verification — the chat top-bar overflow must surface BOTH `Export chat` /
 * `导出对话` AND `Settings` / `设置` items, in both English and Chinese app
 * languages, when the active conversation is a companion conversation
 * (companionCardId != null on the seeded Conversation).
 */
@RunWith(AndroidJUnit4::class)
class ChatTopBarOverflowInstrumentationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val companionConversation = Conversation(
        id = "conversation-overflow-1",
        contactId = "contact-1",
        contactName = "Aether",
        contactTitle = "AIGC-enabled conversation surface.",
        avatarText = "AE",
        lastMessage = "",
        lastTimestamp = "",
        unreadCount = 0,
        isOnline = false,
        messages = emptyList(),
        companionCardId = "card-1",
    )

    @Test
    fun englishOverflowExposesExportAndSettings() {
        var settingsClicks = 0
        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.English) {
                ChatTopBar(
                    conversation = companionConversation,
                    activePersona = null,
                    onBack = {},
                    onPersonaPillTap = {},
                    onOpenExportDialog = {},
                    onOpenSettings = { settingsClicks++ },
                )
            }
        }

        composeRule.onNodeWithTag("chat-top-overflow-trigger").performClick()
        composeRule.onNodeWithTag("chat-top-overflow-export-text").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-top-overflow-settings-text").assertIsDisplayed()

        composeRule.onNodeWithTag("chat-top-overflow-settings").performClick()
        assertEquals(1, settingsClicks)
    }

    @Test
    fun chineseOverflowExposesExportAndSettings() {
        composeRule.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.Chinese) {
                ChatTopBar(
                    conversation = companionConversation,
                    activePersona = null,
                    onBack = {},
                    onPersonaPillTap = {},
                    onOpenExportDialog = {},
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithTag("chat-top-overflow-trigger").performClick()
        composeRule.onNodeWithTag("chat-top-overflow-export-text").assertIsDisplayed()
        composeRule.onNodeWithTag("chat-top-overflow-settings-text").assertIsDisplayed()
    }
}
