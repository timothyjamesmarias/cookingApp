package com.timothymarias.cookingapp.domain.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateIngredientRequest(
    val name: String
)

@Serializable
data class UpdateIngredientRequest(
    val name: String
)

@Serializable
data class IngredientResponse(
    val localId: String,
    val name: String
)
