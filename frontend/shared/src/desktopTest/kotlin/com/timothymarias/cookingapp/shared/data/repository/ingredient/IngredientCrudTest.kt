package com.timothymarias.cookingapp.shared.data.repository.ingredient

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.timothymarias.cookingapp.shared.db.CookingDatabase
import com.timothymarias.cookingapp.domain.model.Ingredient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Basic foundational CRUD tests for Ingredient repository.
 * Tests individual operations in isolation.
 */
class IngredientCrudTest {
    private lateinit var database: CookingDatabase
    private lateinit var repo: DbIngredientRepository

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookingDatabase.Schema.create(driver)
        database = CookingDatabase(driver)
        repo = DbIngredientRepository(database)
    }

    @Test
    fun `create ingredient returns ingredient with generated id`() = runTest {
        val ingredient = Ingredient(localId = "", name = "Test Ingredient")
        val created = repo.create(ingredient)

        assertNotNull(created.localId)
        assertTrue(created.localId.isNotEmpty())
        assertEquals("Test Ingredient", created.name)
    }

    @Test
    fun `create ingredient with empty name succeeds`() = runTest {
        val ingredient = Ingredient(localId = "", name = "")
        val created = repo.create(ingredient)

        assertNotNull(created.localId)
        assertEquals("", created.name)
    }

    @Test
    fun `watchById returns null for non-existent id`() = runTest {
        val result = repo.watchById("non-existent-id").first()
        assertNull(result)
    }

    @Test
    fun `watchById returns ingredient after creation`() = runTest {
        val created = repo.create(Ingredient(localId = "", name = "Tomato"))
        val found = repo.watchById(created.localId).first()

        assertNotNull(found)
        assertEquals("Tomato", found.name)
        assertEquals(created.localId, found.localId)
    }

    @Test
    fun `updateName changes ingredient name`() = runTest {
        val created = repo.create(Ingredient(localId = "", name = "Original"))
        val updated = repo.updateName(created.localId, "Updated")

        assertEquals("Updated", updated.name)
        assertEquals(created.localId, updated.localId)

        val found = repo.watchById(created.localId).first()
        assertEquals("Updated", found?.name)
    }

    @Test
    fun `delete removes ingredient`() = runTest {
        val created = repo.create(Ingredient(localId = "", name = "To Delete"))
        
        // Verify it exists
        assertNotNull(repo.watchById(created.localId).first())

        // Delete it
        repo.delete(created.localId)

        // Verify it's gone
        assertNull(repo.watchById(created.localId).first())
    }

    @Test
    fun `watchAll returns empty list initially`() = runTest {
        val ingredients = repo.watchAll().first()
        assertEquals(0, ingredients.size)
    }

    @Test
    fun `watchAll returns all created ingredients`() = runTest {
        repo.create(Ingredient(localId = "", name = "Ingredient 1"))
        repo.create(Ingredient(localId = "", name = "Ingredient 2"))
        repo.create(Ingredient(localId = "", name = "Ingredient 3"))

        val ingredients = repo.watchAll().first()
        assertEquals(3, ingredients.size)
    }

    @Test
    fun `multiple creates result in unique ids`() = runTest {
        val i1 = repo.create(Ingredient(localId = "", name = "Ingredient 1"))
        val i2 = repo.create(Ingredient(localId = "", name = "Ingredient 2"))
        val i3 = repo.create(Ingredient(localId = "", name = "Ingredient 3"))

        val ids = setOf(i1.localId, i2.localId, i3.localId)
        assertEquals(3, ids.size, "All IDs should be unique")
    }

    @Test
    fun `delete non-existent ingredient does not throw`() = runTest {
        // Should not throw an exception
        repo.delete("non-existent-id")
    }

    @Test
    fun `create and read preserves ingredient data`() = runTest {
        val original = Ingredient(
            localId = "",
            name = "Complex Ingredient"
        )
        val created = repo.create(original)
        val retrieved = repo.watchById(created.localId).first()

        assertNotNull(retrieved)
        assertEquals(original.name, retrieved.name)
    }
}
