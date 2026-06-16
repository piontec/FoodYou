package com.maksimowiczm.foodyou.app.infrastructure

import com.maksimowiczm.foodyou.common.log.Logger

actual object FoodYouLogger : Logger {
    actual override fun d(tag: String, throwable: Throwable?, message: () -> String) =
        println("D/$tag: ${message()}${throwable?.let { " | $it" } ?: ""}")

    actual override fun w(tag: String, throwable: Throwable?, message: () -> String) =
        println("W/$tag: ${message()}${throwable?.let { " | $it" } ?: ""}")

    actual override fun e(tag: String, throwable: Throwable?, message: () -> String) =
        println("E/$tag: ${message()}${throwable?.let { " | $it" } ?: ""}")

    actual override fun i(tag: String, throwable: Throwable?, message: () -> String) =
        println("I/$tag: ${message()}${throwable?.let { " | $it" } ?: ""}")
}
