package com.maksimowiczm.foodyou.food.infrastructure.tandoor.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TandoorRecipeListItemDto(
    val id: Int,
    val name: String,
    @SerialName("image") val imageUrl: String? = null,
)
