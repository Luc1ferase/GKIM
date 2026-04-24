package com.gkim.im.android.feature.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §2.1 verification — the alt-greeting picker exposes each option as a ~120-character
 * localized preview; tapping an option opens a modal whose commit CTA selects the option.
 *
 * The preview text itself is produced by a pure [truncatePreview] helper covered here; the
 * modal-invocation / selection-commit flow in the composable is structured so the same
 * helper drives the rendering (option.body -> preview text rendered in the row; option.body
 * -> full text rendered in the modal), meaning this test covers the data contract that the
 * composable relies on.
 */
class AltGreetingPickerPresentationTest {

    @Test
    fun `body shorter than the limit passes through unchanged`() {
        val body = "Short greeting."
        assertEquals(body, truncatePreview(body, limit = 120))
    }

    @Test
    fun `body exactly at the limit passes through unchanged (no ellipsis)`() {
        val body = "a".repeat(120)
        assertEquals(body, truncatePreview(body, limit = 120))
    }

    @Test
    fun `body just over the limit is truncated with an ellipsis`() {
        val body = "a".repeat(121)
        val out = truncatePreview(body, limit = 120)
        assertEquals(120 + 1, out.length) // 120 chars + single "…" codepoint
        assertTrue("must end with ellipsis character", out.endsWith("…"))
    }

    @Test
    fun `truncation trims trailing whitespace before appending ellipsis`() {
        // "..." + 117 'a's + 3 spaces = 123 chars; substring(0,120) = "..." + 117 'a'.
        // No trailing space inside the 120-char window → ellipsis appended directly.
        val body = "..." + "a".repeat(117) + "   "
        val out = truncatePreview(body, limit = 120)
        assertTrue(out.endsWith("a…"))

        // Trailing space inside the cut: "......" + 114 'a's + 4 spaces + more content.
        // First 120 chars end with "a" + two spaces; trimEnd removes the spaces, appends "…".
        val body2 = "......" + "a".repeat(112) + "  " + "rest of body"
        val out2 = truncatePreview(body2, limit = 120)
        assertTrue("trailing spaces must be trimmed before ellipsis", out2.endsWith("a…"))
    }

    @Test
    fun `Chinese content is counted by UTF-16 code units (one per BMP codepoint)`() {
        val body = "你好".repeat(80) // 160 code units = 160 "characters"
        val out = truncatePreview(body, limit = 120)
        // 120 code units + ellipsis. Since the string is pure BMP Chinese, cuts never split.
        assertEquals(121, out.length)
        assertTrue(out.endsWith("你…") || out.endsWith("好…"))
    }

    @Test
    fun `custom limit shorter than default still enforces truncation`() {
        val body = "a".repeat(50)
        val out = truncatePreview(body, limit = 20)
        assertEquals(21, out.length)
        assertTrue(out.endsWith("…"))
    }

    @Test
    fun `limit of zero yields an empty preview`() {
        assertEquals("", truncatePreview("anything at all", limit = 0))
    }

    @Test
    fun `negative limit is treated as zero and yields an empty preview`() {
        assertEquals("", truncatePreview("anything at all", limit = -5))
    }

    @Test
    fun `empty body returns empty regardless of limit`() {
        assertEquals("", truncatePreview("", limit = 120))
        assertEquals("", truncatePreview("", limit = 0))
    }

    @Test
    fun `option's preview never exceeds the configured limit by more than one codepoint`() {
        // The composable shows each option with truncatePreview(option.body, previewLimit).
        // Guarantee the contract the composable depends on.
        val bodies = listOf(
            "tiny",
            "a".repeat(120),
            "a".repeat(121),
            "你好".repeat(200),
            "line 1\nline 2\nline 3".repeat(20),
        )
        bodies.forEach { body ->
            val preview = truncatePreview(body, limit = 120)
            assertTrue(
                "preview of ${body.length}-char body is ${preview.length} chars: '$preview'",
                preview.length <= 120 + 1,
            )
        }
    }
}
