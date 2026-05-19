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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.timothymarias.cookingapp.domain.model.Ingredient
import com.timothymarias.cookingapp.shared.presentation.components.SectionCard
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
                    recipeName = recipe.name,
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
                SectionCard(title = "Steps") {
                    EmptyHint("No steps added yet")
                }
            }

            item {
                SectionCard(title = "Tags") {
                    EmptyHint("No tags added yet")
                }
            }
        }
    }

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
    recipeName: String,
    isEditMode: Boolean,
    onUpdate: (String) -> Unit
) {
    SectionCard(title = "Recipe Details") {
        if (isEditMode) {
            OutlinedTextField(
                value = recipeName,
                onValueChange = onUpdate,
                label = { Text("Recipe Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            return@SectionCard
        }

        Text(
            text = recipeName,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth()
        )
    }
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

    val addButton: @Composable (() -> Unit)? = if (isEditMode) {
        {
            IconButton(onClick = {
                recipeStore.dispatch(RecipeAction.ManageIngredientsOpen(recipeId))
                showAssignDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Ingredient")
            }
        }
    } else null

    SectionCard(title = "Ingredients", headerAction = addButton) {
        IngredientsList(
            recipeState = recipeState,
            ingredientItems = ingredientItems,
            unitItems = unitState.items,
            isEditMode = isEditMode,
            onEditQuantity = { recipeStore.dispatch(RecipeAction.OpenQuantityEditor(it)) },
            onRemove = { recipeStore.dispatch(RecipeAction.RemoveIngredient(recipeId, it)) }
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
    recipeState: RecipeState,
    ingredientItems: List<Ingredient>,
    unitItems: List<UnitModel>,
    isEditMode: Boolean,
    onEditQuantity: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    if (recipeState.assignedIngredientIds.isEmpty()) {
        EmptyHint("No ingredients added yet")
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
private fun EmptyHint(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        fontStyle = FontStyle.Italic,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
