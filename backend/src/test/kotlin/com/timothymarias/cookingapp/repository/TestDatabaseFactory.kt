package com.timothymarias.cookingapp.repository

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.testcontainers.containers.PostgreSQLContainer

object TestDatabaseFactory {
    private val postgres = PostgreSQLContainer("postgres:latest").apply {
        withDatabaseName("cooking_test")
        withUsername("test")
        withPassword("test")
    }

    fun init() {
        if (!postgres.isRunning) postgres.start()

        val dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 4
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        })

        Flyway.configure().dataSource(dataSource).cleanDisabled(false).load().apply {
            clean()
            migrate()
        }
        Database.connect(dataSource)
    }
}
