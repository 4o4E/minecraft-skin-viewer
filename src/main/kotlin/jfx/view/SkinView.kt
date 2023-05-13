package top.e404.skin.jfx.view

import javafx.scene.image.Image
import javafx.scene.paint.Color
import top.e404.skin.jfx.view.SkinApp.Companion.app
import top.e404.skin.jfx.png
import top.e404.skin.jfx.runTask
import top.e404.skin.jfx.snapshot
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object SkinView {
    private const val u = PI / 180
    private val lock = Object()

    fun getSkin(
        bytes: ByteArray,
        slim: Boolean,
        bg: Color,
        light: Color?,
        head: Double,
    ): ByteArray {
        var snapshot: Image? = null
        var t: Throwable? = null
        synchronized(lock) {
            runCatching {
                runTask {
                    val image = bytes.inputStream().use { Image(it) }
                    app.update(image, slim, light, head)
                    snapshot = snapshot(bg, app.pane)
                }
            }.onFailure {
                t = it
            }
        }
        t?.let { throw it }
        return snapshot!!.png()
    }

    fun getSkinRotate(
        bytes: ByteArray,
        slim: Boolean,
        bg: Color,
        frameCount: Int,
        y: Int,
        light: Color?,
        head: Double,
    ): List<ByteArray> {
        val images = ArrayList<Image>()
        var t: Throwable? = null
        synchronized(lock) {
            runCatching {
                runTask {
                    val step = 360 / frameCount
                    val image = bytes.inputStream().use { Image(it) }
                    app.update(image, slim, light, head)
                    app.canvas.apply {
                        val r = if (step > 0) 1 else -1
                        for (i in 0 until 360 step step * r) {
                            val d = i.toDouble() * r
                            yRotate.angle = -d
                            zRotate.angle = sin(d * u) * y
                            xRotate.angle = cos(d * u) * y
                            images.add(snapshot(bg, app.pane))
                        }
                    }
                }
            }.onFailure {
                t = it
            }
        }
        t?.let { throw it }
        return images.map { it.png() }
    }
}
