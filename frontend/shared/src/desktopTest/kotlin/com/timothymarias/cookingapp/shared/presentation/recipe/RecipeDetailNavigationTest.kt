package com.timothymarias.cookingapp.shared.presentation.recipe

import com.timothymarias.cookingapp.shared.data.repository.recipe.IngredientWithQuantity
import com.timothymarias.cookingapp.shared.data.repository.recipe.RecipeRepository
import com.timothymarias.cookingapp.shared.data.repository.quantity.QuantityRepository
import com.timothymarias.cookingapp.domain.model.Ingredient
import com.timothymarias.cookingapp.domain.model.Recipe
import com.timothymarias.cookingapp.domain.model.Quantity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Tests for recipe detail navigation and edit mode state management.
 * Uses a fake repository to avoid database complexity and focus on state transitions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecipeDetailNavigationTest {
    private lateinit var fakeRepo: FakeRecipeRepository
    private lateinit var fakeQuantityRepo: FakeQuantityRepository
    private lateinit var store: RecipeStore
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        fakeRepo = FakeRecipeRepository()
        fakeQuantityRepo = FakeQuantityRepository()
        store = RecipeStore(fakeRepo, fakeQuantityRepo, testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        store.close()
    }

    @Test
    fun `ViewRecipeDetail sets selectedRecipeId and loads ingredients`() = runTest(testDispatcher) {
        // Given
        val recipeId = "recipe-123"
        val ingredients = listOf(
            Ingredient("ing-1", "Flour"),
            Ingredient("ing-2", "Sugar")
        )
        fakeRepo.setIngredients(recipeId, ingredients)

        // When
        store.dispatch(RecipeAction.ViewRecipeDetail(recipeId))
        advanceUntilIdle()

        // Then
        val state = store.state.value
        assertEquals(recipeId, state.selectedRecipeId)
        assertEquals(setOf("ing-1", "ing-2"), state.assignedIngredientIds)
        assertFalse(state.isEditMode)
    }

    @Test
    fun `ViewRecipeDetailInEditMode sets selectedRecipeId and edit mode`() = runTest(testDispatcher) {
        // Given
        val recipeId = "recipe-456"
        fakeRepo.setIngredients(recipeId, emptyList())

        // When
        store.dispatch(RecipeAction.ViewRecipeDetailInEditMode(recipeId))
        advanceUntilIdle()

        // Then
        val state = store.state.value
        assertEquals(recipeId, state.selectedRecipeId)
        assertTrue(state.isEditMode)
    }

    @Test
    fun `EnterEditMode toggles edit mode to true`() = runTest(testDispatcher) {
        // Given - already in detail view
        val recipeId = "recipe-789"
        fakeRepo.setIngredients(recipeId, emptyList())
        store.dispatch(RecipeAction.ViewRecipeDetail(recipeId))
        advanceUntilIdle()

        assertFalse(store.state.value.isEditMode)

        // When
        store.dispatch(RecipeAction.EnterEditMode)
        advanceUntilIdle()

        // Then
        assertTrue(store.state.value.isEditMode)
    }

    @Test
    fun `ExitEditMode toggles edit mode to false`() = runTest(testDispatcher) {
        // Given - in edit mode
        val recipeId = "recipe-101"
        fakeRepo.setIngredients(recipeId, emptyList())
        store.dispatch(RecipeAction.ViewRecipeDetailInEditMode(recipeId))
        advanceUntilIdle()

        assertTrue(store.state.value.isEditMode)

        // When
        store.dispatch(RecipeAction.ExitEditMode)
        advanceUntilIdle()

        // Then
        assertFalse(store.state.value.isEditMode)
    }

    @Test
    fun `CloseRecipeDetail clears selectedRecipeId and resets edit mode`() = runTest(testDispatcher) {
        // Given - in detail view with edit mode on
        val recipeId = "recipe-202"
        fakeRepo.setIngredients(recipeId, listOf(Ingredient("ing-1", "Flour")))
        store.dispatch(RecipeAction.ViewRecipeDetailInEditMode(recipeId))
        advanceUntilIdle()

        // When
        store.dispatch(RecipeAction.CloseRecipeDetail)
        advanceUntilIdle()

        // Then
        val state = store.state.value
        assertNull(state.selectedRecipeId)
        assertEquals(emptySet(), state.assignedIngredientIds)
        assertFalse(state.isEditMode)
    }

    @Test
    fun `REGRESSION - edit mode does not persist when navigating between recipes`() = runTest(testDispatcher) {
        // This is a regression test for the bug where edit mode would persist
        // when clicking on a different recipe after being in edit mode

        // Given - Recipe 1 in edit mode
        fakeRepo.setIngredients("recipe-1", listOf(Ingredient("ing-1", "Flour")))
        fakeRepo.setIngredients("recipe-2", listOf(Ingredient("ing-2", "Sugar")))

        store.dispatch(RecipeAction.ViewRecipeDetailInEditMode("recipe-1"))
        advanceUntilIdle()

        assertEquals("recipe-1", store.state.value.selectedRecipeId)
        assertTrue(store.state.value.isEditMode, "Should be in edit mode for recipe 1")

        // When - Navigate to recipe 2 via normal click (not edit icon)
        store.dispatch(RecipeAction.ViewRecipeDetail("recipe-2"))
        advanceUntilIdle()

        // Then - Should be on recipe 2 in VIEW mode (NOT edit mode)
        val state = store.state.value
        assertEquals("recipe-2", state.selectedRecipeId)
        assertEquals(setOf("ing-2"), state.assignedIngredientIds)
        assertFalse(state.isEditMode, "Edit mode should NOT persist across recipes")
    }

    @Test
    fun `edit mode persists when managing ingredients`() = runTest(testDispatcher) {
        // This tests that opening AssignIngredientsDialog doesn't exit edit mode

        // Given - In edit mode
        fakeRepo.setIngredients("recipe-1", emptyList())

        store.dispatch(RecipeAction.ViewRecipeDetailInEditMode("recipe-1"))
        advanceUntilIdle()
        assertTrue(store.state.value.isEditMode)

        // When - Open ingredient dialog
        store.dispatch(RecipeAction.ManageIngredientsOpen("recipe-1"))
        advanceUntilIdle()

        // Then - Still in edit mode
        assertTrue(store.state.value.isEditMode)
        assertEquals("recipe-1", store.state.value.managingIngredientsId)

        // When - Close dialog
        store.dispatch(RecipeAction.EditClose)
        advanceUntilIdle()

        // Then - Still in edit mode (EditClose only closes dialog, not edit mode)
        assertTrue(store.state.value.isEditMode)
        assertNull(store.state.value.managingIngredientsId)
    }

    @Test
    fun `full edit flow - open in edit, toggle to view, back to list`() = runTest(testDispatcher) {
        // Given
        fakeRepo.setIngredients("recipe-1", emptyList())

        // When - User clicks edit icon from list
        store.dispatch(RecipeAction.ViewRecipeDetailInEditMode("recipe-1"))
        advanceUntilIdle()

        // Then - Detail screen opens in edit mode
        with(store.state.value) {
            assertEquals("recipe-1", selectedRecipeId)
            assertTrue(isEditMode)
        }

        // When - User clicks Done
        store.dispatch(RecipeAction.ExitEditMode)
        advanceUntilIdle()

        // Then - Still on detail but view mode
        with(store.state.value) {
            assertEquals("recipe-1", selectedRecipeId)
            assertFalse(isEditMode)
        }

        // When - User clicks back
        store.dispatch(RecipeAction.CloseRecipeDetail)
        advanceUntilIdle()

        // Then - Back to list
        with(store.state.value) {
            assertNull(selectedRecipeId)
            assertFalse(isEditMode)
        }
    }

    @Test
    fun `clicking recipe name opens in view mode`() = runTest(testDispatcher) {
        // Given
        fakeRepo.setIngredients("recipe-1", emptyList())

        // When - User clicks recipe name (not edit icon)
        store.dispatch(RecipeAction.ViewRecipeDetail("recipe-1"))
        advanceUntilIdle()

        // Then - Opens in view mode (not edit)
        assertEquals("recipe-1", store.state.value.selectedRecipeId)
        assertFalse(store.state.value.isEditMode)
    }

    @Test
    fun `clicking edit icon opens in edit mode`() = runTest(testDispatcher) {
        // Given
        fakeRepo.setIngredients("recipe-1", emptyList())

        // When - User clicks edit icon from list
        store.dispatch(RecipeAction.ViewRecipeDetailInEditMode("recipe-1"))
        advanceUntilIdle()

        // Then - Opens in edit mode
        assertEquals("recipe-1", store.state.value.selectedRecipeId)
        assertTrue(store.state.value.isEditMode)
    }
}

/**
 * Fake repository for testing. Avoids database complexity and keeps tests fast.
 */
private class FakeRecipeRepository : RecipeRepository {
    private val ingredientsByRecipe = mutableMapOf<String, List<Ingredient>>()

    fun setIngredients(recipeId: String, ingredients: List<Ingredient>) {
        ingredientsByRecipe[recipeId] = ingredients
    }

    override fun watchAll(): Flow<List<Recipe>> = flowOf(emptyList())

    override fun watchById(localId: String): Flow<Recipe?> = flowOf(null)

    override suspend fun create(recipe: Recipe): Recipe = recipe

    override suspend fun updateName(localId: String, name: String): Recipe = Recipe(localId, name)

    override suspend fun delete(localId: String) {}

    override suspend fun assignIngredient(recipeId: String, ingredientId: String) {}

    override suspend fun removeIngredient(recipeId: String, ingredientId: String) {}

    override suspend fun getIngredients(localId: String): List<Ingredient> {
        return ingredientsByRecipe[localId] ?: emptyList()
    }

    override suspend fun isIngredientAssigned(recipeId: String, ingredientId: String): Boolean = false

    override suspend fun updateIngredientQuantity(recipeId: String, ingredientId: String, quantityId: String?) {}

    override suspend fun getIngredientsWithQuantities(recipeId: String): List<IngredientWithQuantity> {
        return ingredientsByRecipe[recipeId]?.map { ingredient ->
            IngredientWithQuantity(
                ingredientId = ingredient.localId,
                ingredientName = ingredient.name,
                quantityId = null,
                amount = null,
                unitId = null
            )
        } ?: emptyList()
    }
}

/**
 * Fake quantity repository for testing.
 */
private class FakeQuantityRepository : QuantityRepository {
    private val quantities = mutableMapOf<String, Quantity>()
    private var idCounter = 0

    override fun watchAll(): Flow<List<Quantity>> = flowOf(quantities.values.toList())

    override fun watchById(localId: String): Flow<Quantity?> = flowOf(quantities[localId])

    override suspend fun getAll(): List<Quantity> = quantities.values.toList()

    override suspend fun getById(localId: String): Quantity? = quantities[localId]

    override suspend fun getByUnitId(unitId: String): List<Quantity> {
        return quantities.values.filter { it.unitId == unitId }
    }

    override suspend fun create(quantity: Quantity): Quantity {
        val id = if (quantity.localId.isEmpty()) "quantity-${idCounter++}" else quantity.localId
        val saved = quantity.copy(localId = id)
        quantities[id] = saved
        return saved
    }

    override suspend fun update(quantity: Quantity): Quantity {
        quantities[quantity.localId] = quantity
        return quantity
    }

    override suspend fun delete(localId: String) {
        quantities.remove(localId)
    }
}
