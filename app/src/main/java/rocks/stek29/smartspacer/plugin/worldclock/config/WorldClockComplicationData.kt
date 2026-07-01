package rocks.stek29.smartspacer.plugin.worldclock.config

import com.google.gson.annotations.SerializedName
import java.time.ZoneId

data class WorldClockComplicationData(
    @SerializedName("timezone")
    override val timezoneId: String = ZoneId.systemDefault().id,
    @SerializedName("mode")
    override val mode: Mode = Mode.HOME,
    @SerializedName("time_format")
    override val timeFormat: TimeFormat = TimeFormat.SYSTEM_DEFAULT,
    @SerializedName("custom_label")
    override val customLabel: String = "",
    @SerializedName("show_offset_label")
    override val showOffsetLabel: Boolean = false,
    @SerializedName("icon_style")
    override val iconStyle: IconStyle = IconStyle.HOME,
    @SerializedName("label_mode")
    private val storedLabelMode: LabelMode? = null
) : WorldClockSettings {
    override val labelMode: LabelMode
        get() = storedLabelMode ?: when {
            customLabel.isNotBlank() -> LabelMode.CUSTOM
            showOffsetLabel -> LabelMode.OFFSET
            else -> LabelMode.NONE
        }

    fun withLabelMode(labelMode: LabelMode): WorldClockComplicationData {
        return copy(
            storedLabelMode = labelMode,
            customLabel = if (labelMode == LabelMode.CUSTOM) customLabel else "",
            showOffsetLabel = labelMode == LabelMode.OFFSET
        )
    }

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

    enum class LabelMode {
        NONE,
        TIMEZONE_NAME,
        OFFSET,
        CUSTOM
    }
}
