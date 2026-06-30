package rocks.stek29.smartspacer.plugin.worldclock.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

object WorldClockConfigRepository {

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
                }.getOrNull() }
            }
            .catch { emit(null) }
    }

    suspend fun putConfig(
        dataStore: DataStore<Preferences>,
        gson: Gson,
        smartspacerId: String,
        data: WorldClockComplicationData
    ) {
        dataStore.edit { preferences ->
            preferences[keyFor(smartspacerId)] = gson.toJson(data)
        }
    }

    suspend fun deleteConfig(dataStore: DataStore<Preferences>, smartspacerId: String) {
        dataStore.edit { preferences ->
            preferences.remove(keyFor(smartspacerId))
        }
    }

    private fun keyFor(smartspacerId: String) = stringPreferencesKey("worldclock/$smartspacerId")
}
