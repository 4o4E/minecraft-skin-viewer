package top.e404.skin.client

import kotlinx.serialization.json.Json

internal object ClientJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
    }
}
