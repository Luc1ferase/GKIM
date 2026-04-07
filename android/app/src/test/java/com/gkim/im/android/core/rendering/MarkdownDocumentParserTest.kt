package com.gkim.im.android.core.rendering

import com.gkim.im.android.core.model.RichBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownDocumentParserTest {
    private val parser = MarkdownDocumentParser()

    @Test
    fun `parse builds typed rich document blocks`() {
        val document = parser.parse(
            markdown = "# Blueprint\n\n> orbit note\n\n```kotlin\nval signal = 1\n```\n\n- alpha\n- beta",
            mdxReady = true,
            cssHint = "architectural-glitch",
        )

        assertTrue(document.blocks[0] is RichBlock.Heading)
        assertEquals("Blueprint", (document.blocks[0] as RichBlock.Heading).text)
        assertEquals("orbit note", (document.blocks[1] as RichBlock.Quote).text)
        assertEquals("val signal = 1", (document.blocks[2] as RichBlock.Code).code)
        assertEquals(listOf("alpha", "beta"), (document.blocks[3] as RichBlock.BulletList).items)
        assertTrue(document.mdxReady)
        assertEquals("architectural-glitch", document.cssHint)
    }
}
