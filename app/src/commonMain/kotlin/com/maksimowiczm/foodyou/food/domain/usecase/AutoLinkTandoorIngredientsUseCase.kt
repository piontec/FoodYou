package com.maksimowiczm.foodyou.food.domain.usecase

import com.maksimowiczm.foodyou.common.domain.measurement.Measurement
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorFoodMatchRepository
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorIngredientResolution

internal class AutoLinkTandoorIngredientsUseCase(
    private val matchRepository: TandoorFoodMatchRepository,
) {
    suspend fun autoLink(
        resolutions: List<TandoorIngredientResolution>,
    ): List<TandoorIngredientResolution> =
        resolutions.map { resolution ->
            when (resolution) {
                is TandoorIngredientResolution.Unresolved ->
                    if (resolution.reason == TandoorIngredientResolution.UnresolvedReason.NO_MEASUREMENT) {
                        matchRepository.findConfidentMatch(resolution.ingredient.foodName)?.let { product ->
                            TandoorIngredientResolution.AutoLinkedToFood(
                                ingredient = resolution.ingredient,
                                foodId = product.id,
                                measurement = Measurement.Gram(100.0),
                                foodName = product.name,
                            )
                        } ?: resolution
                    } else {
                        resolution
                    }

                else -> resolution
            }
        }
}
