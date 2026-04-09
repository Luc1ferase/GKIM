package com.gkim.im.android.feature.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WelcomeAtmosphereAccentCatalogTest {
    @Test
    fun ctaAreaNoLongerIncludesStandalonePanelAccent() {
        val catalogClass = Class.forName("com.gkim.im.android.feature.navigation.WelcomeAtmosphereAccentCatalog")
        val instance = catalogClass.getField("INSTANCE").get(null)
        val method = catalogClass.getDeclaredMethod("visibleAccentSlots")
        val slots = method.invoke(instance) as List<*>
        val slotNames = slots.map { it.toString() }

        assertEquals(listOf("TopStartOrb", "CenterEndOrb"), slotNames)
        assertTrue("BottomStartCtaPanel" !in slotNames)
    }
}
