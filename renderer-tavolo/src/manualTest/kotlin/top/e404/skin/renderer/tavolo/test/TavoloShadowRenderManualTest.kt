package top.e404.skin.renderer.tavolo.test

import kotlin.test.Test
import top.e404.tavolo.draw.render3d.OrbitCamera
import top.e404.tavolo.draw.render3d.Vec3
import java.io.File
import javax.imageio.ImageIO

class TavoloShadowRenderManualTest {
    @Test
    fun renderShadowPlatformPngs() {
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
                lightDirection = Vec3(.5f, .9f, .35f).normalized(),
                shadows = true,
                showPlatform = true
            ).encodeToData()!!.let { data ->
                tavoloRenderOutputDir.resolve("shadow_platform_$fileName").writeBytes(data.bytes)
            }
        }
    }

    @Test
    fun renderShadowOverlayYawPitchGrid() {
        tavoloRenderOutputDir.mkdirs()
        for ((fileName, isSlim) in manualSkinFiles) {
            val file = File(fileName)
            val renderedViews = shadowGridPitches.flatMap { pitch ->
                shadowGridYaws.map { yaw ->
                    renderTavoloFile(
                        file = file,
                        isSlim = isSlim,
                        width = 420,
                        height = 620,
                        camera = OrbitCamera(Vec3(0f, 10f, 0f), yaw = yaw, pitch = pitch, distance = 58f),
                        lightIntensity = .7f,
                        lightDirection = Vec3(.5f, .9f, .35f).normalized(),
                        shadows = true,
                        showPlatform = true
                    )
                }
            }

            ImageIO.write(
                stitchImages(renderedViews, columns = shadowGridYaws.size),
                "png",
                tavoloRenderOutputDir.resolve("shadow_overlay3d_yaw_pitch_${file.nameWithoutExtension}.png")
            )
        }
    }
}
