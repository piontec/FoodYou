package com.maksimowiczm.foodyou.app.ui.database.tandoor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.maksimowiczm.foodyou.food.domain.entity.TandoorRecipeListItem
import com.maksimowiczm.foodyou.food.domain.repository.TandoorCredentialsRepository
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorRecipePagingSource
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorRemoteDataSource
import io.ktor.client.HttpClient
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal sealed interface TandoorBrowseState {
    data object Loading : TandoorBrowseState
    data object MissingCredentials : TandoorBrowseState
    data object Ready : TandoorBrowseState
}

internal class TandoorBrowseViewModel(
    private val credentialsRepository: TandoorCredentialsRepository,
    private val client: HttpClient,
) : ViewModel() {
    private val queryFlow = MutableStateFlow("")
    val query: StateFlow<String> = queryFlow.asStateFlow()

    private val credentialsState =
        MutableStateFlow<TandoorBrowseCredentialsState>(TandoorBrowseCredentialsState.Loading)

    val state: StateFlow<TandoorBrowseState> =
        credentialsState
            .map { credentialsState ->
                when (credentialsState) {
                    TandoorBrowseCredentialsState.Loading -> TandoorBrowseState.Loading
                    TandoorBrowseCredentialsState.MissingCredentials ->
                        TandoorBrowseState.MissingCredentials
                    is TandoorBrowseCredentialsState.Ready -> TandoorBrowseState.Ready
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(2_000),
                initialValue = TandoorBrowseState.Loading,
            )

    @OptIn(FlowPreview::class)
    val pagingData =
        credentialsState
            .flatMapLatest { credentialsState ->
                when (credentialsState) {
                    TandoorBrowseCredentialsState.Loading,
                    TandoorBrowseCredentialsState.MissingCredentials -> flowOf(PagingData.empty())

                    is TandoorBrowseCredentialsState.Ready ->
                        query
                            .debounce(300)
                            .map(String::trim)
                            .distinctUntilChanged()
                            .flatMapLatest { query ->
                                Pager(
                                    config =
                                        PagingConfig(
                                            pageSize = TANDOOR_PAGE_SIZE,
                                            initialLoadSize = TANDOOR_PAGE_SIZE,
                                            enablePlaceholders = false,
                                        )
                                ) {
                                    TandoorRecipePagingSource(
                                        dataSource =
                                            TandoorRemoteDataSource(
                                                client = client,
                                                serverUrl = credentialsState.serverUrl,
                                                apiToken = credentialsState.apiToken,
                                            ),
                                        query = query,
                                        pageSize = TANDOOR_PAGE_SIZE,
                                    )
                                }.flow
                            }
                }
            }
            .cachedIn(viewModelScope)

    init {
        reload()
    }

    fun onQueryChange(query: String) {
        queryFlow.value = query
    }

    fun reload() {
        viewModelScope.launch {
            credentialsState.value = TandoorBrowseCredentialsState.Loading
            credentialsState.value =
                credentialsRepository
                    .load()
                    ?.let { (serverUrl, apiToken) ->
                        TandoorBrowseCredentialsState.Ready(
                            serverUrl = serverUrl.trimEnd('/'),
                            apiToken = apiToken,
                        )
                    }
                    ?: TandoorBrowseCredentialsState.MissingCredentials
        }
    }

    private sealed interface TandoorBrowseCredentialsState {
        data object Loading : TandoorBrowseCredentialsState
        data object MissingCredentials : TandoorBrowseCredentialsState

        data class Ready(
            val serverUrl: String,
            val apiToken: String,
        ) : TandoorBrowseCredentialsState
    }

    private companion object {
        private const val TANDOOR_PAGE_SIZE = 20
    }
}
