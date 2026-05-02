package com.timothymarias.cookingapp.shared.data.repository.recipe

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.timothymarias.cookingapp.shared.db.CookingDatabase
import com.timothymarias.cookingapp.domain.model.Recipe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Basic foundational CRUD tests for Recipe repository.
 * Tests individual operations in isolation.
 */
class RecipeCrudTest {
    private lateinit var database: CookingDatabase
    private lateinit var repo: DbRecipeRepository

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookingDatabase.Schema.create(driver)
        database = CookingDatabase(driver)
        repo = DbRecipeRepository(database)
    }

    @Test
    fun `create recipe returns recipe with generated id`() = runTest {
        val recipe = Recipe(localId = "", name = "Test Recipe")
        val created = repo.create(recipe)

        assertNotNull(created.localId)
        assertTrue(created.localId.isNotEmpty())
        assertEquals("Test Recipe", created.name)
    }

    @Test
    fun `create recipe with empty name succeeds`() = runTest {
        val recipe = Recipe(localId = "", name = "")
        val created = repo.create(recipe)

        assertNotNull(created.localId)
        assertEquals("", created.name)
    }

    @Test
    fun `watchById returns null for non-existent id`() = runTest {
        val result = repo.watchById("non-existent-id").first()
        assertNull(result)
    }

    @Test
    fun `watchById returns recipe after creation`() = runTest {
        val created = repo.create(Recipe(localId = "", name = "Pasta"))
        val found = repo.watchById(created.localId).first()

        assertNotNull(found)
        assertEquals("Pasta", found.name)
        assertEquals(created.localId, found.localId)
    }

    @Test
    fun `updateName changes recipe name`() = runTest {
        val created = repo.create(Recipe(localId = "", name = "Original"))
        val updated = repo.updateName(created.localId, "Updated")

        assertEquals("Updated", updated.name)
        assertEquals(created.localId, updated.localId)

        val found = repo.watchById(created.localId).first()
        assertEquals("Updated", found?.name)
    }

    @Test
    fun `delete removes recipe`() = runTest {
        val created = repo.create(Recipe(localId = "", name = "To Delete"))
        
        // Verify it exists
        assertNotNull(repo.watchById(created.localId).first())

        // Delete it
        repo.delete(created.localId)

        // Verify it's gone
        assertNull(repo.watchById(created.localId).first())
    }

    @Test
    fun `watchAll returns empty list initially`() = runTest {
        val recipes = repo.watchAll().first()
        assertEquals(0, recipes.size)
    }

    @Test
    fun `watchAll returns all created recipes`() = runTest {
        repo.create(Recipe(localId = "", name = "Recipe 1"))
        repo.create(Recipe(localId = "", name = "Recipe 2"))
        repo.create(Recipe(localId = "", name = "Recipe 3"))

        val recipes = repo.watchAll().first()
        assertEquals(3, recipes.size)
    }

    @Test
    fun `multiple creates result in unique ids`() = runTest {
        val r1 = repo.create(Recipe(localId = "", name = "Recipe 1"))
        val r2 = repo.create(Recipe(localId = "", name = "Recipe 2"))
        val r3 = repo.create(Recipe(localId = "", name = "Recipe 3"))

        val ids = setOf(r1.localId, r2.localId, r3.localId)
        assertEquals(3, ids.size, "All IDs should be unique")
    }

    @Test
    fun `delete non-existent recipe does not throw`() = runTest {
        // Should not throw an exception
        repo.delete("non-existent-id")
    }

    @Test
    fun `create and read preserves recipe data`() = runTest {
        val original = Recipe(
            localId = "",
            name = "Complex Recipe"
        )
        val created = repo.create(original)
        val retrieved = repo.watchById(created.localId).first()

        assertNotNull(retrieved)
        assertEquals(original.name, retrieved.name)
    }
}
