package com.maksimowiczm.foodyou.food.infrastructure.tandoor

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TandoorRemoteDataSourceTest {

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
    fun testConnectionSuccessOn200() = runBlocking {
        val (client, _) = buildMockClient(HttpStatusCode.OK)
        val serverUrl = "https://tandoor.example.com"
        val token = "tda_test123"

        val dataSource = TandoorRemoteDataSource(client, serverUrl, token)
        val result = dataSource.testConnection()

        assertTrue(result.isSuccess)
    }

    @Test
    fun testConnectionFailsOnAuthError403() = runBlocking<Unit> {
        val (client, _) = buildMockClient(HttpStatusCode.Forbidden)
        val serverUrl = "https://tandoor.example.com"
        val token = "bad_token"

        val dataSource = TandoorRemoteDataSource(client, serverUrl, token)
        val result = dataSource.testConnection()

        assertTrue(result.isFailure)
        assertIs<TandoorConnectionError.AuthError>(result.exceptionOrNull())
    }

    @Test
    fun testConnectionSendsBearerToken() = runBlocking {
        var capturedAuthHeader: String? = null
        val (client, _) = buildMockClient(
            onRequest = { _, headers ->
                capturedAuthHeader = headers["Authorization"]?.firstOrNull()
            },
        )
        val serverUrl = "https://tandoor.example.com"
        val token = "tda_test123"

        val dataSource = TandoorRemoteDataSource(client, serverUrl, token)
        dataSource.testConnection()

        assertEquals("Bearer tda_test123", capturedAuthHeader)
    }

    @Test
    fun testConnectionUsesCorrectEndpoint() = runBlocking {
        var capturedUrl: String? = null
        val (client, _) = buildMockClient(
            onRequest = { url, _ -> capturedUrl = url },
        )
        val serverUrl = "https://tandoor.example.com"
        val dataSource = TandoorRemoteDataSource(client, serverUrl, "tda_test123")
        dataSource.testConnection()

        assertTrue(capturedUrl?.contains("/api/recipe/") == true)
        assertTrue(capturedUrl?.contains("page_size=1") == true)
    }
}
