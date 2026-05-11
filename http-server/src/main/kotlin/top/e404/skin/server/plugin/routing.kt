package top.e404.skin.server.plugin

import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.skia.*
import top.e404.skin.server.Skin
import top.e404.skin.server.sql.pojo.SkinData
import top.e404.skin.server.service.RenderFileCache
import top.e404.skin.server.service.TavoloSkinRenderer
import top.e404.tavolo.util.*

private const val DEFAULT_BG_COLOR = 0xFF1F1B1D.toInt()
private const val RENDERER_ID = "tavolo-cpu-v1"

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
                val headScale = parameters["head"]?.toDoubleOrNull() ?: 1.0
                val duration = parameters["duration"]?.toIntOrNull() ?: 40
                call.respondCachedRender(data, "sneak", "gif", ContentType.Image.GIF, mapOf(
                    "slim" to slim,
                    "bg" to bg.hexColor(),
                    "light" to light?.hexColor(),
                    "head" to headScale,
                    "duration" to duration,
                    "width" to 600,
                    "height" to 900,
                    "aa" to 1,
                    "voxelOverlay" to true,
                )) {
                    TavoloSkinRenderer.renderSneak(
                        bytes = skinBytes,
                        slim = slim,
                        backgroundColor = bg,
                        lightColor = light,
                        headScale = headScale,
                        duration = duration
                    )
                }
            }

            "sk" -> {
                val slim = parameters["slim"]?.toBoolean() ?: parameters["t"]?.toBoolean() ?: data.slim
                val headScale = parameters["head"]?.toDoubleOrNull() ?: 1.0
                call.respondCachedRender(data, "sk", "png", ContentType.Image.PNG, mapOf(
                    "slim" to slim,
                    "bg" to bg.hexColor(),
                    "light" to light?.hexColor(),
                    "head" to headScale,
                    "width" to 600,
                    "height" to 900,
                    "aa" to 2,
                    "voxelOverlay" to true,
                )) {
                    TavoloSkinRenderer.renderSkin(
                        bytes = skinBytes,
                        slim = slim,
                        backgroundColor = bg,
                        lightColor = light,
                        headScale = headScale
                    )
                }
            }

            "dsk" -> {
                val slim = parameters["slim"]?.toBoolean() ?: parameters["t"]?.toBoolean() ?: data.slim
                val frameCount = parameters["x"]?.toIntOrNull() ?: 20
                val pitchAmplitude = parameters["y"]?.toIntOrNull() ?: 20
                val headScale = parameters["head"]?.toDoubleOrNull() ?: 1.0
                val duration = parameters["duration"]?.toIntOrNull() ?: 40
                call.respondCachedRender(data, "dsk", "gif", ContentType.Image.GIF, mapOf(
                    "slim" to slim,
                    "bg" to bg.hexColor(),
                    "light" to light?.hexColor(),
                    "head" to headScale,
                    "frameCount" to frameCount,
                    "pitchAmplitude" to pitchAmplitude,
                    "duration" to duration,
                    "width" to 600,
                    "height" to 900,
                    "aa" to 1,
                    "voxelOverlay" to true,
                )) {
                    TavoloSkinRenderer.renderSkinRotate(
                        bytes = skinBytes,
                        slim = slim,
                        backgroundColor = bg,
                        frameCount = frameCount,
                        pitchAmplitude = pitchAmplitude,
                        lightColor = light,
                        headScale = headScale,
                        duration = duration
                    )
                }
            }

            "head" -> {
                call.respondCachedRender(data, "head", "png", ContentType.Image.PNG, mapOf(
                    "bg" to bg.hexColor(),
                    "light" to light?.hexColor(),
                    "width" to 400,
                    "height" to 400,
                    "aa" to 2,
                    "voxelOverlay" to true,
                )) {
                    TavoloSkinRenderer.renderHead(
                        bytes = skinBytes,
                        backgroundColor = bg,
                        lightColor = light
                    )
                }
            }

            "dhead" -> {
                val frameCount = parameters["x"]?.toIntOrNull() ?: 20
                val pitchAmplitude = parameters["y"]?.toIntOrNull() ?: 20
                val duration = parameters["duration"]?.toIntOrNull() ?: 40
                call.respondCachedRender(data, "dhead", "gif", ContentType.Image.GIF, mapOf(
                    "bg" to bg.hexColor(),
                    "light" to light?.hexColor(),
                    "frameCount" to frameCount,
                    "pitchAmplitude" to pitchAmplitude,
                    "duration" to duration,
                    "width" to 400,
                    "height" to 400,
                    "aa" to 1,
                    "voxelOverlay" to true,
                )) {
                    TavoloSkinRenderer.renderHeadRotate(
                        bytes = skinBytes,
                        backgroundColor = bg,
                        frameCount = frameCount,
                        pitchAmplitude = pitchAmplitude,
                        lightColor = light,
                        duration = duration
                    )
                }
            }

            "homo" -> {
                val slim = parameters["slim"]?.toBoolean() ?: parameters["t"]?.toBoolean() ?: data.slim
                val headScale = parameters["head"]?.toDoubleOrNull() ?: 1.0
                call.respondCachedRender(data, "homo", "png", ContentType.Image.PNG, mapOf(
                    "slim" to slim,
                    "bg" to bg.hexColor(),
                    "light" to light?.hexColor(),
                    "head" to headScale,
                    "width" to 1024,
                    "height" to 768,
                    "aa" to 2,
                    "voxelOverlay" to true,
                )) {
                    TavoloSkinRenderer.renderHomo(
                        bytes = skinBytes,
                        slim = slim,
                        backgroundColor = bg,
                        lightColor = light,
                        headScale = headScale
                    )
                }
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

private suspend fun ApplicationCall.respondCachedRender(
    data: SkinData,
    position: String,
    ext: String,
    contentType: ContentType,
    params: Map<String, Any?>,
    render: suspend () -> ByteArray,
) {
    val paramsMd5 = RenderFileCache.paramsMd5(
        params + mapOf(
            "renderer" to RENDERER_ID,
            "position" to position,
            "ext" to ext,
        )
    )
    val bytes = RenderFileCache.getOrRender(data, paramsMd5, ext, render)
    respondBytes(bytes, contentType)
}

private fun Int.hexColor(): String =
    toUInt().toString(16).padStart(8, '0')
