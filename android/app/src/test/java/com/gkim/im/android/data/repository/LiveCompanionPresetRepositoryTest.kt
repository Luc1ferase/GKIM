package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.Preset
import com.gkim.im.android.data.remote.im.AuthResponseDto
import com.gkim.im.android.data.remote.im.BootstrapBundleDto
import com.gkim.im.android.data.remote.im.DevSessionResponseDto
import com.gkim.im.android.data.remote.im.FriendRequestViewDto
import com.gkim.im.android.data.remote.im.ImBackendClient
import com.gkim.im.android.data.remote.im.LocalizedTextDto
import com.gkim.im.android.data.remote.im.MessageHistoryPageDto
import com.gkim.im.android.data.remote.im.PresetDto
import com.gkim.im.android.data.remote.im.PresetListDto
import com.gkim.im.android.data.remote.im.SendDirectImageMessageRequestDto
import com.gkim.im.android.data.remote.im.SendDirectMessageResultDto
import com.gkim.im.android.data.remote.im.UserSearchResultDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveCompanionPresetRepositoryTest {

    private fun preset(
        id: String,
        en: String = "Name",
        cn: String = "名字",
        desc: String = "Desc",
        descCn: String = "描述",
        isBuiltIn: Boolean = false,
        isActive: Boolean = false,
    ): Preset = Preset(
        id = id,
        displayName = LocalizedText(en, cn),
        description = LocalizedText(desc, descCn),
        isBuiltIn = isBuiltIn,
        isActive = isActive,
    )

    private fun buildRepo(
        seed: List<Preset> = emptyList(),
        baseUrl: String? = "https://api.example.com/",
        token: String? = "session-token",
        backend: FakePresetBackend = FakePresetBackend(),
    ): Triple<LiveCompanionPresetRepository, DefaultCompanionPresetRepository, FakePresetBackend> {
        val default = DefaultCompanionPresetRepository(
            initialPresets = seed,
            idGenerator = { "preset-new" },
            clock = { 100L },
        )
        val live = LiveCompanionPresetRepository(
            default = default,
            backendClient = backend,
            baseUrlProvider = { baseUrl },
            tokenProvider = { token },
        )
        return Triple(live, default, backend)
    }

    @Test
    fun `refresh merges remote list with active preset id from bootstrap`() = runBlocking {
        val (live, default, backend) = buildRepo(seed = emptyList())
        backend.listResponse = PresetListDto(
            presets = listOf(
                PresetDto(
                    id = "built",
                    displayName = LocalizedTextDto("Default", "默认"),
                    description = LocalizedTextDto("Neutral", "中性"),
                    isBuiltIn = true,
                    isActive = false,
                ),
                PresetDto(
                    id = "user-1",
                    displayName = LocalizedTextDto("Roleplay", "扮演"),
                    description = LocalizedTextDto("Immersive", "沉浸"),
                ),
            ),
            activePresetId = "user-1",
        )

        live.refresh()

        val presets = default.observePresets().first()
        assertEquals(2, presets.size)
        assertEquals(1, presets.count { it.isActive })
        assertEquals("user-1", presets.single { it.isActive }.id)
        assertFalse(presets.single { it.id == "built" }.isActive)
    }

    @Test
    fun `refresh reconciles active preset returned by getActivePreset separately`() =
        runBlocking {
            val (live, default, backend) = buildRepo(seed = emptyList())
            backend.listResponse = PresetListDto(
                presets = listOf(
                    PresetDto(
                        id = "built",
                        displayName = LocalizedTextDto("Default", "默认"),
                        description = LocalizedTextDto("Neutral", "中性"),
                        isBuiltIn = true,
                    ),
                ),
                activePresetId = null,
            )
            backend.activeResponse = PresetDto(
                id = "built",
                displayName = LocalizedTextDto("Default", "默认"),
                description = LocalizedTextDto("Neutral", "中性"),
                isBuiltIn = true,
                isActive = true,
            )

            live.refresh()

            val active = default.observeActivePreset().first()
            assertNotNull(active)
            assertEquals("built", active?.id)
            assertTrue(active?.isActive == true)
        }

    @Test
    fun `refresh is a no-op when session has no base url or token`() = runBlocking {
        val (live, default, backend) = buildRepo(seed = emptyList(), baseUrl = null)

        live.refresh()

        assertEquals(0, backend.listCalls)
        assertTrue(default.observePresets().first().isEmpty())
    }

    @Test
    fun `activate flips active flag locally and reaches backend with new active record`() =
        runBlocking {
            val existing = preset("built", isBuiltIn = true, isActive = true)
            val other = preset("other")
            val (live, default, backend) = buildRepo(seed = listOf(existing, other))
            backend.activateResponse = PresetDto(
                id = "other",
                displayName = LocalizedTextDto("Name", "名字"),
                description = LocalizedTextDto("Desc", "描述"),
                isActive = true,
            )

            val result = live.activate("other")

            assertTrue(result is CompanionPresetMutationResult.Success)
            assertEquals("other", backend.lastActivateId)
            val presets = default.observePresets().first()
            assertEquals(1, presets.count { it.isActive })
            assertEquals("other", presets.single { it.isActive }.id)
        }

    @Test
    fun `activate rolls back local state when backend returns 409`() = runBlocking {
        val existing = preset("built", isBuiltIn = true, isActive = true)
        val other = preset("other")
        val backend = FakePresetBackend(activateFailure = RuntimeException("HTTP 409"))
        val (live, default, _) = buildRepo(seed = listOf(existing, other), backend = backend)

        val result = live.activate("other")

        assertTrue(result is CompanionPresetMutationResult.Failed)
        val presets = default.observePresets().first()
        assertEquals("built", presets.single { it.isActive }.id)
    }

    @Test
    fun `delete rolls back local state when backend returns 409 for active preset`() =
        runBlocking {
            val built = preset("built", isBuiltIn = true, isActive = false)
            val user = preset("user")
            val backend = FakePresetBackend(deleteFailure = RuntimeException("HTTP 409"))
            val (live, default, _) = buildRepo(seed = listOf(built, user), backend = backend)

            val result = live.delete("user")

            assertTrue(result is CompanionPresetMutationResult.Failed)
            val presets = default.observePresets().first()
            assertEquals(2, presets.size)
            assertNotNull(presets.firstOrNull { it.id == "user" })
        }

    @Test
    fun `delete short-circuits on local built-in rejection without reaching backend`() = runBlocking {
        val built = preset("built", isBuiltIn = true, isActive = true)
        val (live, _, backend) = buildRepo(seed = listOf(built))

        val result = live.delete("built")

        assertEquals(
            CompanionPresetMutationResult.Rejected(
                CompanionPresetMutationResult.RejectionReason.BuiltInPresetImmutable,
            ),
            result,
        )
        assertEquals(0, backend.deleteCalls)
    }

    @Test
    fun `delete short-circuits on local active rejection without reaching backend`() = runBlocking {
        val built = preset("built", isBuiltIn = true, isActive = false)
        val active = preset("user-active", isActive = true)
        val (live, _, backend) = buildRepo(seed = listOf(built, active))

        val result = live.delete("user-active")

        assertEquals(
            CompanionPresetMutationResult.Rejected(
                CompanionPresetMutationResult.RejectionReason.ActivePresetNotDeletable,
            ),
            result,
        )
        assertEquals(0, backend.deleteCalls)
    }

    @Test
    fun `create rolls back local state when backend fails`() = runBlocking {
        val backend = FakePresetBackend(createFailure = RuntimeException("HTTP 500"))
        val (live, default, _) = buildRepo(seed = emptyList(), backend = backend)

        val result = live.create(
            preset(id = "", en = "New", cn = "新建", desc = "D", descCn = "描"),
        )

        assertTrue(result is CompanionPresetMutationResult.Failed)
        assertTrue(default.observePresets().first().isEmpty())
    }

    @Test
    fun `create reconciles local record with server-returned preset on success`() = runBlocking {
        val backend = FakePresetBackend()
        backend.createResponse = PresetDto(
            id = "preset-server-1",
            displayName = LocalizedTextDto("New", "新建"),
            description = LocalizedTextDto("D", "描"),
            createdAt = 5L,
            updatedAt = 5L,
        )
        val (live, default, _) = buildRepo(seed = emptyList(), backend = backend)

        val result = live.create(
            preset(id = "", en = "New", cn = "新建", desc = "D", descCn = "描"),
        )

        assertTrue(result is CompanionPresetMutationResult.Success)
        val reconciled = (result as CompanionPresetMutationResult.Success).preset
        assertEquals("preset-server-1", reconciled.id)
        val presets = default.observePresets().first()
        assertEquals(1, presets.size)
        assertEquals("preset-server-1", presets.single().id)
    }

    @Test
    fun `duplicate rolls back local state when backend create fails`() = runBlocking {
        val source = preset("src", isBuiltIn = true, isActive = true)
        val backend = FakePresetBackend(createFailure = RuntimeException("HTTP 500"))
        val (live, default, _) = buildRepo(seed = listOf(source), backend = backend)

        val result = live.duplicate("src")

        assertTrue(result is CompanionPresetMutationResult.Failed)
        assertEquals(1, default.observePresets().first().size)
    }

    @Test
    fun `update rolls back local state when backend fails`() = runBlocking {
        val original = Preset(
            id = "preset-1",
            displayName = LocalizedText("Old", "旧"),
            description = LocalizedText("Old desc", "旧描述"),
            createdAt = 1L,
            updatedAt = 1L,
        )
        val backend = FakePresetBackend(updateFailure = RuntimeException("HTTP 422"))
        val (live, default, _) = buildRepo(seed = listOf(original), backend = backend)

        val result = live.update(
            original.copy(description = LocalizedText("New", "新")),
        )

        assertTrue(result is CompanionPresetMutationResult.Failed)
        val after = default.observePresets().first().single()
        assertEquals("Old desc", after.description.english)
        assertEquals("旧描述", after.description.chinese)
    }

    @Test
    fun `mutations without session short-circuit and skip backend`() = runBlocking {
        val (live, default, backend) = buildRepo(
            seed = listOf(preset("built", isBuiltIn = true, isActive = true)),
            baseUrl = null,
        )

        val created = live.create(preset(id = "", en = "X", cn = "X", desc = "X", descCn = "X"))

        assertTrue(created is CompanionPresetMutationResult.Success)
        assertEquals(0, backend.createCalls)
        assertNull(backend.lastActivateId)
        assertEquals(2, default.observePresets().first().size)
    }

    private class FakePresetBackend(
        var listResponse: PresetListDto? = null,
        var activeResponse: PresetDto? = null,
        var createResponse: PresetDto? = null,
        var updateResponse: PresetDto? = null,
        var activateResponse: PresetDto? = null,
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

        override suspend fun listPresets(baseUrl: String, token: String): PresetListDto {
            listCalls += 1
            return listResponse ?: PresetListDto(presets = emptyList(), activePresetId = null)
        }

        override suspend fun createPreset(
            baseUrl: String,
            token: String,
            preset: PresetDto,
        ): PresetDto {
            createCalls += 1
            createFailure?.let { throw it }
            return createResponse ?: preset
        }

        override suspend fun updatePreset(
            baseUrl: String,
            token: String,
            preset: PresetDto,
        ): PresetDto {
            updateFailure?.let { throw it }
            return updateResponse ?: preset
        }

        override suspend fun deletePreset(baseUrl: String, token: String, presetId: String) {
            deleteCalls += 1
            deleteFailure?.let { throw it }
        }

        override suspend fun activatePreset(
            baseUrl: String,
            token: String,
            presetId: String,
        ): PresetDto {
            lastActivateId = presetId
            activateFailure?.let { throw it }
            return activateResponse ?: PresetDto(
                id = presetId,
                displayName = LocalizedTextDto("Name", "名字"),
                description = LocalizedTextDto("Desc", "描述"),
                isActive = true,
            )
        }

        override suspend fun getActivePreset(baseUrl: String, token: String): PresetDto =
            activeResponse ?: error("no active preset configured")
    }
}
