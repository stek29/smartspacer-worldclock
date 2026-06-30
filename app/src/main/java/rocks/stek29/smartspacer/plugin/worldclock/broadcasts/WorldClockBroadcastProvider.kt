package rocks.stek29.smartspacer.plugin.worldclock.broadcasts

import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.gson.Gson
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerBroadcastProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import rocks.stek29.smartspacer.plugin.worldclock.BuildConfig
import rocks.stek29.smartspacer.plugin.worldclock.complications.WorldClockComplication
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockConfigRepository
import rocks.stek29.smartspacer.plugin.worldclock.utils.TimeFormatter

class WorldClockBroadcastProvider : SmartspacerBroadcastProvider() {

    private val dataStore by inject<DataStore<Preferences>>()
    private val gson by inject<Gson>()

    companion object {
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.broadcasts.worldclock"
    }

    override fun onReceive(intent: Intent) {
        if (intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            WorldClockConfigRepository.invalidateAll()
        }
        notifyBroadcastConfigChanged()
        SmartspacerComplicationProvider.notifyChange(
            provideContext(),
            WorldClockComplication::class.java
        )
    }

    override fun getConfig(smartspacerId: String): Config {
        val data = runBlocking {
            WorldClockConfigRepository.getConfigOnce(dataStore, gson, smartspacerId)
        }
        val includeMinuteTicks = data?.let { TimeFormatter.isVisible(it) } ?: true
        return Config(listOf(IntentFilter().apply {
            if (includeMinuteTicks) {
                addAction(Intent.ACTION_TIME_TICK)
            }
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }))
    }

    private fun notifyBroadcastConfigChanged() {
        val uri = Uri.Builder()
            .scheme("content")
            .authority(AUTHORITY)
            .build()
        provideContext().contentResolver.notifyChange(uri, null)
    }
}
