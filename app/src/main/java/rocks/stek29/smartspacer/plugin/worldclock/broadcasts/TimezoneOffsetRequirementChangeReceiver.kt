package rocks.stek29.smartspacer.plugin.worldclock.broadcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import rocks.stek29.smartspacer.plugin.worldclock.config.TimezoneOffsetRequirementRepository
import rocks.stek29.smartspacer.plugin.worldclock.requirements.TimezoneOffsetRequirement

class TimezoneOffsetRequirementChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_DATE_CHANGED -> {
                TimezoneOffsetRequirementRepository.invalidateAll()
                SmartspacerRequirementProvider.notifyChange(
                    context,
                    TimezoneOffsetRequirement::class.java
                )
            }
        }
    }
}
