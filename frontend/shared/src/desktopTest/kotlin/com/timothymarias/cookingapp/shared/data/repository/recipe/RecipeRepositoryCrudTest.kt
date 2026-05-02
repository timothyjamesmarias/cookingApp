package com.timothymarias.cookingapp.shared.data.repository.recipe

import app.cash.turbine.test
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.timothymarias.cookingapp.shared.db.CookingDatabase
import com.timothymarias.cookingapp.domain.model.Recipe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RecipeRepositoryCrudTest {
    @Test
    fun `create, updateName, delete via repository affect watchAll and watchById`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookingDatabase.Schema.create(driver)
        val db = CookingDatabase(driver)
        val repo = DbRecipeRepository(db)

        // Create via repo
        val created = repo.create(Recipe(localId = "", name = "Pancakes"))
        assertNotNull(created.localId)

        // watchById reflects value
        repo.watchById(created.localId).test {
            assertEquals("Pancakes", awaitItem()?.name)

            // Update name via repo
            repo.updateName(created.localId, "Blueberry Pancakes")
            assertEquals("Blueberry Pancakes", awaitItem()?.name)

            // Delete via repo
            repo.delete(created.localId)
            assertNull(awaitItem())

            cancelAndConsumeRemainingEvents()
        }

        // watchAll sequence
        repo.watchAll().test {
            // Consume initial emission (should be empty in fresh in-memory DB)
            assertEquals(0, awaitItem().size, "initial emission should be empty")

            // Insert another to see flow reaction
            val r2 = repo.create(Recipe(localId = "", name = "Omelette"))
            // Next emission should contain the newly created recipe
            assertEquals(listOf("Omelette"), awaitItem().map { it.name })

            // Cleanup
            repo.delete(r2.localId)
            // After deletion, list should be empty again
            assertEquals(emptyList(), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }
}
