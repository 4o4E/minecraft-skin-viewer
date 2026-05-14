package top.e404.skin.renderer.opengl.test

import kotlin.test.Test
import kotlin.test.assertTrue
import top.e404.skin.core.SkinOverlayMode
import top.e404.skin.core.SkinRenderVec3
import top.e404.skin.renderer.opengl.OpenGlSkinPngRenderer
import java.io.File

class OpenGlBasicRenderManualTest {
    private val renderer = OpenGlSkinPngRenderer()

    @Test
    fun renderFullBodyPngs() {
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
                        lightDirection = SkinRenderVec3(.5f, .3f, .3f).normalized(),
                        lightIntensity = .7f,
                        overlayMode = SkinOverlayMode.THREE_D,
                        shadows = false
                    )
                )
                outputDir.resolve("rendered_$fileName").writeBytes(bytes)
            }
        } finally {
            renderer.close()
        }
    }
}
