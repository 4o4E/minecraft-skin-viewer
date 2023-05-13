package top.e404.skin.jfx.view

import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.stage.Stage
import top.e404.skin.jfx.SkinCanvas
import kotlin.concurrent.thread

class SkinApp : Application() {
    companion object {
        private const val w = 600.0
        private const val h = 900.0
        lateinit var app: SkinApp
        var afterLoad: (SkinApp) -> Unit = {}

        @JvmStatic
        fun launch() {
            thread(true) {
                launch(SkinApp::class.java)
            }
        }

        @JvmStatic
        fun launch(block: (SkinApp) -> Unit) {
            afterLoad = block
            thread(true) {
                launch(SkinApp::class.java)
            }
        }
    }

    val pane = StackPane()
    lateinit var canvas: SkinCanvas

    override fun start(stage: Stage) {
        Platform.setImplicitExit(false)
        stage.scene = Scene(pane, w, h)
        stage.isResizable = false
        app = this
        HeadView.load()
        HomoView.load()
        SneakView.load()
        afterLoad.invoke(this)
    }

    fun update(image: Image, slim: Boolean, light: Color?, head: Double) {
        pane.children.apply {
            clear()
            canvas = SkinCanvas(image, slim, w, h, light, head)
            add(canvas)
        }
    }
}
