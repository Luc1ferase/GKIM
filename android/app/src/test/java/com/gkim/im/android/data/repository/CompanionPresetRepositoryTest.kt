package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.Preset
import com.gkim.im.android.core.model.PresetParams
import com.gkim.im.android.core.model.PresetTemplate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionPresetRepositoryTest {

    private fun makePreset(
        id: String,
        english: String = "Name",
        chinese: String = "名字",
        description: String = "Description",
        descriptionCn: String = "描述",
        isBuiltIn: Boolean = false,
        isActive: Boolean = false,
        template: PresetTemplate = PresetTemplate(),
        params: PresetParams = PresetParams(),
    ): Preset = Preset(
        id = id,
        displayName = LocalizedText(english = english, chinese = chinese),
        description = LocalizedText(english = description, chinese = descriptionCn),
        template = template,
        params = params,
        isBuiltIn = isBuiltIn,
        isActive = isActive,
    )

    private fun repositoryWithSeed(
        initial: List<Preset>,
        idStart: Int = 1,
    ): DefaultCompanionPresetRepository {
        var counter = idStart
        return DefaultCompanionPresetRepository(
            initialPresets = initial,
            idGenerator = { "preset-generated-${counter++}" },
            clock = { 100L },
        )
    }

    @Test
    fun `activate flips active flag exclusively across the library`() = runBlocking {
        val built = makePreset("built", isBuiltIn = true, isActive = true)
        val other = makePreset("other")
        val repo = repositoryWithSeed(listOf(built, other))

        val result = repo.activate("other")

        assertTrue(result is CompanionPresetMutationResult.Success)
        val presets = repo.observePresets().first()
        val actives = presets.filter { it.isActive }
        assertEquals(1, actives.size)
        assertEquals("other", actives.single().id)
        assertFalse(presets.first { it.id == "built" }.isActive)
    }

    @Test
    fun `activate unknown preset returns Rejected UnknownPreset`() = runBlocking {
        val built = makePreset("built", isBuiltIn = true, isActive = true)
        val repo = repositoryWithSeed(listOf(built))

        val result = repo.activate("missing")

        assertEquals(
            CompanionPresetMutationResult.Rejected(
                CompanionPresetMutationResult.RejectionReason.UnknownPreset,
            ),
            result,
        )
    }

    @Test
    fun `delete of built-in preset returns Rejected BuiltInPresetImmutable`() = runBlocking {
        val built = makePreset("built", isBuiltIn = true, isActive = true)
        val repo = repositoryWithSeed(listOf(built))

        val result = repo.delete("built")

        assertEquals(
            CompanionPresetMutationResult.Rejected(
                CompanionPresetMutationResult.RejectionReason.BuiltInPresetImmutable,
            ),
            result,
        )
        assertEquals(1, repo.observePresets().first().size)
    }

    @Test
    fun `delete of currently active preset returns Rejected ActivePresetNotDeletable`() =
        runBlocking {
            val built = makePreset("built", isBuiltIn = true, isActive = false)
            val userActive = makePreset("user-active", isActive = true)
            val repo = repositoryWithSeed(listOf(built, userActive))

            val result = repo.delete("user-active")

            assertEquals(
                CompanionPresetMutationResult.Rejected(
                    CompanionPresetMutationResult.RejectionReason.ActivePresetNotDeletable,
                ),
                result,
            )
            assertEquals(2, repo.observePresets().first().size)
        }

    @Test
    fun `delete of inactive user-owned preset succeeds and removes it from library`() =
        runBlocking {
            val built = makePreset("built", isBuiltIn = true, isActive = true)
            val userInactive = makePreset("user-inactive")
            val repo = repositoryWithSeed(listOf(built, userInactive))

            val result = repo.delete("user-inactive")

            assertTrue(result is CompanionPresetMutationResult.Success)
            val presets = repo.observePresets().first()
            assertEquals(1, presets.size)
            assertEquals("built", presets.single().id)
        }

    @Test
    fun `duplicate produces user-owned preset with bilingual copy suffix`() = runBlocking {
        val source = makePreset(
            id = "src",
            english = "Neutral Default",
            chinese = "中性默认",
            description = "Baseline",
            descriptionCn = "基线",
            isBuiltIn = true,
            isActive = true,
            template = PresetTemplate(
                systemPrefix = LocalizedText("you are a companion", "你是一个伙伴"),
            ),
            params = PresetParams(temperature = 0.8, topP = null, maxReplyTokens = 512),
        )
        val repo = repositoryWithSeed(listOf(source))

        val result = repo.duplicate("src")

        assertTrue(result is CompanionPresetMutationResult.Success)
        val copy = (result as CompanionPresetMutationResult.Success).preset
        assertNotEquals("src", copy.id)
        assertFalse(copy.isBuiltIn)
        assertFalse(copy.isActive)
        assertEquals("Neutral Default (copy)", copy.displayName.english)
        assertEquals("中性默认（副本）", copy.displayName.chinese)
        assertEquals("Baseline", copy.description.english)
        assertEquals("基线", copy.description.chinese)
        assertEquals("you are a companion", copy.template.systemPrefix.english)
        assertEquals(0.8, copy.params.temperature!!, 1e-9)
        assertEquals(512, copy.params.maxReplyTokens)

        val presets = repo.observePresets().first()
        assertEquals(2, presets.size)
        assertEquals(
            1,
            presets.count { it.displayName.english == "Neutral Default (copy)" },
        )
    }

    @Test
    fun `duplicate of unknown preset returns Rejected UnknownPreset`() = runBlocking {
        val built = makePreset("built", isBuiltIn = true, isActive = true)
        val repo = repositoryWithSeed(listOf(built))

        val result = repo.duplicate("missing")

        assertEquals(
            CompanionPresetMutationResult.Rejected(
                CompanionPresetMutationResult.RejectionReason.UnknownPreset,
            ),
            result,
        )
        assertEquals(1, repo.observePresets().first().size)
    }

    @Test
    fun `update rejects built-in preset as BuiltInPresetImmutable`() = runBlocking {
        val built = makePreset("built", isBuiltIn = true, isActive = true)
        val repo = repositoryWithSeed(listOf(built))

        val draft = built.copy(
            displayName = LocalizedText("Hacked", "被黑"),
        )
        val result = repo.update(draft)

        assertEquals(
            CompanionPresetMutationResult.Rejected(
                CompanionPresetMutationResult.RejectionReason.BuiltInPresetImmutable,
            ),
            result,
        )
        val stored = repo.observePresets().first().single()
        assertEquals("Name", stored.displayName.english)
    }

    @Test
    fun `update of unknown preset returns Rejected UnknownPreset`() = runBlocking {
        val built = makePreset("built", isBuiltIn = true, isActive = true)
        val repo = repositoryWithSeed(listOf(built))

        val draft = makePreset("missing")
        val result = repo.update(draft)

        assertEquals(
            CompanionPresetMutationResult.Rejected(
                CompanionPresetMutationResult.RejectionReason.UnknownPreset,
            ),
            result,
        )
    }

    @Test
    fun `create normalizes new user-owned preset with generated id and timestamps`() =
        runBlocking {
            val built = makePreset("built", isBuiltIn = true, isActive = true)
            val repo = repositoryWithSeed(listOf(built))

            val draft = makePreset(id = "", isBuiltIn = true, isActive = true)
            val result = repo.create(draft)

            assertTrue(result is CompanionPresetMutationResult.Success)
            val created = (result as CompanionPresetMutationResult.Success).preset
            assertTrue(created.id.startsWith("preset-generated-"))
            assertFalse(created.isBuiltIn)
            assertFalse(created.isActive)
            assertEquals(100L, created.createdAt)
            assertEquals(100L, created.updatedAt)
            assertEquals(2, repo.observePresets().first().size)
        }

    @Test
    fun `update on user preset preserves isBuiltIn isActive and createdAt but rewrites updatedAt`() =
        runBlocking {
            val user = Preset(
                id = "user",
                displayName = LocalizedText("Mine", "我的"),
                description = LocalizedText("Default", "默认"),
                isBuiltIn = false,
                isActive = true,
                createdAt = 1L,
                updatedAt = 1L,
            )
            val repo = repositoryWithSeed(listOf(user))

            val draft = user.copy(
                description = LocalizedText("New desc", "新描述"),
                isBuiltIn = true,
                isActive = false,
                createdAt = 999L,
                updatedAt = 999L,
            )
            val result = repo.update(draft)

            assertTrue(result is CompanionPresetMutationResult.Success)
            val updated = (result as CompanionPresetMutationResult.Success).preset
            assertEquals("New desc", updated.description.english)
            assertFalse(updated.isBuiltIn)
            assertTrue(updated.isActive)
            assertEquals(1L, updated.createdAt)
            assertEquals(100L, updated.updatedAt)
        }

    @Test
    fun `observeActivePreset emits active preset and null when none set`() = runBlocking {
        val built = makePreset("built", isBuiltIn = true, isActive = false)
        val repo = repositoryWithSeed(listOf(built))

        assertNull(repo.observeActivePreset().first())

        repo.activate("built")

        val active = repo.observeActivePreset().first()
        assertEquals("built", active?.id)
    }

    @Test
    fun `enforces single-active on snapshot ingest`() = runBlocking {
        val repo = repositoryWithSeed(emptyList())

        repo.setSnapshot(
            listOf(
                makePreset("a", isActive = true),
                makePreset("b", isActive = true),
                makePreset("c"),
            ),
        )

        val presets = repo.observePresets().first()
        val actives = presets.filter { it.isActive }
        assertEquals(1, actives.size)
        assertEquals("a", actives.single().id)
    }

    @Test
    fun `refresh is a no-op on default repository`() = runBlocking {
        val built = makePreset("built", isBuiltIn = true, isActive = true)
        val repo = repositoryWithSeed(listOf(built))

        val before = repo.observePresets().first()
        repo.refresh()
        val after = repo.observePresets().first()

        assertEquals(before, after)
    }
}
