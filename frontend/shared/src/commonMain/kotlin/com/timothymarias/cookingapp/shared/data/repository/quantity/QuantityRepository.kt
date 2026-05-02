package com.timothymarias.cookingapp.shared.data.repository.quantity

import com.timothymarias.cookingapp.domain.model.Quantity
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction for accessing quantities from the local database (SQLDelight-backed).
 * Provides CRUD operations and filtering by unit.
 */
interface QuantityRepository {
    // Read - Reactive
    fun watchAll(): Flow<List<Quantity>>
    fun watchById(localId: String): Flow<Quantity?>

    // Read - Suspend
    suspend fun getAll(): List<Quantity>
    suspend fun getById(localId: String): Quantity?
    suspend fun getByUnitId(unitId: String): List<Quantity>

    // Write
    suspend fun create(quantity: Quantity): Quantity
    suspend fun update(quantity: Quantity): Quantity
    suspend fun delete(localId: String)
}
