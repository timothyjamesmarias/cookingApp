package com.timothymarias.cookingapp.repository

import com.timothymarias.cookingapp.db.*
import com.timothymarias.cookingapp.domain.api.CreateIngredientRequest
import com.timothymarias.cookingapp.domain.api.IngredientResponse
import com.timothymarias.cookingapp.domain.api.UpdateIngredientRequest
import kotlinx.uuid.UUID
import kotlinx.uuid.generateUUID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class IngredientRepository {

    suspend fun findAll(): List<IngredientResponse> = dbQuery {
        Ingredients.selectAll().map { it.toResponse() }
    }

    suspend fun findByLocalId(localId: String): IngredientResponse? = dbQuery {
        Ingredients.selectAll().where { Ingredients.localId eq localId }
            .singleOrNull()?.toResponse()
    }

    suspend fun create(request: CreateIngredientRequest): IngredientResponse = dbQuery {
        val localId = UUID.generateUUID().toString()
        Ingredients.insert {
            it[Ingredients.localId] = localId
            it[name] = request.name
        }
        IngredientResponse(localId = localId, name = request.name)
    }

    suspend fun update(localId: String, request: UpdateIngredientRequest): IngredientResponse? = dbQuery {
        val updated = Ingredients.update({ Ingredients.localId eq localId }) {
            it[name] = request.name
        }
        if (updated == 0) null
        else IngredientResponse(localId = localId, name = request.name)
    }

    suspend fun delete(localId: String): Boolean = dbQuery {
        Ingredients.deleteWhere { Ingredients.localId eq localId } > 0
    }

    suspend fun search(query: String): List<IngredientResponse> = dbQuery {
        Ingredients.selectAll()
            .where { Ingredients.name.lowerCase() like "%${query.lowercase()}%" }
            .map { it.toResponse() }
    }

    private fun ResultRow.toResponse() = IngredientResponse(
        localId = this[Ingredients.localId],
        name = this[Ingredients.name]
    )
}
