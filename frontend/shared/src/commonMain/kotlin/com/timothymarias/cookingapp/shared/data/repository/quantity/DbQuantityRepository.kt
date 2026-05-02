package com.timothymarias.cookingapp.shared.data.repository.quantity

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.timothymarias.cookingapp.shared.db.CookingDatabase
import com.timothymarias.cookingapp.domain.model.Quantity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.uuid.UUID
import kotlinx.uuid.generateUUID

/**
 * SQLDelight-backed repository for Quantity persistence.
 * Handles CRUD operations and filtering by unit.
 */
class DbQuantityRepository(
    private val db: CookingDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : QuantityRepository {

    // Read - Reactive
    override fun watchAll(): Flow<List<Quantity>> =
        db.quantitiesQueries.selectAll()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toQuantity() } }

    override fun watchById(localId: String): Flow<Quantity?> =
        db.quantitiesQueries.selectById(localId)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { row -> row?.toQuantity() }

    // Read - Suspend
    override suspend fun getAll(): List<Quantity> =
        db.quantitiesQueries.selectAll()
            .executeAsList()
            .map { it.toQuantity() }

    override suspend fun getById(localId: String): Quantity? =
        db.quantitiesQueries.selectById(localId)
            .executeAsOneOrNull()
            ?.toQuantity()

    override suspend fun getByUnitId(unitId: String): List<Quantity> =
        db.quantitiesQueries.selectByUnitId(unitId)
            .executeAsList()
            .map { it.toQuantity() }

    // Write
    override suspend fun create(quantity: Quantity): Quantity {
        val newLocalId = quantity.localId.takeIf { it.isNotBlank() } ?: UUID.generateUUID().toString()
        db.quantitiesQueries.insertQuantity(
            local_id = newLocalId,
            amount = quantity.amount,
            unit_id = quantity.unitId
        )
        return quantity.copy(localId = newLocalId)
    }

    override suspend fun update(quantity: Quantity): Quantity {
        db.quantitiesQueries.updateQuantity(
            amount = quantity.amount,
            unit_id = quantity.unitId,
            local_id = quantity.localId
        )
        return quantity
    }

    override suspend fun delete(localId: String) {
        db.quantitiesQueries.deleteById(localId)
    }

    // Helper to convert SQLDelight generated row to domain model
    private fun com.timothymarias.cookingapp.shared.db.migrations.Quantities.toQuantity() = Quantity(
        localId = local_id,
        amount = amount,
        unitId = unit_id
    )
}
