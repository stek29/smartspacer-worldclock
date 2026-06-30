package rocks.stek29.smartspacer.plugin.worldclock.utils

import android.content.Context
import android.text.format.DateFormat
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockComplicationData
import java.text.SimpleDateFormat
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

object TimeFormatter {

    const val MAX_CONTENT_LENGTH = 12
    private const val SEPARATOR = " "

    fun isVisible(data: WorldClockComplicationData, clock: Clock = Clock.systemUTC()): Boolean {
        val instant = clock.instant()
        return when (data.mode) {
            WorldClockComplicationData.Mode.NORMAL -> true
            WorldClockComplicationData.Mode.HOME -> hasDifferentOffset(
                homeZone = ZoneId.of(data.timezoneId),
                deviceZone = ZoneId.systemDefault(),
                instant = instant
            )
        }
    }

    fun hasDifferentOffset(homeZone: ZoneId, deviceZone: ZoneId, clock: Clock): Boolean {
        return hasDifferentOffset(homeZone, deviceZone, clock.instant())
    }

    private fun hasDifferentOffset(homeZone: ZoneId, deviceZone: ZoneId, instant: Instant): Boolean {
        return homeZone.rules.getOffset(instant) != deviceZone.rules.getOffset(instant)
    }

    fun buildContent(
        context: Context,
        data: WorldClockComplicationData,
        clock: Clock = Clock.systemUTC(),
        locale: Locale = Locale.getDefault()
    ): String {
        val instant = clock.instant()
        val zone = ZoneId.of(data.timezoneId)
        val time = formatTime(context, zone, data.timeFormat, instant)
            .let {
                if (it.length <= MAX_CONTENT_LENGTH) {
                    it
                } else {
                    formatCompactTime(zone, instant, locale)
                }
            }
        return when {
            data.customLabel.isNotBlank() -> truncateContent(time, data.customLabel.trim())
            data.showOffsetLabel -> truncateOffsetContent(time, zone, instant, locale)
            else -> time
        }
    }

    fun formatTime(
        context: Context,
        zone: ZoneId,
        format: WorldClockComplicationData.TimeFormat,
        clock: Clock = Clock.systemUTC()
    ): String {
        return formatTime(context, zone, format, clock.instant())
    }

    private fun formatTime(
        context: Context,
        zone: ZoneId,
        format: WorldClockComplicationData.TimeFormat,
        instant: Instant
    ): String {
        val dateFormat = when (format) {
            WorldClockComplicationData.TimeFormat.SYSTEM_DEFAULT -> DateFormat.getTimeFormat(context)
            WorldClockComplicationData.TimeFormat.HOUR_12 -> SimpleDateFormat("h:mm a", Locale.getDefault())
            WorldClockComplicationData.TimeFormat.HOUR_24 -> SimpleDateFormat("HH:mm", Locale.getDefault())
        }
        dateFormat.timeZone = TimeZone.getTimeZone(zone)
        return dateFormat.format(Date.from(instant))
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
        return formatOffset(zone, clock.instant(), locale)
    }

    private fun formatOffset(zone: ZoneId, instant: Instant, locale: Locale): String {
        return DateTimeFormatter.ofPattern("O", locale)
            .withZone(zone)
            .format(instant)
    }

    fun truncateOffsetContent(time: String, zone: ZoneId, clock: Clock, locale: Locale): String {
        return truncateOffsetContent(time, zone, clock.instant(), locale)
    }

    private fun truncateOffsetContent(
        time: String,
        zone: ZoneId,
        instant: Instant,
        locale: Locale
    ): String {
        val labels = listOf(formatOffset(zone, instant, locale)) + formatCompactOffsets(zone, instant)
        return labels
            .distinct()
            .firstNotNullOfOrNull { label -> contentIfFits(time, label) }
            ?: time
    }

    fun truncateContent(time: String, label: String): String {
        if (label.isBlank() || time.length >= MAX_CONTENT_LENGTH) return time
        val available = MAX_CONTENT_LENGTH - time.length - SEPARATOR.length
        if (available <= 0) return time
        return "$time$SEPARATOR${label.take(available)}"
    }

    private fun contentIfFits(time: String, label: String): String? {
        if (label.isBlank() || time.length >= MAX_CONTENT_LENGTH) return null
        val content = "$time$SEPARATOR$label"
        return content.takeIf { it.length <= MAX_CONTENT_LENGTH }
    }

    private fun formatCompactOffsets(zone: ZoneId, instant: Instant): List<String> {
        val totalSeconds = zone.rules.getOffset(instant).totalSeconds
        val sign = if (totalSeconds >= 0) "+" else "-"
        val totalMinutes = abs(totalSeconds) / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        val colonOffset = if (minutes == 0) {
            "$sign$hours"
        } else {
            "$sign$hours:${minutes.toString().padStart(2, '0')}"
        }
        val decimalOffset = if (minutes == 30) "$sign$hours.5" else null
        return listOfNotNull(colonOffset, decimalOffset)
    }

    private fun formatCompactTime(zone: ZoneId, instant: Instant, locale: Locale): String {
        return DateTimeFormatter.ofPattern("HH:mm", locale)
            .withZone(zone)
            .format(instant)
    }
}
