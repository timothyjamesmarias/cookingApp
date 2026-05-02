package com.timothymarias.cookingapp.shared.presentation.recipe.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.timothymarias.cookingapp.domain.model.Recipe

@Composable
fun RecipeRow(
    recipe: Recipe,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onManageIngredients: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = recipe.name,
                style = MaterialTheme.typography.titleLarge
            )
        }
        Row {
            IconButton(onClick = onManageIngredients) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Recipe")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Recipe")
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}