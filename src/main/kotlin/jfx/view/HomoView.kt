package top.e404.skin.jfx.view

import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Color
import javafx.stage.Stage
import top.e404.skin.jfx.SkinCanvas
import top.e404.skin.jfx.png
import top.e404.skin.jfx.runTask
import top.e404.skin.jfx.snapshot

object HomoView {
    private const val w = 1024.0
    private const val h = 768.0
    private val lock = Object()
    lateinit var pane: AnchorPane
    lateinit var stage: Stage

    fun load() {
        val bgImage = this::class.java
            .classLoader
            .getResourceAsStream("homo.png")
            .use { Image(it) }
        val bg = ImageView(bgImage)
        pane = AnchorPane()
        pane.children.add(0, bg)
        stage = Stage().apply {
            scene = Scene(pane, w, h)
            isResizable = false
        }
    }

    var canvas: SkinCanvas? = null
    private fun update(image: Image, slim: Boolean, light: Color?, head: Double) {
        pane.children.apply {
            canvas?.let { remove(it) }
            canvas = SkinCanvas(image, slim, w, h, light, head).apply { homo() }
            add(canvas)
        }
    }

    fun getHomo(
        bytes: ByteArray,
        slim: Boolean,
        light: Color?,
        head: Double,
    ): ByteArray {
        var snapshot: Image? = null
        var t: Throwable? = null
        synchronized(lock) {
            runCatching {
                runTask {
                    bytes.inputStream().use {
                        update(Image(it), slim, light, head)
                    }
                    snapshot = snapshot(Color.web("#00000000"), pane)
                }
            }.onFailure {
                t = it
            }
        }
        t?.let { throw it }
        return snapshot!!.png()
    }
}
