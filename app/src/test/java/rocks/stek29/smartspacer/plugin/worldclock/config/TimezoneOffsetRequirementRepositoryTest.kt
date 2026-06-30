package rocks.stek29.smartspacer.plugin.worldclock.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class TimezoneOffsetRequirementRepositoryTest {

    private val gson = Gson()
    private val smartspacerId = "test-id"

    @After
    fun tearDown() {
        TimezoneOffsetRequirementRepository.invalidateAll()
    }

    @Test
    fun getConfigOnceUsesCachedValueUntilInvalidated() = runBlocking {
        val dataStore = createDataStore()
        val cached = data("Asia/Tokyo")
        val updated = data("Europe/London")

        assertTrue(TimezoneOffsetRequirementRepository.putConfig(
            dataStore,
            gson,
            smartspacerId,
            cached
        ))
        assertEquals(
            cached,
            TimezoneOffsetRequirementRepository.getConfigOnce(dataStore, gson, smartspacerId)
        )

        writeConfigDirectly(dataStore, updated)

        assertEquals(
            cached,
            TimezoneOffsetRequirementRepository.getConfigOnce(dataStore, gson, smartspacerId)
        )

        TimezoneOffsetRequirementRepository.invalidateConfig(smartspacerId)

        assertEquals(
            updated,
            TimezoneOffsetRequirementRepository.getConfigOnce(dataStore, gson, smartspacerId)
        )
    }

    @Test
    fun putConfigRejectsInvalidTimezone() = runBlocking {
        val dataStore = createDataStore()

        val success = TimezoneOffsetRequirementRepository.putConfig(
            dataStore,
            gson,
            smartspacerId,
            data("Not/AZone")
        )

        assertFalse(success)
        assertNull(TimezoneOffsetRequirementRepository.getConfigOnce(dataStore, gson, smartspacerId))
    }

    @Test
    fun putConfigUpdatesCachedValue() = runBlocking {
        val dataStore = createDataStore()
        val original = data("Asia/Tokyo")
        val updated = data("Europe/London")

        TimezoneOffsetRequirementRepository.putConfig(dataStore, gson, smartspacerId, original)
        TimezoneOffsetRequirementRepository.putConfig(dataStore, gson, smartspacerId, updated)

        assertEquals(
            updated,
            TimezoneOffsetRequirementRepository.getConfigOnce(dataStore, gson, smartspacerId)
        )
    }

    @Test
    fun deleteConfigClearsCachedValue() = runBlocking {
        val dataStore = createDataStore()

        TimezoneOffsetRequirementRepository.putConfig(
            dataStore,
            gson,
            smartspacerId,
            data("Asia/Tokyo")
        )

        TimezoneOffsetRequirementRepository.deleteConfig(dataStore, smartspacerId)

        assertNull(TimezoneOffsetRequirementRepository.getConfigOnce(dataStore, gson, smartspacerId))
    }

    @Test
    fun getConfigFlowRefreshesCache() = runBlocking {
        val dataStore = createDataStore()
        val cached = data("Asia/Tokyo")
        val updated = data("Europe/London")

        TimezoneOffsetRequirementRepository.putConfig(dataStore, gson, smartspacerId, cached)
        writeConfigDirectly(dataStore, updated)

        assertEquals(
            updated,
            TimezoneOffsetRequirementRepository.getConfig(dataStore, gson, smartspacerId).first()
        )
        assertEquals(
            updated,
            TimezoneOffsetRequirementRepository.getConfigOnce(dataStore, gson, smartspacerId)
        )
    }

    private fun createDataStore(): DataStore<Preferences> {
        val directory = Files.createTempDirectory("timezone-offset-repository-test")
        val file = directory.resolve("settings.preferences_pb").toFile()
        return PreferenceDataStoreFactory.create { file }
    }

    private suspend fun writeConfigDirectly(
        dataStore: DataStore<Preferences>,
        data: TimezoneOffsetRequirementData
    ) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("timezone_offset_requirement/$smartspacerId")] =
                gson.toJson(data)
        }
    }

    private fun data(timezoneId: String): TimezoneOffsetRequirementData {
        return TimezoneOffsetRequirementData(timezoneId)
    }
}
