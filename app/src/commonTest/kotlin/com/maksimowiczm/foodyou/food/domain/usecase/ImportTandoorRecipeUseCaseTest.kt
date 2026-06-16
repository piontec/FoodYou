package com.maksimowiczm.foodyou.food.domain.usecase

import com.maksimowiczm.foodyou.common.domain.database.TransactionProvider
import com.maksimowiczm.foodyou.common.domain.database.TransactionScope
import com.maksimowiczm.foodyou.common.domain.date.DateProvider
import com.maksimowiczm.foodyou.common.domain.food.FoodSource
import com.maksimowiczm.foodyou.common.domain.food.NutrientValue
import com.maksimowiczm.foodyou.common.domain.food.NutritionFacts
import com.maksimowiczm.foodyou.common.domain.measurement.Measurement
import com.maksimowiczm.foodyou.common.log.Logger
import com.maksimowiczm.foodyou.common.result.Result
import com.maksimowiczm.foodyou.food.domain.entity.FoodHistory
import com.maksimowiczm.foodyou.food.domain.entity.FoodId
import com.maksimowiczm.foodyou.food.domain.entity.Product
import com.maksimowiczm.foodyou.food.domain.entity.Recipe
import com.maksimowiczm.foodyou.food.domain.entity.RecipeIngredient
import com.maksimowiczm.foodyou.food.domain.entity.TandoorIngredientDraft
import com.maksimowiczm.foodyou.food.domain.entity.TandoorIngredientProperty
import com.maksimowiczm.foodyou.food.domain.entity.TandoorRecipeDraft
import com.maksimowiczm.foodyou.food.domain.repository.FoodHistoryRepository
import com.maksimowiczm.foodyou.food.domain.repository.ProductRepository
import com.maksimowiczm.foodyou.food.domain.repository.RecipeRepository
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorIngredientResolution
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.autoResolve
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class ImportTandoorRecipeUseCaseTest {

    @Test
    fun fullyResolvableDraftCreatesProductsAndRecipeInOneTransaction() = kotlinx.coroutines.runBlocking {
        val productRepository = FakeProductRepository()
        val recipeRepository = FakeRecipeRepository()
        val historyRepository = FakeFoodHistoryRepository()
        val transactionProvider =
            FakeTransactionProvider(
                participants = listOf(productRepository, recipeRepository, historyRepository),
            )
        val createRecipeUseCase =
            CreateRecipeUseCase(
                recipeRepository = recipeRepository,
                productRepository = productRepository,
                historyRepository = historyRepository,
                transactionProvider = transactionProvider,
                logger = NoOpLogger,
            )
        val useCase =
            ImportTandoorRecipeUseCase(
                productRepository = productRepository,
                createRecipeUseCase = createRecipeUseCase,
                transactionProvider = transactionProvider,
                dateProvider = FakeDateProvider(Instant.parse("2026-06-16T12:34:56Z")),
            )
        val draft = fullyResolvableDraft()
        val resolutions = draft.ingredients.map(::autoResolve)

        assertTrue(resolutions.all { it is TandoorIngredientResolution.CanAutoResolve })

        val result = useCase.import(draft, resolutions)

        val success = assertIs<Result.Success<FoodId.Recipe, ImportTandoorRecipeError>>(result)
        assertEquals(FoodId.Recipe(1L), success.data)
        assertEquals(2, productRepository.insertedProducts.size)
        assertEquals(FoodSource.Type.Tandoor, productRepository.insertedProducts[0].source.type)
        assertEquals(
            NutrientValue.Complete(350.0),
            productRepository.insertedProducts[0].nutritionFacts.energy,
        )
        assertEquals(
            NutrientValue.Complete(12.0),
            productRepository.insertedProducts[0].nutritionFacts.proteins,
        )
        assertEquals(
            NutrientValue.Complete(71.0),
            productRepository.insertedProducts[0].nutritionFacts.carbohydrates,
        )
        assertEquals(
            NutrientValue.Complete(2.0),
            productRepository.insertedProducts[0].nutritionFacts.fats,
        )
        assertEquals("Marry me Orzo", recipeRepository.insertedRecipes.single().name)
        assertEquals(4, recipeRepository.insertedRecipes.single().servings)
        assertEquals(
            "Cook the orzo in salted water...\n\nStir in the cream and parmesan.\n\nSource: https://example.com/recipe",
            recipeRepository.insertedRecipes.single().note,
        )
        assertEquals(2, recipeRepository.insertedRecipes.single().ingredients.size)
        assertEquals(Measurement.Gram(300.0), recipeRepository.insertedRecipes.single().ingredients[0].measurement)
        assertEquals(
            Measurement.Milliliter(200.0),
            recipeRepository.insertedRecipes.single().ingredients[1].measurement,
        )
        val importedHistory = assertIs<FoodHistory.Imported>(historyRepository.inserted.single().history)
        assertEquals(Instant.parse("2026-06-16T12:34:56Z"), importedHistory.timestamp)
    }

    @Test
    fun importIsBlockedWhenAnyIngredientIsUnresolved() = kotlinx.coroutines.runBlocking {
        val productRepository = FakeProductRepository()
        val recipeRepository = FakeRecipeRepository()
        val historyRepository = FakeFoodHistoryRepository()
        val transactionProvider =
            FakeTransactionProvider(
                participants = listOf(productRepository, recipeRepository, historyRepository),
            )
        val createRecipeUseCase =
            CreateRecipeUseCase(
                recipeRepository = recipeRepository,
                productRepository = productRepository,
                historyRepository = historyRepository,
                transactionProvider = transactionProvider,
                logger = NoOpLogger,
            )
        val useCase =
            ImportTandoorRecipeUseCase(
                productRepository = productRepository,
                createRecipeUseCase = createRecipeUseCase,
                transactionProvider = transactionProvider,
                dateProvider = FakeDateProvider(Instant.parse("2026-06-16T12:34:56Z")),
            )
        val draft =
            fullyResolvableDraft().copy(
                ingredients =
                    fullyResolvableDraft().ingredients +
                        TandoorIngredientDraft(
                            tandoorFoodId = 99,
                            foodName = "Garlic",
                            amount = 2.0,
                            unitName = "clove",
                            unitBaseUnit = null,
                            properties = emptyList(),
                            propertiesFoodAmount = 100.0,
                            propertiesFoodBaseUnit = "g",
                            conversions = emptyList(),
                            noAmount = false,
                            note = "minced",
                        ),
            )
        val resolutions = draft.ingredients.map(::autoResolve)

        val result = useCase.import(draft, resolutions)

        val error = assertIs<Result.Error<FoodId.Recipe, ImportTandoorRecipeError>>(result)
        val needsResolution =
            assertIs<ImportTandoorRecipeError.NeedsResolution>(error.error)
        assertEquals(1, needsResolution.unresolved.size)
        assertEquals(0, productRepository.insertedProducts.size)
        assertEquals(0, recipeRepository.insertedRecipes.size)
    }

    @Test
    fun importRollsBackWhenRecipeCreationFailsAfterProductsAreInserted() = kotlinx.coroutines.runBlocking {
        val productRepository = FakeProductRepository()
        val recipeRepository = FakeRecipeRepository(shouldFailInsert = true)
        val historyRepository = FakeFoodHistoryRepository()
        val transactionProvider =
            FakeTransactionProvider(
                participants = listOf(productRepository, recipeRepository, historyRepository),
            )
        val createRecipeUseCase =
            CreateRecipeUseCase(
                recipeRepository = recipeRepository,
                productRepository = productRepository,
                historyRepository = historyRepository,
                transactionProvider = transactionProvider,
                logger = NoOpLogger,
            )
        val useCase =
            ImportTandoorRecipeUseCase(
                productRepository = productRepository,
                createRecipeUseCase = createRecipeUseCase,
                transactionProvider = transactionProvider,
                dateProvider = FakeDateProvider(Instant.parse("2026-06-16T12:34:56Z")),
            )

        val result = useCase.import(fullyResolvableDraft(), fullyResolvableDraft().ingredients.map(::autoResolve))

        val error = assertIs<Result.Error<FoodId.Recipe, ImportTandoorRecipeError>>(result)
        val unexpected = assertIs<ImportTandoorRecipeError.Unexpected>(error.error)
        assertEquals("DB error", unexpected.throwable.message)
        assertEquals(0, productRepository.insertedProducts.size)
        assertEquals(0, recipeRepository.insertedRecipes.size)
        assertEquals(0, historyRepository.inserted.size)
    }

    private fun fullyResolvableDraft() =
        TandoorRecipeDraft(
            id = 1,
            name = "Marry me Orzo",
            servings = 4,
            note = "Cook the orzo in salted water...\n\nStir in the cream and parmesan.\n\nSource: https://example.com/recipe",
            ingredients =
                listOf(
                    TandoorIngredientDraft(
                        tandoorFoodId = 50,
                        foodName = "Orzo pasta",
                        amount = 300.0,
                        unitName = "gram",
                        unitBaseUnit = "g",
                        properties =
                            listOf(
                                TandoorIngredientProperty("property-calories", 350.0),
                                TandoorIngredientProperty("property-proteins", 12.0),
                                TandoorIngredientProperty("property-fats", 2.0),
                                TandoorIngredientProperty("property-carbohydrates", 71.0),
                            ),
                        propertiesFoodAmount = 100.0,
                        propertiesFoodBaseUnit = "g",
                        conversions = emptyList(),
                        noAmount = false,
                        note = null,
                    ),
                    TandoorIngredientDraft(
                        tandoorFoodId = 60,
                        foodName = "Vegetable stock",
                        amount = 200.0,
                        unitName = "cup",
                        unitBaseUnit = null,
                        properties =
                            listOf(
                                TandoorIngredientProperty("property-calories", 15.0),
                                TandoorIngredientProperty("property-proteins", 1.0),
                                TandoorIngredientProperty("property-fats", 0.0),
                                TandoorIngredientProperty("property-carbohydrates", 2.0),
                            ),
                        propertiesFoodAmount = 100.0,
                        propertiesFoodBaseUnit = "ml",
                        conversions =
                            listOf(
                                com.maksimowiczm.foodyou.food.domain.entity.TandoorIngredientConversion(
                                    amount = 200.0,
                                    baseUnit = "ml",
                                )
                            ),
                        noAmount = false,
                        note = null,
                    ),
                ),
        )
}

private interface TransactionParticipant {
    fun snapshot(): Any

    fun restore(snapshot: Any)
}

private class FakeTransactionProvider(
    private val participants: List<TransactionParticipant>,
) : TransactionProvider {
    private var depth = 0
    private var snapshots: List<Any>? = null
    private var rollbackResult: Any? = null

    override suspend fun <T> withTransaction(block: suspend TransactionScope<T>.() -> T): T {
        val isOutermost = depth == 0
        if (isOutermost) {
            snapshots = participants.map { it.snapshot() }
            rollbackResult = null
        }
        depth++

        return try {
            val result =
                object : TransactionScope<T> {
                    override suspend fun rollback(result: T) {
                        rollbackResult = result
                    }
                }.block()

            depth--
            if (isOutermost) {
                @Suppress("UNCHECKED_CAST")
                val forcedResult =
                    if (rollbackResult != null) {
                        restoreSnapshots()
                        rollbackResult as T
                    } else {
                        result
                    }
                snapshots = null
                rollbackResult = null
                forcedResult
            } else {
                result
            }
        } catch (throwable: Throwable) {
            depth--
            if (isOutermost) {
                restoreSnapshots()
                snapshots = null
                rollbackResult = null
            }
            throw throwable
        }
    }

    private fun restoreSnapshots() {
        val currentSnapshots = snapshots.orEmpty()
        participants.zip(currentSnapshots).forEach { (participant, snapshot) ->
            participant.restore(snapshot)
        }
    }
}

private class FakeProductRepository : ProductRepository, TransactionParticipant {
    data class InsertedProduct(
        val id: FoodId.Product,
        val name: String,
        val note: String?,
        val isLiquid: Boolean,
        val source: FoodSource,
        val nutritionFacts: NutritionFacts,
    )

    val insertedProducts = mutableListOf<InsertedProduct>()

    override fun observeProduct(id: FoodId.Product): Flow<Product?> =
        flowOf(
            insertedProducts
                .firstOrNull { it.id == id }
                ?.let { product ->
                    Product(
                        id = product.id,
                        name = product.name,
                        brand = null,
                        barcode = null,
                        note = product.note,
                        isLiquid = product.isLiquid,
                        packageWeight = null,
                        servingWeight = null,
                        source = product.source,
                        nutritionFacts = product.nutritionFacts,
                    )
                }
        )

    override fun observeProducts(limit: Int, offset: Int): Flow<List<Product>> = emptyFlow()

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
    ): FoodId.Product {
        val id = FoodId.Product(insertedProducts.size.toLong() + 1)
        insertedProducts +=
            InsertedProduct(
                id = id,
                name = name,
                note = note,
                isLiquid = isLiquid,
                source = source,
                nutritionFacts = nutritionFacts,
            )
        return id
    }

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

    override fun snapshot(): Any = insertedProducts.toList()

    override fun restore(snapshot: Any) {
        insertedProducts.clear()
        @Suppress("UNCHECKED_CAST")
        insertedProducts += snapshot as List<InsertedProduct>
    }
}

private class FakeRecipeRepository(
    private val shouldFailInsert: Boolean = false,
) : RecipeRepository, TransactionParticipant {
    data class InsertedRecipe(
        val id: FoodId.Recipe,
        val name: String,
        val servings: Int,
        val note: String?,
        val isLiquid: Boolean,
        val ingredients: List<RecipeIngredient>,
    )

    val insertedRecipes = mutableListOf<InsertedRecipe>()

    override fun observeRecipe(recipeId: FoodId.Recipe): Flow<Recipe?> =
        flowOf(
            insertedRecipes
                .firstOrNull { it.id == recipeId }
                ?.let { recipe ->
                    Recipe(
                        id = recipe.id,
                        name = recipe.name,
                        servings = recipe.servings,
                        ingredients = recipe.ingredients,
                        note = recipe.note,
                        isLiquid = recipe.isLiquid,
                    )
                }
        )

    override suspend fun insertRecipe(
        name: String,
        servings: Int,
        note: String?,
        isLiquid: Boolean,
        ingredients: List<RecipeIngredient>,
    ): FoodId.Recipe {
        if (shouldFailInsert) {
            throw RuntimeException("DB error")
        }

        val id = FoodId.Recipe(insertedRecipes.size.toLong() + 1)
        insertedRecipes +=
            InsertedRecipe(
                id = id,
                name = name,
                servings = servings,
                note = note,
                isLiquid = isLiquid,
                ingredients = ingredients,
            )
        return id
    }

    override suspend fun updateRecipe(recipe: Recipe) = error("Not used in test")

    override suspend fun deleteRecipe(recipe: Recipe) = error("Not used in test")

    override fun snapshot(): Any = insertedRecipes.toList()

    override fun restore(snapshot: Any) {
        insertedRecipes.clear()
        @Suppress("UNCHECKED_CAST")
        insertedRecipes += snapshot as List<InsertedRecipe>
    }
}

private class FakeFoodHistoryRepository : FoodHistoryRepository, TransactionParticipant {
    data class InsertedHistory(val foodId: FoodId, val history: FoodHistory)

    val inserted = mutableListOf<InsertedHistory>()

    override suspend fun insert(foodId: FoodId, history: FoodHistory) {
        inserted += InsertedHistory(foodId, history)
    }

    override fun observeFoodHistory(foodId: FoodId): Flow<List<FoodHistory>> = emptyFlow()

    override fun snapshot(): Any = inserted.toList()

    override fun restore(snapshot: Any) {
        inserted.clear()
        @Suppress("UNCHECKED_CAST")
        inserted += snapshot as List<InsertedHistory>
    }
}

private class FakeDateProvider(
    private val now: Instant,
) : DateProvider {
    override fun nowInstant(): Instant = now

    override fun observeInstant(interval: kotlin.time.Duration): Flow<Instant> = flowOf(now)

    override fun observeDate(timeZone: TimeZone): Flow<LocalDate> =
        flowOf(now.toLocalDateTime(timeZone).date)
}

private object NoOpLogger : Logger {
    override fun d(tag: String, throwable: Throwable?, message: () -> String) = Unit

    override fun w(tag: String, throwable: Throwable?, message: () -> String) = Unit

    override fun e(tag: String, throwable: Throwable?, message: () -> String) = Unit

    override fun i(tag: String, throwable: Throwable?, message: () -> String) = Unit
}
