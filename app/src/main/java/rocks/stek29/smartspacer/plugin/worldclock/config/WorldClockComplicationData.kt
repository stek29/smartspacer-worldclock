package rocks.stek29.smartspacer.plugin.worldclock.config

import com.google.gson.annotations.SerializedName
import java.time.ZoneId

data class WorldClockComplicationData(
    @SerializedName("timezone")
    val timezoneId: String = ZoneId.systemDefault().id,
    @SerializedName("mode")
    val mode: Mode = Mode.HOME,
    @SerializedName("time_format")
    val timeFormat: TimeFormat = TimeFormat.SYSTEM_DEFAULT,
    @SerializedName("custom_label")
    val customLabel: String = "",
    @SerializedName("show_offset_label")
    val showOffsetLabel: Boolean = false
) {
    enum class Mode {
        NORMAL,
        HOME
    }

    enum class TimeFormat {
        SYSTEM_DEFAULT,
        HOUR_12,
        HOUR_24
    }
}
