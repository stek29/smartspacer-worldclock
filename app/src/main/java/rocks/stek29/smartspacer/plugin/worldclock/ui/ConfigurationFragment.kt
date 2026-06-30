package rocks.stek29.smartspacer.plugin.worldclock.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.google.gson.Gson
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import rocks.stek29.smartspacer.plugin.worldclock.R
import rocks.stek29.smartspacer.plugin.worldclock.complications.WorldClockComplication
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockComplicationData
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockConfigRepository
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockIconStyle
import rocks.stek29.smartspacer.plugin.worldclock.utils.TimeFormatter
import java.time.Clock
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class ConfigurationFragment : Fragment() {

    private val dataStore by inject<DataStore<Preferences>>()
    private val gson by inject<Gson>()
    private val smartspacerId by lazy { requireArguments().getString(ARG_SMARTSPACER_ID)!! }

    private var bindingConfig = false

    private lateinit var previewIcon: ImageView
    private lateinit var previewContent: TextView
    private lateinit var previewState: TextView
    private lateinit var iconGroup: MaterialButtonToggleGroup
    private lateinit var modeGroup: MaterialButtonToggleGroup
    private lateinit var timezoneCard: MaterialCardView
    private lateinit var timezoneTitle: TextView
    private lateinit var timezoneSubtitle: TextView
    private lateinit var timeFormatGroup: MaterialButtonToggleGroup
    private lateinit var customLabel: TextInputEditText
    private lateinit var showOffset: MaterialSwitch

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
            bindConfig(loadConfig() ?: WorldClockComplicationData())
        }
    }

    private fun bindViews(view: View) {
        previewIcon = view.findViewById(R.id.preview_icon)
        previewContent = view.findViewById(R.id.preview_content)
        previewState = view.findViewById(R.id.preview_state)
        iconGroup = view.findViewById(R.id.icon_group)
        modeGroup = view.findViewById(R.id.mode_group)
        timezoneCard = view.findViewById(R.id.timezone_card)
        timezoneTitle = view.findViewById(R.id.timezone_title)
        timezoneSubtitle = view.findViewById(R.id.timezone_subtitle)
        timeFormatGroup = view.findViewById(R.id.time_format_group)
        customLabel = view.findViewById(R.id.custom_label)
        showOffset = view.findViewById(R.id.show_offset)
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
        customLabel.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (bindingConfig) return
                updateConfig { copy(customLabel = s?.toString()?.trim().orEmpty()) }
            }
        })
        showOffset.setOnCheckedChangeListener { _, checked ->
            if (bindingConfig) return@setOnCheckedChangeListener
            updateConfig { copy(showOffsetLabel = checked) }
        }
    }

    private fun bindConfig(data: WorldClockComplicationData) {
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
        if (customLabel.text?.toString() != data.customLabel) {
            customLabel.setText(data.customLabel)
        }
        showOffset.isChecked = data.showOffsetLabel
        showOffset.isEnabled = data.customLabel.isBlank()
        bindTimezone(data)
        bindPreview(data)
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

    private fun bindPreview(data: WorldClockComplicationData) {
        previewIcon.setImageResource(WorldClockIconStyle.drawableFor(data.iconStyle))
        if (TimeFormatter.isVisible(data)) {
            previewContent.text = TimeFormatter.buildContent(requireContext(), data)
            previewContent.alpha = 1f
            previewIcon.alpha = 1f
            previewState.text = getString(R.string.preview_visible)
        } else {
            previewContent.text = TimeFormatter.buildContent(
                requireContext(),
                data.copy(mode = WorldClockComplicationData.Mode.NORMAL)
            )
            previewContent.alpha = 0.52f
            previewIcon.alpha = 0.52f
            previewState.text = getString(R.string.preview_hidden_home)
        }
    }

    private fun updateConfig(transform: WorldClockComplicationData.() -> WorldClockComplicationData) {
        viewLifecycleOwner.lifecycleScope.launch {
            val updated = (loadConfig() ?: WorldClockComplicationData()).transform()
            WorldClockConfigRepository.putConfig(dataStore, gson, smartspacerId, updated)
            bindConfig(updated)
            SmartspacerComplicationProvider.notifyChange(
                requireContext(),
                WorldClockComplication::class.java,
                smartspacerId
            )
        }
    }

    private suspend fun loadConfig(): WorldClockComplicationData? {
        return WorldClockConfigRepository.getConfig(dataStore, gson, smartspacerId).first()
    }

    companion object {
        private const val ARG_SMARTSPACER_ID = "smartspacer_id"

        fun newInstance(smartspacerId: String): ConfigurationFragment {
            return ConfigurationFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SMARTSPACER_ID, smartspacerId)
                }
            }
        }
    }
}
