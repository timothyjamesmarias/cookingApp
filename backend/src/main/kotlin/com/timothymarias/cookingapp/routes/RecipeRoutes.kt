package com.timothymarias.cookingapp.routes

import com.timothymarias.cookingapp.domain.api.CreateRecipeRequest
import com.timothymarias.cookingapp.domain.api.Recipes
import com.timothymarias.cookingapp.domain.api.UpdateRecipeRequest
import com.timothymarias.cookingapp.repository.RecipeRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import io.ktor.server.resources.get as getResource
import io.ktor.server.resources.post as postResource
import io.ktor.server.resources.put as putResource
import io.ktor.server.resources.delete as deleteResource

fun Route.recipeRoutes() {
    val repo by inject<RecipeRepository>()

    getResource<Recipes> {
        call.respond(repo.findAll())
    }

    getResource<Recipes.ById> { resource ->
        val recipe = repo.findByLocalId(resource.localId)
        if (recipe != null) call.respond(recipe)
        else call.respond(HttpStatusCode.NotFound)
    }

    postResource<Recipes> {
        val request = call.receive<CreateRecipeRequest>()
        require(request.name.isNotBlank() && request.name.length <= 100) { "Name must be 1-100 characters" }
        call.respond(HttpStatusCode.Created, repo.create(request))
    }

    putResource<Recipes.ById> { resource ->
        val request = call.receive<UpdateRecipeRequest>()
        require(request.name.isNotBlank() && request.name.length <= 100) { "Name must be 1-100 characters" }
        val updated = repo.update(resource.localId, request)
        if (updated != null) call.respond(updated)
        else call.respond(HttpStatusCode.NotFound)
    }

    deleteResource<Recipes.ById> { resource ->
        if (repo.delete(resource.localId)) call.respond(HttpStatusCode.NoContent)
        else call.respond(HttpStatusCode.NotFound)
    }
}
