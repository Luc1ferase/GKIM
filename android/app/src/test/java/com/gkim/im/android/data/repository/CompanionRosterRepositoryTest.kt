package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.CompanionCharacterCard
import com.gkim.im.android.core.model.CompanionCharacterSource
import com.gkim.im.android.core.model.LocalizedText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.JsonObject

@OptIn(ExperimentalCoroutinesApi::class)
class CompanionRosterRepositoryTest {
    @Test
    fun `repository exposes preset roster and empty owned cards on first load`() = runTest {
        val repository = DefaultCompanionRosterRepository(
            presetCharacters = seedPresetCharacters,
            drawPool = seedDrawPoolCharacters,
        )

        assertEquals(seedPresetCharacters.map { it.id }, repository.presetCharacters.value.map { it.id })
        assertTrue(repository.ownedCharacters.value.isEmpty())
        assertTrue(repository.userCharacters.value.isEmpty())
        assertEquals("architect-oracle", repository.activeCharacterId.value)
    }

    @Test
    fun `draw adds new character to owned roster and reports explicit result`() = runTest {
        val repository = DefaultCompanionRosterRepository(
            presetCharacters = seedPresetCharacters,
            drawPool = seedDrawPoolCharacters,
        )

        val result = repository.drawCharacter()

        assertNotNull(repository.lastDrawResult.value)
        assertTrue(result.wasNew)
        assertTrue(repository.ownedCharacters.value.any { it.id == result.card.id })
    }

    @Test
    fun `activating character updates active selection for preset and drawn cards`() = runTest {
        val repository = DefaultCompanionRosterRepository(
            presetCharacters = seedPresetCharacters,
            drawPool = seedDrawPoolCharacters,
        )

        repository.activateCharacter("sunlit-almoner")
        assertEquals("sunlit-almoner", repository.activeCharacterId.value)

        val drawn = repository.drawCharacter()
        repository.activateCharacter(drawn.card.id)

        assertEquals(drawn.card.id, repository.activeCharacterId.value)
        assertFalse(repository.presetCharacters.value.any { it.id == drawn.card.id })
    }

    @Test
    fun `upsert creates user authored card with generated id`() {
        val repository = DefaultCompanionRosterRepository(
            presetCharacters = seedPresetCharacters,
            drawPool = seedDrawPoolCharacters,
            idGenerator = { "user-custom-1" },
        )

        val result = repository.upsertUserCharacter(blankDraft())

        assertTrue(result is CompanionCardMutationResult.Success)
        result as CompanionCardMutationResult.Success
        assertEquals("user-custom-1", result.card.id)
        assertEquals(CompanionCharacterSource.UserAuthored, result.card.source)
        assertTrue(repository.userCharacters.value.any { it.id == "user-custom-1" })
    }

    @Test
    fun `upsert rejects preset mutation`() {
        val repository = DefaultCompanionRosterRepository(
            presetCharacters = seedPresetCharacters,
            drawPool = seedDrawPoolCharacters,
        )

        val result = repository.upsertUserCharacter(seedPresetCharacters.first())

        assertEquals(
            CompanionCardMutationResult.Rejected(CompanionCardMutationResult.RejectionReason.PresetImmutable),
            result,
        )
    }

    @Test
    fun `upsert updates drawn card but delete rejects it`() = runTest {
        val repository = DefaultCompanionRosterRepository(
            presetCharacters = seedPresetCharacters,
            drawPool = seedDrawPoolCharacters,
        )
        val drawn = repository.drawCharacter().card

        val updated = drawn.copy(creatorNotes = "edited")
        val updateResult = repository.upsertUserCharacter(updated)
        val deleteResult = repository.deleteUserCharacter(drawn.id)

        assertTrue(updateResult is CompanionCardMutationResult.Success)
        assertEquals("edited", repository.characterById(drawn.id)?.creatorNotes)
        assertEquals(
            CompanionCardMutationResult.Rejected(CompanionCardMutationResult.RejectionReason.DrawnCardNotDeletable),
            deleteResult,
        )
    }

    @Test
    fun `delete removes user authored card`() {
        val repository = DefaultCompanionRosterRepository(
            presetCharacters = seedPresetCharacters,
            drawPool = seedDrawPoolCharacters,
            idGenerator = { "user-custom-2" },
        )
        val created = (repository.upsertUserCharacter(blankDraft()) as CompanionCardMutationResult.Success).card

        val deleteResult = repository.deleteUserCharacter(created.id)

        assertTrue(deleteResult is CompanionCardMutationResult.Success)
        assertTrue(repository.userCharacters.value.none { it.id == created.id })
    }

    private fun blankDraft(): CompanionCharacterCard = CompanionCharacterCard(
        id = "",
        displayName = LocalizedText("Custom One", "自建一号"),
        roleLabel = LocalizedText("Tester", "测试角色"),
        summary = LocalizedText("Summary", "摘要"),
        firstMes = LocalizedText("Hello", "你好"),
        alternateGreetings = emptyList(),
        systemPrompt = LocalizedText("System", "系统"),
        personality = LocalizedText("Calm", "冷静"),
        scenario = LocalizedText("Tavern", "酒馆"),
        exampleDialogue = LocalizedText("A: hi", "甲：你好"),
        tags = listOf("custom"),
        creator = "test",
        creatorNotes = "note",
        characterVersion = "1.0.0",
        avatarText = "C1",
        accent = com.gkim.im.android.core.model.AccentTone.Primary,
        source = CompanionCharacterSource.UserAuthored,
        extensions = JsonObject(emptyMap()),
    )
}
