package com.timothymarias.cookingapp.shared.data.repository.recipe

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.timothymarias.cookingapp.shared.db.CookingDatabase
import com.timothymarias.cookingapp.domain.model.Ingredient
import com.timothymarias.cookingapp.domain.model.Recipe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.BooleanArraySerializer
import kotlinx.uuid.UUID
import kotlinx.uuid.generateUUID

/**
 * Local-first repository backed by SQLDelight. Currently read-focused; write methods are TODO for TDD.
 */
class DbRecipeRepository(
    private val db: CookingDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : RecipeRepository {
    override fun watchAll(): Flow<List<Recipe>> =
        db.recipesQueries.selectAll()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { Recipe(localId = it.local_id, name = it.name) } }

    override fun watchById(localId: String): Flow<Recipe?> =
        db.recipesQueries.selectById(localId)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { row -> row?.let { Recipe(localId = it.local_id, name = it.name) } }

    override suspend fun create(recipe: Recipe): Recipe {
        val newLocalId = recipe.localId.takeIf { it.isNotBlank() } ?: UUID.generateUUID().toString()
        db.recipesQueries.insertRecipe(newLocalId, recipe.name)
        return Recipe(localId = newLocalId, name = recipe.name)
    }

    override suspend fun updateName(localId: String, name: String): Recipe {
        db.recipesQueries.updateRecipeName(name = name, local_id = localId)
        return Recipe(localId = localId, name = name)
    }

    override suspend fun delete(localId: String) {
        db.recipesQueries.deleteById(local_id = localId)
    }

    override suspend fun getIngredients(localId: String): List<Ingredient> {
        return db.recipesQueries.selectIngredients(localId)
            .executeAsList()
            .map { Ingredient(localId = it.local_id, name = it.name) }
    }

    override suspend fun assignIngredient(recipeId: String, ingredientId: String) {
        db.recipesQueries.assignIngredient(recipeId, ingredientId)
    }

    override suspend fun removeIngredient(recipeId: String, ingredientId: String) {
        db.recipesQueries.removeIngredient(recipeId, ingredientId)
    }

    override suspend fun isIngredientAssigned(recipeId: String, ingredientId: String): Boolean {
        return db.recipesQueries.isIngredientAssigned(recipeId, ingredientId)
            .executeAsOne()
    }

    override suspend fun updateIngredientQuantity(recipeId: String, ingredientId: String, quantityId: String?) {
        db.recipesQueries.updateIngredientQuantity(
            quantity_id = quantityId,
            recipe_id = recipeId,
            ingredient_id = ingredientId
        )
    }

    override suspend fun getIngredientsWithQuantities(recipeId: String): List<IngredientWithQuantity> {
        return db.recipesQueries.selectIngredientsWithQuantities(recipeId)
            .executeAsList()
            .map {
                IngredientWithQuantity(
                    ingredientId = it.ingredient_id,
                    ingredientName = it.ingredient_name,
                    quantityId = it.quantity_id,
                    amount = it.amount,
                    unitId = it.unit_id
                )
            }
    }
}
