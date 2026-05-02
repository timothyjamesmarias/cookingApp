package com.timothymarias.cookingapp.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a unit of measurement for ingredients.
 *
 * @property localId Client-generated UUID for offline-first support
 * @property name Display name (e.g., "gram", "cup", "tablespoon")
 * @property symbol Short representation (e.g., "g", "c", "tbsp")
 * @property measurementType Category: WEIGHT, VOLUME, or COUNT
 * @property baseConversionFactor Multiplier to convert to base unit within type
 *                                (e.g., 1000 for kg->g, 0.25 for cup->liter)
 *                                Base units: gram (WEIGHT), milliliter (VOLUME), 1 (COUNT)
 */
@Serializable
data class Unit(
    val localId: String,
    val name: String,
    val symbol: String,
    val measurementType: MeasurementType,
    val baseConversionFactor: Double
)
