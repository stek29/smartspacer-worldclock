package rocks.stek29.smartspacer.plugin.worldclock.requirements

import android.content.Intent
import android.graphics.drawable.Icon
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.gson.Gson
import com.kieronquinn.app.smartspacer.sdk.model.Backup
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import rocks.stek29.smartspacer.plugin.worldclock.R
import rocks.stek29.smartspacer.plugin.worldclock.config.TimezoneOffsetRequirementData
import rocks.stek29.smartspacer.plugin.worldclock.config.TimezoneOffsetRequirementRepository
import rocks.stek29.smartspacer.plugin.worldclock.ui.TimezoneOffsetRequirementActivity
import rocks.stek29.smartspacer.plugin.worldclock.utils.TimeFormatter
import java.time.Clock
import java.time.ZoneId
import java.util.Locale

class TimezoneOffsetRequirement : SmartspacerRequirementProvider() {

    private val dataStore by inject<DataStore<Preferences>>()
    private val gson by inject<Gson>()

    override fun isRequirementMet(smartspacerId: String): Boolean {
        val data = getStoredConfig(smartspacerId) ?: return false
        return runCatching {
            TimeFormatter.hasDifferentOffset(
                homeZone = ZoneId.of(data.timezoneId),
                deviceZone = ZoneId.systemDefault(),
                clock = Clock.systemUTC()
            )
        }.getOrDefault(false)
    }

    override fun getConfig(smartspacerId: String?): Config {
        val context = provideContext()
        val data = smartspacerId?.let { getStoredConfig(it) }
        return Config(
            label = context.getString(R.string.requirement_timezone_offset_label),
            description = data?.let {
                context.getString(
                    R.string.requirement_timezone_offset_description_configured,
                    it.timezoneId,
                    formatOffset(ZoneId.of(it.timezoneId)),
                    formatOffset(ZoneId.systemDefault())
                )
            } ?: context.getString(R.string.requirement_timezone_offset_description),
            icon = Icon.createWithResource(context, R.drawable.ic_globe_24),
            allowAddingMoreThanOnce = true,
            configActivity = Intent(context, TimezoneOffsetRequirementActivity::class.java),
            setupActivity = Intent(context, TimezoneOffsetRequirementActivity::class.java)
        )
    }

    override fun onProviderRemoved(smartspacerId: String) {
        runBlocking {
            TimezoneOffsetRequirementRepository.deleteConfig(dataStore, smartspacerId)
        }
    }

    override fun createBackup(smartspacerId: String): Backup {
        val data = getStoredConfig(smartspacerId) ?: return Backup()
        return Backup(gson.toJson(data), data.timezoneId)
    }

    override fun restoreBackup(smartspacerId: String, backup: Backup): Boolean {
        val data = runCatching {
            gson.fromJson(backup.data, TimezoneOffsetRequirementData::class.java)
        }.getOrNull() ?: return false
        return runBlocking {
            TimezoneOffsetRequirementRepository.putConfig(dataStore, gson, smartspacerId, data)
                .also { success ->
                    if (success) notifyChange(smartspacerId)
                }
        }
    }

    private fun getStoredConfig(smartspacerId: String): TimezoneOffsetRequirementData? {
        return runBlocking {
            TimezoneOffsetRequirementRepository.getConfigOnce(dataStore, gson, smartspacerId)
        }
    }

    private fun formatOffset(zone: ZoneId): String {
        return TimeFormatter.formatOffset(zone, Clock.systemUTC(), Locale.getDefault())
    }
}
