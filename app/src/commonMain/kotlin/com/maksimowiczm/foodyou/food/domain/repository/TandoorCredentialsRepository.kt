package com.maksimowiczm.foodyou.food.domain.repository

import kotlinx.coroutines.flow.Flow

interface TandoorCredentialsRepository {
    suspend fun store(serverUrl: String, apiToken: String)

    suspend fun clear()

    fun hasCredentials(): Flow<Boolean>

    suspend fun load(): Pair<String, String>?
}
