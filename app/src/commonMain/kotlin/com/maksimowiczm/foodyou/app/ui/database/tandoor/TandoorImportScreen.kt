package com.maksimowiczm.foodyou.app.ui.database.tandoor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maksimowiczm.foodyou.app.ui.common.component.ArrowBackIconButton
import com.maksimowiczm.foodyou.app.ui.common.utility.stringResource as measurementStringResource
import com.maksimowiczm.foodyou.common.compose.extension.LaunchedCollectWithLifecycle
import com.maksimowiczm.foodyou.common.domain.measurement.Measurement
import com.maksimowiczm.foodyou.food.domain.entity.FoodId
import com.maksimowiczm.foodyou.food.domain.usecase.ImportTandoorRecipeError
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorConnectionError
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorIngredientResolution
import foodyou.app.generated.resources.*
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun TandoorImportScreen(
    recipeId: Int,
    onBack: () -> Unit,
    onImported: (FoodId.Recipe) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<TandoorImportViewModel> { parametersOf(recipeId) }
    val latestOnImported by rememberUpdatedState(onImported)
    LaunchedCollectWithLifecycle(viewModel.events) { event ->
        when (event) {
            is TandoorImportEvent.Imported -> latestOnImported(event.recipeId)
        }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    TandoorImportScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::reload,
        onImport = viewModel::import,
        onResolveWithWeight = viewModel::resolveWithWeight,
        onResolveWithExistingFood = viewModel::resolveWithExistingFood,
        onResolveWithEmptyProduct = viewModel::resolveWithEmptyProduct,
        onUnresolve = viewModel::unresolve,
        searchFood = viewModel::searchFood,
        modifier = modifier,
    )
}

@Composable
internal fun TandoorImportScreen(
    state: TandoorImportState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onImport: () -> Unit,
    onResolveWithWeight: (Int, Double, Boolean) -> Unit,
    onResolveWithExistingFood: (Int, FoodId, Measurement) -> Unit,
    onResolveWithEmptyProduct: (Int, Measurement) -> Unit,
    onUnresolve: (Int) -> Unit,
    searchFood: (String) -> Flow<List<FoodSearchResult>>,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(Res.string.headline_tandoor_import)) },
                navigationIcon = { ArrowBackIconButton(onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
        ) {
            when (state) {
                TandoorImportState.Loading ->
                    LoadingState(modifier = Modifier.fillMaxSize())

                is TandoorImportState.Error ->
                    ErrorState(
                        message =
                            when (val error = state.error) {
                                TandoorImportScreenError.MissingCredentials ->
                                    stringResource(Res.string.error_tandoor_browse)
                                is TandoorImportScreenError.Remote ->
                                    stringResource(error.error.toResource())
                                TandoorImportScreenError.Generic ->
                                    stringResource(Res.string.error_tandoor_import)
                            },
                        onRetry = onRetry,
                        modifier = Modifier.align(Alignment.Center),
                    )

                is TandoorImportState.Ready ->
                    ReadyState(
                        state = state,
                        onImport = onImport,
                        onResolveWithWeight = onResolveWithWeight,
                        onResolveWithExistingFood = onResolveWithExistingFood,
                        onResolveWithEmptyProduct = onResolveWithEmptyProduct,
                        onUnresolve = onUnresolve,
                        searchFood = searchFood,
                        modifier = Modifier.fillMaxSize(),
                    )
            }
        }
    }
}

@Composable
private fun ReadyState(
    state: TandoorImportState.Ready,
    onImport: () -> Unit,
    onResolveWithWeight: (Int, Double, Boolean) -> Unit,
    onResolveWithExistingFood: (Int, FoodId, Measurement) -> Unit,
    onResolveWithEmptyProduct: (Int, Measurement) -> Unit,
    onUnresolve: (Int) -> Unit,
    searchFood: (String) -> Flow<List<FoodSearchResult>>,
    modifier: Modifier = Modifier,
) {
    var searchIngredientIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            SummaryCard(
                name = state.draft.name,
                servings = state.draft.servings,
            )
        }

        state.importError?.let { importError ->
            item {
                MessageState(
                    message = stringResource(importError.toResource()),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        itemsIndexed(state.resolutions) { index, resolution ->
            IngredientResolutionItem(
                index = index,
                resolution = resolution,
                onResolveWithWeight = onResolveWithWeight,
                onLinkFood = { searchIngredientIndex = index },
                onResolveWithEmptyProduct = onResolveWithEmptyProduct,
                onUnresolve = onUnresolve,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Text(
                text =
                    if (state.canImport) {
                        stringResource(Res.string.description_tandoor_import_all_resolved)
                    } else {
                        stringResource(Res.string.description_tandoor_import_blocked)
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Button(
                onClick = onImport,
                enabled = state.canImport,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(Res.string.action_import))
                }
            }
        }
    }

    searchIngredientIndex?.let { ingredientIndex ->
        FoodSearchDialog(
            onDismissRequest = { searchIngredientIndex = null },
            onSearch = searchFood,
            onSelected = { foodId ->
                onResolveWithExistingFood(
                    ingredientIndex,
                    foodId,
                    Measurement.Gram(100.0),
                )
                searchIngredientIndex = null
            },
        )
    }
}

@Composable
private fun SummaryCard(
    name: String,
    servings: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "${stringResource(Res.string.recipe_servings)}: $servings",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun IngredientResolutionItem(
    index: Int,
    resolution: TandoorIngredientResolution,
    onResolveWithWeight: (Int, Double, Boolean) -> Unit,
    onLinkFood: () -> Unit,
    onResolveWithEmptyProduct: (Int, Measurement) -> Unit,
    onUnresolve: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (resolution) {
        is TandoorIngredientResolution.Unresolved ->
            UnresolvedIngredientResolutionItem(
                index = index,
                resolution = resolution,
                onResolveWithWeight = onResolveWithWeight,
                onLinkFood = onLinkFood,
                onResolveWithEmptyProduct = onResolveWithEmptyProduct,
                modifier = modifier,
            )

        else ->
            ResolvedIngredientResolutionItem(
                index = index,
                resolution = resolution,
                onUnresolve = onUnresolve,
                modifier = modifier,
            )
    }
}

@Composable
private fun UnresolvedIngredientResolutionItem(
    index: Int,
    resolution: TandoorIngredientResolution.Unresolved,
    onResolveWithWeight: (Int, Double, Boolean) -> Unit,
    onLinkFood: () -> Unit,
    onResolveWithEmptyProduct: (Int, Measurement) -> Unit,
    modifier: Modifier = Modifier,
) {
    val amountState = rememberTextFieldState()
    var isMilliliter by rememberSaveable(index) { mutableStateOf(false) }
    val amount = amountState.text.toString().toDoubleOrNull()
    val canConfirm = amount != null && amount > 0.0

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = resolution.ingredient.foodName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(resolution.reason.toResource()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            OutlinedTextField(
                state = amountState,
                label = { Text(stringResource(Res.string.action_tandoor_assign_weight)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !isMilliliter,
                    onClick = { isMilliliter = false },
                    label = { Text(stringResource(Res.string.label_gram)) },
                )
                FilterChip(
                    selected = isMilliliter,
                    onClick = { isMilliliter = true },
                    label = { Text(stringResource(Res.string.label_milliliter)) },
                )
            }

            Button(
                onClick = { onResolveWithWeight(index, amount ?: return@Button, isMilliliter) },
                enabled = canConfirm,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.action_confirm))
            }

            OutlinedButton(
                onClick = onLinkFood,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.action_tandoor_link_food))
            }

            TextButton(
                onClick = { onResolveWithEmptyProduct(index, Measurement.Gram(1.0)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.action_tandoor_create_empty_product))
            }
        }
    }
}

@Composable
private fun ResolvedIngredientResolutionItem(
    index: Int,
    resolution: TandoorIngredientResolution,
    onUnresolve: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = resolution.tint(),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = resolution.ingredient.foodName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = resolution.summary(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { onUnresolve(index) }) {
                    Text(
                        stringResource(
                            if (resolution is TandoorIngredientResolution.AutoLinkedToFood) {
                                Res.string.action_tandoor_override
                            } else {
                                Res.string.action_reset
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun FoodSearchDialog(
    onDismissRequest: () -> Unit,
    onSearch: (String) -> Flow<List<FoodSearchResult>>,
    onSelected: (FoodId.Product) -> Unit,
    modifier: Modifier = Modifier,
) {
    val queryState = rememberTextFieldState()
    val query = queryState.text.toString()
    val results by remember(query, onSearch) { onSearch(query) }.collectAsStateWithLifecycle(emptyList())

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
        title = { Text(stringResource(Res.string.action_tandoor_link_food)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    state = queryState,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.action_search)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                        )
                    },
                )

                if (results.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.neutral_no_food_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        itemsIndexed(results) { _, result ->
                            TextButton(
                                onClick = { onSelected(result.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = result.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Text(
                                        text = stringResource(Res.string.headline_product),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onRetry) {
                Text(stringResource(Res.string.action_retry))
            }
        }
    }
}

@Composable
private fun MessageState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Text(
            text = message,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun TandoorConnectionError.toResource() =
    when (this) {
        is TandoorConnectionError.AuthError -> Res.string.error_tandoor_auth
        is TandoorConnectionError.ReachabilityError -> Res.string.error_tandoor_reachability
    }

private fun ImportTandoorRecipeError.toResource() =
    when (this) {
        is ImportTandoorRecipeError.NeedsResolution -> Res.string.description_tandoor_import_blocked
        is ImportTandoorRecipeError.CreateRecipeFailed,
        is ImportTandoorRecipeError.Unexpected -> Res.string.description_tandoor_import_unexpected_error
    }

private val TandoorIngredientResolution.ingredient
    get() =
        when (this) {
            is TandoorIngredientResolution.CanAutoResolve -> ingredient
            is TandoorIngredientResolution.ManuallyWeighed -> ingredient
            is TandoorIngredientResolution.LinkedToFood -> ingredient
            is TandoorIngredientResolution.AutoLinkedToFood -> ingredient
            is TandoorIngredientResolution.EmptyProduct -> ingredient
            is TandoorIngredientResolution.Unresolved -> ingredient
        }

@Composable
private fun TandoorIngredientResolution.summary(): String =
    when (this) {
        is TandoorIngredientResolution.CanAutoResolve ->
            stringResource(Res.string.label_tandoor_resolved_auto) +
                " • " +
                measurement.measurementStringResource()

        is TandoorIngredientResolution.ManuallyWeighed ->
            stringResource(Res.string.label_tandoor_resolved_weight) +
                " • " +
                measurement.measurementStringResource()

        is TandoorIngredientResolution.LinkedToFood ->
            stringResource(Res.string.label_tandoor_resolved_linked) +
                " • " +
                measurement.measurementStringResource()

        is TandoorIngredientResolution.AutoLinkedToFood ->
            stringResource(Res.string.label_tandoor_resolved_auto_linked, foodName) +
                " • " +
                measurement.measurementStringResource()

        is TandoorIngredientResolution.EmptyProduct ->
            stringResource(Res.string.label_tandoor_resolved_empty) +
                " • " +
                measurement.measurementStringResource()

        is TandoorIngredientResolution.Unresolved ->
            stringResource(Res.string.description_tandoor_import_needs_resolution)
    }

private fun TandoorIngredientResolution.UnresolvedReason.toResource() =
    when (this) {
        TandoorIngredientResolution.UnresolvedReason.NO_AMOUNT -> Res.string.label_tandoor_no_amount
        TandoorIngredientResolution.UnresolvedReason.NO_MEASUREMENT -> Res.string.label_tandoor_no_conversion
    }

@Composable
private fun TandoorIngredientResolution.tint() =
    if (this is TandoorIngredientResolution.AutoLinkedToFood) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }
