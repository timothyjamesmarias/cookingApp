package com.timothymarias.cookingapp.shared.data.repository.unit

import com.timothymarias.cookingapp.domain.model.MeasurementType
import com.timothymarias.cookingapp.domain.model.Unit
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction for accessing units from the local database (SQLDelight-backed).
 * Provides CRUD operations and filtering by measurement type.
 */
interface UnitRepository {
    // Read - Reactive
    fun watchAll(): Flow<List<Unit>>
    fun watchById(localId: String): Flow<Unit?>
    fun watchByMeasurementType(type: MeasurementType): Flow<List<Unit>>

    // Read - Suspend
    suspend fun getAll(): List<Unit>
    suspend fun getById(localId: String): Unit?
    suspend fun getByMeasurementType(type: MeasurementType): List<Unit>
    suspend fun searchByName(query: String): List<Unit>

    // Write
    suspend fun create(unit: Unit): Unit
    suspend fun update(unit: Unit): Unit
    suspend fun delete(localId: String)
}
