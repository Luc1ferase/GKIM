package com.gkim.im.android.core.model

sealed interface RichBlock {
    data class Heading(val level: Int, val text: String) : RichBlock
    data class Paragraph(val text: String) : RichBlock
    data class Quote(val text: String) : RichBlock
    data class Code(val language: String?, val code: String) : RichBlock
    data class BulletList(val items: List<String>) : RichBlock
}

data class RichDocument(
    val blocks: List<RichBlock>,
    val mdxReady: Boolean = false,
    val cssHint: String? = null,
)
