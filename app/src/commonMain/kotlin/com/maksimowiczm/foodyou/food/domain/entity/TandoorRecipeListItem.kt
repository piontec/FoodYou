package com.maksimowiczm.foodyou.food.domain.entity

data class TandoorRecipeListItem(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    /** Epoch seconds of the Tandoor recipe's last modification time. */
    val updatedAt: Long? = null,
)
