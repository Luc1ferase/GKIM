package com.gkim.im.android.feature.settings

import com.gkim.im.android.core.designsystem.ContentPolicyCopy
import com.gkim.im.android.testing.FakePreferencesStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContentPolicyAcknowledgmentPresentationTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `initial ui state carries the current policy version and unacknowledged flags`() = runTest(dispatcher) {
        val prefs = FakePreferencesStore()
        val vm = ContentPolicyAcknowledgmentViewModel(
            submitter = { _ -> 0L },
            preferencesStore = prefs,
        )

        val state = vm.uiState.value
        assertEquals(ContentPolicyCopy.currentVersion, state.version)
        assertFalse("initial state should not be submitting", state.isSubmitting)
        assertFalse("initial state should not be acknowledged", state.isAcknowledged)
        assertNull("initial state should have no error", state.errorMessage)
    }

    @Test
    fun `bilingual policy copy is populated for english and chinese`() {
        assertTrue("english body must not be blank", ContentPolicyCopy.body.english.isNotBlank())
        assertTrue("chinese body must not be blank", ContentPolicyCopy.body.chinese.isNotBlank())
        assertTrue("english title must not be blank", ContentPolicyCopy.title.english.isNotBlank())
        assertTrue("chinese title must not be blank", ContentPolicyCopy.title.chinese.isNotBlank())
        assertTrue("english accept cta must not be blank", ContentPolicyCopy.acceptCta.english.isNotBlank())
        assertTrue("chinese accept cta must not be blank", ContentPolicyCopy.acceptCta.chinese.isNotBlank())
    }

    @Test
    fun `policy copy version constant is non-empty and stable`() {
        assertTrue(
            "policy version must be non-empty",
            ContentPolicyCopy.currentVersion.isNotBlank(),
        )
    }

    @Test
    fun `accept flow submits with current version and persists acknowledgment on success`() = runTest(dispatcher) {
        val prefs = FakePreferencesStore()
        var capturedVersion: String? = null
        val acceptedMillis = 1_700_000_000_000L
        val vm = ContentPolicyAcknowledgmentViewModel(
            submitter = { version ->
                capturedVersion = version
                acceptedMillis
            },
            preferencesStore = prefs,
        )

        vm.accept()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(ContentPolicyCopy.currentVersion, capturedVersion)
        assertTrue("state must transition to acknowledged on success", state.isAcknowledged)
        assertFalse("state must not remain submitting", state.isSubmitting)
        assertNull("error must be cleared on success", state.errorMessage)
        assertEquals(acceptedMillis, prefs.currentContentPolicyAcceptedAtMillis)
        assertEquals(ContentPolicyCopy.currentVersion, prefs.currentContentPolicyVersion)
    }

    @Test
    fun `accept flow uses clock fallback when backend returns zero accepted-at`() = runTest(dispatcher) {
        val prefs = FakePreferencesStore()
        val fixedClockMillis = 2_000_000_000_000L
        val vm = ContentPolicyAcknowledgmentViewModel(
            submitter = { _ -> 0L },
            preferencesStore = prefs,
            clock = { fixedClockMillis },
        )

        vm.accept()
        advanceUntilIdle()

        assertEquals(fixedClockMillis, prefs.currentContentPolicyAcceptedAtMillis)
        assertTrue(vm.uiState.value.isAcknowledged)
    }

    @Test
    fun `accept flow with custom version override forwards that version to the submitter`() = runTest(dispatcher) {
        val prefs = FakePreferencesStore()
        var capturedVersion: String? = null
        val override = "2030-01-01-override"
        val vm = ContentPolicyAcknowledgmentViewModel(
            submitter = { version ->
                capturedVersion = version
                1L
            },
            preferencesStore = prefs,
            version = override,
        )

        vm.accept()
        advanceUntilIdle()

        assertEquals(override, capturedVersion)
        assertEquals(override, prefs.currentContentPolicyVersion)
        assertEquals(override, vm.uiState.value.version)
    }

    @Test
    fun `accept flow surfaces error fallback when submitter throws`() = runTest(dispatcher) {
        val prefs = FakePreferencesStore()
        val vm = ContentPolicyAcknowledgmentViewModel(
            submitter = { _ -> throw IllegalStateException("network down") },
            preferencesStore = prefs,
        )

        vm.accept()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse("state must not be acknowledged on failure", state.isAcknowledged)
        assertFalse("state must stop submitting after failure", state.isSubmitting)
        assertNotNull("error must be surfaced on failure", state.errorMessage)
        assertEquals("network down", state.errorMessage)
        assertNull("preferences must not persist on failure", prefs.currentContentPolicyAcceptedAtMillis)
        assertEquals("", prefs.currentContentPolicyVersion)
    }

    @Test
    fun `accept flow is idempotent once acknowledged`() = runTest(dispatcher) {
        val prefs = FakePreferencesStore()
        var submitCount = 0
        val vm = ContentPolicyAcknowledgmentViewModel(
            submitter = { _ ->
                submitCount++
                1_000_000L
            },
            preferencesStore = prefs,
        )

        vm.accept()
        advanceUntilIdle()
        assertEquals(1, submitCount)

        vm.accept()
        advanceUntilIdle()
        assertEquals("second accept after success must not re-submit", 1, submitCount)
    }

    @Test
    fun `accept flow ignores a second accept while still submitting`() = runTest(dispatcher) {
        val prefs = FakePreferencesStore()
        var submitCount = 0
        val vm = ContentPolicyAcknowledgmentViewModel(
            submitter = { _ ->
                submitCount++
                kotlinx.coroutines.delay(50)
                1_000_000L
            },
            preferencesStore = prefs,
        )

        vm.accept()
        vm.accept()
        advanceUntilIdle()
        assertEquals("second accept while submitting must not re-submit", 1, submitCount)
    }

    @Test
    fun `clearError wipes the error without resetting acknowledgment`() = runTest(dispatcher) {
        val prefs = FakePreferencesStore()
        val vm = ContentPolicyAcknowledgmentViewModel(
            submitter = { _ -> throw IllegalStateException("boom") },
            preferencesStore = prefs,
        )

        vm.accept()
        advanceUntilIdle()
        assertEquals("boom", vm.uiState.value.errorMessage)

        vm.clearError()
        val state = vm.uiState.value
        assertNull("clearError must null out errorMessage", state.errorMessage)
        assertFalse("clearError must not mark acknowledged", state.isAcknowledged)
    }

    @Test
    fun `retry after a failure succeeds and persists`() = runTest(dispatcher) {
        val prefs = FakePreferencesStore()
        var callIndex = 0
        val vm = ContentPolicyAcknowledgmentViewModel(
            submitter = { _ ->
                callIndex++
                if (callIndex == 1) throw IllegalStateException("transient")
                777L
            },
            preferencesStore = prefs,
        )

        vm.accept()
        advanceUntilIdle()
        assertNotNull("first attempt must surface error", vm.uiState.value.errorMessage)

        vm.accept()
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue("retry must succeed", state.isAcknowledged)
        assertNull("retry success must clear error", state.errorMessage)
        assertEquals(777L, prefs.currentContentPolicyAcceptedAtMillis)
    }

    @Test
    fun `error fallback copy is available bilingually for UI to render`() {
        assertTrue(
            "error fallback english copy must be populated",
            ContentPolicyCopy.errorFallback.english.isNotBlank(),
        )
        assertTrue(
            "error fallback chinese copy must be populated",
            ContentPolicyCopy.errorFallback.chinese.isNotBlank(),
        )
    }

    @Test
    fun `accepted copy is available bilingually for UI to render`() {
        assertTrue(
            "accepted english copy must be populated",
            ContentPolicyCopy.acceptedCopy.english.isNotBlank(),
        )
        assertTrue(
            "accepted chinese copy must be populated",
            ContentPolicyCopy.acceptedCopy.chinese.isNotBlank(),
        )
    }

    @Test
    fun `accepting copy is available bilingually for UI to render`() {
        assertTrue(
            "accepting english copy must be populated",
            ContentPolicyCopy.accepting.english.isNotBlank(),
        )
        assertTrue(
            "accepting chinese copy must be populated",
            ContentPolicyCopy.accepting.chinese.isNotBlank(),
        )
    }
}
