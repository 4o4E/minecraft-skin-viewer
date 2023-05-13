package top.e404.skin.server.plugin

import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*

fun Application.logging() {
    install(CallLogging) {
        format { call ->
            val status = call.response.status()?.value
            val path = call.request.path()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            "[$status] [$httpMethod] $path $userAgent"
        }
    }
}
