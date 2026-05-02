package com.timothymarias.cookingapp.shared

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.timothymarias.cookingapp.shared.presentation.app.AppRoot
import com.timothymarias.cookingapp.shared.presentation.app.AppState
import com.timothymarias.cookingapp.shared.presentation.ingredient.IngredientStore
import com.timothymarias.cookingapp.shared.presentation.recipe.RecipeStore
import com.timothymarias.cookingapp.shared.presentation.unit.UnitStore
import org.koin.mp.KoinPlatform.getKoin

@Composable
fun App() {
    val koin = remember { getKoin() }
    val recipeStore = remember { koin.get<RecipeStore>() }
    val ingredientStore = remember { koin.get<IngredientStore>() }
    val unitStore = remember { koin.get<UnitStore>() }

    var appState by remember { mutableStateOf(AppState()) }

    MaterialTheme {
        AppRoot(
            appState = appState,
            onScreenSelected = { screen ->
                appState = appState.copy(currentScreen = screen)
            },
            recipeStore = recipeStore,
            ingredientStore = ingredientStore,
            unitStore = unitStore
        )
    }
}
