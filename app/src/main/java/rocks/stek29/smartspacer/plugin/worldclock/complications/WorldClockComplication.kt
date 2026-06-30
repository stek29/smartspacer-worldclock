package rocks.stek29.smartspacer.plugin.worldclock.complications

import android.content.Intent
import android.graphics.drawable.Icon as AndroidIcon
import android.provider.AlarmClock
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.gson.Gson
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import rocks.stek29.smartspacer.plugin.worldclock.R
import rocks.stek29.smartspacer.plugin.worldclock.broadcasts.WorldClockBroadcastProvider
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockComplicationData
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockConfigRepository
import rocks.stek29.smartspacer.plugin.worldclock.ui.ConfigurationActivity
import rocks.stek29.smartspacer.plugin.worldclock.utils.TimeFormatter

class WorldClockComplication : SmartspacerComplicationProvider() {

    private val dataStore by inject<DataStore<Preferences>>()
    private val gson by inject<Gson>()

    override fun getSmartspaceActions(smartspacerId: String): List<SmartspaceAction> {
        val data = getStoredConfig(smartspacerId) ?: WorldClockComplicationData()
        if (!TimeFormatter.isVisible(data)) return emptyList()
        val context = provideContext()
        val content = TimeFormatter.buildContent(context, data)
        return listOf(
            ComplicationTemplate.Basic(
                id = "worldclock_$smartspacerId",
                icon = Icon(
                    AndroidIcon.createWithResource(context, R.drawable.ic_world_clock),
                    context.getString(R.string.complication_world_clock_label)
                ),
                content = Text(content),
                onClick = TapAction(intent = Intent(AlarmClock.ACTION_SHOW_ALARMS))
            ).create()
        )
    }

    override fun getConfig(smartspacerId: String?): Config {
        val context = provideContext()
        val data = smartspacerId?.let { getStoredConfig(it) }
        return Config(
            label = when {
                data?.mode == WorldClockComplicationData.Mode.HOME -> {
                    context.getString(R.string.settings_label_home, data.timezoneId)
                }
                data != null -> context.getString(R.string.settings_label_zone, data.timezoneId)
                else -> context.getString(R.string.complication_world_clock_label)
            },
            description = when {
                data?.mode == WorldClockComplicationData.Mode.HOME -> {
                    context.getString(R.string.settings_description_home, data.timezoneId)
                }
                data != null -> context.getString(R.string.settings_description_zone, data.timezoneId)
                else -> context.getString(R.string.complication_world_clock_description)
            },
            icon = AndroidIcon.createWithResource(context, R.drawable.ic_world_clock),
            allowAddingMoreThanOnce = true,
            configActivity = Intent(context, ConfigurationActivity::class.java),
            setupActivity = Intent(context, ConfigurationActivity::class.java),
            broadcastProvider = WorldClockBroadcastProvider.AUTHORITY
        )
    }

    override fun onProviderRemoved(smartspacerId: String) {
        runBlocking {
            WorldClockConfigRepository.deleteConfig(dataStore, smartspacerId)
        }
    }

    private fun getStoredConfig(smartspacerId: String): WorldClockComplicationData? {
        return runBlocking {
            WorldClockConfigRepository.getConfig(dataStore, gson, smartspacerId).first()
        }
    }
}
