package top.e404.skin.renderer.opengl.test

import kotlin.test.Test
import kotlin.test.assertTrue
import top.e404.skin.core.SkinOverlayMode
import top.e404.skin.core.SkinRenderVec3
import top.e404.skin.renderer.opengl.OpenGlSkinPngRenderer
import java.io.File

class OpenGlAnimationRenderManualTest {
    private val renderer = OpenGlSkinPngRenderer()

    @Test
    fun renderOverlayShadowGifs() {
        val outputDir = openGlRenderOutputDir.apply { mkdirs() }
        renderer.startup()
        try {
            for ((fileName, isSlim) in manualSkinFiles) {
                val file = File(fileName)
                assertTrue(file.isFile, "Put $fileName in the run directory first.")
                val frames = (0 until GIF_FRAME_COUNT).map { index ->
                    renderer.renderPng(
                        renderRequest(
                            skinFile = file,
                            slim = isSlim,
                            width = 480,
                            height = 720,
                            target = SkinRenderVec3(0f, 10f, 0f),
                            yaw = 45f,
                            pitch = 15f,
                            distance = 65f,
                            lightDirection = SkinRenderVec3(.5f, .9f, .35f).normalized(),
                            lightIntensity = .7f,
                            overlayMode = SkinOverlayMode.THREE_D,
                            shadows = true,
                            showPlatform = true,
                            modelYaw = 360f * index / GIF_FRAME_COUNT
                        )
                    )
                }
                writeGif(
                    frames = frames,
                    outputFile = outputDir.resolve("overlay3d_shadow_${file.nameWithoutExtension}.gif"),
                    durationMs = GIF_FRAME_DURATION_MS
                )
            }
        } finally {
            renderer.close()
        }
    }
}
