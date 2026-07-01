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
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.file.Files

class WorldClockConfigRepositoryTest {

    private val gson = Gson()
    private val smartspacerId = "test-id"

    @After
    fun tearDown() {
        WorldClockConfigRepository.invalidateAll()
    }

    @Test
    fun getConfigOnceUsesCachedValueUntilInvalidated() = runBlocking {
        val dataStore = createDataStore()
        val cached = data("Asia/Tokyo")
        val updated = data("Europe/London")

        WorldClockConfigRepository.putConfig(dataStore, gson, smartspacerId, cached)
        assertEquals(cached, WorldClockConfigRepository.getConfigOnce(dataStore, gson, smartspacerId))

        writeConfigDirectly(dataStore, updated)

        assertEquals(cached, WorldClockConfigRepository.getConfigOnce(dataStore, gson, smartspacerId))

        WorldClockConfigRepository.invalidateConfig(smartspacerId)

        assertEquals(updated, WorldClockConfigRepository.getConfigOnce(dataStore, gson, smartspacerId))
    }

    @Test
    fun putConfigUpdatesCachedValue() = runBlocking {
        val dataStore = createDataStore()
        val original = data("Asia/Tokyo")
        val updated = data("Europe/London")

        WorldClockConfigRepository.putConfig(dataStore, gson, smartspacerId, original)
        assertEquals(original, WorldClockConfigRepository.getConfigOnce(dataStore, gson, smartspacerId))

        WorldClockConfigRepository.putConfig(dataStore, gson, smartspacerId, updated)

        assertEquals(updated, WorldClockConfigRepository.getConfigOnce(dataStore, gson, smartspacerId))
    }

    @Test
    fun deleteConfigClearsCachedValue() = runBlocking {
        val dataStore = createDataStore()

        WorldClockConfigRepository.putConfig(dataStore, gson, smartspacerId, data("Asia/Tokyo"))
        assertEquals("Asia/Tokyo", WorldClockConfigRepository.getConfigOnce(
            dataStore,
            gson,
            smartspacerId
        )?.timezoneId)

        WorldClockConfigRepository.deleteConfig(dataStore, smartspacerId)

        assertNull(WorldClockConfigRepository.getConfigOnce(dataStore, gson, smartspacerId))
    }

    @Test
    fun targetConfigUsesSeparateKeyFromComplicationConfig() = runBlocking {
        val dataStore = createDataStore()
        val complication = data("Asia/Tokyo")
        val target = targetData("Europe/London")

        WorldClockConfigRepository.putConfig(dataStore, gson, smartspacerId, complication)
        WorldClockConfigRepository.putTargetConfig(dataStore, gson, smartspacerId, target)

        assertEquals(complication, WorldClockConfigRepository.getConfigOnce(
            dataStore,
            gson,
            smartspacerId
        ))
        assertEquals(target, WorldClockConfigRepository.getTargetConfigOnce(
            dataStore,
            gson,
            smartspacerId
        ))
    }

    @Test
    fun deleteTargetConfigDoesNotDeleteComplicationConfig() = runBlocking {
        val dataStore = createDataStore()
        val complication = data("Asia/Tokyo")
        val target = targetData("Europe/London")

        WorldClockConfigRepository.putConfig(dataStore, gson, smartspacerId, complication)
        WorldClockConfigRepository.putTargetConfig(dataStore, gson, smartspacerId, target)

        WorldClockConfigRepository.deleteTargetConfig(dataStore, smartspacerId)

        assertEquals(complication, WorldClockConfigRepository.getConfigOnce(
            dataStore,
            gson,
            smartspacerId
        ))
        assertNull(WorldClockConfigRepository.getTargetConfigOnce(dataStore, gson, smartspacerId))
    }

    @Test
    fun invalidateAllForcesAllConfigsToReload() = runBlocking {
        val dataStore = createDataStore()
        val firstId = "first"
        val secondId = "second"
        val firstCached = data("Asia/Tokyo")
        val secondCached = data("Europe/Berlin")
        val firstUpdated = data("Europe/London")
        val secondUpdated = data("America/New_York")

        WorldClockConfigRepository.putConfig(dataStore, gson, firstId, firstCached)
        WorldClockConfigRepository.putConfig(dataStore, gson, secondId, secondCached)

        writeConfigDirectly(dataStore, firstId, firstUpdated)
        writeConfigDirectly(dataStore, secondId, secondUpdated)

        assertEquals(firstCached, WorldClockConfigRepository.getConfigOnce(dataStore, gson, firstId))
        assertEquals(secondCached, WorldClockConfigRepository.getConfigOnce(dataStore, gson, secondId))

        WorldClockConfigRepository.invalidateAll()

        assertEquals(firstUpdated, WorldClockConfigRepository.getConfigOnce(dataStore, gson, firstId))
        assertEquals(secondUpdated, WorldClockConfigRepository.getConfigOnce(dataStore, gson, secondId))
    }

    @Test
    fun getConfigFlowRefreshesCache() = runBlocking {
        val dataStore = createDataStore()
        val cached = data("Asia/Tokyo")
        val updated = data("Europe/London")

        WorldClockConfigRepository.putConfig(dataStore, gson, smartspacerId, cached)
        writeConfigDirectly(dataStore, updated)

        assertEquals(updated, WorldClockConfigRepository.getConfig(dataStore, gson, smartspacerId).first())
        assertEquals(updated, WorldClockConfigRepository.getConfigOnce(dataStore, gson, smartspacerId))
    }

    private fun createDataStore(): DataStore<Preferences> {
        val directory = Files.createTempDirectory("worldclock-repository-test")
        val file = directory.resolve("settings.preferences_pb").toFile()
        return PreferenceDataStoreFactory.create { file }
    }

    private suspend fun writeConfigDirectly(
        dataStore: DataStore<Preferences>,
        data: WorldClockComplicationData
    ) {
        writeConfigDirectly(dataStore, smartspacerId, data)
    }

    private suspend fun writeConfigDirectly(
        dataStore: DataStore<Preferences>,
        id: String,
        data: WorldClockComplicationData
    ) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("worldclock/$id")] = gson.toJson(data)
        }
    }

    private fun data(timezoneId: String): WorldClockComplicationData {
        return WorldClockComplicationData(
            timezoneId = timezoneId,
            mode = WorldClockComplicationData.Mode.NORMAL,
            timeFormat = WorldClockComplicationData.TimeFormat.HOUR_24,
            iconStyle = WorldClockComplicationData.IconStyle.WORLD_CLOCK
        )
    }

    private fun targetData(timezoneId: String): WorldClockTargetData {
        return WorldClockTargetData(
            timezoneId = timezoneId,
            mode = WorldClockComplicationData.Mode.NORMAL,
            timeFormat = WorldClockComplicationData.TimeFormat.HOUR_24,
            iconStyle = WorldClockComplicationData.IconStyle.WORLD_CLOCK,
            hideSubtitleOnAod = true
        )
    }
}
