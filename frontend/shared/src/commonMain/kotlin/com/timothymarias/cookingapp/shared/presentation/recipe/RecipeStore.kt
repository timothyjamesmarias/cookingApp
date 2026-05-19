package com.timothymarias.cookingapp.shared.presentation.recipe

import com.timothymarias.cookingapp.shared.data.repository.quantity.QuantityRepository
import com.timothymarias.cookingapp.shared.data.repository.recipe.IngredientWithQuantity
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
            is RecipeAction.Create -> handleCreate(action.name)
            is RecipeAction.Rename -> handleRename(action.id, action.name)
            is RecipeAction.Delete -> handleDelete(action.id)
            is RecipeAction.QueryChanged -> _state.update { it.copy(query = action.name) }
            RecipeAction.Load -> { /* handled by init collector */ }

            // Recipe detail
            is RecipeAction.ViewRecipeDetail -> handleViewDetail(action.id, editMode = false)
            is RecipeAction.ViewRecipeDetailInEditMode -> handleViewDetail(action.id, editMode = true)
            RecipeAction.CloseRecipeDetail -> _state.update {
                it.copy(selectedRecipeId = null, assignedIngredientIds = emptySet(), isEditMode = false)
            }
            RecipeAction.EnterEditMode -> _state.update { it.copy(isEditMode = true) }
            RecipeAction.ExitEditMode -> _state.update { it.copy(isEditMode = false) }

            // Ingredient assignment
            is RecipeAction.ManageIngredientsOpen -> handleManageIngredientsOpen(action.id)
            RecipeAction.EditClose -> _state.update { it.copy(managingIngredientsId = null) }
            is RecipeAction.AssignIngredient -> handleAssignIngredient(action.recipeId, action.ingredientId)
            is RecipeAction.RemoveIngredient -> handleRemoveIngredient(action.recipeId, action.ingredientId)

            // Quantity editing
            is RecipeAction.OpenQuantityEditor -> _state.update { it.copy(editingQuantityIngredientId = action.ingredientId) }
            RecipeAction.CloseQuantityEditor -> _state.update { it.copy(editingQuantityIngredientId = null) }
            is RecipeAction.SaveQuantity -> handleSaveQuantity(action)
            is RecipeAction.RemoveQuantity -> handleRemoveQuantity(action)
        }
    }

    private fun handleCreate(name: String) = scope.launch(ioDispatcher) {
        _state.update { it.copy(isSaving = true, error = null) }
        try {
            repo.create(Recipe(localId = "", name = name.trim()))
            _state.update { it.copy(isSaving = false) }
        } catch (e: Exception) {
            _state.update { it.copy(isSaving = false, error = e.message) }
        }
    }

    private fun handleRename(id: String, name: String) = scope.launch(ioDispatcher) {
        try {
            repo.updateName(id, name.trim())
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message) }
        }
    }

    private fun handleDelete(id: String) = scope.launch(ioDispatcher) {
        try {
            repo.delete(id)
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message) }
        }
    }

    private fun handleViewDetail(recipeId: String, editMode: Boolean) = scope.launch(ioDispatcher) {
        val ingredientsWithQuantities = repo.getIngredientsWithQuantities(recipeId)
        val assignedIds = ingredientsWithQuantities.map { it.ingredientId }.toSet()
        val quantities = buildQuantityMap(ingredientsWithQuantities)
        _state.update {
            it.copy(
                selectedRecipeId = recipeId,
                assignedIngredientIds = assignedIds,
                ingredientQuantities = quantities,
                isEditMode = editMode
            )
        }
    }

    private fun handleManageIngredientsOpen(recipeId: String) = scope.launch(ioDispatcher) {
        val assigned = repo.getIngredients(recipeId)
        val assignedIds = assigned.map { ingredient -> ingredient.localId }.toSet()
        _state.update { it.copy(managingIngredientsId = recipeId, assignedIngredientIds = assignedIds) }
    }

    private fun handleAssignIngredient(recipeId: String, ingredientId: String) = scope.launch(ioDispatcher) {
        try {
            repo.assignIngredient(recipeId, ingredientId)
            _state.update { s -> s.copy(assignedIngredientIds = s.assignedIngredientIds + ingredientId) }
        } catch (_: Exception) { }
    }

    private fun handleRemoveIngredient(recipeId: String, ingredientId: String) = scope.launch(ioDispatcher) {
        try {
            repo.removeIngredient(recipeId, ingredientId)
            _state.update { s -> s.copy(assignedIngredientIds = s.assignedIngredientIds - ingredientId) }
        } catch (_: Exception) { }
    }

    private fun handleSaveQuantity(action: RecipeAction.SaveQuantity) = scope.launch(ioDispatcher) {
        try {
            val quantity = Quantity(localId = "", amount = action.amount, unitId = action.unitId)
            val savedQuantity = quantityRepo.create(quantity)
            repo.updateIngredientQuantity(action.recipeId, action.ingredientId, savedQuantity.localId)

            val quantities = reloadQuantities(action.recipeId)
            _state.update { it.copy(ingredientQuantities = quantities, editingQuantityIngredientId = null) }
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message) }
        }
    }

    private fun handleRemoveQuantity(action: RecipeAction.RemoveQuantity) = scope.launch(ioDispatcher) {
        try {
            repo.updateIngredientQuantity(action.recipeId, action.ingredientId, quantityId = null)

            val quantities = reloadQuantities(action.recipeId)
            _state.update { it.copy(ingredientQuantities = quantities, editingQuantityIngredientId = null) }
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message) }
        }
    }

    private suspend fun reloadQuantities(recipeId: String): Map<String, QuantityInfo?> {
        val ingredientsWithQuantities = repo.getIngredientsWithQuantities(recipeId)
        return buildQuantityMap(ingredientsWithQuantities)
    }

    fun close() { scope.cancel() }
}

fun buildQuantityMap(ingredients: List<IngredientWithQuantity>): Map<String, QuantityInfo?> =
    ingredients.associate { ing ->
        val info = if (ing.amount != null && ing.unitId != null) {
            QuantityInfo(amount = ing.amount, unitId = ing.unitId)
        } else null
        ing.ingredientId to info
    }
