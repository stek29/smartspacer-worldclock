package rocks.stek29.smartspacer.plugin.worldclock.utils

import android.content.Context
import android.text.format.DateFormat
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockComplicationData
import java.text.SimpleDateFormat
import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeFormatter {

    const val MAX_CONTENT_LENGTH = 12
    private const val SEPARATOR = "·"

    fun isVisible(data: WorldClockComplicationData, clock: Clock = Clock.systemUTC()): Boolean {
        return when (data.mode) {
            WorldClockComplicationData.Mode.NORMAL -> true
            WorldClockComplicationData.Mode.HOME -> hasDifferentOffset(
                homeZone = ZoneId.of(data.timezoneId),
                deviceZone = ZoneId.systemDefault(),
                clock = clock
            )
        }
    }

    fun hasDifferentOffset(homeZone: ZoneId, deviceZone: ZoneId, clock: Clock): Boolean {
        val instant = clock.instant()
        return homeZone.rules.getOffset(instant) != deviceZone.rules.getOffset(instant)
    }

    fun buildContent(
        context: Context,
        data: WorldClockComplicationData,
        clock: Clock = Clock.systemUTC(),
        locale: Locale = Locale.getDefault()
    ): String {
        val zone = ZoneId.of(data.timezoneId)
        val time = formatTime(context, zone, data.timeFormat, clock)
            .let { if (it.length <= MAX_CONTENT_LENGTH) it else formatCompactTime(zone, clock, locale) }
        val label = when {
            data.customLabel.isNotBlank() -> data.customLabel.trim()
            data.showOffsetLabel -> formatOffset(zone, clock, locale)
            else -> ""
        }
        return truncateContent(time, label)
    }

    fun formatTime(
        context: Context,
        zone: ZoneId,
        format: WorldClockComplicationData.TimeFormat,
        clock: Clock = Clock.systemUTC()
    ): String {
        val dateFormat = when (format) {
            WorldClockComplicationData.TimeFormat.SYSTEM_DEFAULT -> DateFormat.getTimeFormat(context)
            WorldClockComplicationData.TimeFormat.HOUR_12 -> SimpleDateFormat("h:mm a", Locale.getDefault())
            WorldClockComplicationData.TimeFormat.HOUR_24 -> SimpleDateFormat("HH:mm", Locale.getDefault())
        }
        dateFormat.timeZone = TimeZone.getTimeZone(zone)
        return dateFormat.format(Date.from(clock.instant()))
    }

    fun formatTime(
        zone: ZoneId,
        format: WorldClockComplicationData.TimeFormat,
        clock: Clock,
        locale: Locale
    ): String {
        val pattern = when (format) {
            WorldClockComplicationData.TimeFormat.SYSTEM_DEFAULT,
            WorldClockComplicationData.TimeFormat.HOUR_24 -> "HH:mm"
            WorldClockComplicationData.TimeFormat.HOUR_12 -> "h:mm a"
        }
        return DateTimeFormatter.ofPattern(pattern, locale)
            .withZone(zone)
            .format(clock.instant())
    }

    fun formatOffset(zone: ZoneId, clock: Clock, locale: Locale): String {
        return DateTimeFormatter.ofPattern("O", locale)
            .withZone(zone)
            .format(clock.instant())
    }

    fun truncateContent(time: String, label: String): String {
        if (label.isBlank() || time.length >= MAX_CONTENT_LENGTH) return time
        val available = MAX_CONTENT_LENGTH - time.length - SEPARATOR.length
        if (available <= 0) return time
        return "$time$SEPARATOR${label.take(available)}"
    }

    private fun formatCompactTime(zone: ZoneId, clock: Clock, locale: Locale): String {
        return DateTimeFormatter.ofPattern("HH:mm", locale)
            .withZone(zone)
            .format(clock.instant())
    }
}
