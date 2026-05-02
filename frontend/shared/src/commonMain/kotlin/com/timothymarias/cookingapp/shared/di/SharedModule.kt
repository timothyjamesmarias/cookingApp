package com.timothymarias.cookingapp.shared.di

import com.timothymarias.cookingapp.shared.data.local.BuildConfig
import com.timothymarias.cookingapp.shared.data.local.DatabaseDriverFactory
import com.timothymarias.cookingapp.shared.data.local.DatabaseSeeder
import com.timothymarias.cookingapp.shared.data.local.DriverConfig
import com.timothymarias.cookingapp.shared.data.local.createDatabase
import com.timothymarias.cookingapp.shared.data.repository.ingredient.DbIngredientRepository
import com.timothymarias.cookingapp.shared.data.repository.ingredient.IngredientRepository
import com.timothymarias.cookingapp.shared.data.repository.quantity.DbQuantityRepository
import com.timothymarias.cookingapp.shared.data.repository.quantity.QuantityRepository
import com.timothymarias.cookingapp.shared.data.repository.recipe.DbRecipeRepository
import com.timothymarias.cookingapp.shared.data.repository.recipe.RecipeRepository
import com.timothymarias.cookingapp.shared.data.repository.unit.DbUnitRepository
import com.timothymarias.cookingapp.shared.data.repository.unit.UnitRepository
import com.timothymarias.cookingapp.shared.presentation.ingredient.IngredientStore
import com.timothymarias.cookingapp.shared.presentation.recipe.RecipeStore
import com.timothymarias.cookingapp.shared.presentation.unit.UnitStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.dsl.module

fun sharedModule(driverConfig: DriverConfig = DriverConfig()) = module {
    single {
        val driver = DatabaseDriverFactory(driverConfig).createDriver()
        createDatabase(driver)
    }
    single<RecipeRepository> { DbRecipeRepository(get()) }
    single<IngredientRepository> { DbIngredientRepository(get()) }
    single<UnitRepository> { DbUnitRepository(get()) }
    single<QuantityRepository> { DbQuantityRepository(get()) }
    factory { RecipeStore(get(), get()) }
    factory { IngredientStore(get()) }
    factory { UnitStore(get()) }
}

fun initSeeding(koin: org.koin.core.Koin) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    scope.launch {
        val database = koin.get<com.timothymarias.cookingapp.shared.db.CookingDatabase>()
        if (BuildConfig.isDebug) {
            DatabaseSeeder.seedDevelopment(
                database = database,
                recipeRepository = koin.get(),
                ingredientRepository = koin.get(),
                unitRepository = koin.get()
            )
        } else {
            DatabaseSeeder.seedProduction(
                database = database,
                unitRepository = koin.get()
            )
        }
    }
}
