package com.maksimowiczm.foodyou.food.infrastructure.tandoor

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import java.io.IOException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TandoorGetRecipeTest {

    private fun buildMockClient(
        status: HttpStatusCode = HttpStatusCode.OK,
        responseBody: String =
            """
            {
                "id": 1,
                "name": "Marry me Orzo",
                "description": "Creamy orzo dish",
                "servings": 4,
                "servings_text": "portions",
                "source_url": "https://example.com/recipe",
                "steps": []
            }
            """.trimIndent(),
        onRequest: ((String, Map<String, List<String>>) -> Unit)? = null,
    ): HttpClient {
        val engine = MockEngine { request ->
            onRequest?.invoke(request.url.toString(), request.headers.entries().associate { (k, v) -> k to v })
            respond(
                content = responseBody,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }

    @Test
    fun getRecipeSendsBearerToken() = runBlocking {
        var capturedAuthHeader: String? = null
        val client = buildMockClient(
            onRequest = { _, headers ->
                capturedAuthHeader = headers["Authorization"]?.firstOrNull()
            },
        )

        val dataSource = TandoorRemoteDataSource(client, "https://tandoor.example.com", "tda_test123")
        dataSource.getRecipe(7)

        assertEquals("Bearer tda_test123", capturedAuthHeader)
    }

    @Test
    fun getRecipeUsesCorrectDetailUrl() = runBlocking {
        var capturedUrl: String? = null
        val client = buildMockClient(
            onRequest = { url, _ -> capturedUrl = url },
        )

        val dataSource = TandoorRemoteDataSource(client, "https://tandoor.example.com", "tda_test123")
        dataSource.getRecipe(7)

        assertEquals("https://tandoor.example.com/api/recipe/7/", capturedUrl)
    }

    @Test
    fun getRecipeParsesRecipeDetailResponse() = runBlocking {
        val client = buildMockClient(
            responseBody =
                """
                {
                    "id": 1,
                    "name": "Marry me Orzo",
                    "description": "Creamy orzo dish",
                    "servings": 4,
                    "servings_text": "portions",
                    "source_url": "https://example.com/recipe",
                    "steps": [
                        {
                            "id": 10,
                            "instruction": "Cook the orzo in salted water...",
                            "ingredients": [
                                {
                                    "id": 100,
                                    "amount": 300.0,
                                    "unit": {"id": 1, "name": "gram", "base_unit": "g"},
                                    "food": {
                                        "id": 50,
                                        "name": "Orzo pasta",
                                        "properties": [
                                            {
                                                "property": {
                                                    "id": 1,
                                                    "name": "Calories",
                                                    "unit": "kcal",
                                                    "open_data_slug": "property-calories"
                                                },
                                                "property_amount": 350.0
                                            }
                                        ],
                                        "properties_food_amount": 100.0,
                                        "properties_food_unit": {
                                            "id": 1,
                                            "name": "gram",
                                            "base_unit": "g"
                                        }
                                    },
                                    "conversions": [],
                                    "note": "",
                                    "no_amount": false
                                }
                            ]
                        }
                    ]
                }
                """.trimIndent(),
        )

        val dataSource = TandoorRemoteDataSource(client, "https://tandoor.example.com", "tda_test123")
        val result = dataSource.getRecipe(1)

        assertTrue(result.isSuccess)
        val recipe = assertNotNull(result.getOrNull())
        assertEquals(1, recipe.id)
        assertEquals("Marry me Orzo", recipe.name)
        assertEquals("Creamy orzo dish", recipe.description)
        assertEquals(4, recipe.servings)
        assertEquals("portions", recipe.servingsText)
        assertEquals("https://example.com/recipe", recipe.sourceUrl)
        assertEquals(1, recipe.steps.size)
        assertEquals("Cook the orzo in salted water...", recipe.steps.single().instruction)
        val ingredient = recipe.steps.single().ingredients.single()
        assertEquals(100, ingredient.id)
        assertEquals(300.0, ingredient.amount)
        assertEquals("g", ingredient.unit?.baseUnit)
        assertEquals("Orzo pasta", ingredient.food.name)
        assertEquals("property-calories", ingredient.food.properties.single().propertyType?.openDataSlug)
        assertEquals(350.0, ingredient.food.properties.single().propertyAmount)
    }

    @Test
    fun getRecipeReturnsAuthErrorOn403() = runBlocking {
        val client = buildMockClient(status = HttpStatusCode.Forbidden)

        val dataSource = TandoorRemoteDataSource(client, "https://tandoor.example.com", "bad_token")
        val result = dataSource.getRecipe(1)

        assertTrue(result.isFailure)
        val error = assertIs<TandoorConnectionError.AuthError>(result.exceptionOrNull())
        assertEquals("Tandoor API token is invalid (HTTP 403).", error.message)
    }

    @Test
    fun getRecipeReturnsReachabilityErrorOnNetworkException() = runBlocking {
        val engine = MockEngine { throw IOException("timeout") }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val dataSource = TandoorRemoteDataSource(client, "https://tandoor.example.com", "tda_test123")
        val result = dataSource.getRecipe(1)

        assertTrue(result.isFailure)
        val error = assertIs<TandoorConnectionError.ReachabilityError>(result.exceptionOrNull())
        assertEquals("Cannot reach Tandoor server: timeout", error.message)
    }
}
