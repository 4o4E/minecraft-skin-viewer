package top.e404.skin.renderer.opengl.test

import kotlin.test.Test
import kotlin.test.assertTrue
import top.e404.skin.core.SkinOverlayMode
import top.e404.skin.core.SkinRenderVec3
import top.e404.skin.renderer.opengl.OpenGlSkinPngRenderer
import java.io.File
import javax.imageio.ImageIO

class OpenGlShadowRenderManualTest {
    private val renderer = OpenGlSkinPngRenderer()

    @Test
    fun renderShadowPlatformPngs() {
        val outputDir = openGlRenderOutputDir.apply { mkdirs() }
        renderer.startup()
        try {
            for ((fileName, isSlim) in manualSkinFiles) {
                val file = File(fileName)
                assertTrue(file.isFile, "Put $fileName in the run directory first.")
                val bytes = renderer.renderPng(
                    renderRequest(
                        skinFile = file,
                        slim = isSlim,
                        width = 800,
                        height = 1200,
                        target = SkinRenderVec3(0f, 10f, 0f),
                        yaw = 45f,
                        pitch = 20f,
                        distance = 50f,
                        lightDirection = SkinRenderVec3(.5f, .9f, .35f).normalized(),
                        lightIntensity = .7f,
                        overlayMode = SkinOverlayMode.THREE_D,
                        shadows = true,
                        showPlatform = true
                    )
                )
                outputDir.resolve("shadow_platform_$fileName").writeBytes(bytes)
            }
        } finally {
            renderer.close()
        }
    }

    @Test
    fun renderShadowOverlayYawPitchGrid() {
        val outputDir = openGlRenderOutputDir.apply { mkdirs() }
        renderer.startup()
        try {
            for ((fileName, isSlim) in manualSkinFiles) {
                val file = File(fileName)
                assertTrue(file.isFile, "Put $fileName in the run directory first.")
                val renderedViews = shadowGridPitches.flatMap { pitch ->
                    shadowGridYaws.map { yaw ->
                        renderer.renderPng(
                            renderRequest(
                                skinFile = file,
                                slim = isSlim,
                                width = 420,
                                height = 620,
                                target = SkinRenderVec3(0f, 10f, 0f),
                                yaw = yaw,
                                pitch = pitch,
                                distance = 58f,
                                lightDirection = SkinRenderVec3(.5f, .9f, .35f).normalized(),
                                lightIntensity = .7f,
                                overlayMode = SkinOverlayMode.THREE_D,
                                shadows = true,
                                showPlatform = true
                            )
                        )
                    }
                }

                ImageIO.write(
                    stitchPngs(renderedViews, columns = shadowGridYaws.size),
                    "png",
                    outputDir.resolve("shadow_overlay3d_yaw_pitch_${file.nameWithoutExtension}.png")
                )
            }
        } finally {
            renderer.close()
        }
    }

    @Test
    fun renderShadowRotate90Step15ContactSheets() {
        val outputDir = openGlRenderOutputDir.apply { mkdirs() }
        renderer.startup()
        try {
            for ((fileName, isSlim) in manualSkinFiles) {
                val file = File(fileName)
                assertTrue(file.isFile, "Put $fileName in the run directory first.")

                // 0 到 90 度每 15 度一帧，定位旋转时阴影贴图和地台投影的异常变化。
                val frames = (0..90 step 15).map { yaw ->
                    renderer.renderPng(
                        renderRequest(
                            skinFile = file,
                            slim = isSlim,
                            width = 600,
                            height = 900,
                            target = SkinRenderVec3(0f, 10f, 0f),
                            yaw = 45f,
                            pitch = 15f,
                            distance = 65f,
                            lightDirection = SkinRenderVec3(.5f, .9f, .35f).normalized(),
                            lightIntensity = .7f,
                            overlayMode = SkinOverlayMode.THREE_D,
                            shadows = true,
                            showPlatform = true,
                            modelYaw = yaw.toFloat()
                        )
                    )
                }

                ImageIO.write(
                    stitchPngs(frames, columns = 4),
                    "png",
                    outputDir.resolve("shadow_rotate_0_90_step_15_${file.nameWithoutExtension}.png")
                )
            }
        } finally {
            renderer.close()
        }
    }
}
