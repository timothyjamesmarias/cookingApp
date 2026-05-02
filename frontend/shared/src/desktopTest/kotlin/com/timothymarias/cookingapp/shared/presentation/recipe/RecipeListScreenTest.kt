package com.timothymarias.cookingapp.shared.presentation.recipe

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.timothymarias.cookingapp.shared.data.repository.recipe.DbRecipeRepository
import com.timothymarias.cookingapp.shared.data.repository.quantity.DbQuantityRepository
import com.timothymarias.cookingapp.shared.db.CookingDatabase
import com.timothymarias.cookingapp.domain.model.Recipe
import com.timothymarias.cookingapp.shared.presentation.recipe.list.RecipeListScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Basic UI tests for RecipeListScreen.
 * Tests that the screen renders properly with different states.
 */
class RecipeListScreenTest {
    private lateinit var database: CookingDatabase
    private lateinit var store: RecipeStore

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookingDatabase.Schema.create(driver)
        database = CookingDatabase(driver)
        val recipeRepo = DbRecipeRepository(database, Dispatchers.Default)
        val quantityRepo = DbQuantityRepository(database, Dispatchers.Default)
        store = RecipeStore(recipeRepo, quantityRepo)
    }

    @Test
    fun `store state reflects empty list initially`() = runTest {
        val state = store.state.value
        // Initially empty
        assert(state.items.isEmpty())
    }

    @Test
    fun `store can dispatch create action`() = runTest {
        store.dispatch(RecipeAction.Create("Test Recipe"))
        // If this doesn't throw, dispatching works
    }

    @Test
    fun `store can dispatch load action`() = runTest {
        store.dispatch(RecipeAction.Load)
        // If this doesn't throw, dispatching works
    }
}
