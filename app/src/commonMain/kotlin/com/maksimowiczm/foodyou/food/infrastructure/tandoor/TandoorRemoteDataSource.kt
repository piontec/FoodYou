package com.maksimowiczm.foodyou.food.infrastructure.tandoor

import com.maksimowiczm.foodyou.food.infrastructure.tandoor.model.TandoorPageDto
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.model.TandoorRecipeDetailDto
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.model.TandoorRecipeListItemDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.CancellationException
import kotlinx.serialization.SerializationException

internal class TandoorRemoteDataSource(
    private val client: HttpClient,
    private val serverUrl: String,
    private val apiToken: String,
) {
    suspend fun listRecipes(
        query: String,
        page: Int,
        pageSize: Int,
    ): Result<TandoorPageDto<TandoorRecipeListItemDto>> =
        try {
            val response = client.get("$serverUrl/api/recipe/") {
                header(HttpHeaders.Authorization, "Bearer $apiToken")
                url {
                    parameters.append("query", query)
                    parameters.append("page", page.toString())
                    parameters.append("page_size", pageSize.toString())
                }
            }

            when (response.status) {
                HttpStatusCode.OK -> Result.success(response.body<TandoorPageDto<TandoorRecipeListItemDto>>())
                HttpStatusCode.Forbidden -> Result.failure(TandoorConnectionError.AuthError())
                else -> Result.failure(
                    TandoorConnectionError.ReachabilityError(
                        "Unexpected status: ${response.status}"
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(TandoorConnectionError.ReachabilityError(e.message))
        }

    suspend fun getRecipe(id: Int): Result<TandoorRecipeDetailDto> =
        try {
            val response = client.get("$serverUrl/api/recipe/$id/") {
                header(HttpHeaders.Authorization, "Bearer $apiToken")
            }

            when (response.status) {
                HttpStatusCode.OK -> Result.success(response.body<TandoorRecipeDetailDto>())
                HttpStatusCode.Forbidden -> Result.failure(TandoorConnectionError.AuthError())
                else -> Result.failure(
                    TandoorConnectionError.ReachabilityError(
                        "Unexpected status: ${response.status}"
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SerializationException) {
            Result.failure(
                TandoorConnectionError.ReachabilityError("Recipe response schema mismatch: ${e.message}"),
            )
        } catch (e: Exception) {
            Result.failure(TandoorConnectionError.ReachabilityError(e.message))
        }

    /** Issues GET /api/recipe/?page_size=1 to verify connectivity and credentials. */
    suspend fun testConnection(): Result<Unit> =
        try {
            val response = client.get("$serverUrl/api/recipe/") {
                header(HttpHeaders.Authorization, "Bearer $apiToken")
                url { parameters.append("page_size", "1") }
            }
            when (response.status) {
                HttpStatusCode.OK -> Result.success(Unit)
                HttpStatusCode.Forbidden -> Result.failure(TandoorConnectionError.AuthError())
                else -> Result.failure(
                    TandoorConnectionError.ReachabilityError(
                        "Unexpected status: ${response.status}"
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(TandoorConnectionError.ReachabilityError(e.message))
        }
}
