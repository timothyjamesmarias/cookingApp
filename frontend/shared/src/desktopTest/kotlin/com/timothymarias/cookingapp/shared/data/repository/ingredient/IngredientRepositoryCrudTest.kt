package com.timothymarias.cookingapp.shared.data.repository.ingredient

import app.cash.turbine.test
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.timothymarias.cookingapp.shared.db.CookingDatabase
import com.timothymarias.cookingapp.domain.model.Ingredient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class IngredientRepositoryCrudTest {
    @Test
    fun `create, updateName, delete via repository affect watchAll and watchById for ingredients`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookingDatabase.Schema.create(driver)
        val db = CookingDatabase(driver)
        val repo = DbIngredientRepository(db)

        val created = repo.create(Ingredient(localId = "", name = "Garlic"))
        assertNotNull(created.localId)

        // watchById reflects value
        repo.watchById(created.localId).test {
            assertEquals("Garlic", awaitItem()?.name)

            // Update name via repo
            repo.updateName(created.localId, "Minced Garlic")
            assertEquals("Minced Garlic", awaitItem()?.name)

            // Delete via repo
            repo.delete(created.localId)
            assertNull(awaitItem())

            cancelAndConsumeRemainingEvents()
        }

        // watchAll sequence
        repo.watchAll().test {
            // Consume initial emission (should be empty)
            assertEquals(0, awaitItem().size, "initial emission should be empty")

            val i2 = repo.create(Ingredient(localId = "", name = "Onion"))
            // Next emission should contain the newly created ingredient
            assertEquals(listOf("Onion"), awaitItem().map { it.name })

            // Cleanup
            repo.delete(i2.localId)
            // After deletion, list should be empty again
            assertEquals(emptyList(), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }
}
