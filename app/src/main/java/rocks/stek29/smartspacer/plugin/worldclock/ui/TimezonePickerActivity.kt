package rocks.stek29.smartspacer.plugin.worldclock.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Space
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import rocks.stek29.smartspacer.plugin.worldclock.R
import rocks.stek29.smartspacer.plugin.worldclock.utils.TimeFormatter
import java.time.Clock
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Comparator
import java.util.Locale

class TimezonePickerActivity : AppCompatActivity() {

    private lateinit var timezones: RecyclerView
    private lateinit var emptyState: TextView

    private val adapter = TimezoneAdapter { zoneId ->
        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_TIMEZONE_ID, zoneId))
        finish()
    }
    private val zones by lazy {
        val clock = Clock.systemUTC()
        val locale = Locale.getDefault()
        ZoneId.getAvailableZoneIds()
            .filterNot(::isLegacyAlias)
            .map { zoneId ->
                val zone = ZoneId.of(zoneId)
                val offset = TimeFormatter.formatOffset(zone, clock, locale)
                val displayName = friendlyDisplayName(zone, zoneId, locale)
                val subtitle = getString(
                    R.string.timezone_row_subtitle,
                    displayName,
                    offset
                )
                TimezoneRow(
                    id = zoneId,
                    title = zoneId,
                    subtitle = subtitle,
                    offset = offset,
                    searchText = listOf(zoneId, displayName, subtitle, offset)
                        .joinToString(separator = " ")
                )
            }
            .sortedWith(TimezoneRowComparator)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureWindow()
        title = getString(R.string.timezone_picker_title)
        setContentView(R.layout.activity_timezone_picker)
        adapter.selectedId = intent.getStringExtra(EXTRA_SELECTED_TIMEZONE_ID)
        applyInsets()
        timezones = findViewById(R.id.timezones)
        emptyState = findViewById(R.id.empty_state)
        timezones.apply {
            layoutManager = LinearLayoutManager(this@TimezonePickerActivity)
            adapter = this@TimezonePickerActivity.adapter
        }
        findViewById<ImageButton>(R.id.search_back).setOnClickListener { finish() }
        val clearButton = findViewById<ImageButton>(R.id.search_clear)
        val search = findViewById<EditText>(R.id.search)
        clearButton.setOnClickListener { search.text?.clear() }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty()
                clearButton.visibility = if (query.isBlank()) View.GONE else View.VISIBLE
                filter(query)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        adapter.submit(zones)
    }

    @Suppress("DEPRECATION")
    private fun configureWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }

    private fun applyInsets() {
        val root = findViewById<View>(R.id.timezone_picker_root)
        val searchContainer = findViewById<View>(R.id.search_container)
        val baseTop = searchContainer.paddingTop
        val rootBaseBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { target, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            target.setPadding(
                target.paddingLeft,
                target.paddingTop,
                target.paddingRight,
                rootBaseBottom + maxOf(systemBars.bottom, ime.bottom)
            )
            insets
        }
        val topMargin = (searchContainer.layoutParams as ViewGroup.MarginLayoutParams).topMargin
        ViewCompat.setOnApplyWindowInsetsListener(searchContainer) { target, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val layoutParams = target.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.topMargin = topMargin + systemBars.top
            target.layoutParams = layoutParams
            target.setPadding(target.paddingLeft, baseTop, target.paddingRight, target.paddingBottom)
            insets
        }
    }

    private fun filter(query: String) {
        val normalized = query.trim()
        val rows = if (normalized.isBlank()) {
            zones
        } else {
            zones.filter {
                it.searchText.contains(normalized, ignoreCase = true)
            }
        }
        adapter.submit(rows)
        timezones.visibility = if (rows.isEmpty()) View.GONE else View.VISIBLE
        emptyState.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
    }

    companion object {
        const val EXTRA_TIMEZONE_ID = "timezone_id"
        const val EXTRA_SELECTED_TIMEZONE_ID = "selected_timezone_id"
    }
}

private data class TimezoneRow(
    val id: String,
    val title: String,
    val subtitle: String,
    val offset: String,
    val searchText: String
)

private object TimezoneRowComparator : Comparator<TimezoneRow> {
    override fun compare(left: TimezoneRow, right: TimezoneRow): Int {
        return String.CASE_INSENSITIVE_ORDER.compare(left.id, right.id)
    }
}

private class TimezoneAdapter(
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<TimezoneAdapter.ViewHolder>() {

    private val rows = mutableListOf<TimezoneRow>()
    var selectedId: String? = null

    fun submit(newRows: List<TimezoneRow>) {
        rows.clear()
        rows.addAll(newRows)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_timezone, parent, false)
        return ViewHolder(view, onClick)
    }

    override fun getItemCount() = rows.size

    class ViewHolder(
        itemView: View,
        private val onClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val selectedIndicator = itemView.findViewById<ImageView>(R.id.selected_indicator)
        private val selectedSpacer = itemView.findViewById<Space>(R.id.selected_spacer)
        private val title = itemView.findViewById<TextView>(R.id.title)
        private val subtitle = itemView.findViewById<TextView>(R.id.subtitle)

        fun bind(row: TimezoneRow, selectedId: String?) {
            val isSelected = row.id == selectedId
            selectedIndicator.visibility = if (isSelected) View.VISIBLE else View.GONE
            selectedSpacer.visibility = if (isSelected) View.GONE else View.VISIBLE
            title.text = row.title
            subtitle.text = row.subtitle
            itemView.setOnClickListener { onClick(row.id) }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(rows[position], selectedId)
    }
}

private fun isLegacyAlias(zoneId: String): Boolean {
    return !zoneId.contains("/") &&
        zoneId.length <= 3 &&
        zoneId != "UTC" &&
        zoneId != "GMT"
}

private fun friendlyDisplayName(zone: ZoneId, zoneId: String, locale: Locale): String {
    val displayName = zone.getDisplayName(TextStyle.FULL, locale)
    if (displayName != zoneId) return displayName
    val city = zoneId.substringAfterLast('/').replace('_', ' ')
    return "$city Time"
}
