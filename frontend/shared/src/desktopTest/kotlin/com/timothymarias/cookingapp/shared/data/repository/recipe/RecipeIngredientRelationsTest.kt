package com.timothymarias.cookingapp.shared.data.repository.recipe

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.timothymarias.cookingapp.shared.data.repository.ingredient.DbIngredientRepository
import com.timothymarias.cookingapp.shared.db.CookingDatabase
import com.timothymarias.cookingapp.domain.model.Ingredient
import com.timothymarias.cookingapp.domain.model.Recipe
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecipeIngredientRelationsTest {
    @Test
    fun `assign and remove ingredients reflect in queries`() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookingDatabase.Schema.create(driver)
        val db = CookingDatabase(driver)

        val recipeRepo = DbRecipeRepository(db)
        val ingredientRepo = DbIngredientRepository(db)

        // Seed one recipe and two ingredients
        val r = recipeRepo.create(Recipe(localId = "", name = "Salad"))
        val i1 = ingredientRepo.create(Ingredient(localId = "", name = "Lettuce"))
        val i2 = ingredientRepo.create(Ingredient(localId = "", name = "Tomato"))

        // Initially not assigned
        assertFalse(recipeRepo.isIngredientAssigned(r.localId, i1.localId))
        assertFalse(recipeRepo.isIngredientAssigned(r.localId, i2.localId))
        assertEquals(emptyList(), recipeRepo.getIngredients(r.localId))

        // Assign i1
        recipeRepo.assignIngredient(r.localId, i1.localId)
        assertTrue(recipeRepo.isIngredientAssigned(r.localId, i1.localId))
        assertFalse(recipeRepo.isIngredientAssigned(r.localId, i2.localId))
        assertEquals(listOf("Lettuce"), recipeRepo.getIngredients(r.localId).map { it.name })

        // Assign i2
        recipeRepo.assignIngredient(r.localId, i2.localId)
        assertTrue(recipeRepo.isIngredientAssigned(r.localId, i1.localId))
        assertTrue(recipeRepo.isIngredientAssigned(r.localId, i2.localId))
        assertEquals(setOf("Lettuce", "Tomato"), recipeRepo.getIngredients(r.localId).map { it.name }.toSet())

        // Remove i1
        recipeRepo.removeIngredient(r.localId, i1.localId)
        assertFalse(recipeRepo.isIngredientAssigned(r.localId, i1.localId))
        assertTrue(recipeRepo.isIngredientAssigned(r.localId, i2.localId))
        assertEquals(listOf("Tomato"), recipeRepo.getIngredients(r.localId).map { it.name })

        // Remove i2
        recipeRepo.removeIngredient(r.localId, i2.localId)
        assertFalse(recipeRepo.isIngredientAssigned(r.localId, i2.localId))
        assertEquals(emptyList(), recipeRepo.getIngredients(r.localId))
    }
}
