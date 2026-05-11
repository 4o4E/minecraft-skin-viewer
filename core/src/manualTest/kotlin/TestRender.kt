package top.e404.skin.core.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Color
import org.jetbrains.skia.Image
import org.junit.jupiter.api.Test
import top.e404.tavolo.draw.render3d.Mesh
import top.e404.tavolo.draw.render3d.OrbitCamera
import top.e404.tavolo.draw.render3d.RenderConfig
import top.e404.tavolo.draw.render3d.Transformation
import top.e404.tavolo.draw.render3d.Vec2
import top.e404.tavolo.draw.render3d.Vec3
import top.e404.tavolo.draw.render3d.createPlane
import top.e404.tavolo.util.Colors
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
        backgroundMeshes: List<Mesh> = emptyList(),
        lightDirection: Vec3 = Vec3(1f, 1.0f, 1f).normalized(),
        lightIntensity: Float = .8f,
        pose: Map<BodyPart, List<Transformation>> = emptyMap()
    ): Image {
        val skin = Image.makeFromEncoded(file.readBytes())
        val renderConfig = RenderConfig(
            width,
            height,
            camera,
            backgroundColor = backgroundColor,
            lightDirection = lightDirection,
            lightIntensity = lightIntensity,
            antiAliasingLevel = 4,
            shadowMapSize = 4096,
            shadowBias = 0.002f,
            shadowOrthoSize = 30f
        )
        return renderMinecraftView(
            skin,
            isSlim,
            renderConfig,
            backgroundMeshes,
            pose,
            true
        )
    }

    @Test
    fun test_render() {
        val lightIntensity = .7f

        val ground = createPlane(
            center = Vec3(-60f, -40f, 0f), // 脚下
            size = Vec2(160f, 160f),
            color = Color.WHITE,
            normalDirection = Vec3(0f, 1f, 0f)
        )
        val wall = createPlane(
            center = Vec3(-50f, 10f, -50f), // 身后背景墙
            size = Vec2(200f, 200f),
            color = Color.makeRGB(220, 220, 255),
            normalDirection = Vec3(0f, 0f, 1f) // 面向 Z 轴正向
        )
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
                listOf(ground, wall),
                lightIntensity = lightIntensity,
                lightDirection = Vec3(.5f, .3f, .3f).normalized()
            ).encodeToData()!!.let { data ->
                File("rendered_$fileName").writeBytes(data.bytes)
            }
        }
    }

    @Test
    fun test_render_head() {
        val lightIntensity = .7f
        val pose = PosePresets.HEAD_ONLY

        for ((fileName, isSlim) in files) {
            val camera = OrbitCamera(
                target = Vec3(0f, 20f, 0f),
                yaw = 45f,
                pitch = 20f,
                distance = 30f
            )
            renderFile(
                File(fileName),
                isSlim,
                1800,
                1800,
                camera,
                lightDirection = Vec3(.3f, .3f, .3f).normalized(),
                lightIntensity = lightIntensity,
                pose = pose,
            ).encodeToData()!!.let { data ->
                File("head_$fileName").writeBytes(data.bytes)
            }
        }
    }

    @Test
    fun test_render_pos() {
        val posName = "sit"
        val camera = OrbitCamera(
            target = Vec3(0f, 12f, 0f),
            yaw = -40f,
            pitch = 10f,
            distance = 90f
        )
        val ground = createPlane(
            center = Vec3(20f, -10f, 20f), // 脚下
            size = Vec2(130f, 130f),
            color = Color.makeRGB(200, 200, 200), // 灰色地面
            normalDirection = Vec3(0f, .5f, 0f).normalized()
        )
        val wall = createPlane(
            center = Vec3(20f, 10f, -20f), // 身后背景墙
            size = Vec2(80f, 80f),
            color = Color.makeRGB(220, 220, 255),
            normalDirection = Vec3(-.5f, 0f, 1f).normalized() // 面向 Z 轴正向
        )
        for ((fileName, isSlim) in files) {
            val pose = PosePresets.withScale(isSlim, 1.5f, 1.2f, 1.2f)
            renderFile(
                File(fileName),
                isSlim,
                800,
                1200,
                camera,
                // listOf(ground, wall),
                lightDirection = Vec3(.5f, 1.0f, 1f).normalized(),
                pose = pose
            ).encodeToData()!!.let { data ->
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