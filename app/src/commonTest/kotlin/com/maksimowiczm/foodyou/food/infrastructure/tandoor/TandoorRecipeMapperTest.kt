package com.maksimowiczm.foodyou.food.infrastructure.tandoor

import com.maksimowiczm.foodyou.food.infrastructure.tandoor.model.TandoorConversionDto
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.model.TandoorFoodDto
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.model.TandoorFoodPropertyDto
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.model.TandoorIngredientDto
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.model.TandoorPropertyTypeDto
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.model.TandoorRecipeDetailDto
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.model.TandoorStepDto
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.model.TandoorUnitDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TandoorRecipeMapperTest {

    @Test
    fun mapsRecipeMetadataAndComposesNote() {
        val dto =
            TandoorRecipeDetailDto(
                id = 1,
                name = "Marry me Orzo",
                description = "Creamy orzo dish",
                servings = 0,
                sourceUrl = "https://example.com/recipe",
                steps =
                    listOf(
                        TandoorStepDto(
                            id = 10,
                            instruction = "Cook the orzo in salted water...",
                        ),
                        TandoorStepDto(
                            id = 11,
                            instruction = "Stir in the cream and parmesan.",
                        ),
                    ),
            )

        val draft = TandoorRecipeMapper.map(dto)

        assertEquals(1, draft.id)
        assertEquals("Marry me Orzo", draft.name)
        assertEquals(1, draft.servings)
        assertEquals(
            "Cook the orzo in salted water...\n\nStir in the cream and parmesan.\n\nSource: https://example.com/recipe",
            draft.note,
        )
    }

    @Test
    fun mapsIngredientsPropertiesAndConversions() {
        val dto =
            TandoorRecipeDetailDto(
                id = 1,
                name = "Marry me Orzo",
                servings = 4,
                steps =
                    listOf(
                        TandoorStepDto(
                            id = 10,
                            ingredients =
                                listOf(
                                    TandoorIngredientDto(
                                        id = 100,
                                        amount = 300.0,
                                        unit = TandoorUnitDto(id = 1, name = "gram", baseUnit = "g"),
                                        food =
                                            TandoorFoodDto(
                                                id = 50,
                                                name = "Orzo pasta",
                                                properties =
                                                    listOf(
                                                        TandoorFoodPropertyDto(
                                                            propertyType =
                                                                TandoorPropertyTypeDto(
                                                                    openDataSlug = "property-calories",
                                                                ),
                                                            propertyAmount = 350.0,
                                                        ),
                                                        TandoorFoodPropertyDto(
                                                            propertyType =
                                                                TandoorPropertyTypeDto(
                                                                    openDataSlug = "property-proteins",
                                                                ),
                                                            propertyAmount = 12.0,
                                                        ),
                                                    ),
                                            ),
                                        note = "",
                                        conversions =
                                            listOf(
                                                TandoorConversionDto(
                                                    amount = 300.0,
                                                    unit = TandoorUnitDto(baseUnit = "g"),
                                                )
                                            ),
                                    )
                                ),
                        )
                    ),
            )

        val draft = TandoorRecipeMapper.map(dto)

        assertEquals(1, draft.ingredients.size)
        val ingredient = draft.ingredients.single()
        assertEquals(50, ingredient.tandoorFoodId)
        assertEquals("Orzo pasta", ingredient.foodName)
        assertEquals(300.0, ingredient.amount)
        assertEquals("gram", ingredient.unitName)
        assertEquals(2, ingredient.properties.size)
        assertEquals("property-calories", ingredient.properties[0].openDataSlug)
        assertEquals(350.0, ingredient.properties[0].amount)
        assertEquals(1, ingredient.conversions.size)
        assertEquals(300.0, ingredient.conversions[0].amount)
        assertEquals("g", ingredient.conversions[0].baseUnit)
        assertEquals("", ingredient.note)
    }

    @Test
    fun handlesNullAndEmptyFieldsGracefully() {
        val dto =
            TandoorRecipeDetailDto(
                id = 9,
                name = "Garlic",
                servings = -4,
                sourceUrl = "   ",
                steps =
                    listOf(
                        TandoorStepDto(
                            id = 1,
                            instruction = "",
                            ingredients =
                                listOf(
                                    TandoorIngredientDto(
                                        id = 1,
                                        food = TandoorFoodDto(id = 51, name = "Garlic"),
                                        note = null,
                                    )
                                ),
                        )
                    ),
            )

        val draft = TandoorRecipeMapper.map(dto)

        assertEquals(1, draft.servings)
        assertNull(draft.note)
        val ingredient = draft.ingredients.single()
        assertEquals("", ingredient.unitName)
        assertEquals(emptyList(), ingredient.properties)
        assertEquals(emptyList(), ingredient.conversions)
        assertNull(ingredient.note)
    }
}
