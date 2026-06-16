package com.maksimowiczm.foodyou.food.infrastructure.tandoor.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TandoorRecipeDetailDto(
    val id: Int,
    val name: String,
    val description: String? = null,
    val servings: Int = 1,
    @SerialName("servings_text") val servingsText: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    val steps: List<TandoorStepDto> = emptyList(),
)

@Serializable
internal data class TandoorStepDto(
    val id: Int,
    val instruction: String = "",
    val ingredients: List<TandoorIngredientDto> = emptyList(),
)

@Serializable
internal data class TandoorIngredientDto(
    val id: Int,
    val amount: Double = 0.0,
    val unit: TandoorUnitDto? = null,
    val food: TandoorFoodDto,
    val note: String? = null,
    @SerialName("no_amount") val noAmount: Boolean = false,
    val conversions: List<TandoorConversionDto> = emptyList(),
)

@Serializable
internal data class TandoorUnitDto(
    val id: Int = 0,
    val name: String = "",
    @SerialName("base_unit") val baseUnit: String? = null,
)

@Serializable
internal data class TandoorFoodDto(
    val id: Int,
    val name: String,
    val properties: List<TandoorFoodPropertyDto> = emptyList(),
    @SerialName("properties_food_amount") val propertiesFoodAmount: Double = 100.0,
    @SerialName("properties_food_unit") val propertiesFoodUnit: TandoorUnitDto? = null,
)

@Serializable
internal data class TandoorFoodPropertyDto(
    @SerialName("property") val propertyType: TandoorPropertyTypeDto,
    @SerialName("property_amount") val propertyAmount: Double,
)

@Serializable
internal data class TandoorPropertyTypeDto(
    val id: Int = 0,
    val name: String = "",
    val unit: String = "",
    @SerialName("open_data_slug") val openDataSlug: String = "",
)

@Serializable
internal data class TandoorConversionDto(
    val amount: Double = 0.0,
    val unit: TandoorUnitDto = TandoorUnitDto(),
)
