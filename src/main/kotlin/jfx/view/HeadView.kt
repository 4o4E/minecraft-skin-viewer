package top.e404.skin.jfx.view

import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.stage.Stage
import top.e404.skin.jfx.HeadCanvas
import top.e404.skin.jfx.png
import top.e404.skin.jfx.runTask
import top.e404.skin.jfx.snapshot
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object HeadView {
    private const val width = 400.0
    private const val height = 400.0
    private const val u = PI / 180
    private val lock = Object()
    private val pane = StackPane()
    lateinit var stage: Stage

    fun load() {
        stage = Stage().apply {
            scene = Scene(pane, HeadView.width, HeadView.height)
            isResizable = false
        }
    }

    private lateinit var canvas: HeadCanvas
    private fun update(image: Image, light: Color?) {
        pane.children.apply {
            clear()
            canvas = HeadCanvas(image, width, height, light)
            add(canvas)
        }
    }

    fun getHead(bytes: ByteArray, bg: Color, light: Color?): ByteArray {
        var snapshot: Image? = null
        var t: Throwable? = null
        synchronized(lock) {
            runCatching {
                runTask {
                    bytes.inputStream().use {
                        update(Image(it), light)
                    }
                    snapshot = snapshot(bg, pane)
                }
            }.onFailure {
                t = it
            }
        }
        t?.let { throw it }
        return snapshot!!.png()
    }

    fun getHeadRotate(
        bytes: ByteArray,
        bg: Color,
        frameCount: Int,
        y: Int,
        light: Color?,
    ): List<ByteArray> {
        val images = ArrayList<Image>()
        var t: Throwable? = null
        synchronized(lock) {
            runCatching {
                runTask {
                    val step = 360 / frameCount
                    val image = bytes.inputStream().use {
                        Image(it)
                    }
                    update(image, light)
                    canvas.apply {
                        val r = if (step > 0) 1 else -1
                        for (i in 0 until 360 step step * r) {
                            val d = i.toDouble() * r
                            yRotate.angle = -d
                            zRotate.angle = sin(d * u) * y
                            xRotate.angle = cos(d * u) * y
                            images.add(snapshot(bg, pane))
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
