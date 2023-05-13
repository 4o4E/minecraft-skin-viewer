package top.e404.skin.server.plugin

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

fun Application.negotiation() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = false
        })
    }
}
