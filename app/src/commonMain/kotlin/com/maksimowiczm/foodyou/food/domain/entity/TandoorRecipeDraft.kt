package com.maksimowiczm.foodyou.food.domain.entity

data class TandoorRecipeDraft(
    val id: Int,
    val name: String,
    val servings: Int,
    val note: String?,
    val ingredients: List<TandoorIngredientDraft>,
    /** Epoch seconds of the Tandoor recipe's last modification time. */
    val updatedAt: Long? = null,
)

data class TandoorIngredientDraft(
    val tandoorFoodId: Int,
    val foodName: String,
    val amount: Double,
    val unitName: String,
    val unitBaseUnit: String?,
    val properties: List<TandoorIngredientProperty>,
    val propertiesFoodAmount: Double,
    val propertiesFoodBaseUnit: String?,
    val conversions: List<TandoorIngredientConversion>,
    val noAmount: Boolean,
    val note: String?,
)

data class TandoorIngredientProperty(
    val openDataSlug: String,
    val amount: Double,
)

data class TandoorIngredientConversion(
    val amount: Double,
    val baseUnit: String?,
)
