package com.maksimowiczm.foodyou.food.infrastructure.tandoor

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Minimal fake MasterCrypto for testing
class FakeMasterCrypto : com.maksimowiczm.foodyou.common.crypto.MasterCrypto {
    override val isSupported: Flow<Boolean> = MutableStateFlow(true)

    override suspend fun encrypt(data: ByteArray): ByteArray {
        // For testing, just return the data (no real encryption)
        return data
    }

    override suspend fun decrypt(encryptedData: ByteArray): ByteArray {
        // For testing, just return the data (no real decryption)
        return encryptedData
    }
}

// Minimal in-memory DataStore for testing
class FakeDataStore : DataStore<Preferences> {
    private val _data = MutableStateFlow(emptyPreferences())
    override val data: Flow<Preferences> = _data

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val newData = transform(_data.value)
        _data.value = newData
        return newData
    }
}

class TandoorCredentialsRepositoryTest {
    @Test
    fun storeAndLoadCredentials() = kotlinx.coroutines.runBlocking {
        val repo = TandoorCredentialsRepositoryImpl(
            dataStore = FakeDataStore(),
            masterCrypto = FakeMasterCrypto()
        )

        val serverUrl = "https://tandoor.example.com"
        val apiToken = "tda_test123"

        // Store credentials
        repo.store(serverUrl, apiToken)

        // Load them back
        val loaded = repo.load()

        assertEquals(serverUrl to apiToken, loaded)
    }

    @Test
    fun hasCredentialsReturnsTrueAfterStore() = kotlinx.coroutines.runBlocking {
        val repo = TandoorCredentialsRepositoryImpl(
            dataStore = FakeDataStore(),
            masterCrypto = FakeMasterCrypto()
        )

        // Initially no credentials
        var hasCredsFlow = repo.hasCredentials().first()
        assertTrue(!hasCredsFlow)

        // After store
        repo.store("https://tandoor.example.com", "tda_test123")
        hasCredsFlow = repo.hasCredentials().first()
        assertTrue(hasCredsFlow)
    }

    @Test
    fun clearRemovesCredentials() = kotlinx.coroutines.runBlocking {
        val repo = TandoorCredentialsRepositoryImpl(
            dataStore = FakeDataStore(),
            masterCrypto = FakeMasterCrypto()
        )

        repo.store("https://tandoor.example.com", "tda_test123")
        var loaded = repo.load()
        assertEquals("https://tandoor.example.com" to "tda_test123", loaded)

        // Clear
        repo.clear()
        loaded = repo.load()
        assertNull(loaded)

        var hasCredsFlow = repo.hasCredentials().first()
        assertTrue(!hasCredsFlow)
    }
}
