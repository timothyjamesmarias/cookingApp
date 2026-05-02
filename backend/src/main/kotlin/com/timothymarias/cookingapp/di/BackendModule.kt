package com.timothymarias.cookingapp.di

import com.timothymarias.cookingapp.repository.IngredientRepository
import com.timothymarias.cookingapp.repository.RecipeRepository
import org.koin.dsl.module

val backendModule = module {
    single { RecipeRepository() }
    single { IngredientRepository() }
}
