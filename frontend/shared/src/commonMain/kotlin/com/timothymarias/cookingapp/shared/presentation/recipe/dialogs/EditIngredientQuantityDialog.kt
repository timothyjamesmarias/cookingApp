package com.timothymarias.cookingapp.shared.presentation.recipe.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.timothymarias.cookingapp.shared.presentation.components.UnitDropdown
import com.timothymarias.cookingapp.shared.presentation.unit.UnitState

fun parseValidAmount(text: String): Double? {
    val value = text.toDoubleOrNull() ?: return null
    if (value <= 0) return null
    return value
}

@Composable
fun EditIngredientQuantityDialog(
    recipeId: String,
    ingredientId: String,
    ingredientName: String,
    unitState: UnitState,
    onSave: (amount: Double, unitId: String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedUnitId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(unitState.items) {
        if (selectedUnitId == null && unitState.items.isNotEmpty()) {
            selectedUnitId = unitState.items.first().localId
        }
    }

    val selectedUnit = unitState.items.firstOrNull { it.localId == selectedUnitId }
    val parsedAmount = parseValidAmount(amountText)
    val isValid = parsedAmount != null && selectedUnitId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Quantity for $ingredientName") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    placeholder = { Text("e.g., 2.5") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = amountText.isNotEmpty() && amountText.toDoubleOrNull() == null
                )

                UnitDropdown(
                    units = unitState.items,
                    selectedUnit = selectedUnit,
                    onUnitSelected = { selectedUnitId = it.localId }
                )

                if (unitState.items.isEmpty()) {
                    Text(
                        text = "Loading units...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    onClear()
                    onDismiss()
                }) {
                    Text("Clear")
                }

                Button(
                    onClick = {
                        val amount = parsedAmount ?: return@Button
                        val unitId = selectedUnitId ?: return@Button
                        onSave(amount, unitId)
                        onDismiss()
                    },
                    enabled = isValid
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
