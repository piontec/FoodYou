package com.maksimowiczm.foodyou.app.ui.database.tandoor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maksimowiczm.foodyou.food.domain.entity.FoodId
import com.maksimowiczm.foodyou.food.domain.entity.TandoorRecipeDraft
import com.maksimowiczm.foodyou.food.domain.repository.TandoorCredentialsRepository
import com.maksimowiczm.foodyou.food.domain.usecase.ImportTandoorRecipeError
import com.maksimowiczm.foodyou.food.domain.usecase.ImportTandoorRecipeUseCase
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorConnectionError
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorIngredientResolution
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorRecipeMapper
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorRemoteDataSource
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.autoResolve
import io.ktor.client.HttpClient
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal sealed interface TandoorImportState {
    data object Loading : TandoorImportState

    data class Error(val error: TandoorImportScreenError) : TandoorImportState

    data class Ready(
        val draft: TandoorRecipeDraft,
        val resolutions: List<TandoorIngredientResolution>,
        val isImporting: Boolean = false,
        val importError: ImportTandoorRecipeError? = null,
    ) : TandoorImportState {
        val canImport: Boolean
            get() =
                !isImporting && resolutions.none { it is TandoorIngredientResolution.Unresolved }
    }
}

internal sealed interface TandoorImportScreenError {
    data object MissingCredentials : TandoorImportScreenError

    data class Remote(val error: TandoorConnectionError) : TandoorImportScreenError

    data object Generic : TandoorImportScreenError
}

internal sealed interface TandoorImportEvent {
    data class Imported(val recipeId: FoodId.Recipe) : TandoorImportEvent
}

internal class TandoorImportViewModel(
    private val recipeId: Int,
    private val credentialsRepository: TandoorCredentialsRepository,
    private val client: HttpClient,
    private val importTandoorRecipeUseCase: ImportTandoorRecipeUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<TandoorImportState>(TandoorImportState.Loading)
    val state: StateFlow<TandoorImportState> = _state.asStateFlow()

    private val eventBus = Channel<TandoorImportEvent>()
    val events = eventBus.receiveAsFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            _state.value = TandoorImportState.Loading
            val credentials = credentialsRepository.load()
            if (credentials == null) {
                _state.value = TandoorImportState.Error(TandoorImportScreenError.MissingCredentials)
                return@launch
            }

            val (serverUrl, apiToken) = credentials
            val result =
                TandoorRemoteDataSource(
                    client = client,
                    serverUrl = serverUrl.trimEnd('/'),
                    apiToken = apiToken,
                ).getRecipe(recipeId)

            _state.value =
                when {
                    result.isSuccess -> {
                        val draft = TandoorRecipeMapper.map(result.getOrThrow())
                        TandoorImportState.Ready(
                            draft = draft,
                            resolutions = draft.ingredients.map(::autoResolve),
                        )
                    }

                    result.exceptionOrNull() is TandoorConnectionError ->
                        TandoorImportState.Error(
                            TandoorImportScreenError.Remote(
                                result.exceptionOrNull() as TandoorConnectionError,
                            ),
                        )

                    else -> TandoorImportState.Error(TandoorImportScreenError.Generic)
                }
        }
    }

    fun import() {
        val ready = _state.value as? TandoorImportState.Ready ?: return
        if (!ready.canImport) {
            return
        }

        viewModelScope.launch {
            _state.value = ready.copy(isImporting = true, importError = null)

            when (val result = importTandoorRecipeUseCase.import(ready.draft, ready.resolutions)) {
                is com.maksimowiczm.foodyou.common.result.Result.Success -> {
                    _state.value = ready.copy(isImporting = false, importError = null)
                    eventBus.send(TandoorImportEvent.Imported(result.data))
                }

                is com.maksimowiczm.foodyou.common.result.Result.Error -> {
                    _state.value = ready.copy(isImporting = false, importError = result.error)
                }
            }
        }
    }
}
