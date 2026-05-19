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
import com.timothymarias.cookingapp.domain.model.Ingredient
import com.timothymarias.cookingapp.shared.presentation.recipe.QuantityInfo
import com.timothymarias.cookingapp.shared.presentation.recipe.RecipeAction
import com.timothymarias.cookingapp.shared.presentation.recipe.RecipeState
import com.timothymarias.cookingapp.shared.presentation.recipe.RecipeStore
import com.timothymarias.cookingapp.shared.presentation.ingredient.IngredientStore
import com.timothymarias.cookingapp.shared.presentation.ingredient.IngredientAction
import com.timothymarias.cookingapp.shared.presentation.recipe.dialogs.AssignIngredientsDialog
import com.timothymarias.cookingapp.shared.presentation.recipe.dialogs.EditIngredientQuantityDialog
import com.timothymarias.cookingapp.shared.presentation.unit.UnitStore
import com.timothymarias.cookingapp.shared.presentation.unit.UnitState
import com.timothymarias.cookingapp.domain.model.Unit as UnitModel

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
                    val action = if (isEditMode) RecipeAction.ExitEditMode else RecipeAction.EnterEditMode
                    recipeStore.dispatch(action)
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
            item {
                RecipeNameSection(
                    recipe = recipe,
                    isEditMode = isEditMode,
                    onUpdate = { recipeStore.dispatch(RecipeAction.Rename(recipe.localId, it)) }
                )
            }

            item {
                IngredientsSection(
                    recipeId = selectedRecipeId,
                    isEditMode = isEditMode,
                    recipeStore = recipeStore,
                    ingredientStore = ingredientStore,
                    recipeState = recipeState,
                    ingredientItems = ingredientState.items,
                    unitState = unitState
                )
            }

            item {
                PlaceholderSection(title = "Steps", emptyMessage = "No steps added yet", actionLabel = "Add Step", isEditMode = isEditMode)
            }

            item {
                PlaceholderSection(title = "Tags", emptyMessage = "No tags added yet", actionLabel = "Add Tag", isEditMode = isEditMode)
            }
        }
    }

    // Edit Quantity Dialog
    val editingIngredientId = recipeState.editingQuantityIngredientId ?: return
    val editingIngredient = ingredientState.items.firstOrNull { it.localId == editingIngredientId } ?: return

    EditIngredientQuantityDialog(
        recipeId = selectedRecipeId,
        ingredientId = editingIngredientId,
        ingredientName = editingIngredient.name,
        unitState = unitState,
        onSave = { amount, unitId ->
            recipeStore.dispatch(RecipeAction.SaveQuantity(selectedRecipeId, editingIngredientId, amount, unitId))
        },
        onClear = {
            recipeStore.dispatch(RecipeAction.RemoveQuantity(selectedRecipeId, editingIngredientId))
        },
        onDismiss = {
            recipeStore.dispatch(RecipeAction.CloseQuantityEditor)
        }
    )
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
                TextButton(onClick = onEditToggle) { Text("Done") }
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
            Text(text = "Recipe Details", style = MaterialTheme.typography.titleLarge)
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
    recipeState: RecipeState,
    ingredientItems: List<Ingredient>,
    unitState: UnitState
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

            IngredientsList(
                recipeId = recipeId,
                recipeState = recipeState,
                ingredientItems = ingredientItems,
                unitItems = unitState.items,
                isEditMode = isEditMode,
                onEditQuantity = { ingredientId ->
                    recipeStore.dispatch(RecipeAction.OpenQuantityEditor(ingredientId))
                },
                onRemove = { ingredientId ->
                    recipeStore.dispatch(RecipeAction.RemoveIngredient(recipeId, ingredientId))
                }
            )

            if (isEditMode && showAssignDialog) {
                val ingredientState by ingredientStore.state.collectAsState()
                AssignIngredientsDialog(
                    recipeId = recipeId,
                    ingredientState = ingredientState,
                    recipeState = recipeState,
                    onIngredientAction = { ingredientStore.dispatch(it) },
                    onRecipeAction = { recipeStore.dispatch(it) },
                    onDismiss = {
                        showAssignDialog = false
                        ingredientStore.dispatch(IngredientAction.QueryChanged(""))
                        recipeStore.dispatch(RecipeAction.EditClose)
                    }
                )
            }
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
        Text(text = "Ingredients", style = MaterialTheme.typography.titleLarge)

        if (isEditMode) {
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Ingredient")
            }
        }
    }
}

fun formatQuantityText(
    ingredientId: String,
    quantities: Map<String, QuantityInfo?>,
    units: List<UnitModel>
): String {
    val quantityInfo = quantities[ingredientId] ?: return ""
    val unit = units.firstOrNull { it.localId == quantityInfo.unitId } ?: return ""
    return "${quantityInfo.amount} ${unit.symbol} "
}

@Composable
private fun IngredientsList(
    recipeId: String,
    recipeState: RecipeState,
    ingredientItems: List<Ingredient>,
    unitItems: List<UnitModel>,
    isEditMode: Boolean,
    onEditQuantity: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
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
            val ingredient = ingredientItems.firstOrNull { it.localId == ingredientId }
                ?: return@forEach
            val quantityText = formatQuantityText(ingredientId, recipeState.ingredientQuantities, unitItems)

            if (!isEditMode) {
                Text(
                    text = "• ${quantityText}${ingredient.name}",
                    style = MaterialTheme.typography.bodyLarge
                )
                return@forEach
            }

            val supportingText = quantityText.ifEmpty { "Tap edit to set quantity" }
            ListItem(
                headlineContent = { Text(ingredient.name) },
                supportingContent = { Text(supportingText, style = MaterialTheme.typography.bodySmall) },
                trailingContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { onEditQuantity(ingredientId) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit Quantity")
                        }
                        IconButton(onClick = { onRemove(ingredientId) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove")
                        }
                    }
                }
            )
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
                Text(text = title, style = MaterialTheme.typography.titleLarge)

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
