package com.timothymarias.cookingapp

import com.timothymarias.cookingapp.db.DatabaseFactory
import com.timothymarias.cookingapp.di.backendModule
import com.timothymarias.cookingapp.routes.ingredientRoutes
import com.timothymarias.cookingapp.routes.recipeRoutes
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main() {
    embeddedServer(Netty, port = 8080) {
        configurePlugins()
        DatabaseFactory.init()
        configureRouting()
    }.start(wait = true)
}

fun Application.configurePlugins() {
    install(Koin) {
        slf4jLogger()
        modules(backendModule)
    }
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = false
        })
    }
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to (cause.message ?: "Bad request")))
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal server error"))
        }
    }
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
    }
    install(CallLogging)
}

fun Application.configureRouting() {
    routing {
        recipeRoutes()
        ingredientRoutes()
    }
}
