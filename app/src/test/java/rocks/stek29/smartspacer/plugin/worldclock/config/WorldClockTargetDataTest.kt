package rocks.stek29.smartspacer.plugin.worldclock.config

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class WorldClockTargetDataTest {

    @Test
    fun defaultsMatchDefaults() {
        val data = WorldClockTargetData()

        assertEquals(ZoneId.systemDefault().id, data.timezoneId)
        assertEquals(WorldClockComplicationData.Mode.HOME, data.mode)
        assertEquals(WorldClockComplicationData.TimeFormat.SYSTEM_DEFAULT, data.timeFormat)
        assertEquals("", data.customLabel)
        assertFalse(data.showOffsetLabel)
        assertEquals(WorldClockComplicationData.IconStyle.HOME, data.iconStyle)
        assertEquals(WorldClockComplicationData.LabelMode.TIMEZONE_NAME, data.labelMode)
        assertFalse(data.showLabelInSubtitle)
        assertFalse(data.hideSubtitleOnAod)
    }

    @Test
    fun gsonRoundTripPreservesTargetData() {
        val gson = Gson()
        val data = WorldClockTargetData(
            timezoneId = "Asia/Tokyo",
            mode = WorldClockComplicationData.Mode.NORMAL,
            timeFormat = WorldClockComplicationData.TimeFormat.HOUR_24,
            customLabel = "Tokyo",
            showOffsetLabel = true,
            showLabelInSubtitle = true,
            hideSubtitleOnAod = true
        )

        val restored = gson.fromJson(gson.toJson(data), WorldClockTargetData::class.java)

        assertEquals(data, restored)
        assertTrue(gson.toJson(data).contains("show_label_in_subtitle"))
        assertTrue(gson.toJson(data).contains("hide_subtitle_on_aod"))
    }

    @Test
    fun labelModeNoneClearsLegacyLabelFields() {
        val data = WorldClockTargetData(
            customLabel = "Tokyo",
            showOffsetLabel = true
        ).withLabelMode(WorldClockComplicationData.LabelMode.NONE)

        assertEquals(WorldClockComplicationData.LabelMode.NONE, data.labelMode)
        assertEquals("", data.customLabel)
        assertFalse(data.showOffsetLabel)
    }
}
