package com.timothymarias.cookingapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Ingredient(
    val localId: String,
    val name: String
)
