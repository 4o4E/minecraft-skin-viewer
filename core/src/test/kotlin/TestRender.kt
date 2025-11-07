package top.e404.skin.core.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Image
import org.junit.jupiter.api.Test
import top.e404.skiko.draw.render3d.OrbitCamera
import top.e404.skiko.draw.render3d.RenderConfig
import top.e404.skiko.draw.render3d.Transformation
import top.e404.skiko.draw.render3d.Vec3
import top.e404.skiko.util.Colors
import top.e404.skin.core.BodyPart
import top.e404.skin.core.PosePresets
import top.e404.skin.core.renderMinecraftView
import top.e404.skin.core.renderRotate
import java.io.File

class TestRender {
    private val files = listOf(
        "alex_skin.png" to true,
        "steve_skin.png" to false
    )
    private val backgroundColor = Colors.BG.argb

    /**
     * 运行Minecraft皮肤渲染示例的函数。
     */
    fun renderFile(
        file: File,
        isSlim: Boolean,
        width: Int,
        height: Int,
        camera: OrbitCamera,
        lightDirection: Vec3 = Vec3(-0.7f, 1.0f, 0.5f).normalized(),
        lightIntensity: Float = .9f,
        pose: Map<BodyPart, List<Transformation>> = emptyMap()
    ): Image {
        val skin = Image.makeFromEncoded(file.readBytes())
        val renderConfig = RenderConfig(
            width,
            height,
            camera,
            backgroundColor = backgroundColor,
            lightDirection = lightDirection,
            lightIntensity = lightIntensity
        )
        return renderMinecraftView(
            skin,
            isSlim,
            renderConfig,
            pose,
            true
        )
    }

    @Test
    fun test_render() {
        val lightIntensity = 1f
        for ((fileName, isSlim) in files) {
            val camera = OrbitCamera(
                target = Vec3(0f, 10f, 0f),
                yaw = 45f,
                pitch = 20f,
                distance = 50f
            )
            renderFile(
                File(fileName),
                isSlim,
                800,
                1200,
                camera,
                lightIntensity = lightIntensity,
                lightDirection = Vec3(-45f, 50f, 0f)
            ).encodeToData()!!.let { data ->
                File("rendered_$fileName").writeBytes(data.bytes)
            }
        }
    }

    @Test
    fun test_render_pos() {
        val posName = "sit"
        val camera = OrbitCamera(
            target = Vec3(0f, 12f, 0f),
            yaw = -40f,
            pitch = 0f,
            distance = 60f
        )
        for ((fileName, isSlim) in files) {
            val pose = PosePresets.withScale(isSlim, 1.5f, 1.2f, 1.2f)
            renderFile(File(fileName), isSlim, 800, 1200, camera, pose = pose).encodeToData()!!.let { data ->
                File("${posName}_rendered_$fileName").writeBytes(data.bytes)
            }
        }
    }

    @Test
    fun test_gif() {
        val camera = OrbitCamera(Vec3(0f, 12f, 0f), 45f, 20f, 60f)
        runBlocking(Dispatchers.IO) {
            for ((fileName, isSlim) in files) {
                val skin = Image.makeFromEncoded(File(fileName).readBytes())
                val bytes = renderRotate(
                    skin,
                    isSlim,
                    RenderConfig(600, 1000, camera, backgroundColor = backgroundColor),
                    30,
                    40
                )
                File("rotating_$fileName.gif").writeBytes(bytes)
            }
        }
    }
}