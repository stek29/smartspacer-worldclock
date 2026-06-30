package rocks.stek29.smartspacer.plugin.worldclock.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockComplicationData
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class TimeFormatterTest {

    @Test
    fun homeModeComparesOffsetsAtCurrentInstant() {
        val winter = fixedClock("2026-01-15T12:00:00Z")
        val summer = fixedClock("2026-07-15T12:00:00Z")
        val berlin = ZoneId.of("Europe/Berlin")
        val lagos = ZoneId.of("Africa/Lagos")

        assertEquals(false, TimeFormatter.hasDifferentOffset(berlin, lagos, winter))
        assertEquals(true, TimeFormatter.hasDifferentOffset(berlin, lagos, summer))
    }

    @Test
    fun normalModeAlwaysVisible() {
        val data = WorldClockComplicationData(mode = WorldClockComplicationData.Mode.NORMAL)

        assertEquals(true, TimeFormatter.isVisible(data, fixedClock("2026-01-15T12:00:00Z")))
    }

    @Test
    fun labelIsTruncatedBeforeTime() {
        val content = TimeFormatter.truncateContent("09:30", "TokyoStation")

        assertEquals("09:30·TokyoS", content)
        assertTrue(content.length <= TimeFormatter.MAX_CONTENT_LENGTH)
    }

    @Test
    fun separatorIsDroppedWhenLabelCannotFit() {
        assertEquals("12:34:56 PM", TimeFormatter.truncateContent("12:34:56 PM", "NY"))
        assertEquals("123456789012", TimeFormatter.truncateContent("123456789012", "NY"))
    }

    @Test
    fun customLabelWinsBeforeOffsetLabelSelection() {
        val data = WorldClockComplicationData(
            timezoneId = "Asia/Tokyo",
            customLabel = "Tokyo",
            showOffsetLabel = true
        )

        val content = TimeFormatter.truncateContent("21:00", data.customLabel)

        assertEquals("21:00·Tokyo", content)
    }

    @Test
    fun offsetLabelUsesDisplayedTimezone() {
        val clock = fixedClock("2026-06-30T12:00:00Z")
        val tokyo = TimeFormatter.formatOffset(ZoneId.of("Asia/Tokyo"), clock, Locale.US)
        val london = TimeFormatter.formatOffset(ZoneId.of("Europe/London"), clock, Locale.US)

        assertNotEquals(tokyo, london)
        assertTrue(tokyo.contains("+9") || tokyo.contains("+09"))
    }

    @Test
    fun explicitTimeFormatBindsSelectedTimezone() {
        val clock = fixedClock("2026-06-30T12:00:00Z")

        val tokyo = TimeFormatter.formatTime(
            ZoneId.of("Asia/Tokyo"),
            WorldClockComplicationData.TimeFormat.HOUR_24,
            clock,
            Locale.US
        )
        val london = TimeFormatter.formatTime(
            ZoneId.of("Europe/London"),
            WorldClockComplicationData.TimeFormat.HOUR_24,
            clock,
            Locale.US
        )

        assertEquals("21:00", tokyo)
        assertEquals("13:00", london)
    }

    private fun fixedClock(instant: String): Clock {
        return Clock.fixed(Instant.parse(instant), ZoneId.of("UTC"))
    }
}
