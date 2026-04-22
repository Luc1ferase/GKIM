package com.gkim.im.android.feature.settings.worldinfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gkim.im.android.core.designsystem.LocalAppLanguage
import com.gkim.im.android.core.designsystem.PageHeader
import com.gkim.im.android.core.designsystem.pick
import com.gkim.im.android.data.repository.AppContainer

@Composable
fun WorldInfoLibraryRoute(
    @Suppress("UNUSED_PARAMETER") container: AppContainer,
    onBack: () -> Unit,
) {
    val appLanguage = LocalAppLanguage.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("worldinfo-library-screen"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        PageHeader(
            eyebrow = appLanguage.pick("Settings", "设置"),
            title = appLanguage.pick("World Info", "世界信息"),
            description = appLanguage.pick(
                "Manage lorebooks bound to companion characters.",
                "管理绑定到伙伴角色的世界书。",
            ),
            leadingLabel = appLanguage.pick("Back", "返回"),
            onLeading = onBack,
        )
        Text(
            modifier = Modifier.testTag("worldinfo-library-placeholder"),
            text = appLanguage.pick(
                "Lorebook management is coming soon.",
                "世界书管理功能即将推出。",
            ),
        )
    }
}
