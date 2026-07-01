package rocks.stek29.smartspacer.plugin.worldclock.config

interface WorldClockSettings {
    val timezoneId: String
    val mode: WorldClockComplicationData.Mode
    val timeFormat: WorldClockComplicationData.TimeFormat
    val customLabel: String
    val showOffsetLabel: Boolean
    val iconStyle: WorldClockComplicationData.IconStyle
}
