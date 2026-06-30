package rocks.stek29.smartspacer.plugin.worldclock.config

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class TimezoneOffsetRequirementDataTest {

    @Test
    fun defaultsToDeviceTimezone() {
        val data = TimezoneOffsetRequirementData()

        assertEquals(ZoneId.systemDefault().id, data.timezoneId)
    }

    @Test
    fun gsonRoundTripPreservesTimezone() {
        val gson = Gson()
        val data = TimezoneOffsetRequirementData("Asia/Tokyo")

        val restored = gson.fromJson(gson.toJson(data), TimezoneOffsetRequirementData::class.java)

        assertEquals(data, restored)
    }
}
