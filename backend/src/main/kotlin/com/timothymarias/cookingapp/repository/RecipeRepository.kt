package com.timothymarias.cookingapp.repository

import com.timothymarias.cookingapp.db.*
import com.timothymarias.cookingapp.domain.api.CreateRecipeRequest
import com.timothymarias.cookingapp.domain.api.RecipeResponse
import com.timothymarias.cookingapp.domain.api.UpdateRecipeRequest
import com.timothymarias.cookingapp.domain.model.Ingredient
import kotlinx.uuid.UUID
import kotlinx.uuid.generateUUID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class RecipeRepository {

    suspend fun findAll(): List<RecipeResponse> = dbQuery {
        val recipes = Recipes.selectAll().map { row ->
            row[Recipes.localId] to row[Recipes.name]
        }
        recipes.map { (localId, name) ->
            RecipeResponse(localId = localId, name = name, ingredients = findIngredientsByRecipeLocalId(localId))
        }
    }

    suspend fun findByLocalId(localId: String): RecipeResponse? = dbQuery {
        Recipes.selectAll().where { Recipes.localId eq localId }
            .singleOrNull()
            ?.let { row ->
                RecipeResponse(
                    localId = row[Recipes.localId],
                    name = row[Recipes.name],
                    ingredients = findIngredientsByRecipeLocalId(localId)
                )
            }
    }

    suspend fun create(request: CreateRecipeRequest): RecipeResponse = dbQuery {
        val localId = UUID.generateUUID().toString()
        val recipeId = Recipes.insert {
            it[Recipes.localId] = localId
            it[name] = request.name
        }[Recipes.id]

        linkIngredients(recipeId, request.ingredientIds)
        RecipeResponse(localId = localId, name = request.name, ingredients = findIngredientsByRecipeLocalId(localId))
    }

    suspend fun update(localId: String, request: UpdateRecipeRequest): RecipeResponse? = dbQuery {
        val recipeId = Recipes.selectAll().where { Recipes.localId eq localId }
            .singleOrNull()?.get(Recipes.id) ?: return@dbQuery null

        Recipes.update({ Recipes.localId eq localId }) {
            it[name] = request.name
        }

        RecipeIngredients.deleteWhere { RecipeIngredients.recipeId eq recipeId }
        linkIngredients(recipeId, request.ingredientIds)

        RecipeResponse(localId = localId, name = request.name, ingredients = findIngredientsByRecipeLocalId(localId))
    }

    suspend fun delete(localId: String): Boolean = dbQuery {
        val recipeId = Recipes.selectAll().where { Recipes.localId eq localId }
            .singleOrNull()?.get(Recipes.id) ?: return@dbQuery false

        RecipeIngredients.deleteWhere { RecipeIngredients.recipeId eq recipeId }
        Recipes.deleteWhere { Recipes.localId eq localId } > 0
    }

    private fun findIngredientsByRecipeLocalId(recipeLocalId: String): List<Ingredient> {
        val recipeId = Recipes.selectAll().where { Recipes.localId eq recipeLocalId }
            .singleOrNull()?.get(Recipes.id) ?: return emptyList()

        return (RecipeIngredients innerJoin Ingredients)
            .selectAll().where { RecipeIngredients.recipeId eq recipeId }
            .map { row ->
                Ingredient(
                    localId = row[Ingredients.localId],
                    name = row[Ingredients.name]
                )
            }
    }

    private fun linkIngredients(recipeId: Long, ingredientLocalIds: List<String>) {
        for (ingredientLocalId in ingredientLocalIds) {
            val ingredientId = Ingredients.selectAll()
                .where { Ingredients.localId eq ingredientLocalId }
                .singleOrNull()?.get(Ingredients.id) ?: continue

            RecipeIngredients.insert {
                it[RecipeIngredients.recipeId] = recipeId
                it[RecipeIngredients.ingredientId] = ingredientId
            }
        }
    }
}
