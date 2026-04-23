package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.core.model.LocalizedText
import com.gkim.im.android.core.model.Lorebook
import com.gkim.im.android.core.model.LorebookEntry
import com.gkim.im.android.data.remote.im.CharacterBookDto
import com.gkim.im.android.data.remote.im.CharacterBookEntryDto
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
import com.gkim.im.android.data.remote.im.UpdateLorebookBindingRequestDto
import com.gkim.im.android.data.remote.im.UpdateLorebookEntryRequestDto
import com.gkim.im.android.data.remote.im.UpdateLorebookRequestDto
import com.gkim.im.android.data.remote.im.WorldInfoDebugScanResponseDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterBookLorebookMaterializerTest {

    @Test
    fun `materialize creates lorebook + entries + primary binding and returns result ids`() = runBlocking {
        val backend = RecordingWorldInfoBackend()
        val materializer = CharacterBookLorebookMaterializer(backend)

        val book = CharacterBookDto(
            name = "Ancient Library",
            description = "The laws that govern the realm.",
            entries = listOf(
                CharacterBookEntryDto(
                    keys = listOf("dragon"),
                    content = "The dragon guards its hoard.",
                    insertionOrder = 20,
                    name = "Dragon",
                ),
                CharacterBookEntryDto(
                    keys = emptyList(),
                    content = "An ancient law binds every citizen.",
                    constant = true,
                    insertionOrder = 10,
                    name = "Constant",
                ),
            ),
        )
        val result = materializer.materialize(
            baseUrl = "http://backend/",
            token = "tok",
            characterId = "char-42",
            characterBook = book,
            characterDisplayName = LocalizedText("Architect", "建筑师"),
            importLanguage = AppLanguage.English,
        )

        val materialized = result.getOrThrow()
        assertEquals(backend.createdLorebookId, materialized.lorebookId)
        assertEquals(listOf(backend.assignedEntryIds[0], backend.assignedEntryIds[1]), materialized.entryIds)
        assertEquals("char-42", materialized.characterId)

        val bindingRequest = backend.recordedBindings.single()
        assertEquals(backend.createdLorebookId, backend.lastBindingLorebookId)
        assertEquals("char-42", bindingRequest.characterId)
        assertTrue("binding must be primary", bindingRequest.isPrimary)
    }

    @Test
    fun `lorebook request defaults displayName from character name when character_book name is blank`() {
        val request = CharacterBookLorebookMaterializer.buildLorebookRequest(
            book = CharacterBookDto(name = ""),
            characterDisplayName = LocalizedText("Architect", "建筑师"),
        )
        assertEquals("Architect lorebook", request.displayName.english)
        assertEquals("建筑师 世界书", request.displayName.chinese)
    }

    @Test
    fun `lorebook request falls back to generic name when character display name is blank`() {
        val request = CharacterBookLorebookMaterializer.buildLorebookRequest(
            book = CharacterBookDto(name = ""),
            characterDisplayName = LocalizedText("", ""),
        )
        assertEquals("Imported lorebook", request.displayName.english)
        assertEquals("", request.displayName.chinese)
    }

    @Test
    fun `lorebook request honors explicit character_book name over character display name`() {
        val request = CharacterBookLorebookMaterializer.buildLorebookRequest(
            book = CharacterBookDto(name = "Custom Tome"),
            characterDisplayName = LocalizedText("Architect", "建筑师"),
        )
        assertEquals("Custom Tome", request.displayName.english)
        assertEquals("建筑师 世界书", request.displayName.chinese)
    }

    @Test
    fun `lorebook request carries tokenBudget and description from character_book`() {
        val request = CharacterBookLorebookMaterializer.buildLorebookRequest(
            book = CharacterBookDto(
                name = "N",
                description = "World ruled by laws.",
                tokenBudget = 512,
            ),
            characterDisplayName = LocalizedText("Character", "角色"),
        )
        assertEquals(512, request.tokenBudget)
        assertEquals("World ruled by laws.", request.description.english)
    }

    @Test
    fun `lorebook request defaults tokenBudget to Lorebook default when absent`() {
        val request = CharacterBookLorebookMaterializer.buildLorebookRequest(
            book = CharacterBookDto(name = "N"),
            characterDisplayName = LocalizedText("C", "角"),
        )
        assertEquals(Lorebook.DefaultTokenBudget, request.tokenBudget)
    }

    @Test
    fun `lorebook request preserves un-modeled character_book fields under extensions st`() {
        val innerExtensions = buildJsonObject {
            put("foo", JsonPrimitive("bar"))
        }
        val request = CharacterBookLorebookMaterializer.buildLorebookRequest(
            book = CharacterBookDto(
                name = "Library",
                scanDepth = 5,
                recursiveScanning = true,
                extensions = innerExtensions,
            ),
            characterDisplayName = LocalizedText("Character", "角色"),
        )
        val st = request.extensions["st"] as JsonObject
        assertEquals(5, st["scan_depth"]!!.jsonPrimitive.content.toInt())
        assertEquals(true, st["recursive_scanning"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Library", st["name"]!!.jsonPrimitive.content)
        val preservedInner = st["extensions"]!!.jsonObject
        assertEquals("bar", preservedInner["foo"]!!.jsonPrimitive.content)
    }

    @Test
    fun `entry request maps modeled fields 1 to 1 for English import language`() {
        val entry = CharacterBookEntryDto(
            keys = listOf("dragon", "wyrm"),
            content = "The dragon guards its hoard.",
            enabled = true,
            insertionOrder = 20,
            caseSensitive = true,
            constant = false,
            name = "Dragon",
            comment = "keyword-gated",
            secondaryKeys = listOf("scales"),
        )
        val request = CharacterBookLorebookMaterializer.buildEntryRequest(
            entry = entry,
            importLanguage = AppLanguage.English,
        )
        assertEquals("Dragon", request.name.english)
        assertEquals("", request.name.chinese)
        assertEquals(listOf("dragon", "wyrm"), request.keysByLang.english)
        assertEquals(emptyList<String>(), request.keysByLang.chinese)
        assertEquals(listOf("scales"), request.secondaryKeysByLang.english)
        assertEquals("The dragon guards its hoard.", request.content.english)
        assertEquals("", request.content.chinese)
        assertEquals(true, request.enabled)
        assertEquals(false, request.constant)
        assertEquals(true, request.caseSensitive)
        assertEquals(20, request.insertionOrder)
        assertEquals("keyword-gated", request.comment)
    }

    @Test
    fun `entry request routes keys to Chinese language slot when imported as Chinese`() {
        val entry = CharacterBookEntryDto(
            keys = listOf("龙"),
            content = "巨龙守护宝藏。",
            secondaryKeys = listOf("鳞片"),
        )
        val request = CharacterBookLorebookMaterializer.buildEntryRequest(
            entry = entry,
            importLanguage = AppLanguage.Chinese,
        )
        assertEquals(listOf("龙"), request.keysByLang.chinese)
        assertEquals(emptyList<String>(), request.keysByLang.english)
        assertEquals(listOf("鳞片"), request.secondaryKeysByLang.chinese)
        assertEquals("巨龙守护宝藏。", request.content.chinese)
        assertEquals("", request.content.english)
    }

    @Test
    fun `entry request maps selective flag to AND secondary gate`() {
        val request = CharacterBookLorebookMaterializer.buildEntryRequest(
            entry = CharacterBookEntryDto(selective = true),
            importLanguage = AppLanguage.English,
        )
        assertEquals("AND", request.secondaryGate)
    }

    @Test
    fun `entry request leaves secondary gate NONE when selective is false`() {
        val request = CharacterBookLorebookMaterializer.buildEntryRequest(
            entry = CharacterBookEntryDto(selective = false),
            importLanguage = AppLanguage.English,
        )
        assertEquals("NONE", request.secondaryGate)
    }

    @Test
    fun `entry request preserves selective flag and ST extensions under extensions st`() {
        val innerExtensions = buildJsonObject {
            put("probability", JsonPrimitive(75))
            put("position", JsonPrimitive("before_char"))
        }
        val request = CharacterBookLorebookMaterializer.buildEntryRequest(
            entry = CharacterBookEntryDto(
                selective = true,
                extensions = innerExtensions,
            ),
            importLanguage = AppLanguage.English,
        )
        val st = request.extensions["st"] as JsonObject
        assertEquals(true, st["selective"]!!.jsonPrimitive.content.toBoolean())
        val preserved = st["extensions"]!!.jsonObject
        assertEquals(75, preserved["probability"]!!.jsonPrimitive.content.toInt())
        assertEquals("before_char", preserved["position"]!!.jsonPrimitive.content)
    }

    @Test
    fun `entry request reads scanDepth from extensions depth and clamps to server max`() {
        val request = CharacterBookLorebookMaterializer.buildEntryRequest(
            entry = CharacterBookEntryDto(
                extensions = buildJsonObject { put("depth", JsonPrimitive(999)) },
            ),
            importLanguage = AppLanguage.English,
        )
        assertEquals(LorebookEntry.MaxServerScanDepth, request.scanDepth)
    }

    @Test
    fun `entry request clamps negative scanDepth to zero`() {
        val request = CharacterBookLorebookMaterializer.buildEntryRequest(
            entry = CharacterBookEntryDto(
                extensions = buildJsonObject { put("depth", JsonPrimitive(-5)) },
            ),
            importLanguage = AppLanguage.English,
        )
        assertEquals(0, request.scanDepth)
    }

    @Test
    fun `entry request uses default scanDepth when extensions has no depth`() {
        val request = CharacterBookLorebookMaterializer.buildEntryRequest(
            entry = CharacterBookEntryDto(),
            importLanguage = AppLanguage.English,
        )
        assertEquals(LorebookEntry.DefaultScanDepth, request.scanDepth)
    }

    @Test
    fun `empty character_book entries still create lorebook and primary binding`() = runBlocking {
        val backend = RecordingWorldInfoBackend()
        val materializer = CharacterBookLorebookMaterializer(backend)

        val result = materializer.materialize(
            baseUrl = "http://backend/",
            token = "tok",
            characterId = "char-empty",
            characterBook = CharacterBookDto(name = "Empty tome", entries = emptyList()),
            characterDisplayName = LocalizedText("Character", "角色"),
            importLanguage = AppLanguage.English,
        )

        val materialized = result.getOrThrow()
        assertEquals(backend.createdLorebookId, materialized.lorebookId)
        assertEquals(emptyList<String>(), materialized.entryIds)
        assertEquals(1, backend.recordedBindings.size)
        assertTrue(backend.recordedBindings.single().isPrimary)
        assertEquals(0, backend.createEntryCalls)
    }

    @Test
    fun `materialize rolls back lorebook when entry creation fails`() = runBlocking {
        val backend = RecordingWorldInfoBackend().apply { failOnCreateEntry = true }
        val materializer = CharacterBookLorebookMaterializer(backend)

        val result = materializer.materialize(
            baseUrl = "http://backend/",
            token = "tok",
            characterId = "char-x",
            characterBook = CharacterBookDto(
                name = "Breaks",
                entries = listOf(CharacterBookEntryDto(keys = listOf("k"), content = "c")),
            ),
            characterDisplayName = LocalizedText("C", "角"),
            importLanguage = AppLanguage.English,
        )

        assertTrue("materialize must surface entry-create failure", result.isFailure)
        assertEquals(backend.createdLorebookId, backend.deletedLorebookId)
        assertEquals(0, backend.recordedBindings.size)
    }

    @Test
    fun `materialize rolls back lorebook when binding creation fails`() = runBlocking {
        val backend = RecordingWorldInfoBackend().apply { failOnBind = true }
        val materializer = CharacterBookLorebookMaterializer(backend)

        val result = materializer.materialize(
            baseUrl = "http://backend/",
            token = "tok",
            characterId = "char-y",
            characterBook = CharacterBookDto(name = "Bind break"),
            characterDisplayName = LocalizedText("C", "角"),
            importLanguage = AppLanguage.English,
        )

        assertTrue("materialize must surface binding failure", result.isFailure)
        assertEquals(backend.createdLorebookId, backend.deletedLorebookId)
    }

    @Test
    fun `materialize surfaces lorebook creation failure without attempting rollback`() = runBlocking {
        val backend = RecordingWorldInfoBackend().apply { failOnCreateLorebook = true }
        val materializer = CharacterBookLorebookMaterializer(backend)

        val result = materializer.materialize(
            baseUrl = "http://backend/",
            token = "tok",
            characterId = "char-z",
            characterBook = CharacterBookDto(name = "Never"),
            characterDisplayName = LocalizedText("C", "角"),
            importLanguage = AppLanguage.English,
        )

        assertTrue("materialize must surface lorebook creation failure", result.isFailure)
        assertNull("no rollback delete when create never succeeded", backend.deletedLorebookId)
        assertEquals(0, backend.createEntryCalls)
        assertEquals(0, backend.recordedBindings.size)
    }

    @Test
    fun `materialize preserves insertionOrder and name on every entry request`() = runBlocking {
        val backend = RecordingWorldInfoBackend()
        val materializer = CharacterBookLorebookMaterializer(backend)
        val book = CharacterBookDto(
            entries = listOf(
                CharacterBookEntryDto(name = "First", insertionOrder = 10),
                CharacterBookEntryDto(name = "Second", insertionOrder = 20, constant = true),
            ),
        )

        materializer.materialize(
            baseUrl = "http://b/",
            token = "t",
            characterId = "c",
            characterBook = book,
            characterDisplayName = LocalizedText("C", "角"),
            importLanguage = AppLanguage.English,
        ).getOrThrow()

        val requests = backend.entryRequests
        assertEquals(2, requests.size)
        assertEquals("First", requests[0].name.english)
        assertEquals(10, requests[0].insertionOrder)
        assertEquals("Second", requests[1].name.english)
        assertEquals(20, requests[1].insertionOrder)
        assertEquals(true, requests[1].constant)
    }

    @Test
    fun `entry request surfaces constant flag directly to LorebookEntry`() {
        val request = CharacterBookLorebookMaterializer.buildEntryRequest(
            entry = CharacterBookEntryDto(constant = true),
            importLanguage = AppLanguage.English,
        )
        assertTrue(request.constant)
    }

    @Test
    fun `entry request carries empty keysByLang when input keys are empty`() {
        val request = CharacterBookLorebookMaterializer.buildEntryRequest(
            entry = CharacterBookEntryDto(keys = emptyList()),
            importLanguage = AppLanguage.English,
        )
        assertEquals(emptyList<String>(), request.keysByLang.english)
        assertEquals(emptyList<String>(), request.keysByLang.chinese)
    }
}

private class RecordingWorldInfoBackend : ImWorldInfoClient {
    var createdLorebookId: String = "lb-created"
    var deletedLorebookId: String? = null
    val entryRequests: MutableList<CreateLorebookEntryRequestDto> = mutableListOf()
    val recordedBindings: MutableList<CreateLorebookBindingRequestDto> = mutableListOf()
    val assignedEntryIds: MutableList<String> = mutableListOf()
    var createEntryCalls: Int = 0
    var failOnCreateLorebook: Boolean = false
    var failOnCreateEntry: Boolean = false
    var failOnBind: Boolean = false
    private var nextEntryId: Int = 0

    override suspend fun list(baseUrl: String, token: String): LorebookListDto =
        LorebookListDto(lorebooks = emptyList())

    override suspend fun get(baseUrl: String, token: String, lorebookId: String): LorebookDto =
        LorebookDto(id = lorebookId, ownerId = "u", displayName = LocalizedTextDto("", ""))

    override suspend fun create(
        baseUrl: String,
        token: String,
        request: CreateLorebookRequestDto,
    ): LorebookDto {
        if (failOnCreateLorebook) error("create failed")
        return LorebookDto(
            id = createdLorebookId,
            ownerId = "u",
            displayName = request.displayName,
        )
    }

    override suspend fun update(
        baseUrl: String,
        token: String,
        lorebookId: String,
        request: UpdateLorebookRequestDto,
    ): LorebookDto = error("not used")

    override suspend fun delete(baseUrl: String, token: String, lorebookId: String) {
        deletedLorebookId = lorebookId
    }

    override suspend fun duplicate(
        baseUrl: String,
        token: String,
        lorebookId: String,
    ): LorebookDto = error("not used")

    override suspend fun listEntries(
        baseUrl: String,
        token: String,
        lorebookId: String,
    ): LorebookEntryListDto = LorebookEntryListDto(emptyList())

    override suspend fun createEntry(
        baseUrl: String,
        token: String,
        lorebookId: String,
        request: CreateLorebookEntryRequestDto,
    ): LorebookEntryDto {
        createEntryCalls += 1
        if (failOnCreateEntry) error("create entry failed")
        entryRequests += request
        val id = "entry-${nextEntryId++}"
        assignedEntryIds += id
        return LorebookEntryDto(
            id = id,
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
    ): LorebookEntryDto = error("not used")

    override suspend fun deleteEntry(
        baseUrl: String,
        token: String,
        lorebookId: String,
        entryId: String,
    ) = Unit

    override suspend fun listBindings(
        baseUrl: String,
        token: String,
        lorebookId: String,
    ): LorebookBindingListDto = LorebookBindingListDto(emptyList())

    var lastBindingLorebookId: String? = null

    override suspend fun bind(
        baseUrl: String,
        token: String,
        lorebookId: String,
        characterId: String,
        isPrimary: Boolean,
    ): LorebookBindingDto {
        if (failOnBind) error("bind failed")
        recordedBindings += CreateLorebookBindingRequestDto(
            characterId = characterId,
            isPrimary = isPrimary,
        )
        lastBindingLorebookId = lorebookId
        return LorebookBindingDto(
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
    ): LorebookBindingDto = error("not used")

    override suspend fun unbind(
        baseUrl: String,
        token: String,
        lorebookId: String,
        characterId: String,
    ) = Unit

    override suspend fun debugScan(
        baseUrl: String,
        token: String,
        characterId: String,
        scanText: String,
        devAccessHeader: String,
        allowDebug: Boolean,
    ): WorldInfoDebugScanResponseDto = WorldInfoDebugScanResponseDto()
}
