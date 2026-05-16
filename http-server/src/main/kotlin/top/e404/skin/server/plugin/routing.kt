package top.e404.skin.server.plugin

import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import top.e404.skin.core.SkinRenderUseCases
import top.e404.skin.server.Skin
import top.e404.skin.server.sql.pojo.SkinData
import top.e404.skin.server.service.RenderFileCache
import top.e404.skin.server.service.SkinRendererService
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

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

        val parameters = call.request.queryParameters
        val bg = parameters["bg"]?.asColor() ?: DEFAULT_BG_COLOR
        val lightIntensity = parameters["light"]?.asLightIntensity()
        when (call.parameters["position"]!!.lowercase()) {
            "sneak" -> {
                val slim = parameters["slim"]?.toBoolean() ?: parameters["t"]?.toBoolean() ?: data.slim
                val headScale = parameters["head"]?.toDoubleOrNull() ?: 1.0
                val platform = parameters["platform"]?.toBoolean() ?: false
                call.respondCachedRender(data, "sneak", "gif", ContentType.Image.GIF, mapOf(
                    "slim" to slim,
                    "bg" to bg.hexColor(),
                    "light" to lightIntensity,
                    "head" to headScale,
                    "duration" to SkinRenderUseCases.SNEAK_FRAME_DURATION_MS,
                    "platform" to platform,
                    "width" to 600,
                    "height" to 900,
                    "aa" to 1,
                    "voxelOverlay" to true,
                )) {
                    SkinRendererService.renderSneak(
                        bytes = data.skinBytes,
                        slim = slim,
                        backgroundColor = bg,
                        lightIntensity = lightIntensity,
                        headScale = headScale,
                        showPlatform = platform
                    )
                }
            }

            "sk" -> {
                val slim = parameters["slim"]?.toBoolean() ?: parameters["t"]?.toBoolean() ?: data.slim
                val headScale = parameters["head"]?.toDoubleOrNull() ?: 1.0
                val platform = parameters["platform"]?.toBoolean() ?: false
                call.respondCachedRender(data, "sk", "png", ContentType.Image.PNG, mapOf(
                    "slim" to slim,
                    "bg" to bg.hexColor(),
                    "light" to lightIntensity,
                    "head" to headScale,
                    "platform" to platform,
                    "width" to 600,
                    "height" to 900,
                    "aa" to 2,
                    "voxelOverlay" to true,
                )) {
                    SkinRendererService.renderSkin(
                        bytes = data.skinBytes,
                        slim = slim,
                        backgroundColor = bg,
                        lightIntensity = lightIntensity,
                        headScale = headScale,
                        showPlatform = platform
                    )
                }
            }

            "dsk" -> {
                val slim = parameters["slim"]?.toBoolean() ?: parameters["t"]?.toBoolean() ?: data.slim
                val frameCount = parameters["x"]?.toIntOrNull() ?: 20
                val pitchAmplitude = parameters["y"]?.toIntOrNull() ?: 20
                val headScale = parameters["head"]?.toDoubleOrNull() ?: 1.0
                val duration = parameters["duration"]?.toIntOrNull() ?: 40
                val platform = parameters["platform"]?.toBoolean() ?: true
                call.respondCachedRender(data, "dsk", "gif", ContentType.Image.GIF, mapOf(
                    "slim" to slim,
                    "bg" to bg.hexColor(),
                    "light" to lightIntensity,
                    "head" to headScale,
                    "frameCount" to frameCount,
                    "pitchAmplitude" to pitchAmplitude,
                    "duration" to duration,
                    "platform" to platform,
                    "width" to 600,
                    "height" to 900,
                    "aa" to 1,
                    "voxelOverlay" to true,
                )) {
                    SkinRendererService.renderSkinRotate(
                        bytes = data.skinBytes,
                        slim = slim,
                        backgroundColor = bg,
                        frameCount = frameCount,
                        pitchAmplitude = pitchAmplitude,
                        lightIntensity = lightIntensity,
                        headScale = headScale,
                        duration = duration,
                        showPlatform = platform
                    )
                }
            }

            "head" -> {
                val platform = parameters["platform"]?.toBoolean() ?: false
                call.respondCachedRender(data, "head", "png", ContentType.Image.PNG, mapOf(
                    "bg" to bg.hexColor(),
                    "light" to lightIntensity,
                    "platform" to platform,
                    "width" to 400,
                    "height" to 400,
                    "aa" to 2,
                    "voxelOverlay" to true,
                )) {
                    SkinRendererService.renderHead(
                        bytes = data.skinBytes,
                        backgroundColor = bg,
                        lightIntensity = lightIntensity,
                        showPlatform = platform
                    )
                }
            }

            "dhead" -> {
                val frameCount = parameters["x"]?.toIntOrNull() ?: 20
                val pitchAmplitude = parameters["y"]?.toIntOrNull() ?: 20
                val duration = parameters["duration"]?.toIntOrNull() ?: 40
                val platform = parameters["platform"]?.toBoolean() ?: false
                call.respondCachedRender(data, "dhead", "gif", ContentType.Image.GIF, mapOf(
                    "bg" to bg.hexColor(),
                    "light" to lightIntensity,
                    "frameCount" to frameCount,
                    "pitchAmplitude" to pitchAmplitude,
                    "duration" to duration,
                    "platform" to platform,
                    "width" to 400,
                    "height" to 400,
                    "aa" to 1,
                    "voxelOverlay" to true,
                )) {
                    SkinRendererService.renderHeadRotate(
                        bytes = data.skinBytes,
                        backgroundColor = bg,
                        frameCount = frameCount,
                        pitchAmplitude = pitchAmplitude,
                        lightIntensity = lightIntensity,
                        duration = duration,
                        showPlatform = platform
                    )
                }
            }

            "homo" -> {
                val slim = parameters["slim"]?.toBoolean() ?: parameters["t"]?.toBoolean() ?: data.slim
                val headScale = parameters["head"]?.toDoubleOrNull() ?: 1.0
                val platform = parameters["platform"]?.toBoolean() ?: false
                call.respondCachedRender(data, "homo", "png", ContentType.Image.PNG, mapOf(
                    "slim" to slim,
                    "bg" to bg.hexColor(),
                    "light" to lightIntensity,
                    "head" to headScale,
                    "platform" to platform,
                    "width" to 1024,
                    "height" to 768,
                    "aa" to 2,
                    "voxelOverlay" to true,
                )) {
                    SkinRendererService.renderHomo(
                        bytes = data.skinBytes,
                        slim = slim,
                        backgroundColor = bg,
                        lightIntensity = lightIntensity,
                        headScale = headScale,
                        showPlatform = platform
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
        val parameters = call.request.queryParameters
        val bg = parameters["bg"]?.asColor() ?: 0
        val scale = parameters["scale"]?.toIntOrNull() ?: 5
        val margin = parameters["margin"]?.toIntOrNull() ?: 40
        call.respondCachedRender(data, "face", "png", ContentType.Image.PNG, mapOf(
            "bg" to bg.hexColor(),
            "scale" to scale,
            "margin" to margin,
        )) {
            renderFace(data.skinBytes, bg, scale, margin)
        }
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
            "renderer" to SkinRendererService.rendererId,
            "position" to position,
            "ext" to ext,
        )
    )
    val bytes = RenderFileCache.getOrRender(data, paramsMd5, ext, render)
    respondBytes(bytes, contentType)
}

private fun Int.hexColor(): String =
    toUInt().toString(16).padStart(8, '0')

private fun String.asColor(): Int {
    val raw = trim().removePrefix("#").removePrefix("0x").removePrefix("0X")
    val argb = when (raw.length) {
        3 -> "ff" + raw.map { "$it$it" }.joinToString("")
        4 -> raw.map { "$it$it" }.joinToString("")
        6 -> "ff$raw"
        8 -> raw
        else -> error("Invalid color: $this")
    }
    return argb.toULong(16).toInt()
}

private fun String.asLightIntensity(): Float {
    val value = toFloatOrNull() ?: error("Invalid light intensity: $this")
    require(value in 0f..1f) { "Light intensity must be between 0 and 1: $this" }
    return value
}

private fun renderFace(skinBytes: ByteArray, backgroundColor: Int, scale: Int, margin: Int): ByteArray {
    val skin = ImageIO.read(ByteArrayInputStream(skinBytes))
    val pixelScale = (skin.width / 64).coerceAtLeast(1)
    val layer1 = skin.getSubimage(8 * pixelScale, 8 * pixelScale, 8 * pixelScale, 8 * pixelScale)
    val layer2 = skin.getSubimage(40 * pixelScale, 8 * pixelScale, 8 * pixelScale, 8 * pixelScale)
    val size = 64 * scale + 2 * margin
    val result = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val graphics = result.createGraphics()
    try {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        graphics.color = java.awt.Color(backgroundColor, true)
        graphics.fillRect(0, 0, size, size)
        graphics.drawImage(layer1, margin + 4 * scale, margin + 4 * scale, 56 * scale, 56 * scale, null)
        graphics.drawImage(layer2, margin, margin, 64 * scale, 64 * scale, null)
    } finally {
        graphics.dispose()
    }
    return ByteArrayOutputStream().use {
        ImageIO.write(result, "png", it)
        it.toByteArray()
    }
}
