package com.maksimowiczm.foodyou.food.infrastructure.tandoor

import com.maksimowiczm.foodyou.common.domain.measurement.Measurement
import com.maksimowiczm.foodyou.food.domain.entity.TandoorIngredientDraft

internal sealed interface TandoorIngredientResolution {
    data class CanAutoResolve(
        val ingredient: TandoorIngredientDraft,
        val measurement: Measurement,
    ) : TandoorIngredientResolution

    data class Unresolved(
        val ingredient: TandoorIngredientDraft,
        val reason: UnresolvedReason,
    ) : TandoorIngredientResolution

    enum class UnresolvedReason {
        NO_MEASUREMENT,
        NO_AMOUNT,
    }
}

internal fun TandoorIngredientDraft.deriveMeasurement(): Measurement? =
    unitBaseUnit.toMeasurement(amount)
        ?: conversions.firstNotNullOfOrNull { conversion ->
            conversion.baseUnit.toMeasurement(conversion.amount)
        }

internal fun autoResolve(ingredient: TandoorIngredientDraft): TandoorIngredientResolution =
    when {
        ingredient.noAmount ->
            TandoorIngredientResolution.Unresolved(
                ingredient = ingredient,
                reason = TandoorIngredientResolution.UnresolvedReason.NO_AMOUNT,
            )

        else ->
            ingredient.deriveMeasurement()?.let { measurement ->
                TandoorIngredientResolution.CanAutoResolve(
                    ingredient = ingredient,
                    measurement = measurement,
                )
            }
                ?: TandoorIngredientResolution.Unresolved(
                    ingredient = ingredient,
                    reason = TandoorIngredientResolution.UnresolvedReason.NO_MEASUREMENT,
                )
    }

private fun String?.toMeasurement(amount: Double): Measurement? =
    when (this?.lowercase()) {
        "g" -> Measurement.Gram(amount)
        "ml" -> Measurement.Milliliter(amount)
        else -> null
    }
