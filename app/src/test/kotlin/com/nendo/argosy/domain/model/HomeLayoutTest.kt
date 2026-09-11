package com.nendo.argosy.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins which layout answers "show every game" and that the carousel's own flag persists.
 *
 * The predicate used to name AUTO_GRID directly, in three separate copies. What must hold now is
 * that it follows whichever layout is selected: a carousel that reads the grid's flag, or a grid
 * that reads the carousel's, would silently uncap the wrong home.
 */
class HomeLayoutTest {

    @Test
    fun `show every game follows the selected layout`() {
        val onlyGridFlagged = HomeLayoutSettings(
            selected = HomeLayoutKind.CAROUSEL,
            autoGrid = AutoGridConfig(showAllGames = true)
        )
        assertFalse(onlyGridFlagged.showsEveryGame)

        val carouselFlagged = HomeLayoutSettings(
            selected = HomeLayoutKind.CAROUSEL,
            carousel = CarouselConfig(showAllGames = true)
        )
        assertTrue(carouselFlagged.showsEveryGame)

        val gridSelected = HomeLayoutSettings(
            selected = HomeLayoutKind.AUTO_GRID,
            autoGrid = AutoGridConfig(showAllGames = true)
        )
        assertTrue(gridSelected.showsEveryGame)
    }

    @Test
    fun `the custom grid has no every-game mode`() {
        val custom = HomeLayoutSettings(
            selected = HomeLayoutKind.CUSTOM_GRID,
            carousel = CarouselConfig(showAllGames = true),
            autoGrid = AutoGridConfig(showAllGames = true)
        )
        assertFalse(custom.showsEveryGame)
    }

    @Test
    fun `the carousel flag survives a round trip without disturbing the grid`() {
        val settings = HomeLayoutSettings(
            selected = HomeLayoutKind.CAROUSEL,
            carousel = CarouselConfig(showAllGames = true),
            autoGrid = AutoGridConfig(showAllGames = false)
        )
        val restored = HomeLayoutSettings.fromJson(settings.toJson())
        assertTrue(restored.carousel.showAllGames)
        assertFalse(restored.autoGrid.showAllGames)
        assertTrue(restored.showsEveryGame)
    }

    @Test
    fun `a layout stored before the carousel flag existed stays capped`() {
        val stored = """{"selected":"CAROUSEL","carousel":{"showPlatformBadge":false}}"""
        val restored = HomeLayoutSettings.fromJson(stored)
        assertFalse(restored.carousel.showAllGames)
        assertFalse(restored.showsEveryGame)
        assertEquals(false, restored.carousel.showPlatformBadge)
    }
}
