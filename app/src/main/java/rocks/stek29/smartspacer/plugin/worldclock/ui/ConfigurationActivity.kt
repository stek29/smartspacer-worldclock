package rocks.stek29.smartspacer.plugin.worldclock.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
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
import rocks.stek29.smartspacer.plugin.worldclock.config.WorldClockTargetData

class ConfigurationActivity : AppCompatActivity() {

    private val dataStore by inject<DataStore<Preferences>>()
    private val gson by inject<Gson>()

    val smartspacerId: String?
        get() = intent.getStringExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID)

    private val type: Type
        get() = Type.fromIntent(intent)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureWindow()
        setContentView(R.layout.activity_configuration)
        val id = smartspacerId
        if (id == null) {
            finish()
            return
        }
        ensureDefaultConfig(id, type)
        setResult(Activity.RESULT_OK)
        if (savedInstanceState == null) {
            showFragment(id, type)
        }
    }

    @Suppress("DEPRECATION")
    private fun configureWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val id = smartspacerId ?: return finish()
        ensureDefaultConfig(id, type)
        setResult(Activity.RESULT_OK)
        showFragment(id, type)
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
                    val existing = WorldClockConfigRepository.getTargetConfig(
                        dataStore,
                        gson,
                        smartspacerId
                    ).first()
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

    private fun showFragment(smartspacerId: String, type: Type) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ConfigurationFragment.newInstance(smartspacerId, type))
            .commit()
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
        fun createIntent(context: Context, type: Type): Intent {
            return Type.putExtra(Intent(context, ConfigurationActivity::class.java), type)
        }
    }
}
