package com.timothymarias.cookingapp.repository

import com.timothymarias.cookingapp.domain.api.CreateIngredientRequest
import com.timothymarias.cookingapp.domain.api.CreateRecipeRequest
import com.timothymarias.cookingapp.domain.api.UpdateRecipeRequest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecipeRepositoryTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            TestDatabaseFactory.init()
        }
    }

    private val recipeRepo = RecipeRepository()
    private val ingredientRepo = IngredientRepository()

    @BeforeEach
    fun cleanDb() = runTest {
        recipeRepo.findAll().forEach { recipeRepo.delete(it.localId) }
        ingredientRepo.findAll().forEach { ingredientRepo.delete(it.localId) }
    }

    @Test
    fun `create and find recipe`() = runTest {
        val created = recipeRepo.create(CreateRecipeRequest(name = "Toast"))
        assertNotNull(created.localId)
        assertEquals("Toast", created.name)

        val found = recipeRepo.findByLocalId(created.localId)
        assertNotNull(found)
        assertEquals("Toast", found.name)
    }

    @Test
    fun `findAll returns saved recipes`() = runTest {
        recipeRepo.create(CreateRecipeRequest(name = "One"))
        recipeRepo.create(CreateRecipeRequest(name = "Two"))

        val all = recipeRepo.findAll()
        assertEquals(2, all.size)
        assertTrue(all.map { it.name }.containsAll(listOf("One", "Two")))
    }

    @Test
    fun `update recipe name`() = runTest {
        val created = recipeRepo.create(CreateRecipeRequest(name = "Old"))
        val updated = recipeRepo.update(created.localId, UpdateRecipeRequest(name = "New"))
        assertNotNull(updated)
        assertEquals("New", updated.name)
    }

    @Test
    fun `delete recipe`() = runTest {
        val created = recipeRepo.create(CreateRecipeRequest(name = "ToDelete"))
        assertTrue(recipeRepo.delete(created.localId))
        assertNull(recipeRepo.findByLocalId(created.localId))
    }

    @Test
    fun `create recipe with ingredients`() = runTest {
        val salt = ingredientRepo.create(CreateIngredientRequest(name = "Salt"))
        val pepper = ingredientRepo.create(CreateIngredientRequest(name = "Pepper"))

        val recipe = recipeRepo.create(
            CreateRecipeRequest(name = "Seasoned", ingredientIds = listOf(salt.localId, pepper.localId))
        )

        assertEquals(2, recipe.ingredients.size)
        assertTrue(recipe.ingredients.map { it.name }.containsAll(listOf("Salt", "Pepper")))
    }

    @Test
    fun `update recipe replaces ingredients`() = runTest {
        val salt = ingredientRepo.create(CreateIngredientRequest(name = "Salt"))
        val sugar = ingredientRepo.create(CreateIngredientRequest(name = "Sugar"))

        val recipe = recipeRepo.create(
            CreateRecipeRequest(name = "Test", ingredientIds = listOf(salt.localId))
        )
        assertEquals(1, recipe.ingredients.size)

        val updated = recipeRepo.update(
            recipe.localId,
            UpdateRecipeRequest(name = "Test", ingredientIds = listOf(sugar.localId))
        )
        assertNotNull(updated)
        assertEquals(1, updated.ingredients.size)
        assertEquals("Sugar", updated.ingredients[0].name)
    }
}
