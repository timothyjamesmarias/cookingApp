package com.timothymarias.cookingapp.shared.presentation.recipe.list

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
import com.timothymarias.cookingapp.shared.presentation.recipe.RecipeAction
import com.timothymarias.cookingapp.shared.presentation.recipe.RecipeStore

@Composable
fun RecipeListScreen(store: RecipeStore) {
    val state by store.state.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Recipe")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            ListScaffold(
                items = state.items,
                isLoading = state.isLoading,
                error = state.error,
                emptyMessage = "No recipes found"
            ) { recipes ->
                items(recipes) { recipe ->
                    RecipeRow(
                        recipe = recipe,
                        onClick = { store.dispatch(RecipeAction.ViewRecipeDetail(recipe.localId)) },
                        onDelete = { store.dispatch(RecipeAction.Delete(recipe.localId)) },
                        onManageIngredients = { store.dispatch(RecipeAction.ViewRecipeDetailInEditMode(recipe.localId)) }
                    )
                }
            }
        }

        if (showCreateDialog) {
            NameInputDialog(
                title = "Create Recipe",
                confirmLabel = "Create",
                onConfirm = { name ->
                    store.dispatch(RecipeAction.Create(name))
                    showCreateDialog = false
                },
                onDismiss = { showCreateDialog = false }
            )
        }
    }
}
