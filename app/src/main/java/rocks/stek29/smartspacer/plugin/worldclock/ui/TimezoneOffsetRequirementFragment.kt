package rocks.stek29.smartspacer.plugin.worldclock.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerRequirementProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import rocks.stek29.smartspacer.plugin.worldclock.R
import rocks.stek29.smartspacer.plugin.worldclock.config.TimezoneOffsetRequirementData
import rocks.stek29.smartspacer.plugin.worldclock.config.TimezoneOffsetRequirementRepository
import rocks.stek29.smartspacer.plugin.worldclock.requirements.TimezoneOffsetRequirement
import rocks.stek29.smartspacer.plugin.worldclock.utils.TimeFormatter
import java.time.Clock
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class TimezoneOffsetRequirementFragment : Fragment() {

    private val dataStore by inject<DataStore<Preferences>>()
    private val gson by inject<Gson>()
    private val smartspacerId by lazy { requireArguments().getString(ARG_SMARTSPACER_ID)!! }

    private lateinit var previewState: TextView
    private lateinit var previewDetail: TextView
    private lateinit var timezoneCard: MaterialCardView
    private lateinit var timezoneTitle: TextView
    private lateinit var timezoneSubtitle: TextView

    private val timezonePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val timezone = result.data?.getStringExtra(TimezonePickerActivity.EXTRA_TIMEZONE_ID)
            ?: return@registerForActivityResult
        updateConfig(TimezoneOffsetRequirementData(timezone))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_timezone_offset_requirement, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        applyInsets(view)
        bindActions()
        viewLifecycleOwner.lifecycleScope.launch {
            bindConfig(loadConfig() ?: TimezoneOffsetRequirementData())
        }
    }

    private fun bindViews(view: View) {
        previewState = view.findViewById(R.id.requirement_preview_state)
        previewDetail = view.findViewById(R.id.requirement_preview_detail)
        timezoneCard = view.findViewById(R.id.timezone_card)
        timezoneTitle = view.findViewById(R.id.timezone_title)
        timezoneSubtitle = view.findViewById(R.id.timezone_subtitle)
    }

    private fun applyInsets(view: View) {
        val content = view.findViewById<View>(R.id.requirement_configuration_content)
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
        timezoneCard.setOnClickListener {
            timezonePicker.launch(Intent(requireContext(), TimezonePickerActivity::class.java))
        }
    }

    private fun bindConfig(data: TimezoneOffsetRequirementData) {
        val locale = Locale.getDefault()
        val selectedZone = ZoneId.of(data.timezoneId)
        val deviceZone = ZoneId.systemDefault()
        val selectedOffset = TimeFormatter.formatOffset(selectedZone, Clock.systemUTC(), locale)
        val deviceOffset = TimeFormatter.formatOffset(deviceZone, Clock.systemUTC(), locale)
        val isMet = TimeFormatter.hasDifferentOffset(selectedZone, deviceZone, Clock.systemUTC())

        previewState.text = getString(
            if (isMet) {
                R.string.requirement_timezone_offset_preview_met
            } else {
                R.string.requirement_timezone_offset_preview_not_met
            }
        )
        previewDetail.text = getString(
            R.string.requirement_timezone_offset_preview_detail,
            data.timezoneId,
            selectedOffset,
            deviceZone.id,
            deviceOffset
        )
        timezoneTitle.text = data.timezoneId
        timezoneSubtitle.text = getString(
            R.string.configuration_timezone_subtitle,
            selectedZone.getDisplayName(TextStyle.FULL, locale),
            selectedOffset
        )
    }

    private fun updateConfig(data: TimezoneOffsetRequirementData) {
        viewLifecycleOwner.lifecycleScope.launch {
            TimezoneOffsetRequirementRepository.putConfig(dataStore, gson, smartspacerId, data)
            bindConfig(data)
            SmartspacerRequirementProvider.notifyChange(
                requireContext(),
                TimezoneOffsetRequirement::class.java,
                smartspacerId
            )
        }
    }

    private suspend fun loadConfig(): TimezoneOffsetRequirementData? {
        return TimezoneOffsetRequirementRepository.getConfig(dataStore, gson, smartspacerId).first()
    }

    companion object {
        private const val ARG_SMARTSPACER_ID = "smartspacer_id"

        fun newInstance(smartspacerId: String): TimezoneOffsetRequirementFragment {
            return TimezoneOffsetRequirementFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SMARTSPACER_ID, smartspacerId)
                }
            }
        }
    }
}
