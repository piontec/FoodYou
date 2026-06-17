package com.maksimowiczm.foodyou.food.infrastructure.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Recipe")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val servings: Int,
    val note: String?,
    val isLiquid: Boolean,
    /** The Tandoor recipe ID, set when this recipe was imported from Tandoor. */
    val tandoorId: Int? = null,
    /** Epoch seconds of the Tandoor recipe's `updated_at` at the time of import. */
    val tandoorUpdatedAt: Long? = null,
)
