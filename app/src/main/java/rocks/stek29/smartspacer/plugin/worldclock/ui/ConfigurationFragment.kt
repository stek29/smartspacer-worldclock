package rocks.stek29.smartspacer.plugin.worldclock.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.gson.Gson
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import rocks.stek29.smartspacer.plugin.worldclock.R
import rocks.stek29.smartspacer.plugin.worldclock.complications.WorldClockComplication
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockComplicationData
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockConfigRepository

class ConfigurationFragment : PreferenceFragmentCompat() {

    private val dataStore by inject<DataStore<Preferences>>()
    private val gson by inject<Gson>()
    private val smartspacerId by lazy { requireArguments().getString(ARG_SMARTSPACER_ID)!! }

    private val timezonePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val timezone = result.data?.getStringExtra(TimezonePickerActivity.EXTRA_TIMEZONE_ID)
            ?: return@registerForActivityResult
        updateConfig { copy(timezoneId = timezone) }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.world_clock_preferences, rootKey)
        bindPreferences()
    }

    private fun bindPreferences() {
        val mode = findPreference<ListPreference>(KEY_MODE)!!
        val timezone = findPreference<Preference>(KEY_TIMEZONE)!!
        val timeFormat = findPreference<ListPreference>(KEY_TIME_FORMAT)!!
        val customLabel = findPreference<EditTextPreference>(KEY_CUSTOM_LABEL)!!
        val showOffset = findPreference<SwitchPreferenceCompat>(KEY_SHOW_OFFSET_LABEL)!!

        mode.setOnPreferenceChangeListener { _, newValue ->
            updateConfig { copy(mode = WorldClockComplicationData.Mode.valueOf(newValue as String)) }
            true
        }
        timezone.setOnPreferenceClickListener {
            timezonePicker.launch(Intent(requireContext(), TimezonePickerActivity::class.java))
            true
        }
        timeFormat.setOnPreferenceChangeListener { _, newValue ->
            updateConfig {
                copy(timeFormat = WorldClockComplicationData.TimeFormat.valueOf(newValue as String))
            }
            true
        }
        customLabel.setOnPreferenceChangeListener { _, newValue ->
            updateConfig { copy(customLabel = (newValue as String).trim()) }
            true
        }
        showOffset.setOnPreferenceChangeListener { _, newValue ->
            updateConfig { copy(showOffsetLabel = newValue as Boolean) }
            true
        }

        lifecycleScope.launch {
            loadConfig()?.let { data ->
                mode.value = data.mode.name
                timezone.summary = data.timezoneId
                timeFormat.value = data.timeFormat.name
                customLabel.text = data.customLabel
                showOffset.isChecked = data.showOffsetLabel
                showOffset.isEnabled = data.customLabel.isBlank()
            }
        }
    }

    private fun updateConfig(transform: WorldClockComplicationData.() -> WorldClockComplicationData) {
        lifecycleScope.launch {
            val updated = (loadConfig() ?: WorldClockComplicationData()).transform()
            WorldClockConfigRepository.putConfig(dataStore, gson, smartspacerId, updated)
            findPreference<Preference>(KEY_TIMEZONE)?.summary = updated.timezoneId
            findPreference<SwitchPreferenceCompat>(KEY_SHOW_OFFSET_LABEL)?.isEnabled =
                updated.customLabel.isBlank()
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
        private const val KEY_MODE = "mode"
        private const val KEY_TIMEZONE = "timezone"
        private const val KEY_TIME_FORMAT = "time_format"
        private const val KEY_CUSTOM_LABEL = "custom_label"
        private const val KEY_SHOW_OFFSET_LABEL = "show_offset_label"

        fun newInstance(smartspacerId: String): ConfigurationFragment {
            return ConfigurationFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SMARTSPACER_ID, smartspacerId)
                }
            }
        }
    }
}
