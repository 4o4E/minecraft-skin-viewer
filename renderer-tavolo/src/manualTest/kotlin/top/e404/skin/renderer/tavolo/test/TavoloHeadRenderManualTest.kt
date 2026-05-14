package top.e404.skin.renderer.tavolo.test

import kotlin.test.Test
import top.e404.skin.core.PosePresets
import top.e404.tavolo.draw.render3d.OrbitCamera
import top.e404.tavolo.draw.render3d.Vec3
import java.io.File

class TavoloHeadRenderManualTest {
    @Test
    fun renderHeadPngs() {
        tavoloRenderOutputDir.mkdirs()
        for ((fileName, isSlim) in manualSkinFiles) {
            val camera = OrbitCamera(Vec3(0f, 20f, 0f), yaw = 45f, pitch = 20f, distance = 30f)
            renderTavoloFile(
                file = File(fileName),
                isSlim = isSlim,
                width = 1800,
                height = 1800,
                camera = camera,
                lightDirection = Vec3(.3f, .3f, .3f).normalized(),
                lightIntensity = .7f,
                pose = PosePresets.HEAD_ONLY,
            ).encodeToData()!!.let { data ->
                tavoloRenderOutputDir.resolve("head_$fileName").writeBytes(data.bytes)
            }
        }
    }
}
