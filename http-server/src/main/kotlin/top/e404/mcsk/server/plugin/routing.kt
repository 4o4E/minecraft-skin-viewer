package top.e404.mcsk.server.plugin

import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import top.e404.mcsk.core.BodyPart
import top.e404.mcsk.core.SkinLightingMode
import top.e404.mcsk.core.SkinOverlayMode
import top.e404.mcsk.core.SkinRenderOptions
import top.e404.mcsk.core.SkinRenderUseCases
import top.e404.mcsk.core.SkinRenderVec3
import top.e404.mcsk.core.SkinTransform
import top.e404.mcsk.server.Skin
import top.e404.mcsk.server.sql.pojo.SkinData
import top.e404.mcsk.server.service.RenderFileCache
import top.e404.mcsk.server.service.SkinRendererService
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

private const val DEFAULT_BG_COLOR = 0xFF1F1B1D.toInt()
private const val DEFAULT_FRAME_COUNT = 20
private const val DEFAULT_DURATION_MS = 40
private val QUERY_JSON = Json { ignoreUnknownKeys = true }

private class QueryParameterException(message: String) : IllegalArgumentException(message)

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

        try {
            val parameters = call.request.queryParameters
            when (call.parameters["position"]!!.lowercase()) {
                "sneak" -> {
                    val options = parameters.renderOptions(SkinRenderUseCases.sneakOptions(DEFAULT_BG_COLOR))
                    val slim = parameters.slim(data.slim)
                    val headScale = parameters.headScale()
                    val duration = parameters.intParam(SkinRenderUseCases.SNEAK_FRAME_DURATION_MS, "duration")
                    call.respondCachedRender(data, "sneak", "gif", ContentType.Image.GIF, options.cacheParams() + mapOf(
                        "slim" to slim,
                        "head" to headScale,
                        "duration" to duration,
                        "capeHash" to data.renderCapeHash(options),
                    )) {
                        SkinRendererService.renderSneak(
                            bytes = data.skinBytes,
                            capeBytes = data.renderCapeBytes(options),
                            slim = slim,
                            headScale = headScale,
                            duration = duration,
                            options = options
                        )
                    }
                }

                "sk" -> {
                    val options = parameters.renderOptions(SkinRenderUseCases.skinOptions(DEFAULT_BG_COLOR))
                    val slim = parameters.slim(data.slim)
                    val headScale = parameters.headScale()
                    call.respondCachedRender(data, "sk", "png", ContentType.Image.PNG, options.cacheParams() + mapOf(
                        "slim" to slim,
                        "head" to headScale,
                        "capeHash" to data.renderCapeHash(options),
                    )) {
                        SkinRendererService.renderSkin(
                            bytes = data.skinBytes,
                            capeBytes = data.renderCapeBytes(options),
                            slim = slim,
                            headScale = headScale,
                            options = options
                        )
                    }
                }

                "dsk" -> {
                    val options = parameters.renderOptions(SkinRenderUseCases.skinRotateOptions(DEFAULT_BG_COLOR))
                    val slim = parameters.slim(data.slim)
                    val frameCount = parameters.intParam(DEFAULT_FRAME_COUNT, "frameCount", "x")
                    val headScale = parameters.headScale()
                    val duration = parameters.intParam(DEFAULT_DURATION_MS, "duration")
                    call.respondCachedRender(data, "dsk", "gif", ContentType.Image.GIF, options.cacheParams() + mapOf(
                        "slim" to slim,
                        "head" to headScale,
                        "frameCount" to frameCount,
                        "duration" to duration,
                        "capeHash" to data.renderCapeHash(options),
                    )) {
                        SkinRendererService.renderSkinRotate(
                            bytes = data.skinBytes,
                            capeBytes = data.renderCapeBytes(options),
                            slim = slim,
                            frameCount = frameCount,
                            headScale = headScale,
                            duration = duration,
                            options = options
                        )
                    }
                }

                "head" -> {
                    val options = parameters.renderOptions(SkinRenderUseCases.headOptions(DEFAULT_BG_COLOR))
                    call.respondCachedRender(data, "head", "png", ContentType.Image.PNG, options.cacheParams()) {
                        SkinRendererService.renderHead(
                            bytes = data.skinBytes,
                            options = options
                        )
                    }
                }

                "dhead" -> {
                    val options = parameters.renderOptions(SkinRenderUseCases.headRotateOptions(DEFAULT_BG_COLOR))
                    val frameCount = parameters.intParam(DEFAULT_FRAME_COUNT, "frameCount", "x")
                    val duration = parameters.intParam(DEFAULT_DURATION_MS, "duration")
                    call.respondCachedRender(data, "dhead", "gif", ContentType.Image.GIF, options.cacheParams() + mapOf(
                        "frameCount" to frameCount,
                        "duration" to duration,
                    )) {
                        SkinRendererService.renderHeadRotate(
                            bytes = data.skinBytes,
                            frameCount = frameCount,
                            duration = duration,
                            options = options
                        )
                    }
                }

                "homo" -> {
                    val options = parameters.renderOptions(SkinRenderUseCases.homoOptions(DEFAULT_BG_COLOR))
                    val slim = parameters.slim(data.slim)
                    val headScale = parameters.headScale()
                    call.respondCachedRender(data, "homo", "png", ContentType.Image.PNG, options.cacheParams() + mapOf(
                        "slim" to slim,
                        "head" to headScale,
                        "capeHash" to data.renderCapeHash(options),
                    )) {
                        SkinRendererService.renderHomo(
                            bytes = data.skinBytes,
                            capeBytes = data.renderCapeBytes(options),
                            slim = slim,
                            headScale = headScale,
                            options = options
                        )
                    }
                }

                else -> call.respond(HttpStatusCode.NotFound)
            }
        } catch (e: QueryParameterException) {
            call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid query parameter")
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
        try {
            val parameters = call.request.queryParameters
            val bg = parameters["bg"]?.asColor() ?: 0
            val scale = parameters.intParam(5, "scale")
            val margin = parameters.intParam(40, "margin")
            call.respondCachedRender(data, "face", "png", ContentType.Image.PNG, mapOf(
                "bg" to bg.hexColor(),
                "scale" to scale,
                "margin" to margin,
            )) {
                renderFace(data.skinBytes, bg, scale, margin)
            }
        } catch (e: QueryParameterException) {
            call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid query parameter")
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
        else -> queryParameterError("Invalid color: $this")
    }
    return argb.toULongOrNull(16)?.toInt() ?: queryParameterError("Invalid color: $this")
}

private fun String.asLightIntensity(): Float {
    val value = toFloatOrNull() ?: queryParameterError("Invalid light intensity: $this")
    if (value !in 0f..1f) queryParameterError("Light intensity must be between 0 and 1: $this")
    return value
}

internal fun Parameters.renderOptions(defaults: SkinRenderOptions): SkinRenderOptions {
    val shadows = firstValue("shadow", "shadows")?.asBooleanParam() ?: defaults.shadows
    val lightingMode = firstValue("lighting", "lightingMode")?.asLightingMode()
        ?: if (shadows) SkinLightingMode.DIRECTIONAL else defaults.lightingMode
    if (shadows && lightingMode == SkinLightingMode.AMBIENT) {
        queryParameterError("shadow requires directional lighting")
    }
    val showPlatform = firstValue("platform", "showPlatform")?.asBooleanParam()
        ?: if (shadows) true else defaults.showPlatform

    return defaults.copy(
        width = intParam(defaults.width, "width"),
        height = intParam(defaults.height, "height"),
        target = vec3Param(defaults.target, "target", "targetX", "targetY", "targetZ"),
        yaw = floatParam(defaults.yaw, "yaw"),
        pitch = floatParam(defaults.pitch, "pitch", "y"),
        distance = floatParam(defaults.distance, "distance"),
        backgroundColor = this["bg"]?.asColor() ?: defaults.backgroundColor,
        lightIntensity = this["light"]?.asLightIntensity() ?: defaults.lightIntensity,
        lightDirection = nullableVec3Param(defaults.lightDirection, "lightDirection", "lightDir", "lightX", "lightY", "lightZ"),
        platformTopY = floatParam(defaults.platformTopY, "platformTopY", "platformY"),
        platformThickness = floatParam(defaults.platformThickness, "platformThickness"),
        antiAliasingLevel = intParam(defaults.antiAliasingLevel, "aa", "antiAliasingLevel"),
        overlayMode = firstValue("overlay", "overlayMode")?.asOverlayMode() ?: defaults.overlayMode,
        lightingMode = lightingMode,
        shadows = shadows,
        showPlatform = showPlatform,
        showCape = firstValue("cape", "showCape")?.asBooleanParam() ?: defaults.showCape,
        modelYaw = floatParam(defaults.modelYaw, "modelYaw"),
        pose = this["pose"]?.asPose() ?: defaults.pose,
    )
}

internal fun SkinRenderOptions.cacheParams(): Map<String, Any?> =
    mapOf(
        "width" to width,
        "height" to height,
        "target" to target.cacheString(),
        "yaw" to yaw,
        "pitch" to pitch,
        "distance" to distance,
        "bg" to backgroundColor.hexColor(),
        "light" to lightIntensity,
        "lightDirection" to lightDirection?.normalized()?.cacheString(),
        "platformTopY" to platformTopY,
        "platformThickness" to platformThickness,
        "aa" to antiAliasingLevel,
        "overlay" to overlayMode.name.lowercase(),
        "lighting" to lightingMode.name.lowercase(),
        "shadow" to shadows,
        "platform" to showPlatform,
        "cape" to showCape,
        "modelYaw" to modelYaw,
        "pose" to pose.cacheString()
    )

private fun SkinData.renderCapeHash(options: SkinRenderOptions): String? =
    if (options.showCape) capeHash else null

private fun SkinData.renderCapeBytes(options: SkinRenderOptions): ByteArray? =
    if (options.showCape) capeBytes else null

private fun Parameters.slim(default: Boolean): Boolean =
    firstValue("slim", "t")?.asBooleanParam() ?: default

private fun Parameters.headScale(): Double =
    doubleParam(1.0, "head")

internal fun Parameters.intParam(default: Int, vararg names: String): Int =
    firstValue(*names)?.let { value ->
        value.toIntOrNull() ?: queryParameterError("Invalid integer value for ${names.first()}: $value")
    } ?: default

private fun Parameters.floatParam(default: Float, vararg names: String): Float =
    firstValue(*names)?.let { value ->
        value.toFloatOrNull() ?: queryParameterError("Invalid float value for ${names.first()}: $value")
    } ?: default

private fun Parameters.doubleParam(default: Double, vararg names: String): Double =
    firstValue(*names)?.let { value ->
        value.toDoubleOrNull() ?: queryParameterError("Invalid double value for ${names.first()}: $value")
    } ?: default

private fun Parameters.firstValue(vararg names: String): String? =
    names.firstNotNullOfOrNull { this[it] }

private fun Parameters.vec3Param(default: SkinRenderVec3, combinedName: String, xName: String, yName: String, zName: String): SkinRenderVec3 =
    nullableVec3Param(default, combinedName, combinedName, xName, yName, zName) ?: default

private fun Parameters.nullableVec3Param(
    default: SkinRenderVec3?,
    combinedName: String,
    combinedAlias: String,
    xName: String,
    yName: String,
    zName: String,
): SkinRenderVec3? {
    firstValue(combinedName, combinedAlias)?.let { return it.asVec3() }
    if (this[xName] == null && this[yName] == null && this[zName] == null) return default
    val x = floatComponent(xName, default?.x)
    val y = floatComponent(yName, default?.y)
    val z = floatComponent(zName, default?.z)
    return SkinRenderVec3(x, y, z)
}

private fun Parameters.floatComponent(name: String, default: Float?): Float =
    this[name]?.let { value ->
        value.toFloatOrNull() ?: queryParameterError("Invalid float value for $name: $value")
    } ?: default ?: queryParameterError("Missing vector component: $name")

private fun String.asVec3(): SkinRenderVec3 {
    val values = split(',').map { it.trim().toFloatOrNull() ?: queryParameterError("Invalid vector value: $this") }
    if (values.size != 3) queryParameterError("Vector must contain 3 numbers: $this")
    return SkinRenderVec3(values[0], values[1], values[2])
}

private fun String.asBooleanParam(): Boolean =
    when (lowercase()) {
        "true", "1", "yes", "on" -> true
        "false", "0", "no", "off" -> false
        else -> queryParameterError("Invalid boolean value: $this")
    }

private fun String.asOverlayMode(): SkinOverlayMode =
    when (lowercase()) {
        "none" -> SkinOverlayMode.NONE
        "flat", "2d" -> SkinOverlayMode.FLAT
        "three_d", "three-d", "3d", "voxel", "voxel3d" -> SkinOverlayMode.THREE_D
        else -> queryParameterError("Invalid overlay mode: $this")
    }

private fun String.asLightingMode(): SkinLightingMode =
    when (lowercase()) {
        "ambient" -> SkinLightingMode.AMBIENT
        "directional" -> SkinLightingMode.DIRECTIONAL
        else -> queryParameterError("Invalid lighting mode: $this")
    }

private fun String.asPose(): Map<BodyPart, List<SkinTransform>> {
    try {
        val root = QUERY_JSON.parseToJsonElement(this).jsonObject
        return root.mapKeys { (part, _) -> part.asBodyPart() }
            .mapValues { (_, transforms) ->
                transforms.jsonArray.map { it.jsonObject.asTransform() }
            }
    } catch (e: QueryParameterException) {
        throw e
    } catch (e: Exception) {
        queryParameterError("Invalid pose json: ${e.message ?: this}")
    }
}

private fun String.asBodyPart(): BodyPart =
    when (lowercase().replace("-", "").replace("_", "")) {
        "head" -> BodyPart.HEAD
        "body" -> BodyPart.BODY
        "rightarm" -> BodyPart.RIGHT_ARM
        "leftarm" -> BodyPart.LEFT_ARM
        "rightleg" -> BodyPart.RIGHT_LEG
        "leftleg" -> BodyPart.LEFT_LEG
        "cape" -> BodyPart.CAPE
        else -> queryParameterError("Invalid body part: $this")
    }

private fun JsonObject.asTransform(): SkinTransform {
    val type = (this["type"] ?: this["kind"])?.jsonPrimitive?.content
        ?: queryParameterError("Pose transform missing type")
    return when (type.lowercase()) {
        "rotate" -> SkinTransform.Rotate(floatValue("x"), floatValue("y"), floatValue("z"))
        "scale" -> SkinTransform.Scale(floatValue("x", 1f), floatValue("y", 1f), floatValue("z", 1f))
        "translate" -> SkinTransform.Translate(floatValue("x"), floatValue("y"), floatValue("z"))
        else -> queryParameterError("Invalid pose transform type: $type")
    }
}

private fun JsonObject.floatValue(name: String, default: Float = 0f): Float =
    this[name]?.jsonPrimitive?.floatOrNull
        ?: if (this[name] == null) default else queryParameterError("Invalid float value for pose.$name")

private fun queryParameterError(message: String): Nothing =
    throw QueryParameterException(message)

private fun SkinRenderVec3.cacheString(): String = "$x,$y,$z"

private fun Map<BodyPart, List<SkinTransform>>.cacheString(): String =
    BodyPart.entries.joinToString("|") { part ->
        val transforms = this[part].orEmpty().joinToString(",") { transform ->
            when (transform) {
                is SkinTransform.Rotate -> "rotate(${transform.x},${transform.y},${transform.z})"
                is SkinTransform.Scale -> "scale(${transform.x},${transform.y},${transform.z})"
                is SkinTransform.Translate -> "translate(${transform.x},${transform.y},${transform.z})"
            }
        }
        "${part.name.lowercase()}=[$transforms]"
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
