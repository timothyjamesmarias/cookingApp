package com.timothymarias.cookingapp.shared.presentation.recipe.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.timothymarias.cookingapp.shared.presentation.recipe.RecipeAction
import com.timothymarias.cookingapp.shared.presentation.recipe.RecipeState
import com.timothymarias.cookingapp.shared.presentation.recipe.RecipeStore
import com.timothymarias.cookingapp.shared.presentation.ingredient.IngredientStore
import com.timothymarias.cookingapp.shared.presentation.ingredient.IngredientAction
import com.timothymarias.cookingapp.shared.presentation.recipe.dialogs.AssignIngredientsDialog
import com.timothymarias.cookingapp.shared.presentation.recipe.dialogs.EditIngredientQuantityDialog
import com.timothymarias.cookingapp.shared.presentation.unit.UnitStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeStore: RecipeStore,
    ingredientStore: IngredientStore,
    unitStore: UnitStore
) {
    val recipeState by recipeStore.state.collectAsState()
    val unitState by unitStore.state.collectAsState()
    val ingredientState by ingredientStore.state.collectAsState()

    val selectedRecipeId = recipeState.selectedRecipeId ?: return
    val recipe = recipeState.items.firstOrNull { it.localId == selectedRecipeId } ?: return
    val isEditMode = recipeState.isEditMode

    Scaffold(
        topBar = {
            RecipeDetailTopBar(
                recipeName = recipe.name,
                isEditMode = isEditMode,
                onEditToggle = {
                    if (isEditMode) {
                        recipeStore.dispatch(RecipeAction.ExitEditMode)
                    } else {
                        recipeStore.dispatch(RecipeAction.EnterEditMode)
                    }
                },
                onBack = { recipeStore.dispatch(RecipeAction.CloseRecipeDetail) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Recipe Name Section
            item {
                RecipeNameSection(
                    recipe = recipe,
                    isEditMode = isEditMode,
                    onUpdate = { recipeStore.dispatch(RecipeAction.Rename(recipe.localId, it)) }
                )
            }

            // Ingredients Section
            item {
                IngredientsSection(
                    recipeId = selectedRecipeId,
                    isEditMode = isEditMode,
                    recipeStore = recipeStore,
                    ingredientStore = ingredientStore,
                    unitStore = unitStore,
                    recipeState = recipeState
                )
            }

            // Steps Placeholder Section
            item {
                PlaceholderSection(
                    title = "Steps",
                    emptyMessage = "No steps added yet",
                    actionLabel = "Add Step",
                    isEditMode = isEditMode
                )
            }

            // Tags Placeholder Section
            item {
                PlaceholderSection(
                    title = "Tags",
                    emptyMessage = "No tags added yet",
                    actionLabel = "Add Tag",
                    isEditMode = isEditMode
                )
            }
        }
    }

    // Edit Quantity Dialog
    recipeState.editingQuantityIngredientId?.let { ingredientId ->
        val ingredient = ingredientState.items.firstOrNull { it.localId == ingredientId }

        ingredient?.let {
            EditIngredientQuantityDialog(
                recipeId = selectedRecipeId,
                ingredientId = ingredientId,
                ingredientName = ingredient.name,
                unitState = unitState,
                onSave = { amount, unitId ->
                    recipeStore.dispatch(
                        RecipeAction.SaveQuantity(
                            recipeId = selectedRecipeId,
                            ingredientId = ingredientId,
                            amount = amount,
                            unitId = unitId
                        )
                    )
                },
                onClear = {
                    recipeStore.dispatch(
                        RecipeAction.RemoveQuantity(
                            recipeId = selectedRecipeId,
                            ingredientId = ingredientId
                        )
                    )
                },
                onDismiss = {
                    recipeStore.dispatch(RecipeAction.CloseQuantityEditor)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeDetailTopBar(
    recipeName: String,
    isEditMode: Boolean,
    onEditToggle: () -> Unit,
    onBack: () -> Unit
) {
    TopAppBar(
        title = { Text(recipeName) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            if (isEditMode) {
                TextButton(onClick = onEditToggle) {
                    Text("Done")
                }
            } else {
                IconButton(onClick = onEditToggle) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            }
        }
    )
}

@Composable
private fun RecipeNameSection(
    recipe: com.timothymarias.cookingapp.domain.model.Recipe,
    isEditMode: Boolean,
    onUpdate: (String) -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Recipe Details",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isEditMode) {
                RecipeNameEdit(name = recipe.name, onUpdate = onUpdate)
                return@Column
            }

            RecipeNameView(name = recipe.name)
        }
    }
}

@Composable
private fun RecipeNameView(name: String) {
    Text(
        text = name,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun RecipeNameEdit(name: String, onUpdate: (String) -> Unit) {
    OutlinedTextField(
        value = name,
        onValueChange = onUpdate,
        label = { Text("Recipe Name") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun IngredientsSection(
    recipeId: String,
    isEditMode: Boolean,
    recipeStore: RecipeStore,
    ingredientStore: IngredientStore,
    unitStore: UnitStore,
    recipeState: RecipeState
) {
    var showAssignDialog by remember { mutableStateOf(false) }

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            IngredientsSectionHeader(
                isEditMode = isEditMode,
                onAddClick = {
                    recipeStore.dispatch(RecipeAction.ManageIngredientsOpen(recipeId))
                    showAssignDialog = true
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isEditMode) {
                IngredientsEditList(
                    recipeId = recipeId,
                    recipeState = recipeState,
                    ingredientStore = ingredientStore,
                    unitStore = unitStore,
                    recipeStore = recipeStore
                )

                if (showAssignDialog) {
                    val ingredientState by ingredientStore.state.collectAsState()
                    AssignIngredientsDialog(
                        recipeId = recipeId,
                        ingredientState = ingredientState,
                        recipeState = recipeState,
                        onIngredientAction = { action ->
                            ingredientStore.dispatch(action)
                        },
                        onRecipeAction = { action ->
                            recipeStore.dispatch(action)
                        },
                        onDismiss = {
                            showAssignDialog = false
                            ingredientStore.dispatch(IngredientAction.QueryChanged(""))
                            recipeStore.dispatch(RecipeAction.EditClose)
                        }
                    )
                }
                return@Column
            }

            IngredientsViewList(
                recipeState = recipeState,
                ingredientStore = ingredientStore,
                unitStore = unitStore
            )
        }
    }
}

@Composable
private fun IngredientsSectionHeader(
    isEditMode: Boolean,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Ingredients",
            style = MaterialTheme.typography.titleLarge
        )

        if (isEditMode) {
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Ingredient")
            }
        }
    }
}

@Composable
private fun IngredientsViewList(
    recipeState: RecipeState,
    ingredientStore: IngredientStore,
    unitStore: UnitStore
) {
    val unitState by unitStore.state.collectAsState()

    if (recipeState.assignedIngredientIds.isEmpty()) {
        Text(
            text = "No ingredients added yet",
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        recipeState.assignedIngredientIds.forEach { ingredientId ->
            val ingredientState by ingredientStore.state.collectAsState()
            val ingredient = ingredientState.items.firstOrNull { it.localId == ingredientId }

            ingredient?.let {
                val quantityInfo = recipeState.ingredientQuantities[ingredientId]
                val unit = quantityInfo?.let { info ->
                    unitState.items.firstOrNull { unit -> unit.localId == info.unitId }
                }
                val ingredientText = if (quantityInfo != null && unit != null) {
                    "• ${quantityInfo.amount} ${unit.symbol} ${it.name}"
                } else {
                    "• ${it.name}"
                }

                Text(
                    text = ingredientText,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun IngredientsEditList(
    recipeId: String,
    recipeState: RecipeState,
    ingredientStore: IngredientStore,
    unitStore: UnitStore,
    recipeStore: RecipeStore
) {
    val unitState by unitStore.state.collectAsState()
    if (recipeState.assignedIngredientIds.isEmpty()) {
        Text(
            text = "No ingredients added yet",
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        recipeState.assignedIngredientIds.forEach { ingredientId ->
            val ingredientState by ingredientStore.state.collectAsState()
            val ingredient = ingredientState.items.firstOrNull { it.localId == ingredientId }

            ingredient?.let {
                val quantityInfo = recipeState.ingredientQuantities[ingredientId]
                val unit = quantityInfo?.let { info ->
                    unitState.items.firstOrNull { it.localId == info.unitId }
                }
                val supportingText = if (quantityInfo != null && unit != null) {
                    "${quantityInfo.amount} ${unit.symbol}"
                } else {
                    "Tap edit to set quantity"
                }

                ListItem(
                    headlineContent = { Text(it.name) },
                    supportingContent = { Text(supportingText, style = MaterialTheme.typography.bodySmall) },
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Edit quantity icon
                            IconButton(
                                onClick = {
                                    recipeStore.dispatch(RecipeAction.OpenQuantityEditor(ingredientId))
                                }
                            ) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Edit Quantity")
                            }
                            // Remove ingredient icon
                            IconButton(
                                onClick = {
                                    recipeStore.dispatch(RecipeAction.RemoveIngredient(recipeId, ingredientId))
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PlaceholderSection(
    title: String,
    emptyMessage: String,
    actionLabel: String,
    isEditMode: Boolean
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )

                if (isEditMode) {
                    OutlinedButton(
                        onClick = { /* TODO: Implement later */ },
                        enabled = false
                    ) {
                        Text(actionLabel)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}
