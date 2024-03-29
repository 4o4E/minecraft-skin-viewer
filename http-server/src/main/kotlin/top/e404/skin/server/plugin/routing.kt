package top.e404.skin.server.plugin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import javafx.scene.paint.Color
import org.jetbrains.skia.*
import top.e404.skiko.gif.gif
import top.e404.skiko.util.*
import top.e404.skin.jfx.view.HeadView
import top.e404.skin.jfx.view.HomoView
import top.e404.skin.jfx.view.SkinView
import top.e404.skin.jfx.view.SneakView
import top.e404.skin.server.Skin

private val defaultBgColor = Color.web("#1F1B1D")

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
        val bg = parameters["bg"]?.runCatching { Color.web(this) }?.getOrNull() ?: defaultBgColor
        val light = parameters["light"]?.let { Color.web(it) }
        when (call.parameters["position"]!!.lowercase()) {
            "sneak" -> {
                val slim = parameters["t"]?.toBoolean() ?: data.slim
                val (first, second) = SneakView.getSneak(
                    skinBytes,
                    slim,
                    bg,
                    light,
                    parameters["head"]?.toDoubleOrNull() ?: 1.0
                )
                val d = parameters["duration"]?.toIntOrNull() ?: 40
                val bytes = gif(600, 900) {
                    options {
                        disposalMethod = AnimationDisposalMode.RESTORE_BG_COLOR
                        alphaType = ColorAlphaType.OPAQUE
                    }
                    frame(Bitmap.makeFromImage(Image.makeFromEncoded(first))) { duration = d }
                    frame(Bitmap.makeFromImage(Image.makeFromEncoded(second))) { duration = d }
                }.bytes
                call.respondBytes(bytes, ContentType.Image.GIF)
            }

            "sk" -> {
                val slim = parameters["t"]?.toBoolean() ?: data.slim
                val bytes = SkinView.getSkin(
                    skinBytes,
                    slim,
                    bg,
                    light,
                    parameters["head"]?.toDoubleOrNull() ?: 1.0
                )
                call.respondBytes(bytes, ContentType.Image.PNG)
            }

            "dsk" -> {
                val slim = parameters["t"]?.toBoolean() ?: data.slim
                val bytes = SkinView.getSkinRotate(
                    skinBytes,
                    slim,
                    bg,
                    parameters["x"]?.toIntOrNull() ?: 20,
                    parameters["y"]?.toIntOrNull() ?: 20,
                    light,
                    parameters["head"]?.toDoubleOrNull() ?: 1.0
                ).let { frames ->
                    val d = parameters["duration"]?.toIntOrNull() ?: 40
                    gif(600, 900) {
                        options {
                            disposalMethod = AnimationDisposalMode.RESTORE_BG_COLOR
                            alphaType = ColorAlphaType.OPAQUE
                        }
                        frames.forEach {
                            frame(Bitmap.makeFromImage(Image.makeFromEncoded(it))) { duration = d }
                        }
                    }.bytes
                }
                call.respondBytes(bytes, ContentType.Image.GIF)
            }

            "head" -> {
                val bytes = HeadView.getHead(skinBytes, bg, light)
                call.respondBytes(bytes, ContentType.Image.PNG)
            }

            "dhead" -> {
                val bytes = HeadView.getHeadRotate(
                    skinBytes,
                    bg,
                    parameters["x"]?.toIntOrNull() ?: 20,
                    parameters["y"]?.toIntOrNull() ?: 20,
                    light
                ).let { frames ->
                    val d = parameters["duration"]?.toIntOrNull() ?: 40
                    gif(400, 400) {
                        options {
                            disposalMethod = AnimationDisposalMode.RESTORE_BG_COLOR
                            alphaType = ColorAlphaType.OPAQUE
                        }
                        frames.forEach {
                            frame(Bitmap.makeFromImage(Image.makeFromEncoded(it))) { duration = d }
                        }
                    }.bytes
                }
                call.respondBytes(bytes, ContentType.Image.GIF)
            }

            "homo" -> {
                val bytes = HomoView.getHomo(skinBytes, data.slim, light, parameters["head"]?.toDoubleOrNull() ?: 1.0)
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
