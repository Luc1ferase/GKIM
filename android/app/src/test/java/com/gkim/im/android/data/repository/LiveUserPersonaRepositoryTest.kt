package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.UserPersona
import com.gkim.im.android.data.remote.im.ActiveCompanionSelectionDto
import com.gkim.im.android.data.remote.im.AuthResponseDto
import com.gkim.im.android.data.remote.im.BootstrapBundleDto
import com.gkim.im.android.data.remote.im.CompanionRosterDto
import com.gkim.im.android.data.remote.im.DevSessionResponseDto
import com.gkim.im.android.data.remote.im.FriendRequestViewDto
import com.gkim.im.android.data.remote.im.ImBackendClient
import com.gkim.im.android.data.remote.im.LocalizedTextDto
import com.gkim.im.android.data.remote.im.MessageHistoryPageDto
import com.gkim.im.android.data.remote.im.SendDirectImageMessageRequestDto
import com.gkim.im.android.data.remote.im.SendDirectMessageResultDto
import com.gkim.im.android.data.remote.im.UserPersonaDto
import com.gkim.im.android.data.remote.im.UserPersonaListDto
import com.gkim.im.android.data.remote.im.UserSearchResultDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveUserPersonaRepositoryTest {

    private fun persona(
        id: String,
        en: String = "Name",
        cn: String = "名字",
        desc: String = "Desc",
        descCn: String = "描述",
        isBuiltIn: Boolean = false,
        isActive: Boolean = false,
    ): UserPersona = UserPersona(
        id = id,
        displayName = LocalizedText(en, cn),
        description = LocalizedText(desc, descCn),
        isBuiltIn = isBuiltIn,
        isActive = isActive,
    )

    private fun buildRepo(
        seed: List<UserPersona> = emptyList(),
        baseUrl: String? = "https://api.example.com/",
        token: String? = "session-token",
        backend: FakePersonaBackend = FakePersonaBackend(),
    ): Triple<LiveUserPersonaRepository, DefaultUserPersonaRepository, FakePersonaBackend> {
        val default = DefaultUserPersonaRepository(
            initialPersonas = seed,
            idGenerator = { "persona-new" },
            clock = { 100L },
        )
        val live = LiveUserPersonaRepository(
            default = default,
            backendClient = backend,
            baseUrlProvider = { baseUrl },
            tokenProvider = { token },
        )
        return Triple(live, default, backend)
    }

    @Test
    fun `refresh merges remote list with active persona id from bootstrap`() = runBlocking {
        val (live, default, backend) = buildRepo(seed = emptyList())
        backend.listResponse = UserPersonaListDto(
            personas = listOf(
                UserPersonaDto(
                    id = "built",
                    displayName = LocalizedTextDto("You", "你"),
                    description = LocalizedTextDto("Default", "默认"),
                    isBuiltIn = true,
                    isActive = false,
                ),
                UserPersonaDto(
                    id = "user-1",
                    displayName = LocalizedTextDto("Aria", "阿莉亚"),
                    description = LocalizedTextDto("Adventurer", "冒险者"),
                ),
            ),
            activePersonaId = "user-1",
        )

        live.refresh()

        val personas = default.observePersonas().first()
        assertEquals(2, personas.size)
        assertEquals(1, personas.count { it.isActive })
        assertEquals("user-1", personas.single { it.isActive }.id)
        assertFalse(personas.single { it.id == "built" }.isActive)
    }

    @Test
    fun `refresh reconciles active persona returned by getActivePersona separately`() =
        runBlocking {
            val (live, default, backend) = buildRepo(seed = emptyList())
            backend.listResponse = UserPersonaListDto(
                personas = listOf(
                    UserPersonaDto(
                        id = "built",
                        displayName = LocalizedTextDto("You", "你"),
                        description = LocalizedTextDto("Default", "默认"),
                        isBuiltIn = true,
                    ),
                ),
                activePersonaId = null,
            )
            backend.activeResponse = UserPersonaDto(
                id = "built",
                displayName = LocalizedTextDto("You", "你"),
                description = LocalizedTextDto("Default", "默认"),
                isBuiltIn = true,
                isActive = true,
            )

            live.refresh()

            val active = default.observeActivePersona().first()
            assertNotNull(active)
            assertEquals("built", active?.id)
            assertTrue(active?.isActive == true)
        }

    @Test
    fun `refresh is a no-op when session has no base url or token`() = runBlocking {
        val (live, default, backend) = buildRepo(seed = emptyList(), baseUrl = null)

        live.refresh()

        assertEquals(0, backend.listCalls)
        assertTrue(default.observePersonas().first().isEmpty())
    }

    @Test
    fun `activate flips active flag locally and reaches backend with new active record`() =
        runBlocking {
            val existing = persona("built", isBuiltIn = true, isActive = true)
            val other = persona("other")
            val (live, default, backend) = buildRepo(seed = listOf(existing, other))
            backend.activateResponse = UserPersonaDto(
                id = "other",
                displayName = LocalizedTextDto("Name", "名字"),
                description = LocalizedTextDto("Desc", "描述"),
                isActive = true,
            )

            val result = live.activate("other")

            assertTrue(result is UserPersonaMutationResult.Success)
            assertEquals("other", backend.lastActivateId)
            val personas = default.observePersonas().first()
            assertEquals(1, personas.count { it.isActive })
            assertEquals("other", personas.single { it.isActive }.id)
        }

    @Test
    fun `activate rolls back local state when backend returns 409`() = runBlocking {
        val existing = persona("built", isBuiltIn = true, isActive = true)
        val other = persona("other")
        val backend = FakePersonaBackend(activateFailure = RuntimeException("HTTP 409"))
        val (live, default, _) = buildRepo(seed = listOf(existing, other), backend = backend)

        val result = live.activate("other")

        assertTrue(result is UserPersonaMutationResult.Failed)
        val personas = default.observePersonas().first()
        assertEquals("built", personas.single { it.isActive }.id)
    }

    @Test
    fun `delete rolls back local state when backend returns 409 for active persona`() =
        runBlocking {
            val built = persona("built", isBuiltIn = true, isActive = false)
            val user = persona("user")
            val backend = FakePersonaBackend(deleteFailure = RuntimeException("HTTP 409"))
            val (live, default, _) = buildRepo(seed = listOf(built, user), backend = backend)

            val result = live.delete("user")

            assertTrue(result is UserPersonaMutationResult.Failed)
            val personas = default.observePersonas().first()
            assertEquals(2, personas.size)
            assertNotNull(personas.firstOrNull { it.id == "user" })
        }

    @Test
    fun `delete short-circuits on local rejection without reaching backend`() = runBlocking {
        val built = persona("built", isBuiltIn = true, isActive = true)
        val (live, _, backend) = buildRepo(seed = listOf(built))

        val result = live.delete("built")

        assertEquals(
            UserPersonaMutationResult.Rejected(
                UserPersonaMutationResult.RejectionReason.BuiltInPersonaImmutable,
            ),
            result,
        )
        assertEquals(0, backend.deleteCalls)
    }

    @Test
    fun `create rolls back local state when backend fails`() = runBlocking {
        val backend = FakePersonaBackend(createFailure = RuntimeException("HTTP 500"))
        val (live, default, _) = buildRepo(seed = emptyList(), backend = backend)

        val result = live.create(
            persona(id = "", en = "New", cn = "新建", desc = "D", descCn = "描"),
        )

        assertTrue(result is UserPersonaMutationResult.Failed)
        assertTrue(default.observePersonas().first().isEmpty())
    }

    @Test
    fun `create reconciles local record with server-returned persona on success`() = runBlocking {
        val backend = FakePersonaBackend()
        backend.createResponse = UserPersonaDto(
            id = "persona-server-1",
            displayName = LocalizedTextDto("New", "新建"),
            description = LocalizedTextDto("D", "描"),
            createdAt = 5L,
            updatedAt = 5L,
        )
        val (live, default, _) = buildRepo(seed = emptyList(), backend = backend)

        val result = live.create(
            persona(id = "", en = "New", cn = "新建", desc = "D", descCn = "描"),
        )

        assertTrue(result is UserPersonaMutationResult.Success)
        val reconciled = (result as UserPersonaMutationResult.Success).persona
        assertEquals("persona-server-1", reconciled.id)
        val personas = default.observePersonas().first()
        assertEquals(1, personas.size)
        assertEquals("persona-server-1", personas.single().id)
    }

    @Test
    fun `duplicate rolls back local state when backend create fails`() = runBlocking {
        val source = persona("src", isBuiltIn = true, isActive = true)
        val backend = FakePersonaBackend(createFailure = RuntimeException("HTTP 500"))
        val (live, default, _) = buildRepo(seed = listOf(source), backend = backend)

        val result = live.duplicate("src")

        assertTrue(result is UserPersonaMutationResult.Failed)
        assertEquals(1, default.observePersonas().first().size)
    }

    @Test
    fun `update rolls back local state when backend fails`() = runBlocking {
        val original = UserPersona(
            id = "persona-1",
            displayName = LocalizedText("Old", "旧"),
            description = LocalizedText("Old desc", "旧描述"),
            createdAt = 1L,
            updatedAt = 1L,
        )
        val backend = FakePersonaBackend(updateFailure = RuntimeException("HTTP 422"))
        val (live, default, _) = buildRepo(seed = listOf(original), backend = backend)

        val result = live.update(
            original.copy(description = LocalizedText("New", "新")),
        )

        assertTrue(result is UserPersonaMutationResult.Failed)
        val after = default.observePersonas().first().single()
        assertEquals("Old desc", after.description.english)
        assertEquals("旧描述", after.description.chinese)
    }

    @Test
    fun `mutations without session short-circuit and skip backend`() = runBlocking {
        val (live, default, backend) = buildRepo(
            seed = listOf(persona("built", isBuiltIn = true, isActive = true)),
            baseUrl = null,
        )

        val created = live.create(persona(id = "", en = "X", cn = "X", desc = "X", descCn = "X"))

        assertTrue(created is UserPersonaMutationResult.Success)
        assertEquals(0, backend.createCalls)
        assertNull(backend.lastActivateId)
        assertEquals(2, default.observePersonas().first().size)
    }

    private class FakePersonaBackend(
        var listResponse: UserPersonaListDto? = null,
        var activeResponse: UserPersonaDto? = null,
        var createResponse: UserPersonaDto? = null,
        var updateResponse: UserPersonaDto? = null,
        var activateResponse: UserPersonaDto? = null,
        var createFailure: Throwable? = null,
        var updateFailure: Throwable? = null,
        var deleteFailure: Throwable? = null,
        var activateFailure: Throwable? = null,
    ) : ImBackendClient {
        var listCalls: Int = 0
        var createCalls: Int = 0
        var deleteCalls: Int = 0
        var lastActivateId: String? = null

        override suspend fun issueDevSession(baseUrl: String, externalId: String): DevSessionResponseDto = error("n/a")
        override suspend fun register(baseUrl: String, username: String, password: String, displayName: String): AuthResponseDto = error("n/a")
        override suspend fun login(baseUrl: String, username: String, password: String): AuthResponseDto = error("n/a")
        override suspend fun searchUsers(baseUrl: String, token: String, query: String): List<UserSearchResultDto> = error("n/a")
        override suspend fun sendFriendRequest(baseUrl: String, token: String, toUserId: String): FriendRequestViewDto = error("n/a")
        override suspend fun listFriendRequests(baseUrl: String, token: String): List<FriendRequestViewDto> = error("n/a")
        override suspend fun acceptFriendRequest(baseUrl: String, token: String, requestId: String): FriendRequestViewDto = error("n/a")
        override suspend fun rejectFriendRequest(baseUrl: String, token: String, requestId: String): FriendRequestViewDto = error("n/a")
        override suspend fun loadBootstrap(baseUrl: String, token: String): BootstrapBundleDto = error("n/a")
        override suspend fun sendDirectImageMessage(
            baseUrl: String,
            token: String,
            request: SendDirectImageMessageRequestDto,
        ): SendDirectMessageResultDto = error("n/a")

        override suspend fun loadHistory(
            baseUrl: String,
            token: String,
            conversationId: String,
            limit: Int,
            before: String?,
        ): MessageHistoryPageDto = error("n/a")

        override suspend fun loadCompanionRoster(baseUrl: String, token: String): CompanionRosterDto = error("n/a")
        override suspend fun selectCompanionCharacter(
            baseUrl: String,
            token: String,
            characterId: String,
        ): ActiveCompanionSelectionDto = error("n/a")

        override suspend fun listPersonas(baseUrl: String, token: String): UserPersonaListDto {
            listCalls += 1
            return listResponse ?: UserPersonaListDto(personas = emptyList(), activePersonaId = null)
        }

        override suspend fun createPersona(
            baseUrl: String,
            token: String,
            persona: UserPersonaDto,
        ): UserPersonaDto {
            createCalls += 1
            createFailure?.let { throw it }
            return createResponse ?: persona
        }

        override suspend fun updatePersona(
            baseUrl: String,
            token: String,
            persona: UserPersonaDto,
        ): UserPersonaDto {
            updateFailure?.let { throw it }
            return updateResponse ?: persona
        }

        override suspend fun deletePersona(baseUrl: String, token: String, personaId: String) {
            deleteCalls += 1
            deleteFailure?.let { throw it }
        }

        override suspend fun activatePersona(
            baseUrl: String,
            token: String,
            personaId: String,
        ): UserPersonaDto {
            lastActivateId = personaId
            activateFailure?.let { throw it }
            return activateResponse ?: UserPersonaDto(
                id = personaId,
                displayName = LocalizedTextDto("Name", "名字"),
                description = LocalizedTextDto("Desc", "描述"),
                isActive = true,
            )
        }

        override suspend fun getActivePersona(baseUrl: String, token: String): UserPersonaDto =
            activeResponse ?: error("no active persona configured")
    }
}
