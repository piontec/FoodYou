package com.maksimowiczm.foodyou.food.infrastructure.tandoor

import com.maksimowiczm.foodyou.food.domain.repository.TandoorCredentialsRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.onClose

internal fun Module.tandoorModule() {
    single(named(TandoorRemoteDataSource::class.qualifiedName!!)) {
            HttpClient {
                install(HttpTimeout) {
                    requestTimeoutMillis = 15_000
                    connectTimeoutMillis = 15_000
                    socketTimeoutMillis = 15_000
                }
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            // Treat explicit JSON null as the default value for non-nullable
                            // fields that have a default (e.g. emptyList()). Without this,
                            // a server-side null on a list field throws SerializationException
                            // which is incorrectly mapped to ReachabilityError.
                            coerceInputValues = true
                        },
                    )
                }
            }
        }
        .onClose { it?.close() }

    factoryOf(::TandoorCredentialsRepositoryImpl).bind<TandoorCredentialsRepository>()
    factoryOf(::ProductRepositoryTandoorFoodMatcher).bind<TandoorFoodMatchRepository>()
}
