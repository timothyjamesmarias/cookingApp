package com.timothymarias.cookingapp.shared.presentation.ingredient

import com.timothymarias.cookingapp.domain.model.Ingredient

data class IngredientState(
    val items: List<Ingredient> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val editingId: String? = null,
    val editName: String = "",
)