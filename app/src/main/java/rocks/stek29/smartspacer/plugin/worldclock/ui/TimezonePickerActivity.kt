package rocks.stek29.smartspacer.plugin.worldclock.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import rocks.stek29.smartspacer.plugin.worldclock.R
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class TimezonePickerActivity : AppCompatActivity() {

    private val adapter = TimezoneAdapter { zoneId ->
        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_TIMEZONE_ID, zoneId))
        finish()
    }
    private val zones by lazy {
        ZoneId.getAvailableZoneIds()
            .map { zoneId ->
                val zone = ZoneId.of(zoneId)
                TimezoneRow(
                    id = zoneId,
                    title = "${zone.getDisplayName(TextStyle.FULL, Locale.getDefault())} ($zoneId)"
                )
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.timezone_picker_title)
        setContentView(R.layout.activity_timezone_picker)
        findViewById<RecyclerView>(R.id.timezones).apply {
            layoutManager = LinearLayoutManager(this@TimezonePickerActivity)
            adapter = this@TimezonePickerActivity.adapter
        }
        findViewById<EditText>(R.id.search).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        adapter.submit(zones)
    }

    private fun filter(query: String) {
        val normalized = query.trim()
        val rows = if (normalized.isBlank()) {
            zones
        } else {
            zones.filter {
                it.title.contains(normalized, ignoreCase = true) ||
                    it.id.contains(normalized, ignoreCase = true)
            }
        }
        adapter.submit(rows)
    }

    companion object {
        const val EXTRA_TIMEZONE_ID = "timezone_id"
    }
}

private data class TimezoneRow(
    val id: String,
    val title: String
)

private class TimezoneAdapter(
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<TimezoneAdapter.ViewHolder>() {

    private val rows = mutableListOf<TimezoneRow>()

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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(rows[position])
    }

    override fun getItemCount() = rows.size

    class ViewHolder(
        itemView: View,
        private val onClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val title = itemView.findViewById<TextView>(R.id.title)

        fun bind(row: TimezoneRow) {
            title.text = row.title
            itemView.setOnClickListener { onClick(row.id) }
        }
    }
}
