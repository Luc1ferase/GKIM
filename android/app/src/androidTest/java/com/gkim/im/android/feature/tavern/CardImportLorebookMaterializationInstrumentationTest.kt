package com.gkim.im.android.feature.tavern

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gkim.im.android.data.remote.im.CardImportPreviewDto
import com.gkim.im.android.data.remote.im.CharacterBookDto
import com.gkim.im.android.data.remote.im.CharacterBookEntryDto
import com.gkim.im.android.data.remote.im.CompanionCharacterCardDto
import com.gkim.im.android.data.remote.im.ImBackendClient
import com.gkim.im.android.data.remote.im.ImBackendHttpClient
import com.gkim.im.android.data.remote.im.ImWorldInfoHttpClient
import com.gkim.im.android.data.remote.im.LocalizedTextDto
import com.gkim.im.android.data.repository.CardImportPreview
import com.gkim.im.android.data.repository.CharacterBookLorebookMaterializer
import com.gkim.im.android.data.repository.DefaultCardInteropRepository
import com.gkim.im.android.data.repository.LiveCardInteropRepository
import com.gkim.im.android.feature.navigation.LiveEndpointOverrides
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardImportLorebookMaterializationInstrumentationTest {
    companion object {
        private const val DEBUG_ACCESS_ARG = "liveImDebugAccessHeader"
        private const val DEV_USER_EXTERNAL_ID = "nox-dev"
        // Backend bind validates characterId against its preset roster; use a known preset id so the
        // binding step succeeds without a real `POST /api/cards/commit` creating the character.
        private const val PRESET_CHARACTER_ID = "architect-oracle"
    }

    private val httpBaseUrl = LiveEndpointOverrides.httpBaseUrl()
    private val devAccessHeader: String =
        InstrumentationRegistry.getArguments().getString(DEBUG_ACCESS_ARG)?.trim().orEmpty()

    private val httpClient = OkHttpClient.Builder().build()
    private val realBackendClient = ImBackendHttpClient(httpClient)
    private val worldInfoClient = ImWorldInfoHttpClient(httpClient)

    private lateinit var token: String
    private var committedCharacterId: String = PRESET_CHARACTER_ID
    private var createdLorebookId: String? = null

    @Before
    fun setUp() {
        runBlocking {
            val session = realBackendClient.issueDevSession(httpBaseUrl, DEV_USER_EXTERNAL_ID)
            token = session.token
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            val id = createdLorebookId ?: return@runBlocking
            runCatching { worldInfoClient.unbind(httpBaseUrl, token, id, committedCharacterId) }
            val entries = runCatching { worldInfoClient.listEntries(httpBaseUrl, token, id).entries }
                .getOrDefault(emptyList())
            entries.forEach { entry ->
                runCatching { worldInfoClient.deleteEntry(httpBaseUrl, token, id, entry.id) }
            }
            runCatching { worldInfoClient.delete(httpBaseUrl, token, id) }
        }
    }

    @Test
    fun commitImportMaterializesCharacterBookIntoLorebookWithPrimaryBinding() = runBlocking {
        val nonce = System.currentTimeMillis()
        val characterId = PRESET_CHARACTER_ID
        committedCharacterId = characterId

        val characterBook = CharacterBookDto(
            name = "Ancient Library $nonce",
            description = "Laws that govern the realm.",
            scanDepth = 4,
            tokenBudget = 768,
            recursiveScanning = true,
            entries = listOf(
                CharacterBookEntryDto(
                    keys = listOf("dragon"),
                    content = "The dragon guards its hoard of gold.",
                    name = "Dragon",
                    insertionOrder = 20,
                    comment = "keyword-gated",
                    extensions = buildJsonObject {
                        put("probability", JsonPrimitive(75))
                        put("position", JsonPrimitive("before_char"))
                    },
                ),
                CharacterBookEntryDto(
                    keys = emptyList(),
                    content = "An ancient law binds every citizen.",
                    name = "Constant",
                    constant = true,
                    insertionOrder = 10,
                    selective = false,
                ),
            ),
            extensions = buildJsonObject {
                put("customKey", JsonPrimitive("customValue"))
            },
        )

        val committedDto = CompanionCharacterCardDto(
            id = characterId,
            displayName = LocalizedTextDto(english = "Architect $nonce", chinese = "建筑师 $nonce"),
            roleLabel = LocalizedTextDto(english = "Guide", chinese = "向导"),
            summary = LocalizedTextDto(english = "Calm guide", chinese = "沉着的向导"),
            firstMes = LocalizedTextDto(english = "Hello.", chinese = "你好。"),
            avatarText = "AR",
            accent = "primary",
            source = "userauthored",
            characterBook = characterBook,
        )

        val backendStub = FakeCommitImBackendClient(commitResponse = committedDto)
        val capturedToken = token
        val repository = DefaultCardInteropRepository(
            delegate = LiveCardInteropRepository(
                backendClient = backendStub,
                baseUrlProvider = { httpBaseUrl },
                tokenProvider = { capturedToken },
                characterBookMaterializer = CharacterBookLorebookMaterializer(worldInfoClient),
            ),
        )

        val preview = CardImportPreview(
            previewToken = "preview-$nonce",
            card = committedDto.toCompanionCharacterCard(),
            rawCardDto = committedDto,
            detectedLanguage = "en",
            warnings = emptyList(),
            stExtensionKeys = emptyList(),
        )

        val commitResult = repository.commitImport(preview, overrides = null, languageOverride = null)
        assertTrue(
            "commitImport must succeed; error = ${commitResult.exceptionOrNull()?.message}",
            commitResult.isSuccess,
        )
        assertEquals(characterId, commitResult.getOrThrow().id)
        assertEquals(1, backendStub.importCommitCalls)

        val library = worldInfoClient.list(httpBaseUrl, token).lorebooks
        val createdLorebook = library.firstOrNull {
            it.displayName.english == "Ancient Library $nonce"
        }
        assertNotNull(
            "materialized lorebook must exist in the library; have=${library.map { it.displayName.english }}",
            createdLorebook,
        )
        createdLorebookId = createdLorebook!!.id

        assertEquals(768, createdLorebook.tokenBudget)
        val libraryExtensions = createdLorebook.extensions.jsonObject
        val libraryStNode = libraryExtensions["st"]?.jsonObject
        assertNotNull("lorebook must preserve st.* namespace", libraryStNode)
        assertEquals(
            "Ancient Library $nonce",
            libraryStNode!!["name"]!!.jsonPrimitive.content,
        )
        assertEquals(
            4,
            libraryStNode["scan_depth"]!!.jsonPrimitive.content.toInt(),
        )
        assertEquals(
            true,
            libraryStNode["recursive_scanning"]!!.jsonPrimitive.content.toBoolean(),
        )
        val preservedInnerExtensions = libraryStNode["extensions"]!!.jsonObject
        assertEquals(
            "customValue",
            preservedInnerExtensions["customKey"]!!.jsonPrimitive.content,
        )

        val entries = worldInfoClient.listEntries(httpBaseUrl, token, createdLorebook.id).entries
        assertEquals("materializer must seed both character_book entries", 2, entries.size)

        val dragonEntry = entries.firstOrNull { it.name.english == "Dragon" }
        assertNotNull("dragon entry must exist", dragonEntry)
        assertEquals(20, dragonEntry!!.insertionOrder)
        assertEquals("keyword-gated", dragonEntry.comment)
        assertEquals(listOf("dragon"), dragonEntry.keysByLang.english)
        val dragonStNode = dragonEntry.extensions.jsonObject["st"]?.jsonObject
        assertNotNull("dragon entry must preserve st.* namespace", dragonStNode)
        val dragonInner = dragonStNode!!["extensions"]!!.jsonObject
        assertEquals(75, dragonInner["probability"]!!.jsonPrimitive.content.toInt())
        assertEquals("before_char", dragonInner["position"]!!.jsonPrimitive.content)

        val constantEntry = entries.firstOrNull { it.name.english == "Constant" }
        assertNotNull("constant entry must exist", constantEntry)
        assertEquals(true, constantEntry!!.constant)
        assertEquals(10, constantEntry.insertionOrder)

        val bindings = worldInfoClient.listBindings(httpBaseUrl, token, createdLorebook.id).bindings
        val primaryBinding = bindings.firstOrNull { it.characterId == characterId }
        assertNotNull(
            "primary binding for $characterId must be present on the character detail surface; bindings=$bindings",
            primaryBinding,
        )
        assertTrue("binding must be flagged primary", primaryBinding!!.isPrimary)
    }
}

private class FakeCommitImBackendClient(
    private val commitResponse: CompanionCharacterCardDto,
) : ImBackendClient {
    var importCommitCalls: Int = 0

    override suspend fun issueDevSession(baseUrl: String, externalId: String) = error("unused")
    override suspend fun register(
        baseUrl: String,
        username: String,
        password: String,
        displayName: String,
    ) = error("unused")

    override suspend fun login(baseUrl: String, username: String, password: String) = error("unused")
    override suspend fun searchUsers(baseUrl: String, token: String, query: String) = error("unused")
    override suspend fun sendFriendRequest(baseUrl: String, token: String, toUserId: String) = error("unused")
    override suspend fun listFriendRequests(baseUrl: String, token: String) = error("unused")
    override suspend fun acceptFriendRequest(baseUrl: String, token: String, requestId: String) = error("unused")
    override suspend fun rejectFriendRequest(baseUrl: String, token: String, requestId: String) = error("unused")
    override suspend fun loadBootstrap(baseUrl: String, token: String) = error("unused")
    override suspend fun sendDirectImageMessage(
        baseUrl: String,
        token: String,
        request: com.gkim.im.android.data.remote.im.SendDirectImageMessageRequestDto,
    ) = error("unused")

    override suspend fun loadHistory(
        baseUrl: String,
        token: String,
        conversationId: String,
        limit: Int,
        before: String?,
    ) = error("unused")

    override suspend fun importCardCommit(
        baseUrl: String,
        token: String,
        preview: CardImportPreviewDto,
        overrides: CompanionCharacterCardDto?,
        languageOverride: String?,
    ): CompanionCharacterCardDto {
        importCommitCalls += 1
        return commitResponse
    }
}
