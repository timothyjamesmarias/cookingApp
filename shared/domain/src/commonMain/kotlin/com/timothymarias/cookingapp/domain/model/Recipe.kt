package com.timothymarias.cookingapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Recipe(
    val localId: String,
    val name: String
)
