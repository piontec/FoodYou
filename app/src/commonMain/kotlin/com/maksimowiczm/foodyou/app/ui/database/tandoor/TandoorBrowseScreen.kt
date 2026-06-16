package com.maksimowiczm.foodyou.app.ui.database.tandoor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.maksimowiczm.foodyou.app.ui.common.component.ArrowBackIconButton
import com.maksimowiczm.foodyou.food.domain.entity.TandoorRecipeListItem
import foodyou.app.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TandoorBrowseScreen(
    onBack: () -> Unit,
    onRecipeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: TandoorBrowseViewModel = koinViewModel()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recipes = viewModel.pagingData.collectAsLazyPagingItems()

    TandoorBrowseScreen(
        query = query,
        state = state,
        recipes = recipes,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onRetryCredentials = viewModel::reload,
        onRetryRecipes = recipes::retry,
        onRecipeSelected = onRecipeSelected,
        modifier = modifier,
    )
}

@Composable
internal fun TandoorBrowseScreen(
    query: String,
    state: TandoorBrowseState,
    recipes: LazyPagingItems<TandoorRecipeListItem>,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onRetryCredentials: () -> Unit,
    onRetryRecipes: () -> Unit,
    onRecipeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(Res.string.headline_tandoor_browse)) },
                navigationIcon = { ArrowBackIconButton(onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(Res.string.action_search)) },
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (state) {
                    TandoorBrowseState.Loading ->
                        LoadingState(modifier = Modifier.fillMaxSize())

                    TandoorBrowseState.MissingCredentials ->
                        ErrorState(
                            message = stringResource(Res.string.error_tandoor_missing_credentials),
                            onRetry = onRetryCredentials,
                            modifier = Modifier.align(Alignment.Center),
                        )

                    TandoorBrowseState.Ready ->
                        RecipeResults(
                            query = query,
                            recipes = recipes,
                            onRetry = onRetryRecipes,
                            onRecipeSelected = onRecipeSelected,
                            modifier = Modifier.fillMaxSize(),
                        )
                }
            }
        }
    }
}

@Composable
private fun RecipeResults(
    query: String,
    recipes: LazyPagingItems<TandoorRecipeListItem>,
    onRetry: () -> Unit,
    onRecipeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val refreshState = recipes.loadState.refresh
    val appendState = recipes.loadState.append
    val listState =
        resolveTandoorBrowseListState(
            query = query,
            itemCount = recipes.itemCount,
            refreshState = refreshState,
            appendState = appendState,
        )

    when (listState) {
        TandoorBrowseListState.FullscreenLoading ->
            LoadingState(modifier = modifier)

        is TandoorBrowseListState.FullscreenError ->
            ErrorState(
                message = stringResource(listState.error.toResource()),
                onRetry = onRetry,
                modifier = modifier,
            )

        is TandoorBrowseListState.Empty ->
            EmptyState(
                query = query,
                emptyState = listState.emptyState,
                modifier = modifier,
            )

        is TandoorBrowseListState.Content ->
            LazyColumn(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                if (listState.showRefreshLoading) {
                    item {
                        LoadingRow(modifier = Modifier.fillMaxWidth())
                    }
                }

                listState.refreshError?.let { error ->
                    item {
                        ErrorState(
                            message = stringResource(error.toResource()),
                            onRetry = onRetry,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                items(
                    count = recipes.itemCount,
                    key = recipes.itemKey { it.id },
                ) { index ->
                    recipes[index]?.let { recipe ->
                        RecipeListItem(
                            recipe = recipe,
                            onClick = { onRecipeSelected(recipe.id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                if (listState.showAppendLoading) {
                    item {
                        LoadingRow(modifier = Modifier.fillMaxWidth())
                    }
                }

                listState.appendError?.let { error ->
                    item {
                        ErrorState(
                            message = stringResource(error.toResource()),
                            onRetry = onRetry,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
    }
}

@Composable
private fun RecipeListItem(
    recipe: TandoorRecipeListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier.size(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
private fun LoadingRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun EmptyState(
    query: String,
    emptyState: TandoorBrowseEmptyState,
    modifier: Modifier = Modifier,
) {
    val normalizedQuery = query.trim()
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text =
                when (emptyState) {
                    TandoorBrowseEmptyState.NoRecipes ->
                        stringResource(Res.string.description_tandoor_no_recipes)
                    TandoorBrowseEmptyState.NoSearchResults ->
                        stringResource(Res.string.description_tandoor_no_search_results, normalizedQuery)
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onRetry) {
                Text(stringResource(Res.string.action_retry))
            }
        }
    }
}
