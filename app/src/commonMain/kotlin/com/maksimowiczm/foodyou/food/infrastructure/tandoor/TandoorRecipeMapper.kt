package com.maksimowiczm.foodyou.food.infrastructure.tandoor

import com.maksimowiczm.foodyou.food.domain.entity.TandoorIngredientConversion
import com.maksimowiczm.foodyou.food.domain.entity.TandoorIngredientDraft
import com.maksimowiczm.foodyou.food.domain.entity.TandoorIngredientProperty
import com.maksimowiczm.foodyou.food.domain.entity.TandoorRecipeDraft
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.model.TandoorRecipeDetailDto

internal object TandoorRecipeMapper {

    fun map(dto: TandoorRecipeDetailDto): TandoorRecipeDraft =
        TandoorRecipeDraft(
            id = dto.id,
            name = dto.name,
            servings = maxOf(1, dto.servings),
            note = buildNote(dto),
            ingredients =
                dto.steps.flatMap { step ->
                    step.ingredients.map { ingredient ->
                        TandoorIngredientDraft(
                            tandoorFoodId = ingredient.food.id,
                            foodName = ingredient.food.name,
                            amount = ingredient.amount,
                            unitName = ingredient.unit?.name.orEmpty(),
                            unitBaseUnit = ingredient.unit?.baseUnit,
                            properties =
                                ingredient.food.properties.mapNotNull { property ->
                                    val type = property.propertyType ?: return@mapNotNull null
                                    TandoorIngredientProperty(
                                        openDataSlug = type.openDataSlug,
                                        amount = property.propertyAmount,
                                    )
                                },
                            propertiesFoodAmount = ingredient.food.propertiesFoodAmount,
                            propertiesFoodBaseUnit = ingredient.food.propertiesFoodUnit?.baseUnit,
                            conversions =
                                ingredient.conversions.map { conversion ->
                                    TandoorIngredientConversion(
                                        amount = conversion.amount,
                                        baseUnit = conversion.unit.baseUnit,
                                    )
                                },
                            noAmount = ingredient.noAmount,
                            note = ingredient.note,
                        )
                    }
                },
        )

    private fun buildNote(dto: TandoorRecipeDetailDto): String? {
        val instructions =
            dto.steps
                .map { it.instruction.trim() }
                .filter { it.isNotBlank() }
                .joinToString(separator = "\n\n")
        val sourceNote = dto.sourceUrl?.trim()?.takeIf { it.isNotBlank() }?.let { "Source: $it" }

        return listOfNotNull(
            instructions.takeIf { it.isNotBlank() },
            sourceNote,
        ).takeIf { it.isNotEmpty() }?.joinToString(separator = "\n\n")
    }
}
