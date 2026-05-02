package com.timothymarias.cookingapp.repository

import com.timothymarias.cookingapp.domain.api.CreateIngredientRequest
import com.timothymarias.cookingapp.domain.api.UpdateIngredientRequest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IngredientRepositoryTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            TestDatabaseFactory.init()
        }
    }

    private val repo = IngredientRepository()

    @BeforeEach
    fun cleanDb() = runTest {
        repo.findAll().forEach { repo.delete(it.localId) }
    }

    @Test
    fun `create and find ingredient`() = runTest {
        val created = repo.create(CreateIngredientRequest(name = "Salt"))
        assertNotNull(created.localId)
        assertEquals("Salt", created.name)

        val found = repo.findByLocalId(created.localId)
        assertNotNull(found)
        assertEquals("Salt", found.name)
    }

    @Test
    fun `findAll returns saved ingredients`() = runTest {
        repo.create(CreateIngredientRequest(name = "Salt"))
        repo.create(CreateIngredientRequest(name = "Pepper"))

        val all = repo.findAll()
        assertEquals(2, all.size)
    }

    @Test
    fun `update ingredient name`() = runTest {
        val created = repo.create(CreateIngredientRequest(name = "Salt"))
        val updated = repo.update(created.localId, UpdateIngredientRequest(name = "Sea Salt"))
        assertNotNull(updated)
        assertEquals("Sea Salt", updated.name)
    }

    @Test
    fun `update non-existent ingredient returns null`() = runTest {
        val result = repo.update("non-existent", UpdateIngredientRequest(name = "Nope"))
        assertNull(result)
    }

    @Test
    fun `delete ingredient`() = runTest {
        val created = repo.create(CreateIngredientRequest(name = "ToDelete"))
        assertTrue(repo.delete(created.localId))
        assertNull(repo.findByLocalId(created.localId))
    }

    @Test
    fun `search by name`() = runTest {
        repo.create(CreateIngredientRequest(name = "Salt"))
        repo.create(CreateIngredientRequest(name = "Salted Butter"))
        repo.create(CreateIngredientRequest(name = "Pepper"))

        val results = repo.search("salt")
        assertEquals(2, results.size)
        assertTrue(results.all { "salt" in it.name.lowercase() })
    }
}
