package top.e404.skin.renderer.tavolo.test

import kotlin.test.Test
import org.jetbrains.skia.Image
import top.e404.skin.core.BodyPart
import top.e404.skin.core.PosePresets
import top.e404.skin.core.SkinTransform
import top.e404.skin.renderer.tavolo.renderMinecraftViewTavolo
import top.e404.tavolo.draw.render3d.OrbitCamera
import top.e404.tavolo.draw.render3d.RenderConfig
import top.e404.tavolo.draw.render3d.Vec3
import top.e404.tavolo.util.Colors
import java.io.File

class TestRender {
    private val files = listOf(
        "alex_skin.png" to true,
        "steve_skin.png" to false
    )
    private val backgroundColor = Colors.BG.argb

    fun renderFile(
        file: File,
        isSlim: Boolean,
        width: Int,
        height: Int,
        camera: OrbitCamera,
        lightDirection: Vec3 = Vec3(1f, 1.0f, 1f).normalized(),
        lightIntensity: Float = .8f,
        pose: Map<BodyPart, List<SkinTransform>> = emptyMap()
    ): Image {
        val skin = Image.makeFromEncoded(file.readBytes())
        return renderMinecraftViewTavolo(
            skin = skin,
            isSlim = isSlim,
            renderConfig = RenderConfig(
                width = width,
                height = height,
                camera = camera,
                backgroundColor = backgroundColor,
                lightDirection = lightDirection,
                lightIntensity = lightIntensity,
                antiAliasingLevel = 4
            ),
            pose = pose,
            use3DOverlay = true
        )
    }

    @Test
    fun testRender() {
        for ((fileName, isSlim) in files) {
            val camera = OrbitCamera(Vec3(0f, 10f, 0f), yaw = 45f, pitch = 20f, distance = 50f)
            renderFile(
                File(fileName),
                isSlim,
                800,
                1200,
                camera,
                lightIntensity = .7f,
                lightDirection = Vec3(.5f, .3f, .3f).normalized()
            ).encodeToData()!!.let { data ->
                File("rendered_$fileName").writeBytes(data.bytes)
            }
        }
    }

    @Test
    fun testRenderHead() {
        for ((fileName, isSlim) in files) {
            val camera = OrbitCamera(Vec3(0f, 20f, 0f), yaw = 45f, pitch = 20f, distance = 30f)
            renderFile(
                File(fileName),
                isSlim,
                1800,
                1800,
                camera,
                lightDirection = Vec3(.3f, .3f, .3f).normalized(),
                lightIntensity = .7f,
                pose = PosePresets.HEAD_ONLY,
            ).encodeToData()!!.let { data ->
                File("head_$fileName").writeBytes(data.bytes)
            }
        }
    }
}
