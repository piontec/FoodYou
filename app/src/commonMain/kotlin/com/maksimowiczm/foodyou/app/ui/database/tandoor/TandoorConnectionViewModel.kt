package com.maksimowiczm.foodyou.app.ui.database.tandoor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maksimowiczm.foodyou.food.domain.repository.TandoorCredentialsRepository
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorConnectionError
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorRemoteDataSource
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

internal sealed interface TandoorConnectionState {
    data object Idle : TandoorConnectionState
    data object Testing : TandoorConnectionState
    data object Success : TandoorConnectionState
    data class Error(val error: TandoorConnectionError) : TandoorConnectionState
    data object GenericError : TandoorConnectionState
}

internal class TandoorConnectionViewModel(
    private val credentialsRepository: TandoorCredentialsRepository,
) : ViewModel() {
    val hasCredentials: StateFlow<Boolean> =
        credentialsRepository
            .hasCredentials()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(2_000),
                initialValue = false,
            )

    private val _loadedCredentials = MutableStateFlow<Pair<String, String>?>(null)
    val loadedCredentials: StateFlow<Pair<String, String>?> = _loadedCredentials.asStateFlow()

    init {
        viewModelScope.launch {
            _loadedCredentials.update { credentialsRepository.load() }
        }
    }

    private val _connectionState = MutableStateFlow<TandoorConnectionState>(TandoorConnectionState.Idle)
    val connectionState: StateFlow<TandoorConnectionState> = _connectionState.asStateFlow()

    fun testConnection(serverUrl: String, apiToken: String) {
        viewModelScope.launch {
            _connectionState.value = TandoorConnectionState.Testing
            val client = buildTestClient()
            val dataSource = TandoorRemoteDataSource(client, serverUrl.trimEnd('/'), apiToken)
            val result = dataSource.testConnection()
            _connectionState.value = when {
                result.isSuccess -> TandoorConnectionState.Success
                result.exceptionOrNull() is TandoorConnectionError.AuthError ->
                    TandoorConnectionState.Error(TandoorConnectionError.AuthError())
                result.exceptionOrNull() is TandoorConnectionError.ReachabilityError ->
                    TandoorConnectionState.Error(
                        TandoorConnectionError.ReachabilityError(result.exceptionOrNull()?.message)
                    )
                else -> TandoorConnectionState.GenericError
            }
            client.close()
        }
    }

    fun save(serverUrl: String, apiToken: String) {
        viewModelScope.launch {
            credentialsRepository.store(serverUrl.trimEnd('/'), apiToken)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            credentialsRepository.clear()
            _connectionState.value = TandoorConnectionState.Idle
        }
    }

    private fun buildTestClient() = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 15_000
        }
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }
}
