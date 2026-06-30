package rocks.stek29.smartspacer.plugin.worldclock.broadcasts

import android.content.Intent
import android.content.IntentFilter
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerBroadcastProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import rocks.stek29.smartspacer.plugin.worldclock.BuildConfig
import rocks.stek29.smartspacer.plugin.worldclock.complications.WorldClockComplication
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockConfigRepository

class WorldClockBroadcastProvider : SmartspacerBroadcastProvider() {

    companion object {
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.broadcasts.worldclock"
    }

    override fun onReceive(intent: Intent) {
        if (intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            WorldClockConfigRepository.invalidateAll()
        }
        SmartspacerComplicationProvider.notifyChange(
            provideContext(),
            WorldClockComplication::class.java
        )
    }

    override fun getConfig(smartspacerId: String): Config {
        return Config(listOf(IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }))
    }
}
