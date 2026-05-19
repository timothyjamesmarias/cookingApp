package com.timothymarias.cookingapp.shared.presentation.recipe.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.timothymarias.cookingapp.domain.model.Ingredient
import com.timothymarias.cookingapp.shared.presentation.components.SearchField
import com.timothymarias.cookingapp.shared.presentation.ingredient.IngredientAction
import com.timothymarias.cookingapp.shared.presentation.ingredient.IngredientState
import com.timothymarias.cookingapp.shared.presentation.recipe.RecipeAction
import com.timothymarias.cookingapp.shared.presentation.recipe.RecipeState

@Composable
fun AssignIngredientsDialog(
    recipeId: String,
    ingredientState: IngredientState,
    recipeState: RecipeState,
    onIngredientAction: (IngredientAction) -> Unit,
    onRecipeAction: (RecipeAction) -> Unit,
    onDismiss: () -> Unit
) {
    val hasQuery = ingredientState.query.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Ingredients") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                SearchField(
                    query = ingredientState.query,
                    onQueryChange = { onIngredientAction(IngredientAction.QueryChanged(it)) },
                    placeholder = "Search ingredients..."
                )

                Spacer(modifier = Modifier.height(8.dp))

                IngredientChecklistArea(
                    ingredients = ingredientState.items,
                    assignedIds = recipeState.assignedIngredientIds,
                    query = ingredientState.query,
                    hasQuery = hasQuery,
                    recipeId = recipeId,
                    onRecipeAction = onRecipeAction
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                CreateIngredientButton(
                    hasQuery = hasQuery,
                    query = ingredientState.query,
                    onIngredientAction = onIngredientAction
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun IngredientChecklistArea(
    ingredients: List<Ingredient>,
    assignedIds: Set<String>,
    query: String,
    hasQuery: Boolean,
    recipeId: String,
    onRecipeAction: (RecipeAction) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp, max = 400.dp),
        contentAlignment = Alignment.Center
    ) {
        if (ingredients.isEmpty()) {
            val message = if (hasQuery) "No ingredients found for \"$query\"" else "No ingredients yet."
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Box
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items = ingredients, key = { it.localId }) { ingredient ->
                IngredientChecklistItem(
                    ingredient = ingredient,
                    isAssigned = assignedIds.contains(ingredient.localId),
                    onToggle = { checked ->
                        val action = if (checked) RecipeAction.AssignIngredient(recipeId, ingredient.localId)
                                     else RecipeAction.RemoveIngredient(recipeId, ingredient.localId)
                        onRecipeAction(action)
                    }
                )
            }
        }
    }
}

@Composable
private fun IngredientChecklistItem(
    ingredient: Ingredient,
    isAssigned: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(ingredient.name)
        Checkbox(checked = isAssigned, onCheckedChange = onToggle)
    }
}

@Composable
private fun CreateIngredientButton(
    hasQuery: Boolean,
    query: String,
    onIngredientAction: (IngredientAction) -> Unit,
) {
    val buttonText = if (hasQuery) "Create \"$query\"" else "Create New Ingredient"

    OutlinedButton(
        onClick = {
            if (!hasQuery) return@OutlinedButton
            onIngredientAction(IngredientAction.Create(query))
            onIngredientAction(IngredientAction.QueryChanged(""))
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = hasQuery
    ) {
        Text(buttonText)
    }
}
