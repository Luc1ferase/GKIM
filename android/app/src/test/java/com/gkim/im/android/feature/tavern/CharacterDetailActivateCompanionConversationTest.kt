package com.gkim.im.android.feature.tavern

import com.gkim.im.android.core.model.Contact
import com.gkim.im.android.data.repository.InMemoryMessagingRepository
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * §4.1 verification — the tavern activate handler (both [CharacterDetailRoute.onActivate] and
 * [TavernRoute.onActivateCharacter]) must pass the activated card's id through
 * [MessagingRepository.ensureConversation] so the resulting [Conversation] carries the
 * [companionCardId] marker that §3 dispatches on.
 *
 * The handlers themselves are Compose lambdas captured inside `@Composable` functions; this
 * test exercises the exact call chain they perform (contact construction + ensureConversation
 * with the card id), without standing up a Compose tree.
 */
class CharacterDetailActivateCompanionConversationTest {
    @Test
    fun `activate handler chain populates companionCardId from the activated card`() {
        val repository = InMemoryMessagingRepository(emptyList())
        val activatedCardId = "daylight-listener"

        val contactFromCard = Contact(
            id = activatedCardId,
            nickname = "晴光抚慰者",
            title = "温柔倾听者",
            avatarText = "DL",
            addedAt = "2026-04-24T00:00:00Z",
            isOnline = true,
        )

        val conversation = repository.ensureConversation(
            contact = contactFromCard,
            companionCardId = activatedCardId,
        )

        assertEquals(activatedCardId, conversation.companionCardId)
        assertEquals(activatedCardId, conversation.contactId)
    }

    @Test
    fun `re-activating the same card refreshes the conversation while keeping the marker`() {
        val repository = InMemoryMessagingRepository(emptyList())
        val activatedCardId = "daylight-listener"
        val initialContact = Contact(
            id = activatedCardId,
            nickname = "晴光抚慰者",
            title = "温柔倾听者",
            avatarText = "DL",
            addedAt = "2026-04-24T00:00:00Z",
            isOnline = true,
        )

        repository.ensureConversation(contact = initialContact, companionCardId = activatedCardId)
        val reactivated = repository.ensureConversation(
            contact = initialContact.copy(nickname = "Daylight Listener"),
            companionCardId = activatedCardId,
        )

        assertEquals(activatedCardId, reactivated.companionCardId)
        assertEquals("Daylight Listener", reactivated.contactName)
    }
}
