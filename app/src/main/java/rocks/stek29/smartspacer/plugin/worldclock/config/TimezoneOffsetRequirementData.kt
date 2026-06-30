package rocks.stek29.smartspacer.plugin.worldclock.config

import com.google.gson.annotations.SerializedName
import java.time.ZoneId

data class TimezoneOffsetRequirementData(
    @SerializedName("timezone")
    val timezoneId: String = ZoneId.systemDefault().id
)
