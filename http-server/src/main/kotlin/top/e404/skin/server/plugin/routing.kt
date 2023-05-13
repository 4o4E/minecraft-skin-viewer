package top.e404.skin.server.plugin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import javafx.scene.paint.Color
import org.jetbrains.skia.AnimationDisposalMode
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Image
import top.e404.skiko.gif.gif
import top.e404.skin.jfx.view.HeadView
import top.e404.skin.jfx.view.HomoView
import top.e404.skin.jfx.view.SkinView
import top.e404.skin.jfx.view.SneakView
import top.e404.skin.server.Skin

private val defaultBgColor = Color.web("#1F1B1D")

fun Application.routing() = routing {
    get("/about") {
        call.respond(
            """GET /render/{type}/{content}/{position} 
            |  type: id/name // 通过何种方式指定玩家
            |  content: id/name // 内容
            |  position: sneak/sk/dsk/head/dhead/homo
            |  query param: 生成的参数
            |
        """.trimMargin()
        )
    }

    // type: id/name // 通过何种方式指定玩家
    // content: id/name // 内容
    // position: sneak/sk/dsk/head/dhead/homo
    // query param: 生成的参数
    get("/render/{type}/{content}/{position}") {
        val data = when (call.parameters["type"]!!.lowercase()) {
            "id" -> Skin.getDataById(call.parameters["content"]!!)
            "name" -> Skin.getDataByName(call.parameters["content"]!!)
            else -> {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }
        }
        if (data == null) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        val skinBytes = data.skinBytes

        val parameters = call.request.queryParameters
        val bg = parameters["bg"]?.let { Color.web(it) } ?: defaultBgColor
        val light = parameters["light"]?.let { Color.web(it) }
        when (call.parameters["position"]!!.lowercase()) {
            "sneak" -> {
                val (first, second) = SneakView.getSneak(skinBytes, data.slim, bg, light, parameters["head"]?.toDoubleOrNull() ?: 1.0)
                val bytes = gif(600, 900) {
                    options {
                        disposalMethod = AnimationDisposalMode.RESTORE_BG_COLOR
                        alphaType = ColorAlphaType.OPAQUE
                    }
                    frame(Bitmap.makeFromImage(Image.makeFromEncoded(first))) { duration = 40 }
                    frame(Bitmap.makeFromImage(Image.makeFromEncoded(second))) { duration = 40 }
                }.bytes
                call.respondBytes(bytes, ContentType.Image.GIF)
            }

            "sk" -> {
                val bytes = SkinView.getSkin(
                    skinBytes,
                    data.slim,
                    bg,
                    light,
                    parameters["head"]?.toDoubleOrNull() ?: 1.0
                )
                call.respondBytes(bytes, ContentType.Image.PNG)
            }

            "dsk" -> {
                val bytes = SkinView.getSkinRotate(
                    skinBytes,
                    data.slim,
                    bg,
                    parameters["x"]?.toIntOrNull() ?: 20,
                    parameters["y"]?.toIntOrNull() ?: 20,
                    light,
                    parameters["head"]?.toDoubleOrNull() ?: 1.0
                ).let { frames ->
                    gif(600, 900) {
                        options {
                            disposalMethod = AnimationDisposalMode.RESTORE_BG_COLOR
                            alphaType = ColorAlphaType.OPAQUE
                        }
                        frames.forEach {
                            frame(Bitmap.makeFromImage(Image.makeFromEncoded(it))) { duration = 40 }
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
                    gif(400, 400) {
                        options {
                            disposalMethod = AnimationDisposalMode.RESTORE_BG_COLOR
                            alphaType = ColorAlphaType.OPAQUE
                        }
                        frames.forEach {
                            frame(Bitmap.makeFromImage(Image.makeFromEncoded(it))) { duration = 40 }
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
}
