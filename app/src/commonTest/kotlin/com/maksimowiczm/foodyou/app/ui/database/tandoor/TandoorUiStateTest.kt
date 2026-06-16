package com.maksimowiczm.foodyou.app.ui.database.tandoor

import androidx.paging.LoadState
import com.maksimowiczm.foodyou.food.domain.entity.TandoorIngredientDraft
import com.maksimowiczm.foodyou.food.domain.entity.TandoorRecipeDraft
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorConnectionError
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorIngredientResolution
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TandoorUiStateTest {

    @Test
    fun connectionErrorsMapToSpecificRemoteErrorKinds() {
        assertEquals(
            TandoorRemoteError.Auth,
            TandoorConnectionError.AuthError().toRemoteError(),
        )
        assertEquals(
            TandoorRemoteError.Reachability,
            TandoorConnectionError.ReachabilityError("offline").toRemoteError(),
        )
    }

    @Test
    fun browseStateShowsFullScreenLoadingWhileInitialPageLoads() {
        val state =
            resolveTandoorBrowseListState(
                query = "",
                itemCount = 0,
                refreshState = LoadState.Loading,
                appendState = notLoading(),
            )

        assertTrue(state is TandoorBrowseListState.FullscreenLoading)
    }

    @Test
    fun browseStateDistinguishesNoRecipesFromNoSearchResults() {
        val noRecipes =
            resolveTandoorBrowseListState(
                query = "",
                itemCount = 0,
                refreshState = notLoading(),
                appendState = notLoading(),
            )
        val noSearchResults =
            resolveTandoorBrowseListState(
                query = "pasta",
                itemCount = 0,
                refreshState = notLoading(),
                appendState = notLoading(),
            )

        assertEquals(
            TandoorBrowseEmptyState.NoRecipes,
            assertIs<TandoorBrowseListState.Empty>(noRecipes).emptyState,
        )
        assertEquals(
            TandoorBrowseEmptyState.NoSearchResults,
            assertIs<TandoorBrowseListState.Empty>(noSearchResults).emptyState,
        )
    }

    @Test
    fun browseStateKeepsListVisibleWhileRefreshSpinnerAndAppendErrorAreShown() {
        val state =
            resolveTandoorBrowseListState(
                query = "",
                itemCount = 3,
                refreshState = LoadState.Loading,
                appendState = LoadState.Error(TandoorConnectionError.ReachabilityError("timeout")),
            )

        val content = assertIs<TandoorBrowseListState.Content>(state)
        assertTrue(content.showRefreshLoading)
        assertTrue(content.appendError == TandoorBrowseErrorMessage.Reachability)
    }

    @Test
    fun browseStateKeepsListVisibleWhileRefreshErrorIsShownInline() {
        val state =
            resolveTandoorBrowseListState(
                query = "pasta",
                itemCount = 2,
                refreshState = LoadState.Error(TandoorConnectionError.AuthError()),
                appendState = notLoading(),
            )

        val content = assertIs<TandoorBrowseListState.Content>(state)
        assertTrue(content.refreshError == TandoorBrowseErrorMessage.Auth)
    }

    @Test
    fun importReadyStateUsesDedicatedNoIngredientsMessage() {
        val state =
            TandoorImportState.Ready(
                draft =
                    TandoorRecipeDraft(
                        id = 1,
                        name = "Tea",
                        servings = 1,
                        note = null,
                        ingredients = emptyList(),
                    ),
                resolutions = emptyList(),
            )

        assertTrue(state.canImport)
        assertEquals(TandoorImportStatusMessage.NoIngredients, state.statusMessage())
    }

    @Test
    fun importReadyStateUsesBlockedMessageWhenResolutionIsStillNeeded() {
        val unresolved = unresolvedResolution("Water")
        val state =
            TandoorImportState.Ready(
                draft =
                    TandoorRecipeDraft(
                        id = 2,
                        name = "Soup",
                        servings = 2,
                        note = null,
                        ingredients = listOf(unresolved.ingredient),
                    ),
                resolutions = listOf(unresolved),
            )

        assertFalse(state.canImport)
        assertEquals(TandoorImportStatusMessage.Blocked, state.statusMessage())
    }

    private fun notLoading() = LoadState.NotLoading(endOfPaginationReached = false)

    private fun unresolvedResolution(name: String) =
        TandoorIngredientResolution.Unresolved(
            ingredient =
                TandoorIngredientDraft(
                    tandoorFoodId = 10,
                    foodName = name,
                    amount = 1.0,
                    unitName = "cup",
                    unitBaseUnit = null,
                    properties = emptyList(),
                    propertiesFoodAmount = 100.0,
                    propertiesFoodBaseUnit = "g",
                    conversions = emptyList(),
                    noAmount = false,
                    note = null,
                ),
            reason = TandoorIngredientResolution.UnresolvedReason.NO_MEASUREMENT,
        )
}
