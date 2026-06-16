package com.maksimowiczm.foodyou.app.ui.database.tandoor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maksimowiczm.foodyou.common.domain.measurement.Measurement
import com.maksimowiczm.foodyou.food.domain.entity.FoodId
import com.maksimowiczm.foodyou.food.domain.entity.Product
import com.maksimowiczm.foodyou.food.domain.entity.TandoorIngredientDraft
import com.maksimowiczm.foodyou.food.domain.entity.TandoorRecipeDraft
import com.maksimowiczm.foodyou.food.domain.repository.ProductRepository
import com.maksimowiczm.foodyou.food.domain.repository.TandoorCredentialsRepository
import com.maksimowiczm.foodyou.food.domain.usecase.ImportTandoorRecipeError
import com.maksimowiczm.foodyou.food.domain.usecase.ImportTandoorRecipeUseCase
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorConnectionError
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorIngredientResolution
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorRecipeMapper
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorRemoteDataSource
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.autoResolve
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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

internal data class FoodSearchResult(
    val id: FoodId.Product,
    val name: String,
)

internal class TandoorImportViewModel(
    private val recipeId: Int,
    private val credentialsRepository: TandoorCredentialsRepository,
    private val client: HttpClient,
    private val importTandoorRecipeUseCase: ImportTandoorRecipeUseCase,
    private val productRepository: ProductRepository,
    private val externalScope: CoroutineScope? = null,
) : ViewModel() {
    private val _state = MutableStateFlow<TandoorImportState>(TandoorImportState.Loading)
    val state: StateFlow<TandoorImportState> = _state.asStateFlow()

    private val eventBus = Channel<TandoorImportEvent>()
    val events = eventBus.receiveAsFlow()
    private val coroutineScope: CoroutineScope
        get() = externalScope ?: viewModelScope

    init {
        reload()
    }

    fun reload() {
        coroutineScope.launch {
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

        coroutineScope.launch {
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

    fun resolveWithWeight(
        ingredientIndex: Int,
        amountGrams: Double,
        isMilliliter: Boolean,
    ) {
        if (amountGrams <= 0.0) {
            return
        }
        updateReadyState { ready ->
            ready.resolveWithWeight(
                ingredientIndex = ingredientIndex,
                measurement =
                    if (isMilliliter) {
                        Measurement.Milliliter(amountGrams)
                    } else {
                        Measurement.Gram(amountGrams)
                    },
            )
        }
    }

    fun resolveWithExistingFood(
        ingredientIndex: Int,
        foodId: FoodId,
        measurement: Measurement,
    ) {
        updateReadyState { ready ->
            ready.resolveWithExistingFood(
                ingredientIndex = ingredientIndex,
                foodId = foodId,
                measurement =
                    measurement,
            )
        }
    }

    fun resolveWithEmptyProduct(
        ingredientIndex: Int,
        measurement: Measurement,
    ) {
        updateReadyState { ready ->
            ready.resolveWithEmptyProduct(
                ingredientIndex = ingredientIndex,
                measurement = measurement,
            )
        }
    }

    fun unresolve(ingredientIndex: Int) {
        updateReadyState { ready -> ready.unresolve(ingredientIndex) }
    }

    fun searchFood(query: String): Flow<List<FoodSearchResult>> =
        productRepository.observeFoodSearchResults(query)

    private fun updateReadyState(
        transform: (TandoorImportState.Ready) -> TandoorImportState.Ready,
    ) {
        val ready = _state.value as? TandoorImportState.Ready ?: return
        if (ready.isImporting) {
            return
        }
        _state.value = transform(ready)
    }
}

internal fun ProductRepository.observeFoodSearchResults(
    query: String,
): Flow<List<FoodSearchResult>> =
    observeProducts(limit = 50, offset = 0).map { products ->
        products.toFoodSearchResults(query)
    }

internal fun TandoorImportState.Ready.resolveWithWeight(
    ingredientIndex: Int,
    measurement: Measurement,
): TandoorImportState.Ready =
    updateResolution(ingredientIndex) { ingredient ->
        TandoorIngredientResolution.ManuallyWeighed(
            ingredient = ingredient,
            measurement = measurement,
        )
    }

internal fun TandoorImportState.Ready.resolveWithExistingFood(
    ingredientIndex: Int,
    foodId: FoodId,
    measurement: Measurement,
): TandoorImportState.Ready =
    updateResolution(ingredientIndex) { ingredient ->
        TandoorIngredientResolution.LinkedToFood(
            ingredient = ingredient,
            foodId = foodId,
            measurement = measurement,
        )
    }

internal fun TandoorImportState.Ready.resolveWithEmptyProduct(
    ingredientIndex: Int,
    measurement: Measurement,
): TandoorImportState.Ready =
    updateResolution(ingredientIndex) { ingredient ->
        TandoorIngredientResolution.EmptyProduct(
            ingredient = ingredient,
            measurement = measurement,
        )
    }

internal fun TandoorImportState.Ready.unresolve(ingredientIndex: Int): TandoorImportState.Ready =
    updateResolution(ingredientIndex, ::autoResolve)

internal fun List<Product>.toFoodSearchResults(query: String): List<FoodSearchResult> =
    filter { product ->
        query.isBlank() || product.name.contains(query, ignoreCase = true)
    }.map { product ->
        FoodSearchResult(
            id = product.id,
            name = product.name,
        )
    }

private fun TandoorImportState.Ready.updateResolution(
    ingredientIndex: Int,
    transform: (TandoorIngredientDraft) -> TandoorIngredientResolution,
): TandoorImportState.Ready {
    val ingredient = draft.ingredients.getOrNull(ingredientIndex) ?: return this
    val updatedResolutions =
        resolutions.toMutableList().apply {
            if (ingredientIndex in indices) {
                set(ingredientIndex, transform(ingredient))
            }
        }
    return copy(resolutions = updatedResolutions, importError = null)
}
