package top.e404.skin.server.plugin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.skia.*
import top.e404.skin.server.Skin
import top.e404.skin.server.service.TavoloSkinRenderer
import top.e404.tavolo.util.*

private const val DEFAULT_BG_COLOR = 0xFF1F1B1D.toInt()

fun Application.routing() = routing {
    get("/render/{type}/{content}/{position}") {
        val data = when (call.parameters["type"]!!.lowercase()) {
            "id" -> Skin.getById(call.parameters["content"]!!)
            "name" -> Skin.getByName(call.parameters["content"]!!)
            else -> {
                call.respond(HttpStatusCode.BadRequest, "type must be 'id' or 'name'")
                return@get
            }
        }
        if (data == null) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        val skinBytes = data.skinBytes

        val parameters = call.request.queryParameters
        val bg = parameters["bg"]?.asColor() ?: DEFAULT_BG_COLOR
        val light = parameters["light"]?.asColor()
        when (call.parameters["position"]!!.lowercase()) {
            "sneak" -> {
                val slim = parameters["slim"]?.toBoolean() ?: parameters["t"]?.toBoolean() ?: data.slim
                val bytes = TavoloSkinRenderer.renderSneak(
                    bytes = skinBytes,
                    slim = slim,
                    backgroundColor = bg,
                    lightColor = light,
                    headScale = parameters["head"]?.toDoubleOrNull() ?: 1.0,
                    duration = parameters["duration"]?.toIntOrNull() ?: 40
                )
                call.respondBytes(bytes, ContentType.Image.GIF)
            }

            "sk" -> {
                val slim = parameters["slim"]?.toBoolean() ?: parameters["t"]?.toBoolean() ?: data.slim
                val bytes = TavoloSkinRenderer.renderSkin(
                    bytes = skinBytes,
                    slim = slim,
                    backgroundColor = bg,
                    lightColor = light,
                    headScale = parameters["head"]?.toDoubleOrNull() ?: 1.0
                )
                call.respondBytes(bytes, ContentType.Image.PNG)
            }

            "dsk" -> {
                val slim = parameters["slim"]?.toBoolean() ?: parameters["t"]?.toBoolean() ?: data.slim
                val bytes = TavoloSkinRenderer.renderSkinRotate(
                    bytes = skinBytes,
                    slim = slim,
                    backgroundColor = bg,
                    frameCount = parameters["x"]?.toIntOrNull() ?: 20,
                    pitchAmplitude = parameters["y"]?.toIntOrNull() ?: 20,
                    lightColor = light,
                    headScale = parameters["head"]?.toDoubleOrNull() ?: 1.0,
                    duration = parameters["duration"]?.toIntOrNull() ?: 40
                )
                call.respondBytes(bytes, ContentType.Image.GIF)
            }

            "head" -> {
                val bytes = TavoloSkinRenderer.renderHead(
                    bytes = skinBytes,
                    backgroundColor = bg,
                    lightColor = light
                )
                call.respondBytes(bytes, ContentType.Image.PNG)
            }

            "dhead" -> {
                val bytes = TavoloSkinRenderer.renderHeadRotate(
                    bytes = skinBytes,
                    backgroundColor = bg,
                    frameCount = parameters["x"]?.toIntOrNull() ?: 20,
                    pitchAmplitude = parameters["y"]?.toIntOrNull() ?: 20,
                    lightColor = light,
                    duration = parameters["duration"]?.toIntOrNull() ?: 40
                )
                call.respondBytes(bytes, ContentType.Image.GIF)
            }

            "homo" -> {
                val slim = parameters["slim"]?.toBoolean() ?: parameters["t"]?.toBoolean() ?: data.slim
                val bytes = TavoloSkinRenderer.renderHomo(
                    bytes = skinBytes,
                    slim = slim,
                    backgroundColor = bg,
                    lightColor = light,
                    headScale = parameters["head"]?.toDoubleOrNull() ?: 1.0
                )
                call.respondBytes(bytes, ContentType.Image.PNG)
            }

            else -> call.respond(HttpStatusCode.NotFound)
        }
    }

    get("/refresh/{type}/{content}") {
        val success = when (call.parameters["type"]!!.lowercase()) {
            "id" -> Skin.refreshById(call.parameters["content"]!!)
            "name" -> Skin.refreshByName(call.parameters["content"]!!)
            else -> {
                call.respond(HttpStatusCode.BadRequest, "type must be 'id' or 'name'")
                return@get
            }
        }
        call.respond(if (success) HttpStatusCode.OK else HttpStatusCode.NotFound)
    }

    get("/data/{type}/{content}") {
        val data = when (call.parameters["type"]!!.lowercase()) {
            "id" -> Skin.getById(call.parameters["content"]!!)
            "name" -> Skin.getByName(call.parameters["content"]!!)
            else -> {
                call.respond(HttpStatusCode.BadRequest, "type must be 'id' or 'name'")
                return@get
            }
        }
        if (data == null) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        call.respond(data)
    }

    get("/face/{type}/{content}") {
        val data = when (call.parameters["type"]!!.lowercase()) {
            "id" -> Skin.getById(call.parameters["content"]!!)
            "name" -> Skin.getByName(call.parameters["content"]!!)
            else -> {
                call.respond(HttpStatusCode.BadRequest, "type must be 'id' or 'name'")
                return@get
            }
        }
        if (data == null) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        val image = Image.makeFromEncoded(data.skinBytes)
        val layer1 = image.sub(8, 8, 8, 8)
        val layer2 = image.sub(40, 8, 8, 8)
        val parameters = call.request.queryParameters
        val bg = parameters["bg"]?.asColor() ?: 0
        val scale = parameters["scale"]?.toIntOrNull() ?: 5
        val margin = parameters["margin"]?.toIntOrNull() ?: 40
        val size = 64 * scale + 2 * margin
        val result = Surface.makeRasterN32Premul(size, size).withCanvas {
            drawRect(Rect.makeWH(size.toFloat(), size.toFloat()), Paint().apply { color = bg })
            drawImage(layer1.resize(-700 * scale, -700 * scale, true), margin + 4F * scale, margin + 4F * scale)
            drawImage(layer2.resize(-800 * scale, -800 * scale, true), margin.toFloat(), margin.toFloat())
        }
        call.respondBytes(result.bytes(format = EncodedImageFormat.PNG), ContentType.Image.PNG)
    }
}
