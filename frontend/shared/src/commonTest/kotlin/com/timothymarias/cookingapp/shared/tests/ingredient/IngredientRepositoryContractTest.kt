package com.timothymarias.cookingapp.shared.tests.ingredient

import com.timothymarias.cookingapp.shared.data.repository.ingredient.IngredientRepository
import com.timothymarias.cookingapp.domain.model.Ingredient
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * Contract tests for IngredientRepository CRUD behavior, expressed as skeletons.
 * These are intentionally @Ignore-d until the CRUD methods are implemented
 * and a concrete repository is provided in platform-specific tests.
 */
class IngredientRepositoryContractTest {

    private lateinit var repo: IngredientRepository // Will be provided by platform-specific setup later

    @Test
    @Ignore
    fun create_then_watchById_emits_new_ingredient() {
        // Given
        val ing = Ingredient(localId = "new-id", name = "Garlic")
        // When
        // runBlocking { repo.create(ing) }
        // Then
        // runTest { repo.watchById(ing.localId).test { assertEquals("Garlic", awaitItem()?.name) } }
    }

    @Test
    @Ignore
    fun updateName_reflects_in_watchers() {
        // Create, then update name, observers should see the change
    }

    @Test
    @Ignore
    fun delete_removes_from_watchAll_and_watchById() {
        // After delete, watchById should emit null and watchAll should not contain the ingredient
    }
}
