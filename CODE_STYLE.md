# Code Style Guide

This document defines the code style for this project. The goal is **readable control flow** above all else. A reader should be able to trace what happens, in what order, without mentally unwinding nested lambdas or tracking implicit receivers.

The bias is always toward **simplicity**. If the "clever" version and the "boring" version both work, pick the boring one. Kotlin gives you many ways to write something — most of them make the code harder to follow. Resist the urge to find the most elegant expression. Write code that a tired person can read.

A secondary but equally important goal: **testability**. Small, pure functions with clear inputs and outputs are easy to test. Large methods with interleaved state mutations and side effects are not. Every rule in this document pushes code toward small, deterministic pieces — and that's not a coincidence. If you can't write a simple unit test for a piece of logic, it's a sign the logic needs to be extracted and simplified.

---

## 1. Early Returns Over Else Branches

Prefer early returns (or `return@label`) to eliminate else branches. Each condition should handle its case and exit, so the reader never has to hold two branches in their head at once.

**Before:**
```kotlin
// RecipeDetailScreen.kt — nested if/else for ingredient text
val ingredientText = if (quantityInfo != null && unit != null) {
    "* ${quantityInfo.amount} ${unit.symbol} ${it.name}"
} else {
    "* ${it.name}"
}
```

**After:**
```kotlin
fun formatIngredientText(name: String, quantityInfo: QuantityInfo?, unit: UnitModel?): String {
    if (quantityInfo == null || unit == null) return "* $name"
    return "* ${quantityInfo.amount} ${unit.symbol} $name"
}
```

The same principle applies inside `when` branches, composable functions, and lambdas:

**Before:**
```kotlin
onCheckedChange = { checked ->
    if (checked) {
        onRecipeAction(RecipeAction.AssignIngredient(recipeId, ing.localId))
    } else {
        onRecipeAction(RecipeAction.RemoveIngredient(recipeId, ing.localId))
    }
}
```

**After:**
```kotlin
onCheckedChange = { checked ->
    val action = if (checked) RecipeAction.AssignIngredient(recipeId, ing.localId)
                 else RecipeAction.RemoveIngredient(recipeId, ing.localId)
    onRecipeAction(action)
}
```

Here the `if/else` is a single expression that computes a value — not a branching control flow. That's fine. The rule is about *statements* that branch into two blocks of side effects.

---

## 2. Extract Functions Instead of Nesting

When a block of logic is doing a distinct job, pull it into a named function. This is especially important inside `when` branches and composable lambdas, where inline logic gets deeply nested fast.

**Before:**
```kotlin
// RecipeStore.kt — inline quantity mapping repeated 4 times
is RecipeAction.ViewRecipeDetail -> scope.launch(ioDispatcher) {
    val ingredientsWithQuantities = repo.getIngredientsWithQuantities(action.id)
    val assignedIds = ingredientsWithQuantities.map { it.ingredientId }.toSet()
    val quantities = ingredientsWithQuantities.associate { ing ->
        ing.ingredientId to if (ing.amount != null && ing.unitId != null) {
            QuantityInfo(amount = ing.amount, unitId = ing.unitId)
        } else null
    }
    _state.update {
        it.copy(
            selectedRecipeId = action.id,
            assignedIngredientIds = assignedIds,
            ingredientQuantities = quantities,
            isEditMode = false
        )
    }
}

// Then the exact same block again for ViewRecipeDetailInEditMode,
// and the same associate block again in SaveQuantity and RemoveQuantity
```

**After:**
```kotlin
private fun buildQuantityMap(
    ingredients: List<IngredientWithQuantity>
): Map<String, QuantityInfo?> =
    ingredients.associate { ing ->
        val info = if (ing.amount != null && ing.unitId != null) {
            QuantityInfo(amount = ing.amount, unitId = ing.unitId)
        } else null
        ing.ingredientId to info
    }

private suspend fun loadRecipeDetail(recipeId: String, editMode: Boolean) {
    val ingredientsWithQuantities = repo.getIngredientsWithQuantities(recipeId)
    val assignedIds = ingredientsWithQuantities.map { it.ingredientId }.toSet()
    val quantities = buildQuantityMap(ingredientsWithQuantities)
    _state.update {
        it.copy(
            selectedRecipeId = recipeId,
            assignedIngredientIds = assignedIds,
            ingredientQuantities = quantities,
            isEditMode = editMode
        )
    }
}

// In dispatch():
is RecipeAction.ViewRecipeDetail -> scope.launch(ioDispatcher) {
    loadRecipeDetail(action.id, editMode = false)
}
is RecipeAction.ViewRecipeDetailInEditMode -> scope.launch(ioDispatcher) {
    loadRecipeDetail(action.id, editMode = true)
}
```

---

## 3. Reduce Derived State — Compute, Don't Store

If a value can be derived from other state, compute it rather than storing and syncing it separately. This eliminates entire categories of bugs where stored state drifts out of sync.

**Before:**
```kotlin
// Separate isValid expression, then re-derived in onClick
val isValid = amountText.toDoubleOrNull() != null &&
              amountText.toDoubleOrNull()?.let { it > 0 } == true &&
              selectedUnitId != null

// ... 80 lines later ...
Button(
    onClick = {
        val amount = amountText.toDoubleOrNull()
        val unitId = selectedUnitId
        if (amount != null && unitId != null) {  // Different validation!
            onSave(amount, unitId)
        }
    },
    enabled = isValid
)
```

**After:**
```kotlin
fun parseValidAmount(text: String): Double? {
    val value = text.toDoubleOrNull() ?: return null
    if (value <= 0) return null
    return value
}

// Single source of truth
val parsedAmount = parseValidAmount(amountText)
val isValid = parsedAmount != null && selectedUnitId != null

Button(
    onClick = {
        val amount = parsedAmount ?: return@Button
        val unitId = selectedUnitId ?: return@Button
        onSave(amount, unitId)
        onDismiss()
    },
    enabled = isValid
)
```

One parse function, one validation check, one place to get it wrong. The `onClick` uses the same parsed values instead of re-parsing.

---

## 4. One Scope Function Deep, Max

Never chain or nest scope functions (`let`, `also`, `apply`, `run`). One level is the limit. If you need a second, extract a variable or a function.

**Before:**
```kotlin
// RecipeDetailScreen.kt — double ?.let nesting
recipeState.editingQuantityIngredientId?.let { ingredientId ->
    val ingredient = ingredientState.items.firstOrNull { it.localId == ingredientId }
    ingredient?.let {
        EditIngredientQuantityDialog(
            ingredientName = ingredient.name,
            // ...
        )
    }
}
```

**After:**
```kotlin
val editingIngredientId = recipeState.editingQuantityIngredientId ?: return
val ingredient = ingredientState.items.firstOrNull { it.localId == editingIngredientId } ?: return

EditIngredientQuantityDialog(
    ingredientName = ingredient.name,
    // ...
)
```

Flat. Each line does one thing. The control flow reads top-to-bottom.

---

## 5. Name Lambda Parameters

Never use implicit `it` when the lambda is more than a single expression, or when there's any ambiguity about what `it` refers to. Always use named parameters.

**Before:**
```kotlin
// RecipeDetailScreen.kt — nested `it` refers to different things
ingredient?.let {
    val quantityInfo = recipeState.ingredientQuantities[ingredientId]
    val unit = quantityInfo?.let { info ->
        unitState.items.firstOrNull { it.localId == info.unitId }  // which `it`?
    }
    ListItem(
        headlineContent = { Text(it.name) },  // which `it`?
    )
}
```

**After:**
```kotlin
val quantityInfo = recipeState.ingredientQuantities[ingredientId]
val unit = unitState.items.firstOrNull { unit -> unit.localId == quantityInfo?.unitId }
ListItem(
    headlineContent = { Text(ingredient.name) },
)
```

---

## 6. Use try/catch for Branching Error Handling

`runCatching` with `.onSuccess`/`.onFailure` chains look clean but obscure control flow. Use them only for simple one-liners. When you need different behavior on success vs failure, use try/catch — it's explicit about what happens in each case.

**Before:**
```kotlin
// RecipeStore.kt — runCatching chain with state updates in both branches
is RecipeAction.Create -> scope.launch(ioDispatcher) {
    _state.update { it.copy(isSaving = true, error = null) }
    runCatching { repo.create(Recipe(localId = "", name = action.name.trim())) }
        .onFailure { e -> _state.update { it.copy(isSaving = false, error = e.message) } }
        .onSuccess { _state.update { it.copy(isSaving = false) } }
}
```

**After:**
```kotlin
is RecipeAction.Create -> scope.launch(ioDispatcher) {
    _state.update { it.copy(isSaving = true, error = null) }
    try {
        repo.create(Recipe(localId = "", name = action.name.trim()))
        _state.update { it.copy(isSaving = false) }
    } catch (e: Exception) {
        _state.update { it.copy(isSaving = false, error = e.message) }
    }
}
```

The branching is explicit. You can see at a glance that `isSaving = false` happens in both paths.

---

## 7. Hoist State Collection in Composables

Call `collectAsState()` once at the composable function level. Never call it inside loops, conditionals, or callbacks. Collecting inside a `forEach` creates a new subscription per iteration and causes unnecessary recompositions.

**Before:**
```kotlin
// RecipeDetailScreen.kt — collectAsState inside forEach loop
Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    recipeState.assignedIngredientIds.forEach { ingredientId ->
        val ingredientState by ingredientStore.state.collectAsState()  // per-item!
        val ingredient = ingredientState.items.firstOrNull { it.localId == ingredientId }
        ingredient?.let {
            // ...
        }
    }
}
```

**After:**
```kotlin
// Collected once, at the top of the composable
val ingredientState by ingredientStore.state.collectAsState()

Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    recipeState.assignedIngredientIds.forEach { ingredientId ->
        val ingredient = ingredientState.items.firstOrNull { it.localId == ingredientId }
            ?: return@forEach
        // ...
    }
}
```

---

## 8. Break Up Flow Chains with Named Intermediates

Flow operator chains (`map → transformLatest → flatMapLatest → collect`) are hard to follow when they span 20+ lines. Break them at meaningful boundaries with named variables or extracted functions that describe what the data *is* at that point.

**Before:**
```kotlin
// IngredientStore.kt — long flow chain in init
init {
    scope.launch(ioDispatcher) {
        _state
            .map { it.query }
            .transformLatest { query ->
                if (query.isBlank()) {
                    emit(query)
                } else {
                    delay(300)
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
```

**After:**
```kotlin
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
```

Each intermediate value has a name that tells you what it represents. The flow reads as a sequence of transformations instead of a single nested expression.

---

## 9. Write Small, Testable Pieces

If a block of logic mixes data transformation with side effects (network calls, state mutations, UI), split them apart. Pure functions that take inputs and return outputs are trivially testable. Methods that interleave repo calls, mapping, and state updates in one lambda are not testable at all — you end up needing integration-level setup just to verify a data transformation.

The litmus test: **can you test this logic with just inputs and an assertEquals?** If not, extract the pure part.

**Before:**
```kotlin
// RecipeStore.kt — SaveQuantity mixes repo calls, data mapping, and state updates
is RecipeAction.SaveQuantity -> scope.launch(ioDispatcher) {
    runCatching {
        val quantity = Quantity(localId = "", amount = action.amount, unitId = action.unitId)
        val savedQuantity = quantityRepo.create(quantity)
        repo.updateIngredientQuantity(
            recipeId = action.recipeId,
            ingredientId = action.ingredientId,
            quantityId = savedQuantity.localId
        )

        // Reload and transform — tangled with the repo calls above
        val ingredientsWithQuantities = repo.getIngredientsWithQuantities(action.recipeId)
        val quantities = ingredientsWithQuantities.associate { ing ->
            ing.ingredientId to if (ing.amount != null && ing.unitId != null) {
                QuantityInfo(amount = ing.amount, unitId = ing.unitId)
            } else null
        }
        quantities
    }.onSuccess { quantities ->
        _state.update { it.copy(ingredientQuantities = quantities, editingQuantityIngredientId = null) }
    }.onFailure { e ->
        _state.update { it.copy(error = e.message) }
    }
}
```

Testing `buildQuantityMap` requires mocking two repositories, launching a coroutine, and inspecting state — all to verify a pure data transformation.

**After:**
```kotlin
// Pure function — testable with just a list and an assertEquals
fun buildQuantityMap(
    ingredients: List<IngredientWithQuantity>
): Map<String, QuantityInfo?> =
    ingredients.associate { ing ->
        val info = if (ing.amount != null && ing.unitId != null) {
            QuantityInfo(amount = ing.amount, unitId = ing.unitId)
        } else null
        ing.ingredientId to info
    }

// Store method — only orchestrates side effects
is RecipeAction.SaveQuantity -> scope.launch(ioDispatcher) {
    try {
        val quantity = Quantity(localId = "", amount = action.amount, unitId = action.unitId)
        val savedQuantity = quantityRepo.create(quantity)
        repo.updateIngredientQuantity(action.recipeId, action.ingredientId, savedQuantity.localId)

        val ingredientsWithQuantities = repo.getIngredientsWithQuantities(action.recipeId)
        val quantities = buildQuantityMap(ingredientsWithQuantities)
        _state.update { it.copy(ingredientQuantities = quantities, editingQuantityIngredientId = null) }
    } catch (e: Exception) {
        _state.update { it.copy(error = e.message) }
    }
}
```

Now `buildQuantityMap` can be tested in one line:
```kotlin
@Test
fun `buildQuantityMap returns null for incomplete quantities`() {
    val input = listOf(
        IngredientWithQuantity(ingredientId = "1", ingredientName = "Salt", quantityId = null, amount = null, unitId = null)
    )
    val result = buildQuantityMap(input)
    assertEquals(mapOf("1" to null), result)
}
```

The same principle applies everywhere: validation logic, text formatting, state derivation. If it's a computation, it should be a function you can call in a test without any framework setup.

---

## 10. Don't Duplicate View/Edit Variants — Parameterize

When a "view" and "edit" version of a composable share 80%+ of their structure, write one composable with a parameter. Duplicating large blocks means bugs get fixed in one copy and not the other.

**Before:**
```kotlin
// RecipeDetailScreen.kt — two 40-line composables that differ by ~5 lines
@Composable
private fun IngredientsViewList(/* ... */) {
    // 40 lines: empty state, forEach, ingredient lookup, quantity lookup, display
}

@Composable
private fun IngredientsEditList(/* ... */) {
    // 40 lines: same empty state, same forEach, same lookups, but with edit icons
}
```

**After:**
```kotlin
@Composable
private fun IngredientsList(
    recipeId: String,
    recipeState: RecipeState,
    ingredients: List<Ingredient>,
    units: List<UnitModel>,
    isEditMode: Boolean,
    onEditQuantity: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    if (recipeState.assignedIngredientIds.isEmpty()) {
        EmptyStateText("No ingredients added yet")
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        recipeState.assignedIngredientIds.forEach { ingredientId ->
            val ingredient = ingredients.firstOrNull { it.localId == ingredientId }
                ?: return@forEach
            val quantityText = formatQuantity(ingredientId, recipeState, units)

            if (!isEditMode) {
                Text("* $quantityText${ingredient.name}", style = MaterialTheme.typography.bodyLarge)
                return@forEach
            }

            ListItem(
                headlineContent = { Text(ingredient.name) },
                supportingContent = { Text(quantityText.ifEmpty { "Tap edit to set quantity" }) },
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
```

---

## Summary

| Rule | One-liner |
|---|---|
| Early returns | Handle the edge case and exit. Don't nest the happy path inside an else. |
| Extract functions | If it has a name in your head, give it a name in the code. |
| Derive, don't store | Compute values from state instead of syncing separate copies. |
| One scope function deep | No `?.let { ?.let { } }`. Extract a variable. |
| Name lambda params | `it` is only clear when the lambda is trivial. |
| try/catch over runCatching chains | When both branches have side effects, be explicit. |
| Hoist state collection | `collectAsState()` at the top of the composable, once. |
| Name flow intermediates | Break chains at meaningful boundaries. |
| Small, testable pieces | If you can't test it with inputs and assertEquals, extract the pure part. |
| Parameterize, don't duplicate | One composable with a flag beats two copies. |

The guiding principle: **if you have to read it twice to understand it, rewrite it.**
