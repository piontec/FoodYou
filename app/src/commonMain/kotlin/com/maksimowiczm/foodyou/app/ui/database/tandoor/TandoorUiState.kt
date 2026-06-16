package com.maksimowiczm.foodyou.app.ui.database.tandoor

import androidx.paging.LoadState
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorConnectionError
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorIngredientResolution
import foodyou.app.generated.resources.*

internal enum class TandoorRemoteError {
    Auth,
    Reachability,
}

internal fun TandoorConnectionError.toRemoteError() =
    when (this) {
        is TandoorConnectionError.AuthError -> TandoorRemoteError.Auth
        is TandoorConnectionError.ReachabilityError -> TandoorRemoteError.Reachability
    }

internal fun TandoorRemoteError.toResource() =
    when (this) {
        TandoorRemoteError.Auth -> Res.string.error_tandoor_auth
        TandoorRemoteError.Reachability -> Res.string.error_tandoor_reachability
    }

internal enum class TandoorBrowseErrorMessage {
    Auth,
    Reachability,
    Generic,
}

internal fun TandoorBrowseErrorMessage.toResource() =
    when (this) {
        TandoorBrowseErrorMessage.Auth -> Res.string.error_tandoor_auth
        TandoorBrowseErrorMessage.Reachability -> Res.string.error_tandoor_reachability
        TandoorBrowseErrorMessage.Generic -> Res.string.error_tandoor_browse
    }

internal enum class TandoorBrowseEmptyState {
    NoRecipes,
    NoSearchResults,
}

internal sealed interface TandoorBrowseListState {
    data object FullscreenLoading : TandoorBrowseListState

    data class FullscreenError(
        val error: TandoorBrowseErrorMessage,
    ) : TandoorBrowseListState

    data class Empty(
        val emptyState: TandoorBrowseEmptyState,
    ) : TandoorBrowseListState

    data class Content(
        val showRefreshLoading: Boolean,
        val refreshError: TandoorBrowseErrorMessage?,
        val showAppendLoading: Boolean,
        val appendError: TandoorBrowseErrorMessage?,
    ) : TandoorBrowseListState
}

internal fun resolveTandoorBrowseListState(
    query: String,
    itemCount: Int,
    refreshState: LoadState,
    appendState: LoadState,
): TandoorBrowseListState {
    if (itemCount == 0) {
        return when (refreshState) {
            is LoadState.Loading -> TandoorBrowseListState.FullscreenLoading
            is LoadState.Error ->
                TandoorBrowseListState.FullscreenError(refreshState.error.toBrowseErrorMessage())

            is LoadState.NotLoading ->
                TandoorBrowseListState.Empty(
                    if (query.isBlank()) {
                        TandoorBrowseEmptyState.NoRecipes
                    } else {
                        TandoorBrowseEmptyState.NoSearchResults
                    },
                )
        }
    }

    return TandoorBrowseListState.Content(
        showRefreshLoading = refreshState is LoadState.Loading,
        refreshError = (refreshState as? LoadState.Error)?.error?.toBrowseErrorMessage(),
        showAppendLoading = appendState is LoadState.Loading,
        appendError = (appendState as? LoadState.Error)?.error?.toBrowseErrorMessage(),
    )
}

internal enum class TandoorImportStatusMessage {
    NoIngredients,
    Ready,
    Blocked,
}

internal fun TandoorImportStatusMessage.toResource() =
    when (this) {
        TandoorImportStatusMessage.NoIngredients -> Res.string.description_tandoor_import_no_ingredients
        TandoorImportStatusMessage.Ready -> Res.string.description_tandoor_import_all_resolved
        TandoorImportStatusMessage.Blocked -> Res.string.description_tandoor_import_blocked
    }

internal fun TandoorImportState.Ready.statusMessage() =
    when {
        draft.ingredients.isEmpty() -> TandoorImportStatusMessage.NoIngredients
        resolutions.none { it is TandoorIngredientResolution.Unresolved } ->
            TandoorImportStatusMessage.Ready
        else -> TandoorImportStatusMessage.Blocked
    }

private fun Throwable.toBrowseErrorMessage() =
    when (this) {
        is TandoorConnectionError.AuthError -> TandoorBrowseErrorMessage.Auth
        is TandoorConnectionError.ReachabilityError -> TandoorBrowseErrorMessage.Reachability
        else -> TandoorBrowseErrorMessage.Generic
    }
