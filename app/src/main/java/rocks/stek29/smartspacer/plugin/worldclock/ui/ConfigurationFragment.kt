package rocks.stek29.smartspacer.plugin.worldclock.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import rocks.stek29.smartspacer.plugin.worldclock.R
import rocks.stek29.smartspacer.plugin.worldclock.broadcasts.WorldClockBroadcastProvider
import rocks.stek29.smartspacer.plugin.worldclock.complications.WorldClockComplication
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockComplicationData
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockConfigRepository
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockIconStyle
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockTargetData
import rocks.stek29.smartspacer.plugin.worldclock.targets.WorldClockTarget
import rocks.stek29.smartspacer.plugin.worldclock.utils.TimeFormatter
import java.time.Clock
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class ConfigurationFragment : Fragment() {

    private val dataStore by inject<DataStore<Preferences>>()
    private val gson by inject<Gson>()
    private val smartspacerId by lazy { requireArguments().getString(ARG_SMARTSPACER_ID)!! }
    private val type by lazy {
        ConfigurationActivity.Type.valueOf(
            requireArguments().getString(ARG_TYPE) ?: ConfigurationActivity.Type.COMPLICATION.name
        )
    }

    private var bindingConfig = false

    private lateinit var previewIcon: ImageView
    private lateinit var previewContent: TextView
    private lateinit var previewState: TextView
    private lateinit var previewVisibility: TextView
    private lateinit var iconGroup: MaterialButtonToggleGroup
    private lateinit var modeGroup: MaterialButtonToggleGroup
    private lateinit var timezoneCard: MaterialCardView
    private lateinit var timezoneTitle: TextView
    private lateinit var timezoneSubtitle: TextView
    private lateinit var timeFormatGroup: MaterialButtonToggleGroup
    private lateinit var labelModeDropdown: AutoCompleteTextView
    private lateinit var customLabelContainer: TextInputLayout
    private lateinit var customLabel: TextInputEditText
    private lateinit var hideSubtitleOnAodCard: MaterialCardView
    private lateinit var hideSubtitleOnAod: MaterialSwitch

    private val timezonePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val timezone = result.data?.getStringExtra(TimezonePickerActivity.EXTRA_TIMEZONE_ID)
            ?: return@registerForActivityResult
        updateConfig { copy(timezoneId = timezone) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_configuration, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        applyInsets(view)
        bindActions()
        viewLifecycleOwner.lifecycleScope.launch {
            bindConfig(loadConfigState())
        }
    }

    private fun bindViews(view: View) {
        previewIcon = view.findViewById(R.id.preview_icon)
        previewContent = view.findViewById(R.id.preview_content)
        previewState = view.findViewById(R.id.preview_state)
        previewVisibility = view.findViewById(R.id.preview_visibility)
        iconGroup = view.findViewById(R.id.icon_group)
        modeGroup = view.findViewById(R.id.mode_group)
        timezoneCard = view.findViewById(R.id.timezone_card)
        timezoneTitle = view.findViewById(R.id.timezone_title)
        timezoneSubtitle = view.findViewById(R.id.timezone_subtitle)
        timeFormatGroup = view.findViewById(R.id.time_format_group)
        labelModeDropdown = view.findViewById(R.id.label_mode_dropdown)
        customLabelContainer = view.findViewById(R.id.custom_label_container)
        customLabel = view.findViewById(R.id.custom_label)
        hideSubtitleOnAodCard = view.findViewById(R.id.hide_subtitle_on_aod_card)
        hideSubtitleOnAod = view.findViewById(R.id.hide_subtitle_on_aod)
        hideSubtitleOnAodCard.visibility = if (type == ConfigurationActivity.Type.TARGET) {
            View.VISIBLE
        } else {
            View.GONE
        }
        bindLabelModeAdapter()
    }

    private fun applyInsets(view: View) {
        val content = view.findViewById<View>(R.id.configuration_content)
        val baseTop = content.paddingTop
        val baseBottom = content.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(content) { target, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            target.setPadding(
                target.paddingLeft,
                baseTop + systemBars.top,
                target.paddingRight,
                baseBottom + systemBars.bottom
            )
            insets
        }
    }

    private fun bindActions() {
        iconGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (bindingConfig || !isChecked) return@addOnButtonCheckedListener
            val iconStyle = when (checkedId) {
                R.id.icon_home -> WorldClockComplicationData.IconStyle.HOME
                R.id.icon_heart -> WorldClockComplicationData.IconStyle.HEART
                R.id.icon_work -> WorldClockComplicationData.IconStyle.WORK
                R.id.icon_travel -> WorldClockComplicationData.IconStyle.TRAVEL
                R.id.icon_globe -> WorldClockComplicationData.IconStyle.GLOBE
                else -> WorldClockComplicationData.IconStyle.WORLD_CLOCK
            }
            updateConfig { copy(iconStyle = iconStyle) }
        }
        modeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (bindingConfig || !isChecked) return@addOnButtonCheckedListener
            val mode = when (checkedId) {
                R.id.mode_normal -> WorldClockComplicationData.Mode.NORMAL
                else -> WorldClockComplicationData.Mode.HOME
            }
            updateConfig { copy(mode = mode) }
        }
        timezoneCard.setOnClickListener {
            timezonePicker.launch(Intent(requireContext(), TimezonePickerActivity::class.java))
        }
        timeFormatGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (bindingConfig || !isChecked) return@addOnButtonCheckedListener
            val format = when (checkedId) {
                R.id.time_format_12h -> WorldClockComplicationData.TimeFormat.HOUR_12
                R.id.time_format_24h -> WorldClockComplicationData.TimeFormat.HOUR_24
                else -> WorldClockComplicationData.TimeFormat.SYSTEM_DEFAULT
            }
            updateConfig { copy(timeFormat = format) }
        }
        labelModeDropdown.setOnItemClickListener { _, _, position, _ ->
            if (bindingConfig) return@setOnItemClickListener
            updateLabelMode(availableLabelModes()[position])
        }
        customLabel.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (bindingConfig) return
                val label = s?.toString()?.trim().orEmpty()
                updateConfig {
                    copy(customLabel = label).withLabelMode(WorldClockComplicationData.LabelMode.CUSTOM)
                }
            }
        })
        hideSubtitleOnAod.setOnCheckedChangeListener { _, checked ->
            if (bindingConfig) return@setOnCheckedChangeListener
            updateTargetConfig { copy(hideSubtitleOnAod = checked) }
        }
    }

    private fun bindConfig(state: ConfigState) {
        val data = state.common
        bindingConfig = true
        iconGroup.check(
            when (data.iconStyle) {
                WorldClockComplicationData.IconStyle.WORLD_CLOCK -> R.id.icon_world_clock
                WorldClockComplicationData.IconStyle.HOME -> R.id.icon_home
                WorldClockComplicationData.IconStyle.HEART -> R.id.icon_heart
                WorldClockComplicationData.IconStyle.WORK -> R.id.icon_work
                WorldClockComplicationData.IconStyle.TRAVEL -> R.id.icon_travel
                WorldClockComplicationData.IconStyle.GLOBE -> R.id.icon_globe
            }
        )
        modeGroup.check(
            when (data.mode) {
                WorldClockComplicationData.Mode.NORMAL -> R.id.mode_normal
                WorldClockComplicationData.Mode.HOME -> R.id.mode_home
            }
        )
        timeFormatGroup.check(
            when (data.timeFormat) {
                WorldClockComplicationData.TimeFormat.SYSTEM_DEFAULT -> R.id.time_format_system
                WorldClockComplicationData.TimeFormat.HOUR_12 -> R.id.time_format_12h
                WorldClockComplicationData.TimeFormat.HOUR_24 -> R.id.time_format_24h
            }
        )
        val labelMode = if (
            type == ConfigurationActivity.Type.COMPLICATION &&
            data.labelMode == WorldClockComplicationData.LabelMode.TIMEZONE_NAME
        ) {
            WorldClockComplicationData.LabelMode.NONE
        } else {
            data.labelMode
        }
        labelModeDropdown.setText(labelModeLabel(labelMode), false)
        if (customLabel.text?.toString() != data.customLabel) {
            customLabel.setText(data.customLabel)
        }
        customLabelContainer.visibility = if (
            data.labelMode == WorldClockComplicationData.LabelMode.CUSTOM
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
        hideSubtitleOnAod.isChecked = state.hideSubtitleOnAod
        bindTimezone(data)
        bindPreview(state)
        bindingConfig = false
    }

    private fun bindTimezone(data: WorldClockComplicationData) {
        val zone = ZoneId.of(data.timezoneId)
        val locale = Locale.getDefault()
        val offset = TimeFormatter.formatOffset(zone, Clock.systemUTC(), locale)
        timezoneTitle.text = data.timezoneId
        timezoneSubtitle.text = getString(
            R.string.configuration_timezone_subtitle,
            zone.getDisplayName(TextStyle.FULL, locale),
            offset
        )
    }

    private fun bindPreview(state: ConfigState) {
        val data = state.common
        val isTarget = type == ConfigurationActivity.Type.TARGET
        previewIcon.setImageResource(WorldClockIconStyle.drawableFor(data.iconStyle))
        previewState.maxLines = if (isTarget) 2 else 1
        previewState.visibility = if (isTarget) View.VISIBLE else View.GONE
        if (TimeFormatter.isVisible(data)) {
            previewContent.text = if (isTarget) {
                TimeFormatter.buildTargetTitle(requireContext(), state.toTargetData())
            } else {
                TimeFormatter.buildContent(requireContext(), data)
            }
            previewContent.alpha = 1f
            previewIcon.alpha = 1f
            previewState.alpha = 1f
            if (isTarget) {
                val subtitle = TimeFormatter.buildTargetSubtitle(state.toTargetData())
                previewState.visibility = if (subtitle != null) View.VISIBLE else View.GONE
                previewState.text = subtitle
            }
            previewVisibility.text = getString(R.string.preview_visible)
        } else {
            val visibleData = data.copy(mode = WorldClockComplicationData.Mode.NORMAL)
            previewContent.text = if (isTarget) {
                TimeFormatter.buildTargetTitle(requireContext(), state.copy(common = visibleData).toTargetData())
            } else {
                TimeFormatter.buildContent(requireContext(), visibleData)
            }
            if (isTarget) {
                val subtitle = TimeFormatter.buildTargetSubtitle(state.copy(common = visibleData).toTargetData())
                previewState.visibility = if (subtitle != null) View.VISIBLE else View.GONE
                previewState.text = subtitle
            }
            previewContent.alpha = 0.52f
            previewIcon.alpha = 0.52f
            previewState.alpha = 0.72f
            previewVisibility.text = getString(R.string.preview_hidden_home)
        }
    }

    private fun updateConfig(transform: WorldClockComplicationData.() -> WorldClockComplicationData) {
        viewLifecycleOwner.lifecycleScope.launch {
            val current = loadConfigState()
            val updated = current.copy(common = current.common.transform())
            saveConfigState(updated)
            bindConfig(updated)
            WorldClockConfigRepository.invalidateConfig(smartspacerId)
            WorldClockBroadcastProvider.notifyConfigChanged(requireContext())
            notifyProviderChanged()
        }
    }

    private fun updateLabelMode(labelMode: WorldClockComplicationData.LabelMode) {
        updateConfig { withLabelMode(labelMode) }
    }

    private fun updateTargetConfig(transform: WorldClockTargetData.() -> WorldClockTargetData) {
        if (type != ConfigurationActivity.Type.TARGET) return
        viewLifecycleOwner.lifecycleScope.launch {
            val current = loadConfigState()
            val updatedTarget = current.toTargetData().transform()
            val updated = ConfigState(updatedTarget.toComplicationData(), updatedTarget.hideSubtitleOnAod)
            saveConfigState(updated)
            bindConfig(updated)
            WorldClockConfigRepository.invalidateConfig(smartspacerId)
            WorldClockBroadcastProvider.notifyConfigChanged(requireContext())
            notifyProviderChanged()
        }
    }

    private suspend fun loadConfigState(): ConfigState {
        return when (type) {
            ConfigurationActivity.Type.COMPLICATION -> {
                val data = WorldClockConfigRepository.getConfig(dataStore, gson, smartspacerId).first()
                    ?: WorldClockComplicationData()
                ConfigState(data)
            }
            ConfigurationActivity.Type.TARGET -> {
                val data = WorldClockConfigRepository.getTargetConfig(dataStore, gson, smartspacerId).first()
                    ?: WorldClockTargetData()
                ConfigState(data.toComplicationData(), data.hideSubtitleOnAod)
            }
        }
    }

    private suspend fun saveConfigState(state: ConfigState) {
        when (type) {
            ConfigurationActivity.Type.COMPLICATION -> {
                WorldClockConfigRepository.putConfig(dataStore, gson, smartspacerId, state.common)
            }
            ConfigurationActivity.Type.TARGET -> {
                WorldClockConfigRepository.putTargetConfig(
                    dataStore,
                    gson,
                    smartspacerId,
                    state.toTargetData()
                )
            }
        }
    }

    private fun notifyProviderChanged() {
        when (type) {
            ConfigurationActivity.Type.COMPLICATION -> {
                SmartspacerComplicationProvider.notifyChange(
                    requireContext(),
                    WorldClockComplication::class.java,
                    smartspacerId
                )
            }
            ConfigurationActivity.Type.TARGET -> {
                SmartspacerTargetProvider.notifyChange(
                    requireContext(),
                    WorldClockTarget::class.java,
                    smartspacerId
                )
            }
        }
    }

    companion object {
        private const val ARG_SMARTSPACER_ID = "smartspacer_id"
        private const val ARG_TYPE = "type"

        fun newInstance(
            smartspacerId: String,
            type: ConfigurationActivity.Type
        ): ConfigurationFragment {
            return ConfigurationFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SMARTSPACER_ID, smartspacerId)
                    putString(ARG_TYPE, type.name)
                }
            }
        }
    }

    private data class ConfigState(
        val common: WorldClockComplicationData,
        val hideSubtitleOnAod: Boolean = false
    ) {
        fun toTargetData(): WorldClockTargetData {
            return WorldClockTargetData(
                timezoneId = common.timezoneId,
                mode = common.mode,
                timeFormat = common.timeFormat,
                customLabel = common.customLabel,
                showOffsetLabel = common.showOffsetLabel,
                iconStyle = common.iconStyle,
                hideSubtitleOnAod = hideSubtitleOnAod
            ).withLabelMode(common.labelMode)
        }
    }

    private fun bindLabelModeAdapter() {
        val labels = availableLabelModes().map { labelModeLabel(it) }
        labelModeDropdown.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels)
        )
    }

    private fun availableLabelModes(): List<WorldClockComplicationData.LabelMode> {
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

    private fun labelModeLabel(labelMode: WorldClockComplicationData.LabelMode): String {
        return getString(
            when (labelMode) {
                WorldClockComplicationData.LabelMode.NONE -> R.string.label_mode_none
                WorldClockComplicationData.LabelMode.TIMEZONE_NAME -> R.string.label_mode_timezone
                WorldClockComplicationData.LabelMode.OFFSET -> R.string.label_mode_offset
                WorldClockComplicationData.LabelMode.CUSTOM -> R.string.label_mode_custom
            }
        )
    }
}
