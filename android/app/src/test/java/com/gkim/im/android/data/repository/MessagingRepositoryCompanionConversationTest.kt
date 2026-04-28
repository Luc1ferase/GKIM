package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.Contact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MessagingRepositoryCompanionConversationTest {
    @Test
    fun `ensureConversation with companionCardId populates the marker`() {
        val repository = InMemoryMessagingRepository(emptyList())

        val conversation = repository.ensureConversation(
            contact = Contact(
                id = "daylight-listener",
                nickname = "晴光抚慰者",
                title = "温柔倾听者",
                avatarText = "DL",
                addedAt = "2026-04-24T00:00:00Z",
                isOnline = true,
            ),
            companionCardId = "daylight-listener",
        )

        assertEquals("daylight-listener", conversation.companionCardId)
    }

    @Test
    fun `ensureConversation without companionCardId defaults to null for peer-IM paths`() {
        val repository = InMemoryMessagingRepository(emptyList())

        val conversation = repository.ensureConversation(
            Contact(
                id = "peer-contact",
                nickname = "Friend",
                title = "",
                avatarText = "FR",
                addedAt = "2026-04-24T00:00:00Z",
                isOnline = true,
            ),
        )

        assertNull(conversation.companionCardId)
    }

    @Test
    fun `ensureConversation preserves existing companionCardId when a null refresh is issued`() {
        val repository = InMemoryMessagingRepository(emptyList())
        val contact = Contact(
            id = "daylight-listener",
            nickname = "晴光抚慰者",
            title = "温柔倾听者",
            avatarText = "DL",
            addedAt = "2026-04-24T00:00:00Z",
            isOnline = true,
        )

        repository.ensureConversation(contact = contact, companionCardId = "daylight-listener")
        val refreshed = repository.ensureConversation(contact = contact)

        assertEquals("daylight-listener", refreshed.companionCardId)
    }

    @Test
    fun `ensureConversation updates companionCardId when a non-null value is passed on refresh`() {
        val repository = InMemoryMessagingRepository(emptyList())
        val contact = Contact(
            id = "daylight-listener",
            nickname = "晴光抚慰者",
            title = "温柔倾听者",
            avatarText = "DL",
            addedAt = "2026-04-24T00:00:00Z",
            isOnline = true,
        )

        repository.ensureConversation(contact = contact, companionCardId = "card-a")
        val refreshed = repository.ensureConversation(contact = contact, companionCardId = "card-b")

        assertEquals("card-b", refreshed.companionCardId)
    }
}
