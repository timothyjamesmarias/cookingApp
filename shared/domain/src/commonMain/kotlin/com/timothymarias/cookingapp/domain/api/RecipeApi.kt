package com.timothymarias.cookingapp.domain.api

import com.timothymarias.cookingapp.domain.model.Ingredient
import kotlinx.serialization.Serializable

@Serializable
data class CreateRecipeRequest(
    val name: String,
    val ingredientIds: List<String> = emptyList()
)

@Serializable
data class UpdateRecipeRequest(
    val name: String,
    val ingredientIds: List<String> = emptyList()
)

@Serializable
data class RecipeResponse(
    val localId: String,
    val name: String,
    val ingredients: List<Ingredient> = emptyList()
)
