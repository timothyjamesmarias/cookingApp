package com.timothymarias.cookingapp.routes

import com.timothymarias.cookingapp.domain.api.CreateIngredientRequest
import com.timothymarias.cookingapp.domain.api.Ingredients
import com.timothymarias.cookingapp.domain.api.UpdateIngredientRequest
import com.timothymarias.cookingapp.repository.IngredientRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import io.ktor.server.resources.get as getResource
import io.ktor.server.resources.post as postResource
import io.ktor.server.resources.put as putResource
import io.ktor.server.resources.delete as deleteResource

fun Route.ingredientRoutes() {
    val repo by inject<IngredientRepository>()

    getResource<Ingredients> {
        call.respond(repo.findAll())
    }

    getResource<Ingredients.ById> { resource ->
        val ingredient = repo.findByLocalId(resource.localId)
        if (ingredient != null) call.respond(ingredient)
        else call.respond(HttpStatusCode.NotFound)
    }

    postResource<Ingredients> {
        val request = call.receive<CreateIngredientRequest>()
        require(request.name.isNotBlank() && request.name.length <= 100) { "Name must be 1-100 characters" }
        call.respond(HttpStatusCode.Created, repo.create(request))
    }

    getResource<Ingredients.Search> { resource ->
        call.respond(repo.search(resource.q))
    }

    putResource<Ingredients.ById> { resource ->
        val request = call.receive<UpdateIngredientRequest>()
        require(request.name.isNotBlank() && request.name.length <= 100) { "Name must be 1-100 characters" }
        val updated = repo.update(resource.localId, request)
        if (updated != null) call.respond(updated)
        else call.respond(HttpStatusCode.NotFound)
    }

    deleteResource<Ingredients.ById> { resource ->
        if (repo.delete(resource.localId)) call.respond(HttpStatusCode.NoContent)
        else call.respond(HttpStatusCode.NotFound)
    }
}
