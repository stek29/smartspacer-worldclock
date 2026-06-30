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
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

object TimezoneOffsetRequirementRepository {

    private const val KEY_PREFIX = "timezone_offset_requirement"
    private val configCache = ConcurrentHashMap<String, TimezoneOffsetRequirementData>()

    fun getConfig(
        dataStore: DataStore<Preferences>,
        gson: Gson,
        smartspacerId: String
    ): Flow<TimezoneOffsetRequirementData?> {
        val key = keyFor(smartspacerId)
        return dataStore.data
            .map { preferences ->
                preferences[key]?.let {
                    runCatching {
                        gson.fromJson(it, TimezoneOffsetRequirementData::class.java)
                    }.getOrNull()?.normalized()
                }.also { config ->
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
    ): TimezoneOffsetRequirementData? {
        configCache[smartspacerId]?.let { return it }
        return getConfig(dataStore, gson, smartspacerId).first()
    }

    suspend fun putConfig(
        dataStore: DataStore<Preferences>,
        gson: Gson,
        smartspacerId: String,
        data: TimezoneOffsetRequirementData
    ): Boolean {
        val normalizedData = data.normalized() ?: return false
        dataStore.edit { preferences ->
            preferences[keyFor(smartspacerId)] = gson.toJson(normalizedData)
        }
        configCache[smartspacerId] = normalizedData
        return true
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

    private fun keyFor(smartspacerId: String) = stringPreferencesKey("$KEY_PREFIX/$smartspacerId")

    private fun TimezoneOffsetRequirementData.normalized(): TimezoneOffsetRequirementData? {
        return runCatching {
            ZoneId.of(timezoneId)
            this
        }.getOrNull()
    }
}
