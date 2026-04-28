package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.UserPersona
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserPersonaRepositoryTest {

    private fun makePersona(
        id: String,
        english: String = "Name",
        chinese: String = "名字",
        description: String = "Description",
        descriptionCn: String = "描述",
        isBuiltIn: Boolean = false,
        isActive: Boolean = false,
    ): UserPersona = UserPersona(
        id = id,
        displayName = LocalizedText(english = english, chinese = chinese),
        description = LocalizedText(english = description, chinese = descriptionCn),
        isBuiltIn = isBuiltIn,
        isActive = isActive,
    )

    private fun repositoryWithSeed(
        initial: List<UserPersona>,
        idStart: Int = 1,
    ): DefaultUserPersonaRepository {
        var counter = idStart
        return DefaultUserPersonaRepository(
            initialPersonas = initial,
            idGenerator = { "persona-generated-${counter++}" },
            clock = { 100L },
        )
    }

    @Test
    fun `activate flips active flag exclusively across the library`() = runBlocking {
        val built = makePersona("built", isBuiltIn = true, isActive = true)
        val other = makePersona("other")
        val repo = repositoryWithSeed(listOf(built, other))

        val result = repo.activate("other")

        assertTrue(result is UserPersonaMutationResult.Success)
        val personas = repo.observePersonas().first()
        val actives = personas.filter { it.isActive }
        assertEquals(1, actives.size)
        assertEquals("other", actives.single().id)
        assertFalse(personas.first { it.id == "built" }.isActive)
    }

    @Test
    fun `activate unknown persona returns Rejected UnknownPersona`() = runBlocking {
        val built = makePersona("built", isBuiltIn = true, isActive = true)
        val repo = repositoryWithSeed(listOf(built))

        val result = repo.activate("missing")

        assertEquals(
            UserPersonaMutationResult.Rejected(
                UserPersonaMutationResult.RejectionReason.UnknownPersona,
            ),
            result,
        )
    }

    @Test
    fun `delete of built-in persona returns Rejected BuiltInPersonaImmutable`() = runBlocking {
        val built = makePersona("built", isBuiltIn = true, isActive = true)
        val repo = repositoryWithSeed(listOf(built))

        val result = repo.delete("built")

        assertEquals(
            UserPersonaMutationResult.Rejected(
                UserPersonaMutationResult.RejectionReason.BuiltInPersonaImmutable,
            ),
            result,
        )
        assertEquals(1, repo.observePersonas().first().size)
    }

    @Test
    fun `delete of currently active persona returns Rejected ActivePersonaNotDeletable`() =
        runBlocking {
            val built = makePersona("built", isBuiltIn = true, isActive = false)
            val userActive = makePersona("user-active", isActive = true)
            val repo = repositoryWithSeed(listOf(built, userActive))

            val result = repo.delete("user-active")

            assertEquals(
                UserPersonaMutationResult.Rejected(
                    UserPersonaMutationResult.RejectionReason.ActivePersonaNotDeletable,
                ),
                result,
            )
            assertEquals(2, repo.observePersonas().first().size)
        }

    @Test
    fun `delete of inactive user-owned persona succeeds and removes it from library`() =
        runBlocking {
            val built = makePersona("built", isBuiltIn = true, isActive = true)
            val userInactive = makePersona("user-inactive")
            val repo = repositoryWithSeed(listOf(built, userInactive))

            val result = repo.delete("user-inactive")

            assertTrue(result is UserPersonaMutationResult.Success)
            val personas = repo.observePersonas().first()
            assertEquals(1, personas.size)
            assertEquals("built", personas.single().id)
        }

    @Test
    fun `duplicate produces user-owned persona with bilingual copy suffix`() = runBlocking {
        val source = makePersona(
            id = "src",
            english = "Aria",
            chinese = "阿莉亚",
            description = "Guide",
            descriptionCn = "向导",
            isBuiltIn = true,
            isActive = true,
        )
        val repo = repositoryWithSeed(listOf(source))

        val result = repo.duplicate("src")

        assertTrue(result is UserPersonaMutationResult.Success)
        val copy = (result as UserPersonaMutationResult.Success).persona
        assertNotEquals("src", copy.id)
        assertFalse(copy.isBuiltIn)
        assertFalse(copy.isActive)
        assertEquals("Aria (copy)", copy.displayName.english)
        assertEquals("阿莉亚（副本）", copy.displayName.chinese)
        assertEquals("Guide", copy.description.english)
        assertEquals("向导", copy.description.chinese)

        val personas = repo.observePersonas().first()
        assertEquals(2, personas.size)
        assertEquals(
            1,
            personas.count { it.displayName.english == "Aria (copy)" },
        )
    }

    @Test
    fun `duplicate of unknown persona returns Rejected UnknownPersona`() = runBlocking {
        val built = makePersona("built", isBuiltIn = true, isActive = true)
        val repo = repositoryWithSeed(listOf(built))

        val result = repo.duplicate("missing")

        assertEquals(
            UserPersonaMutationResult.Rejected(
                UserPersonaMutationResult.RejectionReason.UnknownPersona,
            ),
            result,
        )
        assertEquals(1, repo.observePersonas().first().size)
    }

    @Test
    fun `create normalizes new user-owned persona with generated id and timestamps`() =
        runBlocking {
            val built = makePersona("built", isBuiltIn = true, isActive = true)
            val repo = repositoryWithSeed(listOf(built))

            val draft = makePersona(id = "", isBuiltIn = true, isActive = true)
            val result = repo.create(draft)

            assertTrue(result is UserPersonaMutationResult.Success)
            val created = (result as UserPersonaMutationResult.Success).persona
            assertTrue(created.id.startsWith("persona-generated-"))
            assertFalse(created.isBuiltIn)
            assertFalse(created.isActive)
            assertEquals(100L, created.createdAt)
            assertEquals(100L, created.updatedAt)
            assertEquals(2, repo.observePersonas().first().size)
        }

    @Test
    fun `update preserves isBuiltIn isActive and createdAt but rewrites updatedAt`() =
        runBlocking {
            val built = UserPersona(
                id = "built",
                displayName = LocalizedText("You", "你"),
                description = LocalizedText("Default", "默认"),
                isBuiltIn = true,
                isActive = true,
                createdAt = 1L,
                updatedAt = 1L,
            )
            val repo = repositoryWithSeed(listOf(built))

            val draft = built.copy(
                description = LocalizedText("New desc", "新描述"),
                isBuiltIn = false,
                isActive = false,
                createdAt = 999L,
                updatedAt = 999L,
            )
            val result = repo.update(draft)

            assertTrue(result is UserPersonaMutationResult.Success)
            val updated = (result as UserPersonaMutationResult.Success).persona
            assertEquals("New desc", updated.description.english)
            assertTrue(updated.isBuiltIn)
            assertTrue(updated.isActive)
            assertEquals(1L, updated.createdAt)
            assertEquals(100L, updated.updatedAt)
        }

    @Test
    fun `observeActivePersona emits active persona and null when none set`() = runBlocking {
        val built = makePersona("built", isBuiltIn = true, isActive = false)
        val repo = repositoryWithSeed(listOf(built))

        assertNull(repo.observeActivePersona().first())

        repo.activate("built")

        val active = repo.observeActivePersona().first()
        assertEquals("built", active?.id)
    }

    @Test
    fun `enforces single-active on snapshot ingest`() = runBlocking {
        val repo = repositoryWithSeed(emptyList())

        repo.setSnapshot(
            listOf(
                makePersona("a", isActive = true),
                makePersona("b", isActive = true),
                makePersona("c"),
            ),
        )

        val personas = repo.observePersonas().first()
        val actives = personas.filter { it.isActive }
        assertEquals(1, actives.size)
        assertEquals("a", actives.single().id)
    }
}
