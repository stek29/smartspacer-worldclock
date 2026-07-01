package rocks.stek29.smartspacer.plugin.worldclock.ui.compose

import android.content.Context
import rocks.stek29.smartspacer.plugin.worldclock.R
import rocks.stek29.smartspacer.plugin.worldclock.utils.TimeFormatter
import java.time.Clock
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

data class TimezoneRow(
    val id: String,
    val title: String,
    val subtitle: String,
    val offset: String,
    val searchText: String
)

fun buildTimezoneRows(
    context: Context,
    clock: Clock = Clock.systemUTC(),
    locale: Locale = Locale.getDefault()
): List<TimezoneRow> {
    return ZoneId.getAvailableZoneIds()
        .filterNot(::isLegacyAlias)
        .map { zoneId ->
            val zone = ZoneId.of(zoneId)
            val offset = TimeFormatter.formatOffset(zone, clock, locale)
            val displayName = friendlyDisplayName(zone, zoneId, locale)
            val subtitle = context.getString(R.string.timezone_row_subtitle, displayName, offset)
            TimezoneRow(
                id = zoneId,
                title = zoneId,
                subtitle = subtitle,
                offset = offset,
                searchText = listOf(zoneId, displayName, subtitle, offset).joinToString(" ")
            )
        }
        .sortedWith { left, right -> String.CASE_INSENSITIVE_ORDER.compare(left.id, right.id) }
}

fun filterTimezoneRows(rows: List<TimezoneRow>, query: String): List<TimezoneRow> {
    val normalized = query.trim()
    return if (normalized.isBlank()) {
        rows
    } else {
        rows.filter { it.searchText.contains(normalized, ignoreCase = true) }
    }
}

fun isLegacyAlias(zoneId: String): Boolean {
    return !zoneId.contains("/") &&
        zoneId.length <= 3 &&
        zoneId != "UTC" &&
        zoneId != "GMT"
}

fun friendlyDisplayName(zone: ZoneId, zoneId: String, locale: Locale): String {
    val displayName = zone.getDisplayName(TextStyle.FULL, locale)
    if (displayName != zoneId) return displayName
    val city = zoneId.substringAfterLast('/').replace('_', ' ')
    return "$city Time"
}
