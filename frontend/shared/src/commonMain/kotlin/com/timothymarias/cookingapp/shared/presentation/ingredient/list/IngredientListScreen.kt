package com.timothymarias.cookingapp.shared.presentation.ingredient.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.timothymarias.cookingapp.shared.presentation.components.ListScaffold
import com.timothymarias.cookingapp.shared.presentation.components.NameInputDialog
import com.timothymarias.cookingapp.shared.presentation.ingredient.IngredientAction
import com.timothymarias.cookingapp.shared.presentation.ingredient.IngredientStore

@Composable
fun IngredientListScreen(store: IngredientStore) {
    val state by store.state.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Ingredient")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            ListScaffold(
                items = state.items,
                isLoading = state.isLoading,
                error = state.error,
                emptyMessage = "No ingredients found"
            ) { ingredients ->
                items(ingredients) { ingredient ->
                    IngredientRow(
                        ingredient = ingredient,
                        onClick = { store.dispatch(IngredientAction.EditOpen(ingredient.localId)) },
                        onDelete = { store.dispatch(IngredientAction.Delete(ingredient.localId)) }
                    )
                }
            }
        }

        if (showCreateDialog) {
            NameInputDialog(
                title = "Create Ingredient",
                confirmLabel = "Create",
                onConfirm = { name ->
                    store.dispatch(IngredientAction.Create(name))
                    showCreateDialog = false
                },
                onDismiss = { showCreateDialog = false }
            )
        }

        val editingId = state.editingId ?: return@Scaffold

        NameInputDialog(
            title = "Edit Ingredient",
            confirmLabel = "Save",
            initialValue = state.editName,
            onConfirm = { name ->
                store.dispatch(IngredientAction.Rename(editingId, name))
                store.dispatch(IngredientAction.EditClose)
            },
            onDismiss = { store.dispatch(IngredientAction.EditClose) }
        )
    }
}
