package com.maksimowiczm.foodyou.food.domain.usecase

import com.maksimowiczm.foodyou.common.domain.database.TransactionProvider
import com.maksimowiczm.foodyou.common.domain.date.DateProvider
import com.maksimowiczm.foodyou.common.domain.food.FoodSource
import com.maksimowiczm.foodyou.common.domain.food.NutrientValue
import com.maksimowiczm.foodyou.common.domain.food.NutritionFacts
import com.maksimowiczm.foodyou.common.domain.measurement.Measurement
import com.maksimowiczm.foodyou.common.result.Err
import com.maksimowiczm.foodyou.common.result.Ok
import com.maksimowiczm.foodyou.common.result.Result
import com.maksimowiczm.foodyou.food.domain.entity.FoodHistory
import com.maksimowiczm.foodyou.food.domain.entity.FoodId
import com.maksimowiczm.foodyou.food.domain.entity.TandoorIngredientDraft
import com.maksimowiczm.foodyou.food.domain.entity.TandoorRecipeDraft
import com.maksimowiczm.foodyou.food.domain.repository.ProductRepository
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorIngredientResolution

internal sealed interface ImportTandoorRecipeError {
    data class NeedsResolution(
        val unresolved: List<TandoorIngredientResolution.Unresolved>,
    ) : ImportTandoorRecipeError

    data class CreateRecipeFailed(
        val error: CreateRecipeError,
    ) : ImportTandoorRecipeError

    data class Unexpected(
        val throwable: Throwable,
    ) : ImportTandoorRecipeError
}

internal class ImportTandoorRecipeUseCase(
    private val productRepository: ProductRepository,
    private val createRecipeUseCase: CreateRecipeUseCase,
    private val transactionProvider: TransactionProvider,
    private val dateProvider: DateProvider,
) {
    suspend fun import(
        draft: TandoorRecipeDraft,
        resolutions: List<TandoorIngredientResolution>,
    ): Result<FoodId.Recipe, ImportTandoorRecipeError> {
        val unresolved = resolutions.filterIsInstance<TandoorIngredientResolution.Unresolved>()
        if (unresolved.isNotEmpty()) {
            return Err(ImportTandoorRecipeError.NeedsResolution(unresolved))
        }

        val autoResolved = resolutions.filterIsInstance<TandoorIngredientResolution.CanAutoResolve>()

        return try {
            transactionProvider.withTransaction {
                val ingredients =
                    autoResolved.map { resolution ->
                        val ingredient = resolution.ingredient
                        val productId =
                            productRepository.insertProduct(
                                name = ingredient.foodName,
                                brand = null,
                                barcode = null,
                                note = null,
                                isLiquid = resolution.measurement is Measurement.Milliliter,
                                packageWeight = null,
                                servingWeight = null,
                                source = FoodSource(FoodSource.Type.Tandoor),
                                nutritionFacts = ingredient.toNutritionFacts(),
                            )
                        productId to resolution.measurement
                    }

                when (
                    val recipeResult =
                        createRecipeUseCase.create(
                            name = draft.name,
                            servings = draft.servings,
                            note = draft.note,
                            isLiquid = autoResolved.isNotEmpty() && autoResolved.all { it.measurement is Measurement.Milliliter },
                            ingredients = ingredients,
                            history = FoodHistory.Imported(dateProvider.nowInstant()),
                        )
                ) {
                    is Result.Success -> Ok(recipeResult.data)
                    is Result.Error -> {
                        val error = Err<FoodId.Recipe, ImportTandoorRecipeError>(
                            ImportTandoorRecipeError.CreateRecipeFailed(recipeResult.error),
                        )
                        rollback(error)
                        error
                    }
                }
            }
        } catch (throwable: Throwable) {
            Err(ImportTandoorRecipeError.Unexpected(throwable))
        }
    }
}

private fun TandoorIngredientDraft.toNutritionFacts(): NutritionFacts {
    val scale =
        if (propertiesFoodAmount > 0.0) {
            100.0 / propertiesFoodAmount
        } else {
            1.0
        }

    fun nutrient(slug: String): NutrientValue =
        properties.firstOrNull { it.openDataSlug == slug }?.amount?.let { amount ->
            NutrientValue.Complete(amount * scale)
        } ?: NutrientValue.Incomplete(null)

    return NutritionFacts(
        energy = nutrient("property-calories"),
        proteins = nutrient("property-proteins"),
        fats = nutrient("property-fats"),
        carbohydrates = nutrient("property-carbohydrates"),
    )
}
