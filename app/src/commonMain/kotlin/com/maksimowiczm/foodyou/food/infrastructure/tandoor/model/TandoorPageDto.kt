package com.maksimowiczm.foodyou.food.infrastructure.tandoor.model

import kotlinx.serialization.Serializable

@Serializable
internal data class TandoorPageDto<T>(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<T>,
)
