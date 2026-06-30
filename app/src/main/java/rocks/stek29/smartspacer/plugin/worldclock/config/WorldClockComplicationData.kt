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
    val showOffsetLabel: Boolean = false,
    @SerializedName("icon_style")
    val iconStyle: IconStyle = IconStyle.WORLD_CLOCK
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

    enum class IconStyle {
        WORLD_CLOCK,
        HOME,
        HEART,
        WORK,
        TRAVEL,
        GLOBE
    }
}
