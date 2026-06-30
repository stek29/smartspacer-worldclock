package rocks.stek29.smartspacer.plugin.worldclock.config

import android.content.Context
import androidx.annotation.DrawableRes
import rocks.stek29.smartspacer.plugin.worldclock.R

object WorldClockIconStyle {

    @DrawableRes
    fun drawableFor(style: WorldClockComplicationData.IconStyle): Int {
        return when (style) {
            WorldClockComplicationData.IconStyle.WORLD_CLOCK -> R.drawable.ic_world_clock
            WorldClockComplicationData.IconStyle.HOME -> R.drawable.ic_home_24
            WorldClockComplicationData.IconStyle.HEART -> R.drawable.ic_heart_24
            WorldClockComplicationData.IconStyle.WORK -> R.drawable.ic_work_24
            WorldClockComplicationData.IconStyle.TRAVEL -> R.drawable.ic_travel_24
            WorldClockComplicationData.IconStyle.GLOBE -> R.drawable.ic_globe_24
        }
    }

    fun labelFor(context: Context, style: WorldClockComplicationData.IconStyle): String {
        val label = when (style) {
            WorldClockComplicationData.IconStyle.WORLD_CLOCK -> R.string.icon_style_world_clock
            WorldClockComplicationData.IconStyle.HOME -> R.string.icon_style_home
            WorldClockComplicationData.IconStyle.HEART -> R.string.icon_style_heart
            WorldClockComplicationData.IconStyle.WORK -> R.string.icon_style_work
            WorldClockComplicationData.IconStyle.TRAVEL -> R.string.icon_style_travel
            WorldClockComplicationData.IconStyle.GLOBE -> R.string.icon_style_globe
        }
        return context.getString(label)
    }
}
