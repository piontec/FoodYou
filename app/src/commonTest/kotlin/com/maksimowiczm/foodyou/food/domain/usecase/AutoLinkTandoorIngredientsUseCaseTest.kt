package com.maksimowiczm.foodyou.food.domain.usecase

import com.maksimowiczm.foodyou.common.domain.food.FoodSource
import com.maksimowiczm.foodyou.common.domain.food.NutritionFacts
import com.maksimowiczm.foodyou.common.domain.measurement.Measurement
import com.maksimowiczm.foodyou.food.domain.entity.FoodId
import com.maksimowiczm.foodyou.food.domain.entity.Product
import com.maksimowiczm.foodyou.food.domain.entity.TandoorIngredientDraft
import com.maksimowiczm.foodyou.food.domain.entity.TandoorIngredientProperty
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorFoodMatchRepository
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorIngredientResolution
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AutoLinkTandoorIngredientsUseCaseTest {

    @Test
    fun confidentMatchAutoLinksIngredient() = kotlinx.coroutines.runBlocking {
        val useCase =
            AutoLinkTandoorIngredientsUseCase(
                matchRepository =
                    FakeTandoorFoodMatchRepository(
                        matches =
                            mapOf(
                                "garlic" to product(id = FoodId.Product(7L), name = "Garlic"),
                            ),
                    ),
            )
        val unresolved =
            TandoorIngredientResolution.Unresolved(
                ingredient = unresolvedIngredient(foodName = "Garlic"),
                reason = TandoorIngredientResolution.UnresolvedReason.NO_MEASUREMENT,
            )

        val result = useCase.autoLink(listOf(unresolved))

        val linked = assertIs<TandoorIngredientResolution.AutoLinkedToFood>(result.single())
        assertEquals(FoodId.Product(7L), linked.foodId)
        assertEquals(Measurement.Gram(100.0), linked.measurement)
        assertEquals("Garlic", linked.foodName)
    }

    @Test
    fun nonConfidentMatchLeavesIngredientUnresolved() = kotlinx.coroutines.runBlocking {
        val useCase =
            AutoLinkTandoorIngredientsUseCase(
                matchRepository =
                    FakeTandoorFoodMatchRepository(
                        matches =
                            mapOf(
                                "garlic powder" to product(id = FoodId.Product(7L), name = "Garlic powder"),
                            ),
                    ),
            )
        val unresolved =
            TandoorIngredientResolution.Unresolved(
                ingredient = unresolvedIngredient(foodName = "Garlic"),
                reason = TandoorIngredientResolution.UnresolvedReason.NO_MEASUREMENT,
            )

        val result = useCase.autoLink(listOf(unresolved))

        assertEquals(listOf(unresolved), result)
    }

    @Test
    fun noAmountIngredientIsNotAutoLinked() = kotlinx.coroutines.runBlocking {
        val useCase =
            AutoLinkTandoorIngredientsUseCase(
                matchRepository =
                    FakeTandoorFoodMatchRepository(
                        matches =
                            mapOf(
                                "garlic" to product(id = FoodId.Product(7L), name = "Garlic"),
                            ),
                    ),
            )
        val unresolved =
            TandoorIngredientResolution.Unresolved(
                ingredient = unresolvedIngredient(foodName = "Garlic", noAmount = true),
                reason = TandoorIngredientResolution.UnresolvedReason.NO_AMOUNT,
            )

        val result = useCase.autoLink(listOf(unresolved))

        assertEquals(listOf(unresolved), result)
    }

    @Test
    fun alreadyResolvedIngredientsWithPropertiesAreUnchanged() = kotlinx.coroutines.runBlocking {
        val useCase =
            AutoLinkTandoorIngredientsUseCase(
                matchRepository =
                    FakeTandoorFoodMatchRepository(
                        matches =
                            mapOf(
                                "garlic" to product(id = FoodId.Product(7L), name = "Garlic"),
                            ),
                    ),
            )
        val resolved =
            TandoorIngredientResolution.CanAutoResolve(
                ingredient = resolvedIngredient(foodName = "Garlic", withProperties = true),
                measurement = Measurement.Gram(12.0),
            )

        val result = useCase.autoLink(listOf(resolved))

        assertEquals(listOf(resolved), result)
    }

    @Test
    fun resolvedIngredientsWithoutPropertiesAreAutoLinked() = kotlinx.coroutines.runBlocking {
        val useCase =
            AutoLinkTandoorIngredientsUseCase(
                matchRepository =
                    FakeTandoorFoodMatchRepository(
                        matches =
                            mapOf(
                                "garlic" to product(id = FoodId.Product(7L), name = "Garlic"),
                            ),
                    ),
            )
        val resolved =
            TandoorIngredientResolution.CanAutoResolve(
                ingredient = resolvedIngredient(foodName = "Garlic", withProperties = false),
                measurement = Measurement.Gram(12.0),
            )

        val result = useCase.autoLink(listOf(resolved))

        val linked = assertIs<TandoorIngredientResolution.AutoLinkedToFood>(result.single())
        assertEquals(FoodId.Product(7L), linked.foodId)
        assertEquals(Measurement.Gram(12.0), linked.measurement)
        assertEquals("Garlic", linked.foodName)
    }

    @Test
    fun caseInsensitiveMatchIsConfident() = kotlinx.coroutines.runBlocking {
        val useCase =
            AutoLinkTandoorIngredientsUseCase(
                matchRepository =
                    FakeTandoorFoodMatchRepository(
                        matches =
                            mapOf(
                                "garlic" to product(id = FoodId.Product(7L), name = "Garlic"),
                            ),
                    ),
            )
        val unresolved =
            TandoorIngredientResolution.Unresolved(
                ingredient = unresolvedIngredient(foodName = "  garlic  "),
                reason = TandoorIngredientResolution.UnresolvedReason.NO_MEASUREMENT,
            )

        val result = useCase.autoLink(listOf(unresolved))

        val linked = assertIs<TandoorIngredientResolution.AutoLinkedToFood>(result.single())
        assertEquals(FoodId.Product(7L), linked.foodId)
        assertEquals("Garlic", linked.foodName)
    }

    private fun unresolvedIngredient(
        foodName: String,
        noAmount: Boolean = false,
    ) = TandoorIngredientDraft(
        tandoorFoodId = 1,
        foodName = foodName,
        amount = 2.0,
        unitName = "clove",
        unitBaseUnit = null,
        properties = emptyList(),
        propertiesFoodAmount = 100.0,
        propertiesFoodBaseUnit = "g",
        conversions = emptyList(),
        noAmount = noAmount,
        note = null,
    )

    private fun resolvedIngredient(
        foodName: String,
        withProperties: Boolean = false,
    ) = TandoorIngredientDraft(
        tandoorFoodId = 2,
        foodName = foodName,
        amount = 12.0,
        unitName = "gram",
        unitBaseUnit = "g",
        properties =
            if (withProperties) {
                listOf(TandoorIngredientProperty(openDataSlug = "energy", amount = 149.0))
            } else {
                emptyList()
            },
        propertiesFoodAmount = 100.0,
        propertiesFoodBaseUnit = "g",
        conversions = emptyList(),
        noAmount = false,
        note = null,
    )

    private fun product(
        id: FoodId.Product,
        name: String,
    ) = Product(
        id = id,
        name = name,
        brand = null,
        barcode = null,
        note = null,
        isLiquid = false,
        packageWeight = null,
        servingWeight = null,
        source = FoodSource(FoodSource.Type.User),
        nutritionFacts = NutritionFacts.Empty,
    )
}

private class FakeTandoorFoodMatchRepository(
    private val matches: Map<String, Product> = emptyMap(),
) : TandoorFoodMatchRepository {
    override suspend fun findConfidentMatch(ingredientName: String): Product? =
        matches[ingredientName.trim().lowercase()]
}
