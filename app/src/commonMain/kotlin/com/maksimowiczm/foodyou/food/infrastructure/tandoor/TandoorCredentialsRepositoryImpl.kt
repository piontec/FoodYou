package com.maksimowiczm.foodyou.food.infrastructure.tandoor

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import com.maksimowiczm.foodyou.common.crypto.MasterCrypto
import com.maksimowiczm.foodyou.food.domain.repository.TandoorCredentialsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class TandoorCredentialsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val masterCrypto: MasterCrypto,
) : TandoorCredentialsRepository {
    override suspend fun store(serverUrl: String, apiToken: String) {
        dataStore.edit {
            it[serverUrlKey] = masterCrypto.encrypt(serverUrl.encodeToByteArray())
            it[apiTokenKey] = masterCrypto.encrypt(apiToken.encodeToByteArray())
        }
    }

    override suspend fun clear() {
        dataStore.edit {
            it.remove(serverUrlKey)
            it.remove(apiTokenKey)
        }
    }

    override fun hasCredentials(): Flow<Boolean> =
        dataStore.data.map { serverUrlKey in it && apiTokenKey in it }

    override suspend fun load(): Pair<String, String>? {
        val preferences = dataStore.data.first()

        return if (serverUrlKey in preferences && apiTokenKey in preferences) {
            val encryptedServerUrl = preferences[serverUrlKey] ?: return null
            val encryptedApiToken = preferences[apiTokenKey] ?: return null

            val serverUrl = masterCrypto.decrypt(encryptedServerUrl).decodeToString()
            val apiToken = masterCrypto.decrypt(encryptedApiToken).decodeToString()

            serverUrl to apiToken
        } else {
            null
        }
    }

    private companion object {
        private val serverUrlKey = byteArrayPreferencesKey("tandoor:serverUrl")
        private val apiTokenKey = byteArrayPreferencesKey("tandoor:apiToken")
    }
}
