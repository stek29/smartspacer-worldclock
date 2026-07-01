package rocks.stek29.smartspacer.plugin.worldclock

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

private val Context.worldClockDataStore by preferencesDataStore(name = "worldclock")

class WorldClockPlugin : Application() {

    override fun onCreate() {
        super.onCreate()
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        if (GlobalContext.getOrNull() != null) return
        startKoin {
            androidContext(this@WorldClockPlugin)
            modules(module {
                single<DataStore<Preferences>> { this@WorldClockPlugin.worldClockDataStore }
                single { Gson() }
            })
        }
    }
}
