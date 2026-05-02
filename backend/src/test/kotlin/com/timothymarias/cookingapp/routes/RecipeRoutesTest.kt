package com.timothymarias.cookingapp.routes

import com.timothymarias.cookingapp.domain.api.CreateRecipeRequest
import com.timothymarias.cookingapp.domain.api.RecipeResponse
import com.timothymarias.cookingapp.domain.api.UpdateRecipeRequest
import com.timothymarias.cookingapp.repository.RecipeRepository
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

class RecipeRoutesTest {

    private val mockRepo = mockk<RecipeRepository>()
    private val testRecipe = RecipeResponse(localId = "abc-123", name = "Test Recipe", ingredients = emptyList())

    private fun Application.testModule() {
        install(Koin) { modules(module { single { mockRepo } }) }
        install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(Resources)
        install(StatusPages) {
            exception<IllegalArgumentException> { call, cause ->
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (cause.message ?: "Bad request")))
            }
        }
        routing { recipeRoutes() }
    }

    private fun ApplicationTestBuilder.jsonClient() = createClient {
        install(ClientContentNegotiation) { json() }
    }

    @Test
    fun `GET recipes returns list`() = testApplication {
        application { testModule() }
        coEvery { mockRepo.findAll() } returns listOf(testRecipe)

        val response = jsonClient().get("/api/v1/recipes")
        assertEquals(HttpStatusCode.OK, response.status)
        val recipes = response.body<List<RecipeResponse>>()
        assertEquals(1, recipes.size)
        assertEquals("Test Recipe", recipes[0].name)
    }

    @Test
    fun `GET recipe by localId returns recipe`() = testApplication {
        application { testModule() }
        coEvery { mockRepo.findByLocalId("abc-123") } returns testRecipe

        val response = jsonClient().get("/api/v1/recipes/abc-123")
        assertEquals(HttpStatusCode.OK, response.status)
        val recipe = response.body<RecipeResponse>()
        assertEquals("Test Recipe", recipe.name)
    }

    @Test
    fun `GET recipe by localId returns 404 when not found`() = testApplication {
        application { testModule() }
        coEvery { mockRepo.findByLocalId("missing") } returns null

        val response = jsonClient().get("/api/v1/recipes/missing")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST recipe creates and returns 201`() = testApplication {
        application { testModule() }
        coEvery { mockRepo.create(any()) } returns testRecipe

        val response = jsonClient().post("/api/v1/recipes") {
            contentType(ContentType.Application.Json)
            setBody(CreateRecipeRequest(name = "Test Recipe"))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("Test Recipe", response.body<RecipeResponse>().name)
    }

    @Test
    fun `POST recipe with blank name returns 400`() = testApplication {
        application { testModule() }

        val response = jsonClient().post("/api/v1/recipes") {
            contentType(ContentType.Application.Json)
            setBody(CreateRecipeRequest(name = ""))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `PUT recipe updates and returns recipe`() = testApplication {
        application { testModule() }
        val updated = testRecipe.copy(name = "Updated")
        coEvery { mockRepo.update("abc-123", any()) } returns updated

        val response = jsonClient().put("/api/v1/recipes/abc-123") {
            contentType(ContentType.Application.Json)
            setBody(UpdateRecipeRequest(name = "Updated"))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Updated", response.body<RecipeResponse>().name)
    }

    @Test
    fun `DELETE recipe returns 204`() = testApplication {
        application { testModule() }
        coEvery { mockRepo.delete("abc-123") } returns true

        val response = jsonClient().delete("/api/v1/recipes/abc-123")
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `DELETE recipe returns 404 when not found`() = testApplication {
        application { testModule() }
        coEvery { mockRepo.delete("missing") } returns false

        val response = jsonClient().delete("/api/v1/recipes/missing")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
