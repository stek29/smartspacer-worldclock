package rocks.stek29.smartspacer.plugin.worldclock.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import com.google.gson.Gson
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import rocks.stek29.smartspacer.plugin.worldclock.R
import rocks.stek29.smartspacer.plugin.worldclock.broadcasts.WorldClockBroadcastProvider
import rocks.stek29.smartspacer.plugin.worldclock.complications.WorldClockComplication
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockComplicationData
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockConfigRepository
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockIconStyle
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockTargetData
import rocks.stek29.smartspacer.plugin.worldclock.targets.WorldClockTarget
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.AnimatedSection
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.ConfigurationBackground
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.ContainedCard
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.HelperText
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.HorizontalIconSegmentedSelector
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.PreviewAlpha
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.SectionTitle
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.SegmentedSelector
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.SwitchCard
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.TimezoneSelectorCard
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.WorldClockCardShape
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.WorldClockControlSpacing
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.WorldClockHorizontalPadding
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.WorldClockSectionTopPadding
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.WorldClockTheme
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.worldClockSpring
import rocks.stek29.smartspacer.plugin.worldclock.utils.TimeFormatter
import java.time.Clock
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class ConfigurationActivity : ComponentActivity() {

    private val dataStore by inject<DataStore<Preferences>>()
    private val gson by inject<Gson>()

    val smartspacerId: String?
        get() = intent.getStringExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID)

    private val type: Type
        get() = Type.fromIntent(intent)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureWindow()
        val id = smartspacerId
        if (id == null) {
            finish()
            return
        }
        ensureDefaultConfig(id, type)
        setResult(Activity.RESULT_OK)
        setContent {
            WorldClockTheme {
                ConfigurationRoute(
                    smartspacerId = id,
                    type = type,
                    loadConfigState = { loadConfigState(id, type) },
                    saveConfigState = { saveConfigState(id, type, it) },
                    notifyChanged = { notifyProviderChanged(id, type) }
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

    private fun ensureDefaultConfig(smartspacerId: String, type: Type) {
        runBlocking {
            when (type) {
                Type.COMPLICATION -> {
                    val existing = WorldClockConfigRepository.getConfig(dataStore, gson, smartspacerId).first()
                    if (existing == null) {
                        WorldClockConfigRepository.putConfig(
                            dataStore = dataStore,
                            gson = gson,
                            smartspacerId = smartspacerId,
                            data = WorldClockComplicationData()
                        )
                    }
                }
                Type.TARGET -> {
                    val existing = WorldClockConfigRepository.getTargetConfig(dataStore, gson, smartspacerId).first()
                    if (existing == null) {
                        WorldClockConfigRepository.putTargetConfig(
                            dataStore = dataStore,
                            gson = gson,
                            smartspacerId = smartspacerId,
                            data = WorldClockTargetData()
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadConfigState(smartspacerId: String, type: Type): ConfigState {
        return when (type) {
            Type.COMPLICATION -> {
                val data = WorldClockConfigRepository.getConfig(dataStore, gson, smartspacerId).first()
                    ?: WorldClockComplicationData()
                ConfigState(data)
            }
            Type.TARGET -> {
                val data = WorldClockConfigRepository.getTargetConfig(dataStore, gson, smartspacerId).first()
                    ?: WorldClockTargetData()
                ConfigState(data.toComplicationData(), data.hideSubtitleOnAod, data.showLabelInSubtitle)
            }
        }
    }

    private suspend fun saveConfigState(smartspacerId: String, type: Type, state: ConfigState) {
        when (type) {
            Type.COMPLICATION -> {
                WorldClockConfigRepository.putConfig(dataStore, gson, smartspacerId, state.common)
            }
            Type.TARGET -> {
                WorldClockConfigRepository.putTargetConfig(dataStore, gson, smartspacerId, state.toTargetData())
            }
        }
        WorldClockConfigRepository.invalidateConfig(smartspacerId)
        WorldClockBroadcastProvider.notifyConfigChanged(this)
    }

    private fun notifyProviderChanged(smartspacerId: String, type: Type) {
        when (type) {
            Type.COMPLICATION -> {
                SmartspacerComplicationProvider.notifyChange(this, WorldClockComplication::class.java, smartspacerId)
            }
            Type.TARGET -> {
                SmartspacerTargetProvider.notifyChange(this, WorldClockTarget::class.java, smartspacerId)
            }
        }
    }

    enum class Type {
        COMPLICATION,
        TARGET;

        companion object {
            private const val EXTRA_TYPE = "rocks.stek29.smartspacer.plugin.worldclock.extra.TYPE"

            fun fromIntent(intent: Intent): Type {
                return runCatching {
                    valueOf(intent.getStringExtra(EXTRA_TYPE) ?: COMPLICATION.name)
                }.getOrDefault(COMPLICATION)
            }

            fun putExtra(intent: Intent, type: Type): Intent {
                return intent.putExtra(EXTRA_TYPE, type.name)
            }
        }
    }

    companion object {
        private const val MINUTE_MILLIS = 60_000L

        fun createIntent(context: Context, type: Type): Intent {
            return Type.putExtra(Intent(context, ConfigurationActivity::class.java), type)
        }

        fun millisUntilNextMinute(): Long {
            val remainder = System.currentTimeMillis() % MINUTE_MILLIS
            return if (remainder == 0L) MINUTE_MILLIS else MINUTE_MILLIS - remainder
        }
    }
}

data class ConfigState(
    val common: WorldClockComplicationData,
    val hideSubtitleOnAod: Boolean = false,
    val showLabelInSubtitle: Boolean = false
) {
    fun toTargetData(): WorldClockTargetData {
        return WorldClockTargetData(
            timezoneId = common.timezoneId,
            mode = common.mode,
            timeFormat = common.timeFormat,
            customLabel = common.customLabel,
            showOffsetLabel = common.showOffsetLabel,
            iconStyle = common.iconStyle,
            showLabelInSubtitle = showLabelInSubtitle,
            hideSubtitleOnAod = hideSubtitleOnAod
        ).withLabelMode(common.labelMode)
    }
}

@Composable
private fun ConfigurationRoute(
    smartspacerId: String,
    type: ConfigurationActivity.Type,
    loadConfigState: suspend () -> ConfigState,
    saveConfigState: suspend (ConfigState) -> Unit,
    notifyChanged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember(smartspacerId, type) { mutableStateOf<ConfigState?>(null) }
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val timezonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val timezone = result.data?.getStringExtra(TimezonePickerActivity.EXTRA_TIMEZONE_ID)
            ?: return@rememberLauncherForActivityResult
        val current = state ?: return@rememberLauncherForActivityResult
        val updated = current.copy(common = current.common.copy(timezoneId = timezone))
        state = updated
        scope.launch {
            saveConfigState(updated)
            notifyChanged()
        }
    }

    LaunchedEffect(smartspacerId, type) {
        state = loadConfigState()
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(ConfigurationActivity.millisUntilNextMinute())
            tick = System.currentTimeMillis()
        }
    }

    val current = state
    if (current != null) {
        ConfigurationScreen(
            state = current,
            type = type,
            tick = tick,
            onStateChange = { updated ->
                state = updated
                scope.launch {
                    saveConfigState(updated)
                    notifyChanged()
                }
            },
            onPickTimezone = {
                timezonePicker.launch(
                    Intent(context, TimezonePickerActivity::class.java)
                        .putExtra(TimezonePickerActivity.EXTRA_SELECTED_TIMEZONE_ID, current.common.timezoneId)
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigurationScreen(
    state: ConfigState,
    type: ConfigurationActivity.Type,
    tick: Long,
    onStateChange: (ConfigState) -> Unit,
    onPickTimezone: () -> Unit
) {
    val context = LocalContext.current
    val data = state.common
    val isTarget = type == ConfigurationActivity.Type.TARGET
    val labelMode = if (
        type == ConfigurationActivity.Type.COMPLICATION &&
        data.labelMode == WorldClockComplicationData.LabelMode.TIMEZONE_NAME
    ) {
        WorldClockComplicationData.LabelMode.NONE
    } else {
        data.labelMode
    }
    val availableLabelModes = availableLabelModes(type)
    ConfigurationBackground {
        LazyColumn(
            modifier = Modifier.padding(horizontal = WorldClockHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(WorldClockControlSpacing),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 24.dp, bottom = 24.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.configuration_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item { WorldClockPreviewCard(state, type, tick) }
            item { SectionTitle(stringResource(R.string.icon_style_title), Modifier.padding(top = WorldClockSectionTopPadding)) }
            item {
                HorizontalIconSegmentedSelector(
                    options = iconStyleOptions(),
                    selected = data.iconStyle,
                    iconRes = { WorldClockIconStyle.drawableFor(it) },
                    contentDescription = { iconStyleLabel(context, it) },
                    onSelected = { onStateChange(state.copy(common = data.copy(iconStyle = it))) }
                )
            }
            item {
                AnimatedSection(isTarget && !state.showLabelInSubtitle) {
                    HelperText(stringResource(R.string.target_icon_note))
                }
            }
            item { SectionTitle(stringResource(R.string.mode_title), Modifier.padding(top = WorldClockSectionTopPadding)) }
            item {
                SegmentedSelector(
                    options = modeOptions(),
                    selected = data.mode,
                    label = { Text(modeLabel(it)) },
                    onSelected = { onStateChange(state.copy(common = data.copy(mode = it))) }
                )
            }
            item {
                val zone = ZoneId.of(data.timezoneId)
                val locale = Locale.getDefault()
                TimezoneSelectorCard(
                    title = stringResource(R.string.timezone_title),
                    timezoneId = data.timezoneId,
                    subtitle = stringResource(
                        R.string.configuration_timezone_subtitle,
                        zone.getDisplayName(TextStyle.FULL, locale),
                        TimeFormatter.formatOffset(zone, Clock.systemUTC(), locale)
                    ),
                    onClick = onPickTimezone,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            item { SectionTitle(stringResource(R.string.time_format_title), Modifier.padding(top = WorldClockSectionTopPadding)) }
            item {
                SegmentedSelector(
                    options = WorldClockComplicationData.TimeFormat.entries,
                    selected = data.timeFormat,
                    label = { Text(timeFormatLabel(it)) },
                    onSelected = { onStateChange(state.copy(common = data.copy(timeFormat = it))) }
                )
            }
            item { SectionTitle(stringResource(R.string.label_mode_title), Modifier.padding(top = WorldClockSectionTopPadding)) }
            item {
                var expanded by remember { mutableStateOf(false) }
                var menuVisible by remember { mutableStateOf(false) }
                val caretRotation by animateFloatAsState(
                    targetValue = if (expanded) 180f else 0f,
                    animationSpec = worldClockSpring(),
                    label = "labelDropdownCaret"
                )
                LaunchedEffect(expanded) {
                    if (expanded) {
                        menuVisible = true
                    } else {
                        delay(120)
                        menuVisible = false
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = labelModeLabel(labelMode),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_mode_title)) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.rotate(caretRotation)
                            )
                        },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth(),
                        shape = WorldClockCardShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = menuVisible,
                        onDismissRequest = { expanded = false },
                        shape = WorldClockCardShape,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 3.dp,
                        shadowElevation = 3.dp
                    ) {
                        AnimatedVisibility(
                            visible = expanded,
                            enter = fadeIn(worldClockSpring()) + expandVertically(worldClockSpring()),
                            exit = fadeOut(worldClockSpring()) + shrinkVertically(worldClockSpring())
                        ) {
                            Column {
                                availableLabelModes.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(labelModeLabel(mode)) },
                                        onClick = {
                                            expanded = false
                                            onStateChange(state.copy(common = data.withLabelMode(mode)))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                AnimatedSection(data.labelMode == WorldClockComplicationData.LabelMode.CUSTOM) {
                    val bringIntoViewRequester = remember { BringIntoViewRequester() }
                    val scope = rememberCoroutineScope()
                    OutlinedTextField(
                        value = data.customLabel,
                        onValueChange = {
                            onStateChange(
                                state.copy(
                                    common = data.copy(customLabel = it.trim())
                                        .withLabelMode(WorldClockComplicationData.LabelMode.CUSTOM)
                                )
                            )
                        },
                        label = { Text(stringResource(R.string.custom_label_title)) },
                        supportingText = if (type == ConfigurationActivity.Type.COMPLICATION) {
                            { Text(stringResource(R.string.custom_label_helper)) }
                        } else {
                            null
                        },
                        trailingIcon = if (data.customLabel.isNotEmpty()) {
                            {
                                IconButton(onClick = {
                                    onStateChange(state.copy(common = data.copy(customLabel = "")))
                                }) {
                                    Icon(Icons.Filled.Clear, contentDescription = null)
                                }
                            }
                        } else {
                            null
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions.Default,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth()
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .onFocusEvent {
                                if (it.isFocused) {
                                    scope.launch { bringIntoViewRequester.bringIntoView() }
                                }
                            },
                        shape = WorldClockCardShape
                    )
                }
            }
            item {
                AnimatedSection(isTarget && data.labelMode != WorldClockComplicationData.LabelMode.NONE) {
                    SwitchCard(
                        title = stringResource(R.string.show_label_in_subtitle_title),
                        summary = stringResource(R.string.show_label_in_subtitle_summary),
                        checked = state.showLabelInSubtitle,
                        onCheckedChange = { checked ->
                            onStateChange(state.copy(showLabelInSubtitle = checked))
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            item {
                val hasTargetSubtitleLabel = TimeFormatter.buildTargetLabel(state.toTargetData()) != null
                AnimatedSection(isTarget && state.showLabelInSubtitle && hasTargetSubtitleLabel) {
                    SwitchCard(
                        title = stringResource(R.string.hide_subtitle_on_aod_title),
                        summary = stringResource(R.string.hide_subtitle_on_aod_summary),
                        checked = state.hideSubtitleOnAod,
                        onCheckedChange = { checked ->
                            onStateChange(state.copy(hideSubtitleOnAod = checked))
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WorldClockPreviewCard(
    state: ConfigState,
    type: ConfigurationActivity.Type,
    @Suppress("UNUSED_PARAMETER") tick: Long
) {
    val context = LocalContext.current
    val data = state.common
    val isTarget = type == ConfigurationActivity.Type.TARGET
    val visible = TimeFormatter.isVisible(data)
    val visibleData = if (visible) data else data.copy(mode = WorldClockComplicationData.Mode.NORMAL)
    val title = if (isTarget) {
        if (state.showLabelInSubtitle) {
            TimeFormatter.buildTargetTitle(context, state.copy(common = visibleData).toTargetData())
        } else {
            TimeFormatter.buildTargetTitleWithLabel(context, state.copy(common = visibleData).toTargetData())
        }
    } else {
        TimeFormatter.buildContent(context, visibleData)
    }
    val subtitle = if (isTarget && state.showLabelInSubtitle) {
        TimeFormatter.buildTargetLabel(state.copy(common = visibleData).toTargetData())
    } else {
        null
    }
    val previewColors = lightColorScheme()
    ContainedCard(modifier = Modifier.padding(top = 10.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = WorldClockHorizontalPadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.preview_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = WorldClockCardShape,
                color = previewColors.surfaceContainerHigh,
                contentColor = previewColors.onSurface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    PreviewAlpha(visible) {
                        Icon(
                            painter = painterResource(WorldClockIconStyle.drawableFor(data.iconStyle)),
                            contentDescription = stringResource(R.string.icon_style_title),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .weight(1f)
                    ) {
                        PreviewAlpha(visible) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = previewColors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = previewColors.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.alpha(if (visible) 1f else 0.72f)
                            )
                        }
                    }
                }
            }
            Text(
                text = stringResource(if (visible) R.string.preview_visible else R.string.preview_hidden_home),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun modeLabel(mode: WorldClockComplicationData.Mode): String {
    return stringResource(
        when (mode) {
            WorldClockComplicationData.Mode.HOME -> R.string.mode_home
            WorldClockComplicationData.Mode.NORMAL -> R.string.mode_normal
        }
    )
}

@Composable
private fun timeFormatLabel(format: WorldClockComplicationData.TimeFormat): String {
    return stringResource(
        when (format) {
            WorldClockComplicationData.TimeFormat.SYSTEM_DEFAULT -> R.string.time_format_system_short
            WorldClockComplicationData.TimeFormat.HOUR_12 -> R.string.time_format_12h_short
            WorldClockComplicationData.TimeFormat.HOUR_24 -> R.string.time_format_24h_short
        }
    )
}

@Composable
private fun labelModeLabel(labelMode: WorldClockComplicationData.LabelMode): String {
    return stringResource(
        when (labelMode) {
            WorldClockComplicationData.LabelMode.NONE -> R.string.label_mode_none
            WorldClockComplicationData.LabelMode.TIMEZONE_NAME -> R.string.label_mode_timezone
            WorldClockComplicationData.LabelMode.OFFSET -> R.string.label_mode_offset
            WorldClockComplicationData.LabelMode.CUSTOM -> R.string.label_mode_custom
        }
    )
}

private fun iconStyleLabel(
    context: Context,
    iconStyle: WorldClockComplicationData.IconStyle
): String {
    return context.getString(
        when (iconStyle) {
            WorldClockComplicationData.IconStyle.WORLD_CLOCK -> R.string.icon_style_world_clock
            WorldClockComplicationData.IconStyle.HOME -> R.string.icon_style_home
            WorldClockComplicationData.IconStyle.HEART -> R.string.icon_style_heart
            WorldClockComplicationData.IconStyle.WORK -> R.string.icon_style_work
            WorldClockComplicationData.IconStyle.TRAVEL -> R.string.icon_style_travel
            WorldClockComplicationData.IconStyle.GLOBE -> R.string.icon_style_globe
        }
    )
}

private fun availableLabelModes(type: ConfigurationActivity.Type): List<WorldClockComplicationData.LabelMode> {
    return if (type == ConfigurationActivity.Type.TARGET) {
        listOf(
            WorldClockComplicationData.LabelMode.TIMEZONE_NAME,
            WorldClockComplicationData.LabelMode.OFFSET,
            WorldClockComplicationData.LabelMode.CUSTOM,
            WorldClockComplicationData.LabelMode.NONE
        )
    } else {
        listOf(
            WorldClockComplicationData.LabelMode.NONE,
            WorldClockComplicationData.LabelMode.OFFSET,
            WorldClockComplicationData.LabelMode.CUSTOM
        )
    }
}

private fun modeOptions(): List<WorldClockComplicationData.Mode> {
    return listOf(
        WorldClockComplicationData.Mode.HOME,
        WorldClockComplicationData.Mode.NORMAL
    )
}

private fun iconStyleOptions(): List<WorldClockComplicationData.IconStyle> {
    return listOf(
        WorldClockComplicationData.IconStyle.HOME,
        WorldClockComplicationData.IconStyle.HEART,
        WorldClockComplicationData.IconStyle.WORK,
        WorldClockComplicationData.IconStyle.TRAVEL,
        WorldClockComplicationData.IconStyle.GLOBE,
        WorldClockComplicationData.IconStyle.WORLD_CLOCK
    )
}
