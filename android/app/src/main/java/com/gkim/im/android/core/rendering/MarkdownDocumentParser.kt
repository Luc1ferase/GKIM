package com.gkim.im.android.core.rendering

import com.gkim.im.android.core.model.RichBlock
import com.gkim.im.android.core.model.RichDocument
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.Heading
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.node.Text
import org.commonmark.parser.Parser

class MarkdownDocumentParser {
    private val parser = Parser.builder().build()

    fun parse(markdown: String, mdxReady: Boolean = false, cssHint: String? = null): RichDocument {
        val root = parser.parse(markdown)
        val blocks = mutableListOf<RichBlock>()
        var cursor = root.firstChild
        while (cursor != null) {
            cursor.toRichBlocks()?.let { blocks += it }
            cursor = cursor.next
        }
        return RichDocument(blocks = blocks, mdxReady = mdxReady, cssHint = cssHint)
    }

    private fun Node.toRichBlocks(): List<RichBlock>? = when (this) {
        is Heading -> listOf(RichBlock.Heading(level, collectText(this)))
        is Paragraph -> listOf(RichBlock.Paragraph(collectText(this)))
        is BlockQuote -> listOf(RichBlock.Quote(collectText(this)))
        is FencedCodeBlock -> listOf(RichBlock.Code(info.takeIf { it.isNotBlank() }, literal.trimEnd()))
        is BulletList -> listOf(RichBlock.BulletList(collectListItems(this)))
        is Code -> listOf(RichBlock.Code(language = null, code = literal))
        else -> null
    }

    private fun collectText(node: Node): String {
        val builder = StringBuilder()
        var cursor: Node? = node.firstChild
        while (cursor != null) {
            when (cursor) {
                is Text -> builder.append(cursor.literal)
                is Code -> builder.append(cursor.literal)
                else -> builder.append(collectText(cursor))
            }
            cursor = cursor.next
        }
        return builder.toString().trim()
    }

    private fun collectListItems(node: BulletList): List<String> {
        val items = mutableListOf<String>()
        var cursor: Node? = node.firstChild
        while (cursor != null) {
            if (cursor is ListItem) {
                items += collectText(cursor)
            }
            cursor = cursor.next
        }
        return items
    }
}
