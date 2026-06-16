package com.maksimowiczm.foodyou.app.infrastructure.room

import org.koin.core.scope.Scope

internal actual fun Scope.database(): FoodYouDatabase =
    error("Room database is not available on iOS")
