package com.maksimowiczm.foodyou.food.infrastructure.tandoor

import com.maksimowiczm.foodyou.food.domain.entity.Product
import com.maksimowiczm.foodyou.food.domain.repository.ProductRepository
import kotlinx.coroutines.flow.first

internal interface TandoorFoodMatchRepository {
    suspend fun findConfidentMatch(ingredientName: String): Product?
}

internal class ProductRepositoryTandoorFoodMatcher(
    private val productRepository: ProductRepository,
) : TandoorFoodMatchRepository {
    override suspend fun findConfidentMatch(ingredientName: String): Product? {
        val normalizedIngredientName = ingredientName.normalizeName()
        return productRepository
            .observeProducts(limit = 200, offset = 0)
            .first()
            .firstOrNull { product -> product.name.normalizeName() == normalizedIngredientName }
    }
}

private fun String.normalizeName(): String = trim().lowercase()
