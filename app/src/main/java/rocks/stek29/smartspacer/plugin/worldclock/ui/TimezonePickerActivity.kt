package rocks.stek29.smartspacer.plugin.worldclock.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import rocks.stek29.smartspacer.plugin.worldclock.R
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.TimezoneRow
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.WorldClockCardShape
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.WorldClockHorizontalPadding
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.WorldClockTheme
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.buildTimezoneRows
import rocks.stek29.smartspacer.plugin.worldclock.ui.compose.filterTimezoneRows

class TimezonePickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureWindow()
        val selectedId = intent.getStringExtra(EXTRA_SELECTED_TIMEZONE_ID)
        setContent {
            WorldClockTheme {
                TimezonePickerScreen(
                    selectedId = selectedId,
                    onBack = { finish() },
                    onSelected = { zoneId ->
                        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_TIMEZONE_ID, zoneId))
                        finish()
                    }
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun configureWindow() {
        configureEdgeToEdge()
    }

    companion object {
        const val EXTRA_TIMEZONE_ID = "timezone_id"
        const val EXTRA_SELECTED_TIMEZONE_ID = "selected_timezone_id"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimezonePickerScreen(
    selectedId: String?,
    onBack: () -> Unit,
    onSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val rows = remember { buildTimezoneRows(context) }
    var query by remember { mutableStateOf("") }
    val filteredRows = remember(rows, query) { filterTimezoneRows(rows, query) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = WorldClockHorizontalPadding)
    ) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = {},
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = { Text(stringResource(R.string.timezone_search_hint)) },
                    leadingIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.timezone_search_close)
                            )
                        }
                    },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    Icons.Filled.Clear,
                                    contentDescription = stringResource(R.string.timezone_search_clear)
                                )
                            }
                        }
                    }
                )
            },
            expanded = false,
            onExpandedChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            shape = WorldClockCardShape,
            colors = SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            ),
            content = {}
        )
        if (filteredRows.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.timezone_no_results),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.Top
            ) {
                items(filteredRows, key = { it.id }) { row ->
                    TimezoneListItem(
                        row = row,
                        selected = row.id == selectedId,
                        onClick = { onSelected(row.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimezoneListItem(
    row: TimezoneRow,
    selected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingContent = {
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                if (selected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        headlineContent = {
            Text(
                text = row.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = row.subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = ComposeColor.Transparent)
    )
}
