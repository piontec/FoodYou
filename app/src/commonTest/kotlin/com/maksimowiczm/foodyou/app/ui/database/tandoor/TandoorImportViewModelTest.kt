package com.maksimowiczm.foodyou.app.ui.database.tandoor

import com.maksimowiczm.foodyou.common.domain.food.FoodSource
import com.maksimowiczm.foodyou.common.domain.food.NutritionFacts
import com.maksimowiczm.foodyou.common.domain.measurement.Measurement
import com.maksimowiczm.foodyou.food.domain.entity.FoodId
import com.maksimowiczm.foodyou.food.domain.entity.Product
import com.maksimowiczm.foodyou.food.domain.entity.TandoorIngredientDraft
import com.maksimowiczm.foodyou.food.domain.entity.TandoorRecipeDraft
import com.maksimowiczm.foodyou.food.domain.repository.ProductRepository
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorIngredientResolution
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.autoResolve
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TandoorImportViewModelTest {

    @Test
    fun resolveWithWeightAndUnresolveRestoreAutoResolution() {
        val state = readyState()

        val resolved =
            state.resolveWithWeight(
                ingredientIndex = 0,
                measurement = Measurement.Gram(45.0),
            )

        val manual = assertIs<TandoorIngredientResolution.ManuallyWeighed>(resolved.resolutions.single())
        assertEquals(Measurement.Gram(45.0), manual.measurement)
        assertEquals(true, resolved.canImport)

        val unresolved = resolved.unresolve(0)
        val restored = assertIs<TandoorIngredientResolution.Unresolved>(unresolved.resolutions.single())
        assertEquals(TandoorIngredientResolution.UnresolvedReason.NO_MEASUREMENT, restored.reason)
        assertEquals(false, unresolved.canImport)
    }

    @Test
    fun resolveWithExistingFoodAndEmptyProductUpdateResolutionState() {
        val state = readyState()

        val linked =
            state.resolveWithExistingFood(
                ingredientIndex = 0,
                foodId = FoodId.Product(7L),
                measurement = Measurement.Gram(100.0),
            )
        val linkedResolution = assertIs<TandoorIngredientResolution.LinkedToFood>(linked.resolutions.single())
        assertEquals(FoodId.Product(7L), linkedResolution.foodId)
        assertEquals(true, linked.canImport)

        val empty =
            state.resolveWithEmptyProduct(
                ingredientIndex = 0,
                measurement = Measurement.Gram(1.0),
            )
        val emptyResolution = assertIs<TandoorIngredientResolution.EmptyProduct>(empty.resolutions.single())
        assertEquals(Measurement.Gram(1.0), emptyResolution.measurement)
        assertEquals(true, empty.canImport)
    }

    @Test
    fun observeFoodSearchResultsFiltersProductsByQuery() = runBlocking {
        val repository =
            FakeProductRepository(
                products =
                    listOf(
                        product(id = FoodId.Product(1L), name = "Greek yogurt"),
                        product(id = FoodId.Product(2L), name = "Tomato soup"),
                    ),
            )

        val results = repository.observeFoodSearchResults("yog").first()

        assertEquals(listOf(FoodSearchResult(FoodId.Product(1L), "Greek yogurt")), results)
    }

    private fun readyState(): TandoorImportState.Ready {
        val ingredient =
            TandoorIngredientDraft(
                tandoorFoodId = 50,
                foodName = "Garlic",
                amount = 2.0,
                unitName = "clove",
                unitBaseUnit = null,
                properties = emptyList(),
                propertiesFoodAmount = 100.0,
                propertiesFoodBaseUnit = "g",
                conversions = emptyList(),
                noAmount = false,
                note = null,
            )
        val draft =
            TandoorRecipeDraft(
                id = 1,
                name = "Garlic bread",
                servings = 2,
                note = null,
                ingredients = listOf(ingredient),
            )
        return TandoorImportState.Ready(
            draft = draft,
            resolutions = listOf(autoResolve(ingredient)),
        )
    }

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

private class FakeProductRepository(
    private val products: List<Product>,
) : ProductRepository {
    override fun observeProduct(id: FoodId.Product): Flow<Product?> =
        flowOf(products.firstOrNull { it.id == id })

    override fun observeProducts(limit: Int, offset: Int): Flow<List<Product>> =
        flowOf(products.drop(offset).take(limit))

    override suspend fun insertProduct(
        name: String,
        brand: String?,
        barcode: String?,
        note: String?,
        isLiquid: Boolean,
        packageWeight: Double?,
        servingWeight: Double?,
        source: FoodSource,
        nutritionFacts: NutritionFacts,
    ): FoodId.Product = error("Not used in test")

    override suspend fun insertUniqueProduct(
        name: String,
        brand: String?,
        barcode: String?,
        note: String?,
        isLiquid: Boolean,
        packageWeight: Double?,
        servingWeight: Double?,
        source: FoodSource,
        nutritionFacts: NutritionFacts,
    ): FoodId.Product? = error("Not used in test")

    override suspend fun updateProduct(product: Product) = error("Not used in test")

    override suspend fun deleteProduct(product: Product) = error("Not used in test")
}
