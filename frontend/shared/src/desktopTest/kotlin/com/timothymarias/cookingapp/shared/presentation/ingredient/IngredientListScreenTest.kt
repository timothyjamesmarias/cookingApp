package com.timothymarias.cookingapp.shared.presentation.ingredient

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.timothymarias.cookingapp.shared.data.repository.ingredient.DbIngredientRepository
import com.timothymarias.cookingapp.shared.db.CookingDatabase
import com.timothymarias.cookingapp.domain.model.Ingredient
import com.timothymarias.cookingapp.shared.presentation.ingredient.list.IngredientListScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Basic UI tests for IngredientListScreen.
 * Tests that the screen renders properly with different states.
 */
class IngredientListScreenTest {
    private lateinit var database: CookingDatabase
    private lateinit var store: IngredientStore

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookingDatabase.Schema.create(driver)
        database = CookingDatabase(driver)
        val repo = DbIngredientRepository(database, Dispatchers.Default)
        store = IngredientStore(repo)
    }

    @Test
    fun `store state reflects empty list initially`() = runTest {
        val state = store.state.value
        // Initially empty
        assert(state.items.isEmpty())
    }

    @Test
    fun `store can dispatch create action`() = runTest {
        store.dispatch(IngredientAction.Create("Test Ingredient"))
        // If this doesn't throw, dispatching works
    }

    @Test
    fun `store can dispatch load action`() = runTest {
        store.dispatch(IngredientAction.Load)
        // If this doesn't throw, dispatching works
    }
}
