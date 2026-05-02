package com.timothymarias.cookingapp.shared.presentation.ingredient

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.timothymarias.cookingapp.shared.data.repository.ingredient.DbIngredientRepository
import com.timothymarias.cookingapp.shared.db.CookingDatabase
import com.timothymarias.cookingapp.domain.model.Ingredient
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
class IngredientStoreTest {
    private lateinit var store: IngredientStore
    private lateinit var database: CookingDatabase
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookingDatabase.Schema.create(driver)
        database = CookingDatabase(driver)
        val repo = DbIngredientRepository(database, Dispatchers.Default)
        store = IngredientStore(repo, testDispatcher)
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
    fun `adding ingredient to database updates store state`() = runBlocking {
        // Wait for initial load
        delay(100)

        // Add an ingredient to the database
        database.ingredientsQueries.insertIngredient("test-id", "Test Ingredient")

        // Wait for the flow to propagate
        delay(100)

        val state = store.state.value
        assertEquals(1, state.items.size)
        assertEquals("Test Ingredient", state.items[0].name)
    }

    @Test
    fun `Create action adds ingredient to state`() = runBlocking {
        // Wait for initial load
        delay(100)

        store.dispatch(IngredientAction.Create("New Ingredient"))

        // Wait for the action to complete
        delay(200)

        val state = store.state.value
        assertEquals(1, state.items.size)
        assertEquals("New Ingredient", state.items[0].name)
    }

    @Test
    fun `Rename action updates ingredient in state`() = runBlocking {
        // Wait for initial load
        delay(100)

        // First create an ingredient
        database.ingredientsQueries.insertIngredient("test-id", "Original Name")
        delay(100)

        val beforeRename = store.state.value
        assertEquals(1, beforeRename.items.size)
        assertEquals("Original Name", beforeRename.items[0].name)

        store.dispatch(IngredientAction.Rename(id = "test-id", name = "Updated Name"))
        delay(100)

        val afterRename = store.state.value
        assertEquals(1, afterRename.items.size)
        assertEquals("Updated Name", afterRename.items[0].name)
    }

    @Test
    fun `Delete action removes ingredient from state`() = runBlocking {
        // Wait for initial load
        delay(100)

        // First create an ingredient
        database.ingredientsQueries.insertIngredient("test-id", "To Delete")
        delay(100)

        val beforeDelete = store.state.value
        assertEquals(1, beforeDelete.items.size)

        store.dispatch(IngredientAction.Delete("test-id"))
        delay(100)

        val afterDelete = store.state.value
        assertEquals(0, afterDelete.items.size)
    }

    @Test
    fun `multiple ingredients are handled correctly`() = runBlocking {
        // Wait for initial load
        delay(100)

        database.ingredientsQueries.transaction {
            database.ingredientsQueries.insertIngredient("id-1", "Ingredient 1")
            database.ingredientsQueries.insertIngredient("id-2", "Ingredient 2")
            database.ingredientsQueries.insertIngredient("id-3", "Ingredient 3")
        }
        delay(100)

        val state = store.state.value
        assertEquals(3, state.items.size)
    }

    @Test
    fun `QueryChanged action updates query field`() = runBlocking {
        // Wait for initial load
        delay(100)

        val initialState = store.state.value
        assertEquals("", initialState.query)

        store.dispatch(IngredientAction.QueryChanged("ato"))
        delay(50)

        val updatedState = store.state.value
        assertEquals("ato", updatedState.query)
    }

    @Test
    fun `QueryChanged with empty query updates query field`() = runBlocking {
        // Wait for initial load
        delay(100)

        store.dispatch(IngredientAction.QueryChanged("test"))
        delay(50)

        val withQuery = store.state.value
        assertEquals("test", withQuery.query)

        store.dispatch(IngredientAction.QueryChanged(""))
        delay(50)

        val clearedQuery = store.state.value
        assertEquals("", clearedQuery.query)
    }
}
