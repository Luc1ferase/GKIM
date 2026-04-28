package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.Lorebook
import com.gkim.im.android.core.model.LorebookBinding
import com.gkim.im.android.core.model.LorebookEntry
import com.gkim.im.android.core.model.SecondaryGate
import com.gkim.im.android.data.remote.im.CreateLorebookBindingRequestDto
import com.gkim.im.android.data.remote.im.CreateLorebookEntryRequestDto
import com.gkim.im.android.data.remote.im.CreateLorebookRequestDto
import com.gkim.im.android.data.remote.im.ImWorldInfoClient
import com.gkim.im.android.data.remote.im.LocalizedTextDto
import com.gkim.im.android.data.remote.im.LorebookBindingDto
import com.gkim.im.android.data.remote.im.LorebookBindingListDto
import com.gkim.im.android.data.remote.im.LorebookDto
import com.gkim.im.android.data.remote.im.LorebookEntryDto
import com.gkim.im.android.data.remote.im.LorebookEntryListDto
import com.gkim.im.android.data.remote.im.LorebookListDto
import com.gkim.im.android.data.remote.im.PerLanguageStringListDto
import com.gkim.im.android.data.remote.im.UpdateLorebookBindingRequestDto
import com.gkim.im.android.data.remote.im.UpdateLorebookEntryRequestDto
import com.gkim.im.android.data.remote.im.UpdateLorebookRequestDto
import com.gkim.im.android.data.remote.im.WorldInfoDebugScanResponseDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldInfoRepositoryTest {

    private fun lorebook(
        id: String,
        ownerId: String = "user-nox",
        en: String = "Atlas",
        cn: String = "图鉴",
        isGlobal: Boolean = false,
        isBuiltIn: Boolean = false,
    ): Lorebook = Lorebook(
        id = id,
        ownerId = ownerId,
        displayName = LocalizedText(en, cn),
        isGlobal = isGlobal,
        isBuiltIn = isBuiltIn,
    )

    private fun entry(
        id: String,
        lorebookId: String,
        en: String = "Moon",
        cn: String = "月",
    ): LorebookEntry = LorebookEntry(
        id = id,
        lorebookId = lorebookId,
        name = LocalizedText(en, cn),
    )

    @Test
    fun `default createLorebook assigns id + clock and exposes via stateflow`() = runBlocking {
        val repo = DefaultWorldInfoRepository(
            idGenerator = { "rnd" },
            clock = { 100L },
        )
        val result = repo.createLorebook(
            Lorebook(
                id = "",
                ownerId = "user-nox",
                displayName = LocalizedText("New", "新"),
            ),
        )
        assertTrue(result is WorldInfoMutationResult.Success)
        val created = (result as WorldInfoMutationResult.Success).value
        assertEquals("lorebook-rnd", created.id)
        assertEquals(100L, created.createdAt)
        assertEquals(100L, created.updatedAt)

        val stored = repo.observeLorebooks().first()
        assertEquals(listOf(created), stored)
        assertTrue(repo.observeEntries().first().containsKey("lorebook-rnd"))
        assertTrue(repo.observeBindings().first().containsKey("lorebook-rnd"))
    }

    @Test
    fun `default updateLorebook preserves createdAt and isBuiltIn flag`() = runBlocking {
        val seeded = lorebook("lb-1").copy(createdAt = 50L)
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(seeded),
            clock = { 200L },
        )
        val result = repo.updateLorebook(
            seeded.copy(
                displayName = LocalizedText("Renamed", "改名"),
                isBuiltIn = true,
            ),
        )
        assertTrue(result is WorldInfoMutationResult.Success)
        val updated = (result as WorldInfoMutationResult.Success).value
        assertEquals("Renamed", updated.displayName.english)
        assertFalse(updated.isBuiltIn)
        assertEquals(50L, updated.createdAt)
        assertEquals(200L, updated.updatedAt)
    }

    @Test
    fun `default updateLorebook rejects unknown lorebook`() = runBlocking {
        val repo = DefaultWorldInfoRepository()
        val result = repo.updateLorebook(lorebook("unknown"))
        assertTrue(result is WorldInfoMutationResult.Rejected)
        assertEquals(
            WorldInfoMutationResult.RejectionReason.UnknownLorebook,
            (result as WorldInfoMutationResult.Rejected).reason,
        )
    }

    @Test
    fun `default updateLorebook rejects built-in lorebook`() = runBlocking {
        val builtIn = lorebook("lb-built", isBuiltIn = true)
        val repo = DefaultWorldInfoRepository(initialLorebooks = listOf(builtIn))
        val result = repo.updateLorebook(builtIn.copy(tokenBudget = 99))
        assertTrue(result is WorldInfoMutationResult.Rejected)
        assertEquals(
            WorldInfoMutationResult.RejectionReason.BuiltInLorebookImmutable,
            (result as WorldInfoMutationResult.Rejected).reason,
        )
    }

    @Test
    fun `default deleteLorebook rejects when bindings exist`() = runBlocking {
        val lb = lorebook("lb-1")
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(lb),
            initialBindings = mapOf(
                "lb-1" to listOf(LorebookBinding("lb-1", "card-aria", isPrimary = true)),
            ),
        )
        val result = repo.deleteLorebook("lb-1")
        assertEquals(
            WorldInfoMutationResult.RejectionReason.LorebookHasBindings,
            (result as WorldInfoMutationResult.Rejected).reason,
        )
    }

    @Test
    fun `default deleteLorebook drops entries and bindings when none remain`() = runBlocking {
        val lb = lorebook("lb-1")
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(lb),
            initialEntries = mapOf("lb-1" to listOf(entry("e1", "lb-1"))),
        )
        val result = repo.deleteLorebook("lb-1")
        assertTrue(result is WorldInfoMutationResult.Success)
        assertTrue(repo.observeLorebooks().first().isEmpty())
        assertFalse(repo.observeEntries().first().containsKey("lb-1"))
        assertFalse(repo.observeBindings().first().containsKey("lb-1"))
    }

    @Test
    fun `default duplicateLorebook copies entries with fresh ids`() = runBlocking {
        val lb = lorebook("lb-1")
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(lb),
            initialEntries = mapOf(
                "lb-1" to listOf(
                    entry("e1", "lb-1"),
                    entry("e2", "lb-1", en = "Sun"),
                ),
            ),
            idGenerator = IncrementingIdGenerator().asFn(),
            clock = { 300L },
        )
        val result = repo.duplicateLorebook("lb-1")
        assertTrue(result is WorldInfoMutationResult.Success)
        val copy = (result as WorldInfoMutationResult.Success).value
        assertEquals("Atlas (copy)", copy.displayName.english)
        assertEquals("图鉴（副本）", copy.displayName.chinese)
        assertEquals(300L, copy.createdAt)

        val copyEntries = repo.observeEntries().first()[copy.id].orEmpty()
        assertEquals(2, copyEntries.size)
        assertTrue(copyEntries.none { it.id == "e1" || it.id == "e2" })
        assertTrue(copyEntries.all { it.lorebookId == copy.id })
    }

    @Test
    fun `default createEntry rejects unknown lorebook`() = runBlocking {
        val repo = DefaultWorldInfoRepository()
        val result = repo.createEntry("missing", entry("", "missing"))
        assertEquals(
            WorldInfoMutationResult.RejectionReason.UnknownLorebook,
            (result as WorldInfoMutationResult.Rejected).reason,
        )
    }

    @Test
    fun `default createEntry assigns id and rewrites lorebookId`() = runBlocking {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(lorebook("lb-1")),
            idGenerator = { "generated" },
        )
        val result = repo.createEntry(
            lorebookId = "lb-1",
            draft = LorebookEntry(
                id = "",
                lorebookId = "other",
                name = LocalizedText("Moon", "月"),
            ),
        )
        val created = (result as WorldInfoMutationResult.Success).value
        assertEquals("entry-generated", created.id)
        assertEquals("lb-1", created.lorebookId)
        assertEquals(listOf(created), repo.observeEntries().first()["lb-1"])
    }

    @Test
    fun `default updateEntry rejects unknown entry`() = runBlocking {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(lorebook("lb-1")),
            initialEntries = mapOf("lb-1" to emptyList()),
        )
        val result = repo.updateEntry(entry("missing", "lb-1"))
        assertEquals(
            WorldInfoMutationResult.RejectionReason.UnknownEntry,
            (result as WorldInfoMutationResult.Rejected).reason,
        )
    }

    @Test
    fun `default bind rejects duplicate binding on the same character`() = runBlocking {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(lorebook("lb-1")),
            initialBindings = mapOf(
                "lb-1" to listOf(LorebookBinding("lb-1", "card-aria", isPrimary = false)),
            ),
        )
        val result = repo.bind("lb-1", "card-aria", isPrimary = true)
        assertEquals(
            WorldInfoMutationResult.RejectionReason.BindingAlreadyExists,
            (result as WorldInfoMutationResult.Rejected).reason,
        )
    }

    @Test
    fun `default bind sweeps prior primary on other lorebooks for same character`() =
        runBlocking {
            val repo = DefaultWorldInfoRepository(
                initialLorebooks = listOf(lorebook("lb-a"), lorebook("lb-b")),
                initialBindings = mapOf(
                    "lb-a" to listOf(LorebookBinding("lb-a", "card-aria", isPrimary = true)),
                    "lb-b" to emptyList(),
                ),
            )
            val result = repo.bind("lb-b", "card-aria", isPrimary = true)
            assertTrue(result is WorldInfoMutationResult.Success)

            val bindings = repo.observeBindings().first()
            assertFalse(bindings["lb-a"].orEmpty().single { it.characterId == "card-aria" }.isPrimary)
            assertTrue(bindings["lb-b"].orEmpty().single { it.characterId == "card-aria" }.isPrimary)
        }

    @Test
    fun `default updateBinding toggles isPrimary and propagates primary sweep`() = runBlocking {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(lorebook("lb-a"), lorebook("lb-b")),
            initialBindings = mapOf(
                "lb-a" to listOf(LorebookBinding("lb-a", "card-aria", isPrimary = true)),
                "lb-b" to listOf(LorebookBinding("lb-b", "card-aria", isPrimary = false)),
            ),
        )
        val result = repo.updateBinding("lb-b", "card-aria", isPrimary = true)
        assertTrue(result is WorldInfoMutationResult.Success)
        val bindings = repo.observeBindings().first()
        assertFalse(bindings["lb-a"]!!.single().isPrimary)
        assertTrue(bindings["lb-b"]!!.single().isPrimary)
    }

    @Test
    fun `default unbind rejects when no binding exists for character`() = runBlocking {
        val repo = DefaultWorldInfoRepository(
            initialLorebooks = listOf(lorebook("lb-1")),
            initialBindings = mapOf("lb-1" to emptyList()),
        )
        val result = repo.unbind("lb-1", "card-aria")
        assertEquals(
            WorldInfoMutationResult.RejectionReason.UnknownBinding,
            (result as WorldInfoMutationResult.Rejected).reason,
        )
    }

    @Test
    fun `live createLorebook reconciles id from server`() = runBlocking {
        val (live, default, backend) = buildRepo()
        backend.createLorebookResponse = LorebookDto(
            id = "server-assigned",
            ownerId = "user-nox",
            displayName = LocalizedTextDto("New", "新"),
        )
        val result = live.createLorebook(
            Lorebook(
                id = "",
                ownerId = "user-nox",
                displayName = LocalizedText("New", "新"),
            ),
        )
        assertTrue(result is WorldInfoMutationResult.Success)
        assertEquals("server-assigned", (result as WorldInfoMutationResult.Success).value.id)
        val lorebooks = default.observeLorebooks().first()
        assertEquals(listOf("server-assigned"), lorebooks.map { it.id })
    }

    @Test
    fun `live createLorebook rolls back when server rejects`() = runBlocking {
        val (live, default, backend) = buildRepo()
        backend.failOnCreateLorebook = true
        val result = live.createLorebook(
            Lorebook(
                id = "",
                ownerId = "user-nox",
                displayName = LocalizedText("Oops", "失败"),
            ),
        )
        assertTrue(result is WorldInfoMutationResult.Failed)
        assertTrue(default.observeLorebooks().first().isEmpty())
    }

    @Test
    fun `live updateLorebook rolls back on server failure`() = runBlocking {
        val seeded = lorebook("lb-1")
        val (live, default, backend) = buildRepo(seedLorebooks = listOf(seeded))
        backend.failOnUpdateLorebook = true
        val result = live.updateLorebook(seeded.copy(tokenBudget = 4096))
        assertTrue(result is WorldInfoMutationResult.Failed)
        assertEquals(Lorebook.DefaultTokenBudget, default.observeLorebooks().first().single().tokenBudget)
    }

    @Test
    fun `live deleteLorebook rolls back when server fails`() = runBlocking {
        val seeded = lorebook("lb-1")
        val (live, default, backend) = buildRepo(
            seedLorebooks = listOf(seeded),
            seedEntries = mapOf("lb-1" to listOf(entry("e1", "lb-1"))),
        )
        backend.failOnDeleteLorebook = true
        val result = live.deleteLorebook("lb-1")
        assertTrue(result is WorldInfoMutationResult.Failed)
        assertEquals(listOf("lb-1"), default.observeLorebooks().first().map { it.id })
        assertEquals(listOf("e1"), default.observeEntries().first()["lb-1"]!!.map { it.id })
    }

    @Test
    fun `live createEntry reconciles server-assigned id`() = runBlocking {
        val seeded = lorebook("lb-1")
        val (live, default, backend) = buildRepo(
            seedLorebooks = listOf(seeded),
            seedEntries = mapOf("lb-1" to emptyList()),
        )
        backend.createEntryResponse = LorebookEntryDto(
            id = "server-entry",
            lorebookId = "lb-1",
            name = LocalizedTextDto("Moon", "月"),
            keysByLang = PerLanguageStringListDto(english = listOf("moon")),
        )
        val result = live.createEntry(
            lorebookId = "lb-1",
            draft = LorebookEntry(
                id = "",
                lorebookId = "lb-1",
                name = LocalizedText("Moon", "月"),
                keysByLang = mapOf(AppLanguage.English to listOf("moon")),
            ),
        )
        assertTrue(result is WorldInfoMutationResult.Success)
        val entries = default.observeEntries().first()["lb-1"]!!
        assertEquals(listOf("server-entry"), entries.map { it.id })
    }

    @Test
    fun `live updateEntry rolls back to prior entry on server failure`() = runBlocking {
        val seeded = lorebook("lb-1")
        val original = entry("e1", "lb-1")
        val (live, default, backend) = buildRepo(
            seedLorebooks = listOf(seeded),
            seedEntries = mapOf("lb-1" to listOf(original)),
        )
        backend.failOnUpdateEntry = true
        val result = live.updateEntry(original.copy(insertionOrder = 9))
        assertTrue(result is WorldInfoMutationResult.Failed)
        assertEquals(0, default.observeEntries().first()["lb-1"]!!.single().insertionOrder)
    }

    @Test
    fun `live bind propagates primary sweep and records server reconciliation`() = runBlocking {
        val (live, default, backend) = buildRepo(
            seedLorebooks = listOf(lorebook("lb-a"), lorebook("lb-b")),
            seedBindings = mapOf(
                "lb-a" to listOf(LorebookBinding("lb-a", "card-aria", isPrimary = true)),
                "lb-b" to emptyList(),
            ),
        )
        backend.bindResponse = LorebookBindingDto(
            lorebookId = "lb-b",
            characterId = "card-aria",
            isPrimary = true,
        )
        val result = live.bind("lb-b", "card-aria", isPrimary = true)
        assertTrue(result is WorldInfoMutationResult.Success)

        val bindings = default.observeBindings().first()
        assertFalse(bindings["lb-a"]!!.single { it.characterId == "card-aria" }.isPrimary)
        assertTrue(bindings["lb-b"]!!.single { it.characterId == "card-aria" }.isPrimary)
    }

    @Test
    fun `live updateBinding rolls back primary sweep on failure`() = runBlocking {
        val (live, default, backend) = buildRepo(
            seedLorebooks = listOf(lorebook("lb-a"), lorebook("lb-b")),
            seedBindings = mapOf(
                "lb-a" to listOf(LorebookBinding("lb-a", "card-aria", isPrimary = true)),
                "lb-b" to listOf(LorebookBinding("lb-b", "card-aria", isPrimary = false)),
            ),
        )
        backend.failOnUpdateBinding = true
        val result = live.updateBinding("lb-b", "card-aria", isPrimary = true)
        assertTrue(result is WorldInfoMutationResult.Failed)

        val bindings = default.observeBindings().first()
        assertTrue(bindings["lb-a"]!!.single().isPrimary)
        assertFalse(bindings["lb-b"]!!.single().isPrimary)
    }

    @Test
    fun `live refresh loads lorebooks plus entries plus bindings from server`() = runBlocking {
        val (live, default, backend) = buildRepo()
        backend.listResponse = LorebookListDto(
            lorebooks = listOf(
                LorebookDto(
                    id = "lb-atlas",
                    ownerId = "user-nox",
                    displayName = LocalizedTextDto("Atlas", "图鉴"),
                ),
            ),
        )
        backend.entriesByLorebook["lb-atlas"] = LorebookEntryListDto(
            entries = listOf(
                LorebookEntryDto(
                    id = "e1",
                    lorebookId = "lb-atlas",
                    name = LocalizedTextDto("Moon", "月"),
                ),
            ),
        )
        backend.bindingsByLorebook["lb-atlas"] = LorebookBindingListDto(
            bindings = listOf(
                LorebookBindingDto("lb-atlas", "card-aria", isPrimary = true),
            ),
        )

        live.refresh()

        assertEquals(listOf("lb-atlas"), default.observeLorebooks().first().map { it.id })
        assertEquals(listOf("e1"), default.observeEntries().first()["lb-atlas"]!!.map { it.id })
        assertEquals(
            listOf("card-aria"),
            default.observeBindings().first()["lb-atlas"]!!.map { it.characterId },
        )
    }

    @Test
    fun `live refresh does nothing when baseUrl or token is missing`() = runBlocking {
        val seeded = lorebook("lb-1")
        val (live, default, _) = buildRepo(
            seedLorebooks = listOf(seeded),
            baseUrl = null,
        )
        live.refresh()
        assertEquals(listOf("lb-1"), default.observeLorebooks().first().map { it.id })
    }

    private class IncrementingIdGenerator {
        private var counter = 0
        fun asFn(): () -> String = { "gen-${++counter}" }
    }

    private fun buildRepo(
        seedLorebooks: List<Lorebook> = emptyList(),
        seedEntries: Map<String, List<LorebookEntry>> = emptyMap(),
        seedBindings: Map<String, List<LorebookBinding>> = emptyMap(),
        baseUrl: String? = "https://example.com/",
        token: String? = "session-token",
        clientClock: Long = 100L,
    ): Triple<LiveWorldInfoRepository, DefaultWorldInfoRepository, FakeWorldInfoBackend> {
        val default = DefaultWorldInfoRepository(
            initialLorebooks = seedLorebooks,
            initialEntries = seedEntries,
            initialBindings = seedBindings,
            idGenerator = { "local" },
            clock = { clientClock },
        )
        val backend = FakeWorldInfoBackend()
        val live = LiveWorldInfoRepository(
            default = default,
            client = backend,
            baseUrlProvider = { baseUrl },
            tokenProvider = { token },
        )
        return Triple(live, default, backend)
    }
}

private class FakeWorldInfoBackend : ImWorldInfoClient {
    var listResponse: LorebookListDto = LorebookListDto(lorebooks = emptyList())
    var entriesByLorebook: MutableMap<String, LorebookEntryListDto> = mutableMapOf()
    var bindingsByLorebook: MutableMap<String, LorebookBindingListDto> = mutableMapOf()
    var createLorebookResponse: LorebookDto? = null
    var updateLorebookResponse: LorebookDto? = null
    var duplicateLorebookResponse: LorebookDto? = null
    var createEntryResponse: LorebookEntryDto? = null
    var updateEntryResponse: LorebookEntryDto? = null
    var bindResponse: LorebookBindingDto? = null
    var updateBindingResponse: LorebookBindingDto? = null

    var failOnCreateLorebook: Boolean = false
    var failOnUpdateLorebook: Boolean = false
    var failOnDeleteLorebook: Boolean = false
    var failOnDuplicateLorebook: Boolean = false
    var failOnCreateEntry: Boolean = false
    var failOnUpdateEntry: Boolean = false
    var failOnDeleteEntry: Boolean = false
    var failOnBind: Boolean = false
    var failOnUpdateBinding: Boolean = false
    var failOnUnbind: Boolean = false

    override suspend fun list(baseUrl: String, token: String): LorebookListDto = listResponse

    override suspend fun get(
        baseUrl: String,
        token: String,
        lorebookId: String,
    ): LorebookDto = listResponse.lorebooks.first { it.id == lorebookId }

    override suspend fun create(
        baseUrl: String,
        token: String,
        request: CreateLorebookRequestDto,
    ): LorebookDto {
        if (failOnCreateLorebook) error("create failed")
        return createLorebookResponse ?: LorebookDto(
            id = "remote-new",
            ownerId = "user-nox",
            displayName = request.displayName,
        )
    }

    override suspend fun update(
        baseUrl: String,
        token: String,
        lorebookId: String,
        request: UpdateLorebookRequestDto,
    ): LorebookDto {
        if (failOnUpdateLorebook) error("update failed")
        return updateLorebookResponse ?: LorebookDto(
            id = lorebookId,
            ownerId = "user-nox",
            displayName = request.displayName ?: LocalizedTextDto("", ""),
            tokenBudget = request.tokenBudget ?: Lorebook.DefaultTokenBudget,
        )
    }

    override suspend fun delete(baseUrl: String, token: String, lorebookId: String) {
        if (failOnDeleteLorebook) error("delete failed")
    }

    override suspend fun duplicate(
        baseUrl: String,
        token: String,
        lorebookId: String,
    ): LorebookDto {
        if (failOnDuplicateLorebook) error("duplicate failed")
        return duplicateLorebookResponse ?: LorebookDto(
            id = "dup-$lorebookId",
            ownerId = "user-nox",
            displayName = LocalizedTextDto("Copy", "副本"),
        )
    }

    override suspend fun listEntries(
        baseUrl: String,
        token: String,
        lorebookId: String,
    ): LorebookEntryListDto = entriesByLorebook[lorebookId] ?: LorebookEntryListDto(emptyList())

    override suspend fun createEntry(
        baseUrl: String,
        token: String,
        lorebookId: String,
        request: CreateLorebookEntryRequestDto,
    ): LorebookEntryDto {
        if (failOnCreateEntry) error("create entry failed")
        return createEntryResponse ?: LorebookEntryDto(
            id = "remote-entry",
            lorebookId = lorebookId,
            name = request.name,
        )
    }

    override suspend fun updateEntry(
        baseUrl: String,
        token: String,
        lorebookId: String,
        entryId: String,
        request: UpdateLorebookEntryRequestDto,
    ): LorebookEntryDto {
        if (failOnUpdateEntry) error("update entry failed")
        return updateEntryResponse ?: LorebookEntryDto(
            id = entryId,
            lorebookId = lorebookId,
            name = request.name ?: LocalizedTextDto("", ""),
            insertionOrder = request.insertionOrder ?: 0,
        )
    }

    override suspend fun deleteEntry(
        baseUrl: String,
        token: String,
        lorebookId: String,
        entryId: String,
    ) {
        if (failOnDeleteEntry) error("delete entry failed")
    }

    override suspend fun listBindings(
        baseUrl: String,
        token: String,
        lorebookId: String,
    ): LorebookBindingListDto =
        bindingsByLorebook[lorebookId] ?: LorebookBindingListDto(emptyList())

    override suspend fun bind(
        baseUrl: String,
        token: String,
        lorebookId: String,
        characterId: String,
        isPrimary: Boolean,
    ): LorebookBindingDto {
        if (failOnBind) error("bind failed")
        return bindResponse ?: LorebookBindingDto(
            lorebookId = lorebookId,
            characterId = characterId,
            isPrimary = isPrimary,
        )
    }

    override suspend fun updateBinding(
        baseUrl: String,
        token: String,
        lorebookId: String,
        characterId: String,
        isPrimary: Boolean,
    ): LorebookBindingDto {
        if (failOnUpdateBinding) error("update binding failed")
        return updateBindingResponse ?: LorebookBindingDto(
            lorebookId = lorebookId,
            characterId = characterId,
            isPrimary = isPrimary,
        )
    }

    override suspend fun unbind(
        baseUrl: String,
        token: String,
        lorebookId: String,
        characterId: String,
    ) {
        if (failOnUnbind) error("unbind failed")
    }

    override suspend fun debugScan(
        baseUrl: String,
        token: String,
        characterId: String,
        scanText: String,
        devAccessHeader: String,
        allowDebug: Boolean,
    ): WorldInfoDebugScanResponseDto = WorldInfoDebugScanResponseDto()
}
