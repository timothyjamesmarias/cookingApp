package com.timothymarias.cookingapp.routes

import com.timothymarias.cookingapp.domain.api.ApiRoutes
import com.timothymarias.cookingapp.domain.api.CreateIngredientRequest
import com.timothymarias.cookingapp.domain.api.UpdateIngredientRequest
import com.timothymarias.cookingapp.repository.IngredientRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.ingredientRoutes() {
    val repo by inject<IngredientRepository>()

    route(ApiRoutes.INGREDIENTS) {
        get {
            call.respond(repo.findAll())
        }

        get("/{localId}") {
            val localId = call.parameters["localId"]!!
            val ingredient = repo.findByLocalId(localId)
            if (ingredient != null) call.respond(ingredient)
            else call.respond(HttpStatusCode.NotFound)
        }

        post {
            val request = call.receive<CreateIngredientRequest>()
            require(request.name.isNotBlank() && request.name.length <= 100) { "Name must be 1-100 characters" }
            call.respond(HttpStatusCode.Created, repo.create(request))
        }

        get("/search") {
            val query = call.parameters["q"] ?: ""
            call.respond(repo.search(query))
        }

        put("/{localId}") {
            val localId = call.parameters["localId"]!!
            val request = call.receive<UpdateIngredientRequest>()
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
