package com.maksimowiczm.foodyou.food.infrastructure.tandoor

import com.maksimowiczm.foodyou.common.domain.measurement.Measurement
import com.maksimowiczm.foodyou.food.domain.entity.TandoorIngredientConversion
import com.maksimowiczm.foodyou.food.domain.entity.TandoorIngredientDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TandoorMeasurementDerivationTest {

    @Test
    fun derivesMeasurementFromDirectMetricUnit() {
        val cases =
            listOf(
                ingredient(amount = 300.0, unitName = "g") to Measurement.Gram(300.0),
                ingredient(amount = 125.0, unitName = "ml") to Measurement.Milliliter(125.0),
            )

        cases.forEach { (ingredient, expected) ->
            assertEquals(expected, ingredient.deriveMeasurement())
        }
    }

    @Test
    fun derivesMeasurementFromMetricConversionWhenDirectUnitIsNotMetric() {
        val ingredient =
            ingredient(
                amount = 2.0,
                unitName = "clove",
                conversions = listOf(TandoorIngredientConversion(amount = 12.0, baseUnit = "g")),
            )

        assertEquals(Measurement.Gram(12.0), ingredient.deriveMeasurement())
    }

    @Test
    fun autoResolveReturnsNoAmountWhenIngredientHasNoAmount() {
        val resolution = autoResolve(ingredient(amount = 0.0, noAmount = true))

        val unresolved = assertIs<TandoorIngredientResolution.Unresolved>(resolution)
        assertEquals(TandoorIngredientResolution.UnresolvedReason.NO_AMOUNT, unresolved.reason)
    }

    @Test
    fun autoResolveReturnsNoMeasurementWhenIngredientHasNoMetricUnitOrConversion() {
        val resolution = autoResolve(ingredient(amount = 2.0, unitName = "clove"))

        val unresolved = assertIs<TandoorIngredientResolution.Unresolved>(resolution)
        assertEquals(TandoorIngredientResolution.UnresolvedReason.NO_MEASUREMENT, unresolved.reason)
    }

    private fun ingredient(
        amount: Double,
        unitName: String = "",
        conversions: List<TandoorIngredientConversion> = emptyList(),
        noAmount: Boolean = false,
    ) = TandoorIngredientDraft(
        tandoorFoodId = 1,
        foodName = "Ingredient",
        amount = amount,
        unitName = unitName,
        unitBaseUnit = unitName.ifBlank { null },
        properties = emptyList(),
        propertiesFoodAmount = 100.0,
        propertiesFoodBaseUnit = null,
        conversions = conversions,
        noAmount = noAmount,
        note = null,
    )
}
