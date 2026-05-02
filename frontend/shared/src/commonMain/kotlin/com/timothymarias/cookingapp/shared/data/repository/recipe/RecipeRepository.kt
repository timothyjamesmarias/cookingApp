package com.timothymarias.cookingapp.shared.data.repository.recipe

import com.timothymarias.cookingapp.domain.model.Ingredient
import com.timothymarias.cookingapp.domain.model.Recipe
import kotlinx.coroutines.flow.Flow

/**
 * Data class representing an ingredient with optional quantity information.
 */
data class IngredientWithQuantity(
    val ingredientId: String,
    val ingredientName: String,
    val quantityId: String?,
    val amount: Double?,
    val unitId: String?
)

/**
 * Abstraction for accessing recipes from the local database (SQLDelight-backed).
 * Read APIs are implemented; write APIs (CRUD) will be implemented next.
 */
interface RecipeRepository {
    // Read
    fun watchAll(): Flow<List<Recipe>>
    fun watchById(localId: String): Flow<Recipe?>

    // Write (skeleton, to be implemented in TDD)
    suspend fun create(recipe: Recipe): Recipe
    suspend fun updateName(localId: String, name: String): Recipe
    suspend fun delete(localId: String)
    suspend fun getIngredients(localId: String): List<Ingredient>
    suspend fun assignIngredient(recipeId: String, ingredientId: String)
    suspend fun removeIngredient(recipeId: String, ingredientId: String)
    suspend fun isIngredientAssigned(recipeId: String, ingredientId: String): Boolean
    suspend fun updateIngredientQuantity(recipeId: String, ingredientId: String, quantityId: String?)

    /**
     * Get ingredients with their quantities for a recipe.
     * Returns list of ingredients with optional quantity info.
     */
    suspend fun getIngredientsWithQuantities(recipeId: String): List<IngredientWithQuantity>
}