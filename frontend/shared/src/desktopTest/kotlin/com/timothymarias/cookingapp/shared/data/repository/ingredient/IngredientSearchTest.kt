package com.timothymarias.cookingapp.shared.data.repository.ingredient

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.timothymarias.cookingapp.shared.db.CookingDatabase
import com.timothymarias.cookingapp.domain.model.Ingredient
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for ingredient search functionality.
 * Tests the searchByName SQLite query with real database.
 */
class IngredientSearchTest {
    private lateinit var db: CookingDatabase
    private lateinit var repo: IngredientRepository

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookingDatabase.Schema.create(driver)
        db = CookingDatabase(driver)
        repo = DbIngredientRepository(db)
    }

    @Test
    fun `search returns exact match`() = runTest {
        // Given: ingredients in database
        repo.create(Ingredient(localId = "", name = "Tomato"))
        repo.create(Ingredient(localId = "", name = "Onion"))
        repo.create(Ingredient(localId = "", name = "Garlic"))

        // When: searching for exact match
        repo.watchByQuery("Tomato").test {
            val results = awaitItem()

            // Then: only exact match is returned
            assertEquals(1, results.size)
            assertEquals("Tomato", results[0].name)
        }
    }

    @Test
    fun `search is case insensitive`() = runTest {
        // Given
        repo.create(Ingredient(localId = "", name = "Tomato"))

        // When: searching with different case
        repo.watchByQuery("tomato").test {
            val results = awaitItem()

            // Then: match is found
            assertEquals(1, results.size)
            assertEquals("Tomato", results[0].name)
        }
    }

    @Test
    fun `search returns partial matches`() = runTest {
        // Given
        repo.create(Ingredient(localId = "", name = "Tomato"))
        repo.create(Ingredient(localId = "", name = "Cherry Tomato"))
        repo.create(Ingredient(localId = "", name = "Tomato Paste"))
        repo.create(Ingredient(localId = "", name = "Onion"))

        // When: searching for partial match
        repo.watchByQuery("tomato").test {
            val results = awaitItem()

            // Then: all matches containing 'tomato' are returned
            assertEquals(3, results.size)
            assertTrue(results.all { it.name.contains("Tomato", ignoreCase = true) })
        }
    }

    @Test
    fun `search returns empty list when no matches`() = runTest {
        // Given
        repo.create(Ingredient(localId = "", name = "Tomato"))
        repo.create(Ingredient(localId = "", name = "Onion"))

        // When: searching for non-existent ingredient
        repo.watchByQuery("pineapple").test {
            val results = awaitItem()

            // Then: empty list is returned
            assertEquals(0, results.size)
        }
    }

    @Test
    fun `search results are sorted alphabetically`() = runTest {
        // Given: ingredients added in random order
        repo.create(Ingredient(localId = "", name = "Zucchini"))
        repo.create(Ingredient(localId = "", name = "Apple"))
        repo.create(Ingredient(localId = "", name = "Banana"))

        // When: searching for all (empty query would use watchAll, so search for common letter)
        repo.watchByQuery("a").test {
            val results = awaitItem()

            // Then: results are sorted by name
            assertEquals(2, results.size) // Apple, Banana
            assertEquals("Apple", results[0].name)
            assertEquals("Banana", results[1].name)
        }
    }

    @Test
    fun `search updates when new matching ingredient is added`() = runTest {
        // Given: initial ingredients
        repo.create(Ingredient(localId = "", name = "Tomato"))

        // When: watching search results
        repo.watchByQuery("to").test {
            // Then: initial result
            assertEquals(1, awaitItem().size)

            // When: adding another matching ingredient
            repo.create(Ingredient(localId = "", name = "Potato"))

            // Then: flow emits updated results
            val updated = awaitItem()
            assertEquals(2, updated.size)
            assertTrue(updated.any { it.name == "Potato" })
        }
    }

    @Test
    fun `search with special characters is handled safely`() = runTest {
        // Given
        repo.create(Ingredient(localId = "", name = "Salt & Pepper"))

        // When: searching with special characters
        repo.watchByQuery("salt & pepper").test {
            val results = awaitItem()

            // Then: match is found
            assertEquals(1, results.size)
            assertEquals("Salt & Pepper", results[0].name)
        }
    }
}
