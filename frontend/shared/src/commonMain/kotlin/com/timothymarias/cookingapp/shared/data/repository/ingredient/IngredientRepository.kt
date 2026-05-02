package com.timothymarias.cookingapp.shared.data.repository.ingredient

import com.timothymarias.cookingapp.domain.model.Ingredient
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction for accessing Ingredients from the local database (SQLDelight-backed).
 * Read APIs are implemented; write APIs (CRUD) will be implemented next.
 */
interface IngredientRepository {
    // Read - Reactive
    fun watchAll(): Flow<List<Ingredient>>
    fun watchById(localId: String): Flow<Ingredient?>
    fun watchByQuery(query: String): Flow<List<Ingredient>>

    // Read - Suspend
    suspend fun getAll(): List<Ingredient>

    // Write
    suspend fun create(ingredient: Ingredient): Ingredient
    suspend fun updateName(localId: String, name: String): Ingredient
    suspend fun delete(localId: String)
}