package top.e404.skin.renderer.tavolo.test

import kotlin.test.Test
import top.e404.tavolo.draw.render3d.OrbitCamera
import top.e404.tavolo.draw.render3d.Vec3
import java.io.File

class TavoloAnimationRenderManualTest {
    @Test
    fun renderOverlayShadowGifs() {
        tavoloRenderOutputDir.mkdirs()
        for ((fileName, isSlim) in manualSkinFiles) {
            val file = File(fileName)
            val camera = OrbitCamera(Vec3(0f, 10f, 0f), yaw = 45f, pitch = 15f, distance = 65f)
            val frames = (0 until GIF_FRAME_COUNT).map { index ->
                renderTavoloFile(
                    file = file,
                    isSlim = isSlim,
                    width = 480,
                    height = 720,
                    camera = camera,
                    lightIntensity = .7f,
                    lightDirection = Vec3(.5f, .9f, .35f).normalized(),
                    shadows = true,
                    showPlatform = true,
                    modelYaw = 360f * index / GIF_FRAME_COUNT
                )
            }
            writeGif(
                frames = frames,
                outputFile = tavoloRenderOutputDir.resolve("overlay3d_shadow_${file.nameWithoutExtension}.gif"),
                durationMs = GIF_FRAME_DURATION_MS
            )
        }
    }
}
