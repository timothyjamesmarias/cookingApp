package com.timothymarias.cookingapp.shared.presentation.recipe

import com.timothymarias.cookingapp.shared.data.repository.quantity.QuantityRepository
import com.timothymarias.cookingapp.shared.data.repository.recipe.RecipeRepository
import com.timothymarias.cookingapp.domain.model.Quantity
import com.timothymarias.cookingapp.domain.model.Recipe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecipeStore(
    private val repo: RecipeRepository,
    private val quantityRepo: QuantityRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(RecipeState(isLoading = true))
    val state: StateFlow<RecipeState> = _state.asStateFlow()

    init {
        scope.launch(ioDispatcher) {
            repo.watchAll().collect { list ->
                _state.update { it.copy(items = list, isLoading = false) }
            }
        }
    }

    fun dispatch(action: RecipeAction) {
        when (action) {
            is RecipeAction.Create -> scope.launch(ioDispatcher) {
                _state.update { it.copy(isSaving = true, error = null) }
                runCatching { repo.create(Recipe(localId = "", name = action.name.trim())) }
                    .onFailure { e -> _state.update { it.copy(isSaving = false, error = e.message) } }
                    .onSuccess { _state.update { it.copy(isSaving = false) } }
            }
            is RecipeAction.Rename -> scope.launch(ioDispatcher) {
                runCatching { repo.updateName(action.id, action.name.trim()) }
                    .onFailure { e -> _state.update { it.copy(error = e.message) } }
            }
            is RecipeAction.Delete -> scope.launch(ioDispatcher) {
                runCatching { repo.delete(action.id) }
                    .onFailure { e -> _state.update { it.copy(error = e.message) } }
            }
            RecipeAction.EditClose -> _state.update { it.copy(managingIngredientsId = null) }
            RecipeAction.EnterEditMode -> _state.update { it.copy(isEditMode = true) }
            RecipeAction.ExitEditMode -> _state.update { it.copy(isEditMode = false) }
            is RecipeAction.QueryChanged -> _state.update { it.copy(query = action.name) }
            RecipeAction.Load -> { /* already handled by init collector */ }
            is RecipeAction.AssignIngredient -> scope.launch(ioDispatcher) {
                runCatching { repo.assignIngredient(action.recipeId, action.ingredientId) }
                    .onSuccess {
                        _state.update { s -> s.copy(assignedIngredientIds = s.assignedIngredientIds + action.ingredientId) }
                    }
            }
            is RecipeAction.RemoveIngredient -> scope.launch(ioDispatcher) {
                runCatching { repo.removeIngredient(action.recipeId, action.ingredientId) }
                    .onSuccess {
                        _state.update { s -> s.copy(assignedIngredientIds = s.assignedIngredientIds - action.ingredientId) }
                    }
            }
            is RecipeAction.ManageIngredientsOpen -> scope.launch(ioDispatcher) {
                val assigned = repo.getIngredients(action.id)
                _state.update { it.copy(managingIngredientsId = action.id, assignedIngredientIds = assigned.map { it.localId }.toSet()) }
            }
            is RecipeAction.ViewRecipeDetail -> scope.launch(ioDispatcher) {
                val ingredientsWithQuantities = repo.getIngredientsWithQuantities(action.id)
                val assignedIds = ingredientsWithQuantities.map { it.ingredientId }.toSet()
                val quantities = ingredientsWithQuantities.associate { ing ->
                    ing.ingredientId to if (ing.amount != null && ing.unitId != null) {
                        QuantityInfo(amount = ing.amount, unitId = ing.unitId)
                    } else null
                }
                _state.update {
                    it.copy(
                        selectedRecipeId = action.id,
                        assignedIngredientIds = assignedIds,
                        ingredientQuantities = quantities,
                        isEditMode = false
                    )
                }
            }
            is RecipeAction.ViewRecipeDetailInEditMode -> scope.launch(ioDispatcher) {
                val ingredientsWithQuantities = repo.getIngredientsWithQuantities(action.id)
                val assignedIds = ingredientsWithQuantities.map { it.ingredientId }.toSet()
                val quantities = ingredientsWithQuantities.associate { ing ->
                    ing.ingredientId to if (ing.amount != null && ing.unitId != null) {
                        QuantityInfo(amount = ing.amount, unitId = ing.unitId)
                    } else null
                }
                _state.update {
                    it.copy(
                        selectedRecipeId = action.id,
                        assignedIngredientIds = assignedIds,
                        ingredientQuantities = quantities,
                        isEditMode = true
                    )
                }
            }
            RecipeAction.CloseRecipeDetail -> _state.update { it.copy(selectedRecipeId = null, assignedIngredientIds = emptySet(), isEditMode = false) }
            // Quantity editing actions
            is RecipeAction.OpenQuantityEditor -> _state.update { it.copy(editingQuantityIngredientId = action.ingredientId) }
            RecipeAction.CloseQuantityEditor -> _state.update { it.copy(editingQuantityIngredientId = null) }
            is RecipeAction.SaveQuantity -> scope.launch(ioDispatcher) {
                runCatching {
                    // Create or update the quantity
                    val quantity = Quantity(
                        localId = "", // Will be generated by repository
                        amount = action.amount,
                        unitId = action.unitId
                    )
                    val savedQuantity = quantityRepo.create(quantity)

                    // Update recipe_ingredients to reference this quantity
                    repo.updateIngredientQuantity(
                        recipeId = action.recipeId,
                        ingredientId = action.ingredientId,
                        quantityId = savedQuantity.localId
                    )

                    // Reload quantities to refresh UI
                    val ingredientsWithQuantities = repo.getIngredientsWithQuantities(action.recipeId)
                    val quantities = ingredientsWithQuantities.associate { ing ->
                        ing.ingredientId to if (ing.amount != null && ing.unitId != null) {
                            QuantityInfo(amount = ing.amount, unitId = ing.unitId)
                        } else null
                    }
                    quantities
                }.onSuccess { quantities ->
                    _state.update {
                        it.copy(
                            ingredientQuantities = quantities,
                            editingQuantityIngredientId = null
                        )
                    }
                }.onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }
            }
            is RecipeAction.RemoveQuantity -> scope.launch(ioDispatcher) {
                runCatching {
                    // Set quantity_id to NULL in recipe_ingredients
                    repo.updateIngredientQuantity(
                        recipeId = action.recipeId,
                        ingredientId = action.ingredientId,
                        quantityId = null
                    )

                    // Reload quantities to refresh UI
                    val ingredientsWithQuantities = repo.getIngredientsWithQuantities(action.recipeId)
                    val quantities = ingredientsWithQuantities.associate { ing ->
                        ing.ingredientId to if (ing.amount != null && ing.unitId != null) {
                            QuantityInfo(amount = ing.amount, unitId = ing.unitId)
                        } else null
                    }
                    quantities
                }.onSuccess { quantities ->
                    _state.update {
                        it.copy(
                            ingredientQuantities = quantities,
                            editingQuantityIngredientId = null
                        )
                    }
                }.onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }
            }
        }
    }

    fun close() { scope.cancel() }
}