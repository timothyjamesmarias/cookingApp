package com.timothymarias.cookingapp.shared.data.repository.unit

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.timothymarias.cookingapp.shared.db.CookingDatabase
import com.timothymarias.cookingapp.domain.model.MeasurementType
import com.timothymarias.cookingapp.domain.model.Unit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.uuid.UUID
import kotlinx.uuid.generateUUID

/**
 * SQLDelight-backed repository for Unit persistence.
 * Handles CRUD operations and filtering by measurement type.
 */
class DbUnitRepository(
    private val db: CookingDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : UnitRepository {

    // Read - Reactive
    override fun watchAll(): Flow<List<Unit>> =
        db.unitsQueries.selectAll()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toUnit() } }

    override fun watchById(localId: String): Flow<Unit?> =
        db.unitsQueries.selectById(localId)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { row -> row?.toUnit() }

    override fun watchByMeasurementType(type: MeasurementType): Flow<List<Unit>> =
        db.unitsQueries.selectByMeasurementType(type.name)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toUnit() } }

    // Read - Suspend
    override suspend fun getAll(): List<Unit> =
        db.unitsQueries.selectAll()
            .executeAsList()
            .map { it.toUnit() }

    override suspend fun getById(localId: String): Unit? =
        db.unitsQueries.selectById(localId)
            .executeAsOneOrNull()
            ?.toUnit()

    override suspend fun getByMeasurementType(type: MeasurementType): List<Unit> =
        db.unitsQueries.selectByMeasurementType(type.name)
            .executeAsList()
            .map { it.toUnit() }

    override suspend fun searchByName(query: String): List<Unit> =
        db.unitsQueries.searchByName(query)
            .executeAsList()
            .map { it.toUnit() }

    // Write
    override suspend fun create(unit: Unit): Unit {
        val newLocalId = unit.localId.takeIf { it.isNotBlank() } ?: UUID.generateUUID().toString()
        db.unitsQueries.insertUnit(
            local_id = newLocalId,
            name = unit.name,
            symbol = unit.symbol,
            measurement_type = unit.measurementType.name,
            base_conversion_factor = unit.baseConversionFactor
        )
        return unit.copy(localId = newLocalId)
    }

    override suspend fun update(unit: Unit): Unit {
        db.unitsQueries.updateUnit(
            name = unit.name,
            symbol = unit.symbol,
            measurement_type = unit.measurementType.name,
            base_conversion_factor = unit.baseConversionFactor,
            local_id = unit.localId
        )
        return unit
    }

    override suspend fun delete(localId: String) {
        db.unitsQueries.deleteById(localId)
    }

    // Helper to convert SQLDelight generated row to domain model
    private fun com.timothymarias.cookingapp.shared.db.migrations.Units.toUnit() = Unit(
        localId = local_id,
        name = name,
        symbol = symbol,
        measurementType = MeasurementType.valueOf(measurement_type),
        baseConversionFactor = base_conversion_factor
    )
}
