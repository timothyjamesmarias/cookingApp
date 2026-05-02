package com.timothymarias.cookingapp.db

import org.jetbrains.exposed.sql.Table

object Recipes : Table("recipes") {
    val id = long("id").autoIncrement()
    val localId = text("local_id").uniqueIndex()
    val name = varchar("name", 255)
    override val primaryKey = PrimaryKey(id)
}

object Ingredients : Table("ingredients") {
    val id = long("id").autoIncrement()
    val localId = text("local_id").uniqueIndex()
    val name = varchar("name", 255)
    override val primaryKey = PrimaryKey(id)
}

object RecipeIngredients : Table("recipe_ingredients") {
    val recipeId = long("recipe_id").references(Recipes.id)
    val ingredientId = long("ingredient_id").references(Ingredients.id)
    val quantityId = long("quantity_id").references(Quantities.id).nullable()
    override val primaryKey = PrimaryKey(ingredientId, recipeId)
}

object Units : Table("units") {
    val id = long("id").autoIncrement()
    val localId = text("local_id").uniqueIndex()
    val name = varchar("name", 255)
    val symbol = varchar("symbol", 50)
    val measurementType = varchar("measurement_type", 50)
    val baseConversionFactor = double("base_conversion_factor")
    override val primaryKey = PrimaryKey(id)
}

object Quantities : Table("quantities") {
    val id = long("id").autoIncrement()
    val localId = text("local_id").uniqueIndex()
    val amount = double("amount")
    val unitId = long("unit_id").references(Units.id)
    override val primaryKey = PrimaryKey(id)
}
