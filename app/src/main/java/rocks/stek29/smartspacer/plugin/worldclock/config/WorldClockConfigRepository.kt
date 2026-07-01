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
    private val targetConfigCache = ConcurrentHashMap<String, WorldClockTargetData>()

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

    fun getTargetConfig(
        dataStore: DataStore<Preferences>,
        gson: Gson,
        smartspacerId: String
    ): Flow<WorldClockTargetData?> {
        val key = targetKeyFor(smartspacerId)
        return dataStore.data
            .map { preferences ->
                preferences[key]?.let { runCatching {
                    gson.fromJson(it, WorldClockTargetData::class.java)
                }.getOrNull()?.normalized() }
                    .also { config ->
                        if (config != null) {
                            targetConfigCache[smartspacerId] = config
                        } else {
                            targetConfigCache.remove(smartspacerId)
                        }
                    }
            }
            .catch { emit(null) }
    }

    suspend fun getTargetConfigOnce(
        dataStore: DataStore<Preferences>,
        gson: Gson,
        smartspacerId: String
    ): WorldClockTargetData? {
        targetConfigCache[smartspacerId]?.let { return it }
        return getTargetConfig(dataStore, gson, smartspacerId).first()
    }

    suspend fun putTargetConfig(
        dataStore: DataStore<Preferences>,
        gson: Gson,
        smartspacerId: String,
        data: WorldClockTargetData
    ) {
        val normalizedData = data.normalized()
        dataStore.edit { preferences ->
            preferences[targetKeyFor(smartspacerId)] = gson.toJson(normalizedData)
        }
        targetConfigCache[smartspacerId] = normalizedData
    }

    suspend fun deleteTargetConfig(dataStore: DataStore<Preferences>, smartspacerId: String) {
        dataStore.edit { preferences ->
            preferences.remove(targetKeyFor(smartspacerId))
        }
        targetConfigCache.remove(smartspacerId)
    }

    fun invalidateConfig(smartspacerId: String) {
        configCache.remove(smartspacerId)
        targetConfigCache.remove(smartspacerId)
    }

    fun invalidateAll() {
        configCache.clear()
        targetConfigCache.clear()
    }

    private fun keyFor(smartspacerId: String) = stringPreferencesKey("worldclock/$smartspacerId")
    private fun targetKeyFor(smartspacerId: String) =
        stringPreferencesKey("worldclock-target/$smartspacerId")

    private fun WorldClockComplicationData.normalized(): WorldClockComplicationData {
        val safeIconStyle = runCatching {
            WorldClockComplicationData.IconStyle.valueOf(iconStyle.name)
        }.getOrNull()
            ?: WorldClockComplicationData.IconStyle.WORLD_CLOCK
        return copy(iconStyle = safeIconStyle)
    }

    private fun WorldClockTargetData.normalized(): WorldClockTargetData {
        val safeIconStyle = runCatching {
            WorldClockComplicationData.IconStyle.valueOf(iconStyle.name)
        }.getOrNull()
            ?: WorldClockComplicationData.IconStyle.WORLD_CLOCK
        return copy(iconStyle = safeIconStyle)
    }
}
