package rocks.stek29.smartspacer.plugin.worldclock.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockComplicationData
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockTargetData
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
    fun timezoneOffsetRequirementPredicateIsMetOnlyForDifferentOffsets() {
        val clock = fixedClock("2026-06-30T12:00:00Z")

        assertEquals(
            true,
            TimeFormatter.hasDifferentOffset(
                ZoneId.of("Asia/Tokyo"),
                ZoneId.of("Europe/Moscow"),
                clock
            )
        )
        assertEquals(
            false,
            TimeFormatter.hasDifferentOffset(
                ZoneId.of("Europe/Istanbul"),
                ZoneId.of("Europe/Moscow"),
                clock
            )
        )
    }

    @Test
    fun normalModeAlwaysVisible() {
        val data = WorldClockComplicationData(mode = WorldClockComplicationData.Mode.NORMAL)

        assertEquals(true, TimeFormatter.isVisible(data, fixedClock("2026-01-15T12:00:00Z")))
    }

    @Test
    fun labelIsTruncatedBeforeTime() {
        val content = TimeFormatter.truncateContent("09:30", "TokyoStation")

        assertEquals("09:30 TokyoS", content)
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

        assertEquals("21:00 Tokyo", content)
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
    fun offsetLabelFallsBackToCompactSignedOffset() {
        val clock = fixedClock("2026-06-30T12:00:00Z")

        val content = TimeFormatter.truncateOffsetContent(
            time = "9:00 PM",
            zone = ZoneId.of("Asia/Tokyo"),
            clock = clock,
            locale = Locale.US
        )

        assertEquals("9:00 PM +9", content)
        assertTrue(content.length <= TimeFormatter.MAX_CONTENT_LENGTH)
    }

    @Test
    fun halfHourOffsetCanUseDecimalFallback() {
        val clock = fixedClock("2026-06-30T12:00:00Z")

        val content = TimeFormatter.truncateOffsetContent(
            time = "9:00 PM",
            zone = ZoneId.of("Asia/Kolkata"),
            clock = clock,
            locale = Locale.US
        )

        assertEquals("9:00 PM +5.5", content)
        assertTrue(content.length <= TimeFormatter.MAX_CONTENT_LENGTH)
    }

    @Test
    fun offsetLabelIsOmittedWhenCompactValueDoesNotFit() {
        val clock = fixedClock("2026-06-30T12:00:00Z")

        val content = TimeFormatter.truncateOffsetContent(
            time = "11:00 PM",
            zone = ZoneId.of("Asia/Kolkata"),
            clock = clock,
            locale = Locale.US
        )

        assertEquals("11:00 PM", content)
        assertTrue(content.length <= TimeFormatter.MAX_CONTENT_LENGTH)
    }

    @Test
    fun offsetLabelUsesFullGmtLabelWhenItFits() {
        val clock = fixedClock("2026-06-30T12:00:00Z")

        val content = TimeFormatter.truncateOffsetContent(
            time = "09:00",
            zone = ZoneId.of("Asia/Tokyo"),
            clock = clock,
            locale = Locale.US
        )

        assertEquals("09:00 GMT+9", content)
        assertTrue(content.length <= TimeFormatter.MAX_CONTENT_LENGTH)
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

    @Test
    fun targetSubtitleUsesUntrimmedCustomLabel() {
        val data = WorldClockTargetData(
            customLabel = "Tokyo Station Long Label"
        )

        assertEquals("Tokyo Station Long Label", TimeFormatter.buildTargetSubtitle(data))
    }

    @Test
    fun targetSubtitleUsesOffsetWhenEnabled() {
        val clock = fixedClock("2026-06-30T12:00:00Z")
        val data = WorldClockTargetData(
            timezoneId = "Asia/Tokyo",
            showOffsetLabel = true
        )

        assertEquals("GMT+9", TimeFormatter.buildTargetSubtitle(data, clock, Locale.US))
    }

    private fun fixedClock(instant: String): Clock {
        return Clock.fixed(Instant.parse(instant), ZoneId.of("UTC"))
    }
}
