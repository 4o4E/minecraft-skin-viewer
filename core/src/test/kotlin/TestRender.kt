package top.e404.skin.core.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Color
import org.jetbrains.skia.Image
import org.junit.jupiter.api.Test
import top.e404.skiko.draw.render3d.OrbitCamera
import top.e404.skiko.draw.render3d.Vec3
import top.e404.skin.core.BodyPart
import top.e404.skin.core.PosePresets
import top.e404.skin.core.Transformation
import top.e404.skin.core.renderMinecraftView
import top.e404.skin.core.renderRotate
import java.io.File

class TestRender {
    private val files = listOf(
        "alex_skin.png" to true,
        "steve_skin.png" to false
    )
    private val backgroundColor = Color.TRANSPARENT

    /**
     * 运行Minecraft皮肤渲染示例的函数。
     */
    fun renderFile(file: File, isSlim: Boolean, width: Int, height: Int, wavingPose: Map<BodyPart, List<Transformation>> = emptyMap()): Image {
        val camera = OrbitCamera(
            target = Vec3(0f, 12f, 0f),
            azimuthDegrees = 45f,
            elevationDegrees = 30f,
            distance = 60f
        )
        val skin = Image.makeFromEncoded(file.readBytes())
        return renderMinecraftView(skin, isSlim, width, height, backgroundColor, camera,
            Vec3(-0.7f, 1.0f, 0.5f).normalized(),
            wavingPose,
            true)
    }

    @Test
    fun test_render() {
        for ((fileName, isSlim) in files) {
            renderFile(File(fileName), isSlim, 800, 1200).encodeToData()!!.let { data ->
                File("rendered_$fileName").writeBytes(data.bytes)
            }
        }
    }

    @Test
    fun test_render_pos() {
        val walkingPose = PosePresets.SIT
        val posName = "sit"
        for ((fileName, isSlim) in files) {
            renderFile(File(fileName), isSlim, 800, 1200, walkingPose).encodeToData()!!.let { data ->
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
                val bytes = renderRotate(skin, isSlim, 600, 1000, backgroundColor, camera, 30, 40)
                File("rotating_$fileName.gif").writeBytes(bytes)
            }
        }
    }
}