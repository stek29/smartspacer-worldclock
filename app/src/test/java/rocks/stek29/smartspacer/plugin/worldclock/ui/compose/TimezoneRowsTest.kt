package rocks.stek29.smartspacer.plugin.worldclock.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimezoneRowsTest {

    @Test
    fun legacyThreeLetterAliasesAreHidden() {
        assertTrue(isLegacyAlias("ACT"))
        assertTrue(isLegacyAlias("AET"))
        assertFalse(isLegacyAlias("UTC"))
        assertFalse(isLegacyAlias("GMT"))
        assertFalse(isLegacyAlias("Africa/Abidjan"))
    }

    @Test
    fun filterTimezoneRowsMatchesSearchTextCaseInsensitively() {
        val rows = listOf(
            row("Asia/Almaty", "Almaty Time GMT+5"),
            row("Europe/Paris", "Central European Time GMT+1")
        )

        assertEquals(listOf(rows[0]), filterTimezoneRows(rows, "almaty"))
        assertEquals(listOf(rows[1]), filterTimezoneRows(rows, "central"))
        assertEquals(rows, filterTimezoneRows(rows, " "))
        assertEquals(emptyList<TimezoneRow>(), filterTimezoneRows(rows, "not-here"))
    }

    private fun row(id: String, searchText: String): TimezoneRow {
        return TimezoneRow(
            id = id,
            title = id,
            subtitle = searchText,
            offset = searchText.substringAfterLast(' '),
            searchText = searchText
        )
    }
}
