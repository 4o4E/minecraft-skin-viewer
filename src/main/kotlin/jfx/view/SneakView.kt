package top.e404.skin.jfx.view

import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Color
import javafx.stage.Stage
import top.e404.skin.jfx.SkinCanvas
import top.e404.skin.jfx.png
import top.e404.skin.jfx.runTask
import top.e404.skin.jfx.snapshot

object SneakView {
    const val w = 600.0
    const val h = 900.0
    private val lock = Object()
    private val pane = AnchorPane()
    lateinit var stage: Stage

    fun load() {
        stage = Stage().apply {
            scene = Scene(pane, w, h)
            isResizable = false
        }
    }

    private var canvas: SkinCanvas? = null
    private fun update(
        image: Image,
        slim: Boolean,
        light: Color?,
        head: Double
    ) {
        canvas?.let { pane.children.remove(it) }
        canvas = SkinCanvas(image, slim, w, h, light, head)
        pane.children.add(canvas)
    }

    fun getSneak(
        bytes: ByteArray,
        slim: Boolean,
        bg: Color,
        light: Color?,
        head: Double,
    ): Pair<ByteArray, ByteArray> {
        var normal: Image? = null
        var sneak: Image? = null
        var t: Throwable? = null
        synchronized(lock) {
            runCatching {
                runTask {
                    val image = bytes.inputStream().use { Image(it) }
                    update(image, slim, light, head)
                    canvas!!.apply {
                        xRotate.angle = 10.0
                        yRotate.angle = 315.0
                        zRotate.angle = 10.0
                    }
                    normal = snapshot(bg, pane)
                    canvas?.sneak()
                    sneak = snapshot(bg, pane)
                }
            }.onFailure {
                t = it
            }
        }
        t?.let { throw it }
        return Pair(normal!!.png(), sneak!!.png())
    }
}
