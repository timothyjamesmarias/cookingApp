package com.timothymarias.cookingapp.domain.api

import io.ktor.resources.*
import kotlinx.serialization.Serializable

@Serializable
@Resource("/api/v1/recipes")
class Recipes {
    @Serializable
    @Resource("{localId}")
    data class ById(val parent: Recipes = Recipes(), val localId: String)
}

@Serializable
@Resource("/api/v1/ingredients")
class Ingredients {
    @Serializable
    @Resource("{localId}")
    data class ById(val parent: Ingredients = Ingredients(), val localId: String)

    @Serializable
    @Resource("search")
    data class Search(val parent: Ingredients = Ingredients(), val q: String = "")
}
