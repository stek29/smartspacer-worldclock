package rocks.stek29.smartspacer.plugin.worldclock.config

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class WorldClockComplicationDataTest {

    @Test
    fun defaultsMatchDefaults() {
        val data = WorldClockComplicationData()

        assertEquals(ZoneId.systemDefault().id, data.timezoneId)
        assertEquals(WorldClockComplicationData.Mode.HOME, data.mode)
        assertEquals(WorldClockComplicationData.TimeFormat.SYSTEM_DEFAULT, data.timeFormat)
        assertEquals("", data.customLabel)
        assertEquals(false, data.showOffsetLabel)
    }

    @Test
    fun gsonRoundTripPreservesData() {
        val gson = Gson()
        val data = WorldClockComplicationData(
            timezoneId = "Asia/Tokyo",
            mode = WorldClockComplicationData.Mode.NORMAL,
            timeFormat = WorldClockComplicationData.TimeFormat.HOUR_24,
            customLabel = "Tokyo",
            showOffsetLabel = true
        )

        val restored = gson.fromJson(gson.toJson(data), WorldClockComplicationData::class.java)

        assertEquals(data, restored)
        assertTrue(gson.toJson(data).contains("time_format"))
    }
}
