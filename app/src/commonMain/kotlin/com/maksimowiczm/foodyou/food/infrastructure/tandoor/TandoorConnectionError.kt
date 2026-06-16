package com.maksimowiczm.foodyou.food.infrastructure.tandoor

sealed class TandoorConnectionError(message: String?) : Exception(message) {
    class AuthError : TandoorConnectionError("Tandoor API token is invalid (HTTP 403).")
    class ReachabilityError(message: String?) :
        TandoorConnectionError("Cannot reach Tandoor server: $message")
}
