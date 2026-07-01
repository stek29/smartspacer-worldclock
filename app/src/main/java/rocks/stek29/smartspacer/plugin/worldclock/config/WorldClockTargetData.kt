package rocks.stek29.smartspacer.plugin.worldclock.config

import com.google.gson.annotations.SerializedName
import java.time.ZoneId

data class WorldClockTargetData(
    @SerializedName("timezone")
    override val timezoneId: String = ZoneId.systemDefault().id,
    @SerializedName("mode")
    override val mode: WorldClockComplicationData.Mode = WorldClockComplicationData.Mode.HOME,
    @SerializedName("time_format")
    override val timeFormat: WorldClockComplicationData.TimeFormat =
        WorldClockComplicationData.TimeFormat.SYSTEM_DEFAULT,
    @SerializedName("custom_label")
    override val customLabel: String = "",
    @SerializedName("show_offset_label")
    override val showOffsetLabel: Boolean = false,
    @SerializedName("icon_style")
    override val iconStyle: WorldClockComplicationData.IconStyle =
        WorldClockComplicationData.IconStyle.HOME,
    @SerializedName("hide_subtitle_on_aod")
    val hideSubtitleOnAod: Boolean = false
) : WorldClockSettings {
    fun toComplicationData(): WorldClockComplicationData {
        return WorldClockComplicationData(
            timezoneId = timezoneId,
            mode = mode,
            timeFormat = timeFormat,
            customLabel = customLabel,
            showOffsetLabel = showOffsetLabel,
            iconStyle = iconStyle
        )
    }
}
