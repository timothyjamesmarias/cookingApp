package com.timothymarias.cookingapp.shared.presentation.ingredient

import com.timothymarias.cookingapp.shared.data.repository.ingredient.IngredientRepository
import com.timothymarias.cookingapp.domain.model.Ingredient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class IngredientStore(
    private val repo: IngredientRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(IngredientState(isLoading = true))
    val state: StateFlow<IngredientState> = _state.asStateFlow()

    init {
        scope.launch(ioDispatcher) {
            val queryChanges = _state.map { it.query }

            val debouncedQuery = queryChanges.transformLatest { query ->
                if (query.isNotBlank()) delay(300)
                emit(query)
            }

            debouncedQuery
                .flatMapLatest { query ->
                    if (query.isBlank()) repo.watchAll()
                    else repo.watchByQuery(query)
                }
                .collect { list ->
                    _state.update { it.copy(items = list, isLoading = false) }
                }
        }
    }

    fun dispatch(action: IngredientAction) {
        when (action) {
            is IngredientAction.Create -> handleCreate(action.name)
            is IngredientAction.Rename -> handleRename(action.id, action.name)
            is IngredientAction.Delete -> handleDelete(action.id)
            is IngredientAction.EditOpen -> _state.update { s ->
                val current = s.items.firstOrNull { it.localId == action.id }
                s.copy(editingId = action.id, editName = current?.name ?: "")
            }
            IngredientAction.EditClose -> _state.update { it.copy(editingId = null, editName = "") }
            is IngredientAction.QueryChanged -> _state.update { it.copy(query = action.name) }
            IngredientAction.Load -> { /* handled by init collector */ }
        }
    }

    private fun handleCreate(name: String) = scope.launch(ioDispatcher) {
        _state.update { it.copy(isSaving = true, error = null) }
        try {
            repo.create(Ingredient(localId = "", name = name.trim()))
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

    fun close() { scope.cancel() }
}
