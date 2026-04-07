package com.gkim.im.android.core.designsystem

import androidx.compose.runtime.staticCompositionLocalOf
import com.gkim.im.android.core.model.AppLanguage

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.English }

fun AppLanguage.pick(english: String, chinese: String): String = when (this) {
    AppLanguage.English -> english
    AppLanguage.Chinese -> chinese
}
