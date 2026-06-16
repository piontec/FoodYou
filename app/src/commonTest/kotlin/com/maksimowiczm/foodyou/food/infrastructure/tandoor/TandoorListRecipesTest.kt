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

class TandoorListRecipesTest {

    private fun buildMockClient(
        status: HttpStatusCode = HttpStatusCode.OK,
        responseBody: String = """{"count":0,"next":null,"previous":null,"results":[]}""",
        onRequest: ((String, Map<String, List<String>>) -> Unit)? = null,
    ): Pair<HttpClient, List<io.ktor.client.request.HttpRequestData>> {
        val requests = mutableListOf<io.ktor.client.request.HttpRequestData>()
        val engine = MockEngine { request ->
            requests.add(request)
            onRequest?.invoke(request.url.toString(), request.headers.entries().associate { (k, v) -> k to v })
            respond(
                content = responseBody,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return client to requests
    }

    @Test
    fun listRecipesSendsBearerToken() = runBlocking {
        var capturedAuthHeader: String? = null
        val (client, _) = buildMockClient(
            onRequest = { _, headers ->
                capturedAuthHeader = headers["Authorization"]?.firstOrNull()
            },
        )

        val dataSource = TandoorRemoteDataSource(client, "https://tandoor.example.com", "tda_test123")
        dataSource.listRecipes(query = "", page = 1, pageSize = 20)

        assertEquals("Bearer tda_test123", capturedAuthHeader)
    }

    @Test
    fun listRecipesSendsQueryPageAndPageSizeParams() = runBlocking {
        var capturedUrl: String? = null
        val (client, _) = buildMockClient(
            onRequest = { url, _ -> capturedUrl = url },
        )

        val dataSource = TandoorRemoteDataSource(client, "https://tandoor.example.com", "tda_test123")
        dataSource.listRecipes(query = "soup", page = 3, pageSize = 25)

        assertEquals(
            "https://tandoor.example.com/api/recipe/?query=soup&page=3&page_size=25",
            capturedUrl,
        )
    }

    @Test
    fun listRecipesParsesDrfPageResponse() = runBlocking {
        val (client, _) = buildMockClient(
            responseBody = """
                {
                    "count": 42,
                    "next": "https://tandoor.example.com/api/recipe/?page=2",
                    "previous": null,
                    "results": [
                        {
                            "id": 1,
                            "name": "Tomato Soup",
                            "image": "https://tandoor.example.com/media/tomato-soup.jpg"
                        },
                        {
                            "id": 2,
                            "name": "Pancakes",
                            "image": null
                        }
                    ]
                }
            """.trimIndent(),
        )

        val dataSource = TandoorRemoteDataSource(client, "https://tandoor.example.com", "tda_test123")
        val result = dataSource.listRecipes(query = "", page = 1, pageSize = 20)

        assertTrue(result.isSuccess)
        val page = assertNotNull(result.getOrNull())
        assertEquals(42, page.count)
        assertEquals("https://tandoor.example.com/api/recipe/?page=2", page.next)
        assertEquals(null, page.previous)
        assertEquals(2, page.results.size)
        assertEquals(1, page.results[0].id)
        assertEquals("Tomato Soup", page.results[0].name)
        assertEquals("https://tandoor.example.com/media/tomato-soup.jpg", page.results[0].imageUrl)
        assertEquals(2, page.results[1].id)
        assertEquals("Pancakes", page.results[1].name)
        assertEquals(null, page.results[1].imageUrl)
    }

    @Test
    fun listRecipesReturnsAuthErrorOn403() = runBlocking {
        val (client, _) = buildMockClient(status = HttpStatusCode.Forbidden)

        val dataSource = TandoorRemoteDataSource(client, "https://tandoor.example.com", "bad_token")
        val result = dataSource.listRecipes(query = "", page = 1, pageSize = 20)

        assertTrue(result.isFailure)
        val error = assertIs<TandoorConnectionError.AuthError>(result.exceptionOrNull())
        assertEquals("Tandoor API token is invalid (HTTP 403).", error.message)
        Unit
    }

    @Test
    fun listRecipesReturnsReachabilityErrorOnNetworkException() = runBlocking {
        val engine = MockEngine { throw IOException("timeout") }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val dataSource = TandoorRemoteDataSource(client, "https://tandoor.example.com", "tda_test123")
        val result = dataSource.listRecipes(query = "", page = 1, pageSize = 20)

        assertTrue(result.isFailure)
        val error = assertIs<TandoorConnectionError.ReachabilityError>(result.exceptionOrNull())
        assertEquals("Cannot reach Tandoor server: timeout", error.message)
        Unit
    }
}
