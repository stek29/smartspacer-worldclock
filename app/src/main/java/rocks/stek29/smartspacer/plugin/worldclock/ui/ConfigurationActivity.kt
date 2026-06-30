package rocks.stek29.smartspacer.plugin.worldclock.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.gson.Gson
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import rocks.stek29.smartspacer.plugin.worldclock.R
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockComplicationData
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockConfigRepository

class ConfigurationActivity : AppCompatActivity() {

    private val dataStore by inject<DataStore<Preferences>>()
    private val gson by inject<Gson>()

    val smartspacerId: String?
        get() = intent.getStringExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuration)
        val id = smartspacerId
        if (id == null) {
            finish()
            return
        }
        ensureDefaultConfig(id)
        setResult(Activity.RESULT_OK)
        if (savedInstanceState == null) {
            showFragment(id)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val id = smartspacerId ?: return finish()
        ensureDefaultConfig(id)
        setResult(Activity.RESULT_OK)
        showFragment(id)
    }

    private fun ensureDefaultConfig(smartspacerId: String) {
        runBlocking {
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
    }

    private fun showFragment(smartspacerId: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ConfigurationFragment.newInstance(smartspacerId))
            .commit()
    }
}
