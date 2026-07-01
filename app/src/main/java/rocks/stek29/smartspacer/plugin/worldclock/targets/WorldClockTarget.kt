package rocks.stek29.smartspacer.plugin.worldclock.targets

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon as AndroidIcon
import android.provider.AlarmClock
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.gson.Gson
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import rocks.stek29.smartspacer.plugin.worldclock.R
import rocks.stek29.smartspacer.plugin.worldclock.broadcasts.WorldClockBroadcastProvider
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockComplicationData
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockConfigRepository
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockIconStyle
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockTargetData
import rocks.stek29.smartspacer.plugin.worldclock.ui.ConfigurationActivity
import rocks.stek29.smartspacer.plugin.worldclock.utils.TimeFormatter

class WorldClockTarget : SmartspacerTargetProvider() {

    private val dataStore by inject<DataStore<Preferences>>()
    private val gson by inject<Gson>()

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        val data = getStoredConfig(smartspacerId) ?: WorldClockTargetData()
        if (!TimeFormatter.isVisible(data)) return emptyList()
        val context = provideContext()
        val icon = WorldClockIconStyle.drawableFor(data.iconStyle)
        return listOf(
            TargetTemplate.Basic(
                id = "worldclock_target_$smartspacerId",
                componentName = ComponentName(context, WorldClockTarget::class.java),
                icon = Icon(
                    AndroidIcon.createWithResource(context, icon),
                    WorldClockIconStyle.labelFor(context, data.iconStyle)
                ),
                title = Text(TimeFormatter.buildTargetTitle(context, data)),
                subtitle = TimeFormatter.buildTargetSubtitle(data)?.let { Text(it) },
                onClick = getClickAction()
            ).create().apply {
                canTakeTwoComplications = true
                canBeDismissed = false
                hideIfNoComplications = false
                hideSubtitleOnAod = data.hideSubtitleOnAod
            }
        )
    }

    override fun getConfig(smartspacerId: String?): Config {
        val context = provideContext()
        val data = smartspacerId?.let { getStoredConfig(it) }
        val icon = data?.let { WorldClockIconStyle.drawableFor(it.iconStyle) } ?: R.drawable.ic_world_clock
        return Config(
            label = context.getString(R.string.target_world_clock_label),
            description = when {
                data?.mode == WorldClockComplicationData.Mode.HOME -> {
                    context.getString(R.string.settings_description_home, data.timezoneId)
                }
                data != null -> context.getString(R.string.settings_description_zone, data.timezoneId)
                else -> context.getText(R.string.target_world_clock_description)
            },
            icon = AndroidIcon.createWithResource(context, icon),
            allowAddingMoreThanOnce = true,
            configActivity = ConfigurationActivity.createIntent(context, ConfigurationActivity.Type.TARGET),
            setupActivity = ConfigurationActivity.createIntent(context, ConfigurationActivity.Type.TARGET),
            broadcastProvider = WorldClockBroadcastProvider.AUTHORITY
        )
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        return false
    }

    override fun onProviderRemoved(smartspacerId: String) {
        runBlocking {
            WorldClockConfigRepository.deleteTargetConfig(dataStore, smartspacerId)
        }
    }

    private fun getClickAction(): TapAction {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            provideContext(),
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return TapAction(pendingIntent = pendingIntent)
    }

    private fun getStoredConfig(smartspacerId: String): WorldClockTargetData? {
        return runBlocking {
            WorldClockConfigRepository.getTargetConfigOnce(dataStore, gson, smartspacerId)
        }
    }
}
