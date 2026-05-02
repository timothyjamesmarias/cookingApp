package com.timothymarias.cookingapp.shared.data.repository.ingredient

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.timothymarias.cookingapp.shared.db.CookingDatabase
import com.timothymarias.cookingapp.domain.model.Ingredient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.uuid.UUID
import kotlinx.uuid.generateUUID

data class DbIngredientRepository(
    private val db: CookingDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
): IngredientRepository {
    override fun watchAll(): Flow<List<Ingredient>> =
        db.ingredientsQueries.selectAll()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { Ingredient(localId = it.local_id, name = it.name) } }

    override fun watchById(localId: String): Flow<Ingredient?> =
        db.ingredientsQueries.selectById(localId)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { row -> row?.let { Ingredient(localId = it.local_id, name = it.name) } }

    override fun watchByQuery(query: String): Flow<List<Ingredient>> =
        db.ingredientsQueries.searchByName(query)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { Ingredient(localId = it.local_id, name = it.name) } }

    override suspend fun getAll(): List<Ingredient> =
        db.ingredientsQueries.selectAll()
            .executeAsList()
            .map { Ingredient(localId = it.local_id, name = it.name) }

    override suspend fun create(Ingredient: Ingredient): Ingredient {
        val newLocalId = Ingredient.localId.takeIf { it.isNotBlank() } ?: UUID.generateUUID().toString()
        db.ingredientsQueries.insertIngredient(newLocalId, Ingredient.name)
        return Ingredient(localId = newLocalId, name = Ingredient.name)
    }

    override suspend fun updateName(localId: String, name: String): Ingredient {
        db.ingredientsQueries.updateIngredientName(name = name, local_id = localId)
        return Ingredient(localId = localId, name = name)
    }

    override suspend fun delete(localId: String) {
        db.ingredientsQueries.deleteById(local_id = localId)
    }
}
