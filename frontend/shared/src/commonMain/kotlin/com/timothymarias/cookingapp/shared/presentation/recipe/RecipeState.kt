package com.timothymarias.cookingapp.shared.presentation.recipe

import com.timothymarias.cookingapp.domain.model.Recipe
import com.timothymarias.cookingapp.domain.model.Ingredient

/**
 * Represents quantity information for an ingredient in a recipe.
 */
data class QuantityInfo(
    val amount: Double,
    val unitId: String
)

data class RecipeState(
    val items: List<Recipe> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    // UI flags to survive recompositions/navigation
    val managingIngredientsId: String? = null,
    val assignedIngredientIds: Set<String> = emptySet(),
    val selectedRecipeId: String? = null, // For recipe detail screen
    val isEditMode: Boolean = false, // For recipe detail screen edit mode
    // Quantity editing state
    val editingQuantityIngredientId: String? = null, // Which ingredient's quantity is being edited
    // Ingredient quantities: maps ingredient ID to quantity info (null if no quantity set)
    val ingredientQuantities: Map<String, QuantityInfo?> = emptyMap()
)