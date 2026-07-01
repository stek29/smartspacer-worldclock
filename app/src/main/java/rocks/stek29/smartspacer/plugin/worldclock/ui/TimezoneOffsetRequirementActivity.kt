package rocks.stek29.smartspacer.plugin.worldclock.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.gson.Gson
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import rocks.stek29.smartspacer.plugin.worldclock.R
import rocks.stek29.smartspacer.plugin.worldclock.config.TimezoneOffsetRequirementData
import rocks.stek29.smartspacer.plugin.worldclock.config.TimezoneOffsetRequirementRepository
import rocks.stek29.smartspacer.plugin.worldclock.requirements.TimezoneOffsetRequirement
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.ConfigurationBackground
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.ContainedCard
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.TimezoneSelectorCard
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.WorldClockTheme
import rocks.stek29.smartspacer.plugin.worldclock.utils.TimeFormatter
import java.time.Clock
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class TimezoneOffsetRequirementActivity : ComponentActivity() {

    private val dataStore by inject<DataStore<Preferences>>()
    private val gson by inject<Gson>()

    private val smartspacerId: String?
        get() = intent.getStringExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureWindow()
        val id = smartspacerId
        if (id == null) {
            finish()
            return
        }
        ensureDefaultConfig(id)
        setResult(Activity.RESULT_OK)
        setContent {
            WorldClockTheme {
                TimezoneOffsetRequirementRoute(
                    smartspacerId = id,
                    loadConfig = { loadConfig(id) ?: TimezoneOffsetRequirementData() },
                    saveConfig = { saveConfig(id, it) }
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun configureWindow() {
        configureEdgeToEdge()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    private fun ensureDefaultConfig(smartspacerId: String) {
        runBlocking {
            val existing = TimezoneOffsetRequirementRepository
                .getConfig(dataStore, gson, smartspacerId)
                .first()
            if (existing == null) {
                TimezoneOffsetRequirementRepository.putConfig(
                    dataStore = dataStore,
                    gson = gson,
                    smartspacerId = smartspacerId,
                    data = TimezoneOffsetRequirementData()
                )
            }
        }
    }

    private suspend fun loadConfig(smartspacerId: String): TimezoneOffsetRequirementData? {
        return TimezoneOffsetRequirementRepository.getConfig(dataStore, gson, smartspacerId).first()
    }

    private suspend fun saveConfig(smartspacerId: String, data: TimezoneOffsetRequirementData) {
        TimezoneOffsetRequirementRepository.putConfig(dataStore, gson, smartspacerId, data)
        SmartspacerRequirementProvider.notifyChange(
            this,
            TimezoneOffsetRequirement::class.java,
            smartspacerId
        )
    }
}

@Composable
private fun TimezoneOffsetRequirementRoute(
    smartspacerId: String,
    loadConfig: suspend () -> TimezoneOffsetRequirementData,
    saveConfig: suspend (TimezoneOffsetRequirementData) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var data by remember(smartspacerId) { mutableStateOf<TimezoneOffsetRequirementData?>(null) }
    val timezonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val timezone = result.data?.getStringExtra(TimezonePickerActivity.EXTRA_TIMEZONE_ID)
            ?: return@rememberLauncherForActivityResult
        val updated = TimezoneOffsetRequirementData(timezone)
        data = updated
        scope.launch { saveConfig(updated) }
    }

    LaunchedEffect(smartspacerId) {
        data = loadConfig()
    }

    data?.let {
        TimezoneOffsetRequirementScreen(
            data = it,
            onPickTimezone = {
                timezonePicker.launch(
                    Intent(context, TimezonePickerActivity::class.java)
                        .putExtra(TimezonePickerActivity.EXTRA_SELECTED_TIMEZONE_ID, it.timezoneId)
                )
            }
        )
    }
}

@Composable
private fun TimezoneOffsetRequirementScreen(
    data: TimezoneOffsetRequirementData,
    onPickTimezone: () -> Unit
) {
    val locale = Locale.getDefault()
    val selectedZone = ZoneId.of(data.timezoneId)
    val deviceZone = ZoneId.systemDefault()
    val selectedOffset = TimeFormatter.formatOffset(selectedZone, Clock.systemUTC(), locale)
    val deviceOffset = TimeFormatter.formatOffset(deviceZone, Clock.systemUTC(), locale)
    val isMet = TimeFormatter.hasDifferentOffset(selectedZone, deviceZone, Clock.systemUTC())

    ConfigurationBackground {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.requirement_timezone_offset_label),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                ContainedCard {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.requirement_preview_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(
                                if (isMet) {
                                    R.string.requirement_timezone_offset_preview_met
                                } else {
                                    R.string.requirement_timezone_offset_preview_not_met
                                }
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(
                                R.string.requirement_timezone_offset_preview_detail,
                                data.timezoneId,
                                selectedOffset,
                                deviceZone.id,
                                deviceOffset
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            item {
                TimezoneSelectorCard(
                    title = stringResource(R.string.timezone_title),
                    timezoneId = data.timezoneId,
                    subtitle = stringResource(
                        R.string.configuration_timezone_subtitle,
                        selectedZone.getDisplayName(TextStyle.FULL, locale),
                        selectedOffset
                    ),
                    onClick = onPickTimezone
                )
            }
        }
    }
}
