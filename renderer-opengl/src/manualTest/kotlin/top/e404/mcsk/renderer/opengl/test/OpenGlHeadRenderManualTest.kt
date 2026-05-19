package top.e404.mcsk.renderer.opengl.test

import kotlin.test.Test
import kotlin.test.assertTrue
import top.e404.mcsk.core.PosePresets
import top.e404.mcsk.core.SkinOverlayMode
import top.e404.mcsk.core.SkinRenderVec3
import top.e404.mcsk.renderer.opengl.OpenGlSkinPngRenderer
import java.io.File

class OpenGlHeadRenderManualTest {
    private val renderer = OpenGlSkinPngRenderer()

    @Test
    fun renderHeadPngs() {
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
                        width = 1800,
                        height = 1800,
                        target = SkinRenderVec3(0f, 20f, 0f),
                        yaw = 45f,
                        pitch = 20f,
                        distance = 30f,
                        lightDirection = SkinRenderVec3(.3f, .3f, .3f).normalized(),
                        lightIntensity = .7f,
                        pose = PosePresets.HEAD_ONLY,
                        overlayMode = SkinOverlayMode.THREE_D,
                        shadows = false
                    )
                )
                outputDir.resolve("head_$fileName").writeBytes(bytes)
            }
        } finally {
            renderer.close()
        }
    }
}
