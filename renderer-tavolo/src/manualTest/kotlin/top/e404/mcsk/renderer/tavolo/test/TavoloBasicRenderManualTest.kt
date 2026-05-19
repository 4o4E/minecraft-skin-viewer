package top.e404.mcsk.renderer.tavolo.test

import kotlin.test.Test
import top.e404.tavolo.draw.render3d.OrbitCamera
import top.e404.tavolo.draw.render3d.Vec3
import java.io.File

class TavoloBasicRenderManualTest {
    @Test
    fun renderFullBodyPngs() {
        tavoloRenderOutputDir.mkdirs()
        for ((fileName, isSlim) in manualSkinFiles) {
            val camera = OrbitCamera(Vec3(0f, 10f, 0f), yaw = 45f, pitch = 20f, distance = 50f)
            renderTavoloFile(
                file = File(fileName),
                isSlim = isSlim,
                width = 800,
                height = 1200,
                camera = camera,
                lightIntensity = .7f,
                lightDirection = Vec3(.5f, .3f, .3f).normalized()
            ).encodeToData()!!.let { data ->
                tavoloRenderOutputDir.resolve("rendered_$fileName").writeBytes(data.bytes)
            }
        }
    }
}
