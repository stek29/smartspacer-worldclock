package rocks.stek29.smartspacer.plugin.worldclock.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

object WorldClockConfigRepository {

    private val configCache = ConcurrentHashMap<String, WorldClockComplicationData>()

    fun getConfig(
        dataStore: DataStore<Preferences>,
        gson: Gson,
        smartspacerId: String
    ): Flow<WorldClockComplicationData?> {
        val key = keyFor(smartspacerId)
        return dataStore.data
            .map { preferences ->
                preferences[key]?.let { runCatching {
                    gson.fromJson(it, WorldClockComplicationData::class.java)
                }.getOrNull()?.normalized() }
                    .also { config ->
                        if (config != null) {
                            configCache[smartspacerId] = config
                        } else {
                            configCache.remove(smartspacerId)
                        }
                    }
            }
            .catch { emit(null) }
    }

    suspend fun getConfigOnce(
        dataStore: DataStore<Preferences>,
        gson: Gson,
        smartspacerId: String
    ): WorldClockComplicationData? {
        configCache[smartspacerId]?.let { return it }
        return getConfig(dataStore, gson, smartspacerId).first()
    }

    suspend fun putConfig(
        dataStore: DataStore<Preferences>,
        gson: Gson,
        smartspacerId: String,
        data: WorldClockComplicationData
    ) {
        val normalizedData = data.normalized()
        dataStore.edit { preferences ->
            preferences[keyFor(smartspacerId)] = gson.toJson(normalizedData)
        }
        configCache[smartspacerId] = normalizedData
    }

    suspend fun deleteConfig(dataStore: DataStore<Preferences>, smartspacerId: String) {
        dataStore.edit { preferences ->
            preferences.remove(keyFor(smartspacerId))
        }
        configCache.remove(smartspacerId)
    }

    fun invalidateConfig(smartspacerId: String) {
        configCache.remove(smartspacerId)
    }

    fun invalidateAll() {
        configCache.clear()
    }

    private fun keyFor(smartspacerId: String) = stringPreferencesKey("worldclock/$smartspacerId")

    private fun WorldClockComplicationData.normalized(): WorldClockComplicationData {
        val safeIconStyle = runCatching {
            WorldClockComplicationData.IconStyle.valueOf(iconStyle.name)
        }.getOrNull()
            ?: WorldClockComplicationData.IconStyle.WORLD_CLOCK
        return copy(iconStyle = safeIconStyle)
    }
}
