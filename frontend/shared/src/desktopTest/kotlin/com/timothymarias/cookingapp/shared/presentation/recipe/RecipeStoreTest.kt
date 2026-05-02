package com.timothymarias.cookingapp.shared.presentation.recipe

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.timothymarias.cookingapp.shared.data.repository.recipe.DbRecipeRepository
import com.timothymarias.cookingapp.shared.data.repository.quantity.DbQuantityRepository
import com.timothymarias.cookingapp.shared.db.CookingDatabase
import com.timothymarias.cookingapp.domain.model.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeStoreTest {
    private lateinit var store: RecipeStore
    private lateinit var database: CookingDatabase
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookingDatabase.Schema.create(driver)
        database = CookingDatabase(driver)
        val recipeRepo = DbRecipeRepository(database, Dispatchers.Default)
        val quantityRepo = DbQuantityRepository(database, Dispatchers.Default)
        store = RecipeStore(recipeRepo, quantityRepo, testDispatcher)
    }

    @Test
    fun `initial state eventually becomes not loading with empty items`() = runBlocking {
        // Wait for the store to initialize
        delay(100)

        val state = store.state.value
        assertFalse(state.isLoading)
        assertEquals(emptyList(), state.items)
    }

    @Test
    fun `adding recipe to database updates store state`() = runBlocking {
        // Wait for initial load
        delay(100)

        // Add a recipe to the database
        database.recipesQueries.insertRecipe("test-id", "Test Recipe")

        // Wait for the flow to propagate
        delay(100)

        val state = store.state.value
        assertEquals(1, state.items.size)
        assertEquals("Test Recipe", state.items[0].name)
    }

    @Test
    fun `Create action adds recipe to state`() = runBlocking {
        // Wait for initial load
        delay(100)

        store.dispatch(RecipeAction.Create("New Recipe"))

        // Wait for the action to complete
        delay(200)

        val state = store.state.value
        assertEquals(1, state.items.size)
        assertEquals("New Recipe", state.items[0].name)
    }

    @Test
    fun `Rename action updates recipe in state`() = runBlocking {
        // Wait for initial load
        delay(100)

        // First create a recipe
        database.recipesQueries.insertRecipe("test-id", "Original Name")
        delay(100)

        val beforeRename = store.state.value
        assertEquals(1, beforeRename.items.size)
        assertEquals("Original Name", beforeRename.items[0].name)

        store.dispatch(RecipeAction.Rename(id = "test-id", name = "Updated Name"))
        delay(100)

        val afterRename = store.state.value
        assertEquals(1, afterRename.items.size)
        assertEquals("Updated Name", afterRename.items[0].name)
    }

    @Test
    fun `Delete action removes recipe from state`() = runBlocking {
        // Wait for initial load
        delay(100)

        // First create a recipe
        database.recipesQueries.insertRecipe("test-id", "To Delete")
        delay(100)

        val beforeDelete = store.state.value
        assertEquals(1, beforeDelete.items.size)

        store.dispatch(RecipeAction.Delete("test-id"))
        delay(100)

        val afterDelete = store.state.value
        assertEquals(0, afterDelete.items.size)
    }

    @Test
    fun `multiple recipes are handled correctly`() = runBlocking {
        // Wait for initial load
        delay(100)

        database.recipesQueries.transaction {
            database.recipesQueries.insertRecipe("id-1", "Recipe 1")
            database.recipesQueries.insertRecipe("id-2", "Recipe 2")
            database.recipesQueries.insertRecipe("id-3", "Recipe 3")
        }
        delay(100)

        val state = store.state.value
        assertEquals(3, state.items.size)
    }
}
