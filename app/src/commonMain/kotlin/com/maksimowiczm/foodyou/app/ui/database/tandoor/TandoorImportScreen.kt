package com.maksimowiczm.foodyou.app.ui.database.tandoor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maksimowiczm.foodyou.app.ui.common.component.ArrowBackIconButton
import com.maksimowiczm.foodyou.app.ui.common.utility.stringResource as measurementStringResource
import com.maksimowiczm.foodyou.common.compose.extension.LaunchedCollectWithLifecycle
import com.maksimowiczm.foodyou.food.domain.entity.FoodId
import com.maksimowiczm.foodyou.food.domain.usecase.ImportTandoorRecipeError
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorConnectionError
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorIngredientResolution
import foodyou.app.generated.resources.*
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
        modifier = modifier,
    )
}

@Composable
internal fun TandoorImportScreen(
    state: TandoorImportState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onImport: () -> Unit,
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
    modifier: Modifier = Modifier,
) {
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

        itemsIndexed(state.resolutions) { _, resolution ->
            IngredientResolutionItem(
                resolution = resolution,
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
    resolution: TandoorIngredientResolution,
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
            val ingredient =
                when (resolution) {
                    is TandoorIngredientResolution.CanAutoResolve -> resolution.ingredient
                    is TandoorIngredientResolution.Unresolved -> resolution.ingredient
                }
            Text(
                text = ingredient.foodName,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text =
                    when (resolution) {
                        is TandoorIngredientResolution.CanAutoResolve ->
                            stringResource(
                                Res.string.description_tandoor_import_auto_resolved,
                            ) + " • " + resolution.measurement.measurementStringResource()

                        is TandoorIngredientResolution.Unresolved ->
                            stringResource(Res.string.description_tandoor_import_needs_resolution) +
                                " • " +
                                stringResource(resolution.reason.toResource())
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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

private fun TandoorIngredientResolution.UnresolvedReason.toResource() =
    when (this) {
        TandoorIngredientResolution.UnresolvedReason.NO_AMOUNT ->
            Res.string.description_tandoor_import_missing_amount
        TandoorIngredientResolution.UnresolvedReason.NO_MEASUREMENT ->
            Res.string.description_tandoor_import_missing_measurement
    }
