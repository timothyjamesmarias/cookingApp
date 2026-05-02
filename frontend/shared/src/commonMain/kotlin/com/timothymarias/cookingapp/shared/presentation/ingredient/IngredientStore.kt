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
            // Switch between watchAll and watchByQuery based on query state
            _state
                .map { it.query }
                .transformLatest { query ->
                    // Only debounce non-blank queries (actual searches)
                    // Blank queries (initial load, clear search) emit immediately
                    if (query.isBlank()) {
                        emit(query)
                    } else {
                        delay(300) // Debounce search queries
                        emit(query)
                    }
                }
                .flatMapLatest { query ->
                    if (query.isBlank()) {
                        repo.watchAll()
                    } else {
                        repo.watchByQuery(query)
                    }
                }
                .collect { list ->
                    _state.update { it.copy(items = list, isLoading = false) }
                }
        }
    }

    fun dispatch(action: IngredientAction) {
        when (action) {
            is IngredientAction.Create -> scope.launch(ioDispatcher) {
                _state.update { it.copy(isSaving = true, error = null) }
                runCatching { repo.create(Ingredient(localId = "", name = action.name.trim())) }
                    .onFailure { e -> _state.update { it.copy(isSaving = false, error = e.message) } }
                    .onSuccess { _state.update { it.copy(isSaving = false) } }
            }
            is IngredientAction.Rename -> scope.launch(ioDispatcher) {
                runCatching { repo.updateName(action.id, action.name.trim()) }
                    .onFailure { e -> _state.update { it.copy(error = e.message) } }
            }
            is IngredientAction.Delete -> scope.launch(ioDispatcher) {
                runCatching { repo.delete(action.id) }
                    .onFailure { e -> _state.update { it.copy(error = e.message) } }
            }
            is IngredientAction.EditOpen -> _state.update { s ->
                val current = s.items.firstOrNull { it.localId == action.id }
                s.copy(editingId = action.id, editName = current?.name ?: "")
            }
            IngredientAction.EditClose -> _state.update { it.copy(editingId = null, editName = "") }
            is IngredientAction.QueryChanged -> _state.update { it.copy(query = action.name) }
            IngredientAction.Load -> { /* already handled by init collector */ }
        }
    }

    fun close() { scope.cancel() }
}