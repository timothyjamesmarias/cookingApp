package com.timothymarias.cookingapp.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a measured amount with a specific unit.
 *
 * @property localId Client-generated UUID for offline-first support
 * @property amount Numeric value (e.g., 2.5, 150, 0.25)
 * @property unitId Foreign key reference to the Unit this quantity uses
 */
@Serializable
data class Quantity(
    val localId: String,
    val amount: Double,
    val unitId: String
)
