package com.timothymarias.cookingapp.routes

import com.timothymarias.cookingapp.domain.api.ApiRoutes
import com.timothymarias.cookingapp.domain.api.CreateRecipeRequest
import com.timothymarias.cookingapp.domain.api.UpdateRecipeRequest
import com.timothymarias.cookingapp.repository.RecipeRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.recipeRoutes() {
    val repo by inject<RecipeRepository>()

    route(ApiRoutes.RECIPES) {
        get {
            call.respond(repo.findAll())
        }

        get("/{localId}") {
            val localId = call.parameters["localId"]!!
            val recipe = repo.findByLocalId(localId)
            if (recipe != null) call.respond(recipe)
            else call.respond(HttpStatusCode.NotFound)
        }

        post {
            val request = call.receive<CreateRecipeRequest>()
            require(request.name.isNotBlank() && request.name.length <= 100) { "Name must be 1-100 characters" }
            call.respond(HttpStatusCode.Created, repo.create(request))
        }

        put("/{localId}") {
            val localId = call.parameters["localId"]!!
            val request = call.receive<UpdateRecipeRequest>()
            require(request.name.isNotBlank() && request.name.length <= 100) { "Name must be 1-100 characters" }
            val updated = repo.update(localId, request)
            if (updated != null) call.respond(updated)
            else call.respond(HttpStatusCode.NotFound)
        }

        delete("/{localId}") {
            val localId = call.parameters["localId"]!!
            if (repo.delete(localId)) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound)
        }
    }
}
