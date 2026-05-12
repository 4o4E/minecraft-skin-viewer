package top.e404.skin.renderer.opengl.test

import kotlin.test.Test
import kotlin.test.assertTrue
import top.e404.skin.core.PosePresets
import top.e404.skin.core.SkinOverlayMode
import top.e404.skin.core.SkinRenderVec3
import top.e404.skin.renderer.opengl.OpenGlSkinPngRenderer
import java.io.File

class TestRender {
    private val renderer = OpenGlSkinPngRenderer()
    private val files = listOf(
        "alex_skin.png" to true,
        "steve_skin.png" to false
    )

    @Test
    fun testRender() {
        val outputDir = File("manual-test-output/opengl/render").apply { mkdirs() }
        renderer.startup()
        try {
            for ((fileName, isSlim) in files) {
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
                        shadows = true
                    )
                )
                outputDir.resolve("rendered_$fileName").writeBytes(bytes)
            }
        } finally {
            renderer.close()
        }
    }

    @Test
    fun testRenderHead() {
        val outputDir = File("manual-test-output/opengl/render").apply { mkdirs() }
        renderer.startup()
        try {
            for ((fileName, isSlim) in files) {
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
