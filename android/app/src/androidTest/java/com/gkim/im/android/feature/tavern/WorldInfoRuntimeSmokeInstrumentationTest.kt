package com.gkim.im.android.feature.tavern

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gkim.im.android.data.remote.im.CreateLorebookEntryRequestDto
import com.gkim.im.android.data.remote.im.CreateLorebookRequestDto
import com.gkim.im.android.data.remote.im.ImBackendHttpClient
import com.gkim.im.android.data.remote.im.ImWorldInfoHttpClient
import com.gkim.im.android.data.remote.im.LocalizedTextDto
import com.gkim.im.android.data.remote.im.PerLanguageStringListDto
import com.gkim.im.android.feature.navigation.LiveEndpointOverrides
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorldInfoRuntimeSmokeInstrumentationTest {
    companion object {
        private const val DEBUG_ACCESS_ARG = "liveImDebugAccessHeader"
        private const val DEV_USER_EXTERNAL_ID = "nox-dev"
        private const val TARGET_CHARACTER_ID = "architect-oracle"
        private const val SCAN_TEXT = "the dragon roars across the battlefield"
        private const val DRAGON_KEY = "dragon"
        private const val CROWN_KEY = "crown"
        private const val CONSTANT_INSERTION_ORDER = 10
        private const val DRAGON_INSERTION_ORDER = 20
        private const val CROWN_INSERTION_ORDER = 30
    }

    private val httpBaseUrl = LiveEndpointOverrides.httpBaseUrl()
    private val devAccessHeader: String =
        InstrumentationRegistry.getArguments().getString(DEBUG_ACCESS_ARG)?.trim().orEmpty()

    private val backendClient = ImBackendHttpClient(OkHttpClient.Builder().build())
    private val worldInfoClient = ImWorldInfoHttpClient(OkHttpClient.Builder().build())

    private lateinit var token: String
    private var lorebookId: String? = null
    private var constantEntryId: String? = null
    private var dragonEntryId: String? = null
    private var crownEntryId: String? = null

    @Before
    fun setUp() {
        runBlocking {
            assertTrue(
                "Instrumentation arg `$DEBUG_ACCESS_ARG` must be supplied to run this smoke test; " +
                    "pass `-Pandroid.testInstrumentationRunnerArguments.$DEBUG_ACCESS_ARG=<value>` " +
                    "matching APP_DEBUG_ACCESS_KEY on the deployed backend.",
                devAccessHeader.isNotBlank(),
            )

            val session = backendClient.issueDevSession(httpBaseUrl, DEV_USER_EXTERNAL_ID)
            token = session.token
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            val id = lorebookId ?: return@runBlocking
            runCatching {
                worldInfoClient.unbind(httpBaseUrl, token, id, TARGET_CHARACTER_ID)
            }
            listOfNotNull(constantEntryId, dragonEntryId, crownEntryId).forEach { entryId ->
                runCatching { worldInfoClient.deleteEntry(httpBaseUrl, token, id, entryId) }
            }
            runCatching { worldInfoClient.delete(httpBaseUrl, token, id) }
        }
    }

    @Test
    fun debugScanReturnsConstantAndMatchingKeywordEntry() = runBlocking {
        val nonce = System.currentTimeMillis().toString()

        val lorebook = worldInfoClient.create(
            baseUrl = httpBaseUrl,
            token = token,
            request = CreateLorebookRequestDto(
                displayName = LocalizedTextDto(
                    english = "WorldInfo smoke $nonce",
                    chinese = "世界信息冒烟 $nonce",
                ),
            ),
        )
        lorebookId = lorebook.id

        val constantEntry = worldInfoClient.createEntry(
            baseUrl = httpBaseUrl,
            token = token,
            lorebookId = lorebook.id,
            request = CreateLorebookEntryRequestDto(
                name = LocalizedTextDto(english = "Constant", chinese = "常驻"),
                content = LocalizedTextDto(
                    english = "The realm is ruled by ancient laws.",
                    chinese = "古老法则统御王国。",
                ),
                constant = true,
                insertionOrder = CONSTANT_INSERTION_ORDER,
            ),
        )
        constantEntryId = constantEntry.id

        val dragonEntry = worldInfoClient.createEntry(
            baseUrl = httpBaseUrl,
            token = token,
            lorebookId = lorebook.id,
            request = CreateLorebookEntryRequestDto(
                name = LocalizedTextDto(english = "Dragon", chinese = "巨龙"),
                keysByLang = PerLanguageStringListDto(english = listOf(DRAGON_KEY)),
                content = LocalizedTextDto(
                    english = "The dragon guards its hoard of gold.",
                    chinese = "巨龙守护着成堆的黄金。",
                ),
                insertionOrder = DRAGON_INSERTION_ORDER,
            ),
        )
        dragonEntryId = dragonEntry.id

        val crownEntry = worldInfoClient.createEntry(
            baseUrl = httpBaseUrl,
            token = token,
            lorebookId = lorebook.id,
            request = CreateLorebookEntryRequestDto(
                name = LocalizedTextDto(english = "Crown", chinese = "王冠"),
                keysByLang = PerLanguageStringListDto(english = listOf(CROWN_KEY)),
                content = LocalizedTextDto(
                    english = "The crown grants sovereignty over lands.",
                    chinese = "王冠授予统御之权。",
                ),
                insertionOrder = CROWN_INSERTION_ORDER,
            ),
        )
        crownEntryId = crownEntry.id

        worldInfoClient.bind(
            baseUrl = httpBaseUrl,
            token = token,
            lorebookId = lorebook.id,
            characterId = TARGET_CHARACTER_ID,
            isPrimary = false,
        )

        val response = worldInfoClient.debugScan(
            baseUrl = httpBaseUrl,
            token = token,
            characterId = TARGET_CHARACTER_ID,
            scanText = SCAN_TEXT,
            devAccessHeader = devAccessHeader,
            allowDebug = true,
        )

        val ourMatches = response.matches.filter { it.lorebookId == lorebook.id }
        val ourEntryIds = ourMatches.map { it.entryId }.toSet()
        assertTrue(
            "debug scan should include the constant entry ${constantEntry.id}; our matches=$ourEntryIds",
            ourEntryIds.contains(constantEntry.id),
        )
        assertTrue(
            "debug scan should include the dragon keyword entry ${dragonEntry.id}; our matches=$ourEntryIds",
            ourEntryIds.contains(dragonEntry.id),
        )
        assertFalse(
            "debug scan must NOT include the crown entry ${crownEntry.id} when scan text only contains 'dragon'; our matches=$ourEntryIds",
            ourEntryIds.contains(crownEntry.id),
        )
        assertEquals(
            "only the constant entry and the dragon entry from this test's lorebook should match",
            2,
            ourMatches.size,
        )

        val constantMatch = ourMatches.first { it.entryId == constantEntry.id }
        assertTrue("constant entry flag should be true", constantMatch.constant)
        assertNull("constant entry match should not carry a matched key", constantMatch.matchedKey)
        assertNull("constant entry match should not carry a language", constantMatch.language)
        assertEquals(CONSTANT_INSERTION_ORDER, constantMatch.insertionOrder)

        val dragonMatch = ourMatches.first { it.entryId == dragonEntry.id }
        assertFalse("dragon entry is keyword-gated, not constant", dragonMatch.constant)
        assertEquals(DRAGON_KEY, dragonMatch.matchedKey)
        assertEquals("english", dragonMatch.language)
        assertEquals(DRAGON_INSERTION_ORDER, dragonMatch.insertionOrder)

        val expectedOrder = response.matches
            .sortedWith(compareBy({ it.insertionOrder }, { it.lorebookId }, { it.entryId }))
            .map { Triple(it.insertionOrder, it.lorebookId, it.entryId) }
        val actualOrder = response.matches
            .map { Triple(it.insertionOrder, it.lorebookId, it.entryId) }
        assertEquals(
            "debug scan results must be in (insertionOrder asc, lorebookId asc, entryId asc) order",
            expectedOrder,
            actualOrder,
        )
    }
}
