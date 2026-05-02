package com.timothymarias.cookingapp.routes

import com.timothymarias.cookingapp.domain.api.CreateIngredientRequest
import com.timothymarias.cookingapp.domain.api.IngredientResponse
import com.timothymarias.cookingapp.domain.api.UpdateIngredientRequest
import com.timothymarias.cookingapp.repository.IngredientRepository
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.assertEquals
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

class IngredientRoutesTest {

    private val mockRepo = mockk<IngredientRepository>()
    private val testIngredient = IngredientResponse(localId = "ing-123", name = "Salt")

    private fun Application.testModule() {
        install(Koin) { modules(module { single { mockRepo } }) }
        install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(Resources)
        install(StatusPages) {
            exception<IllegalArgumentException> { call, cause ->
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (cause.message ?: "Bad request")))
            }
        }
        routing { ingredientRoutes() }
    }

    private fun ApplicationTestBuilder.jsonClient() = createClient {
        install(ClientContentNegotiation) { json() }
    }

    @Test
    fun `GET ingredients returns list`() = testApplication {
        application { testModule() }
        coEvery { mockRepo.findAll() } returns listOf(testIngredient)

        val response = jsonClient().get("/api/v1/ingredients")
        assertEquals(HttpStatusCode.OK, response.status)
        val ingredients = response.body<List<IngredientResponse>>()
        assertEquals(1, ingredients.size)
        assertEquals("Salt", ingredients[0].name)
    }

    @Test
    fun `GET ingredient by localId returns ingredient`() = testApplication {
        application { testModule() }
        coEvery { mockRepo.findByLocalId("ing-123") } returns testIngredient

        val response = jsonClient().get("/api/v1/ingredients/ing-123")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Salt", response.body<IngredientResponse>().name)
    }

    @Test
    fun `POST ingredient creates and returns 201`() = testApplication {
        application { testModule() }
        coEvery { mockRepo.create(any()) } returns testIngredient

        val response = jsonClient().post("/api/v1/ingredients") {
            contentType(ContentType.Application.Json)
            setBody(CreateIngredientRequest(name = "Salt"))
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `GET search returns matching ingredients`() = testApplication {
        application { testModule() }
        coEvery { mockRepo.search("sal") } returns listOf(testIngredient)

        val response = jsonClient().get("/api/v1/ingredients/search?q=sal")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT ingredient updates and returns ingredient`() = testApplication {
        application { testModule() }
        val updated = testIngredient.copy(name = "Sea Salt")
        coEvery { mockRepo.update("ing-123", any()) } returns updated

        val response = jsonClient().put("/api/v1/ingredients/ing-123") {
            contentType(ContentType.Application.Json)
            setBody(UpdateIngredientRequest(name = "Sea Salt"))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Sea Salt", response.body<IngredientResponse>().name)
    }

    @Test
    fun `DELETE ingredient returns 204`() = testApplication {
        application { testModule() }
        coEvery { mockRepo.delete("ing-123") } returns true

        val response = jsonClient().delete("/api/v1/ingredients/ing-123")
        assertEquals(HttpStatusCode.NoContent, response.status)
    }
}
