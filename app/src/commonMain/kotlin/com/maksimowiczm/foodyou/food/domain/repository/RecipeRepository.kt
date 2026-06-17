package com.maksimowiczm.foodyou.food.domain.repository

import com.maksimowiczm.foodyou.food.domain.entity.FoodId
import com.maksimowiczm.foodyou.food.domain.entity.Recipe
import com.maksimowiczm.foodyou.food.domain.entity.RecipeIngredient
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    fun observeRecipe(recipeId: FoodId.Recipe): Flow<Recipe?>

    suspend fun insertRecipe(
        name: String,
        servings: Int,
        note: String?,
        isLiquid: Boolean,
        ingredients: List<RecipeIngredient>,
    ): FoodId.Recipe

    suspend fun updateRecipe(recipe: Recipe)

    suspend fun deleteRecipe(recipe: Recipe)

    /**
     * Records the Tandoor source for a locally-stored recipe. Call this after importing a recipe
     * from Tandoor so the browse screen can show the import status and detect server-side updates.
     *
     * @param recipeId Local recipe ID.
     * @param tandoorId The Tandoor recipe ID.
     * @param tandoorUpdatedAt Epoch seconds of the Tandoor recipe's `updated_at` at import time.
     */
    suspend fun setTandoorInfo(recipeId: FoodId.Recipe, tandoorId: Int, tandoorUpdatedAt: Long?)

    /**
     * Emits a map of Tandoor recipe ID → epoch seconds of `updated_at` stored at import time
     * (null if Tandoor didn't provide it). Only recipes with a recorded [tandoorId] are included.
     */
    fun observeImportedTandoorRecipes(): Flow<Map<Int, Long?>>
}
