package top.e404.skin.renderer.opengl.test

import kotlin.test.Test
import kotlin.test.assertTrue
import top.e404.skin.core.PosePresets
import top.e404.skin.core.SkinOverlayMode
import top.e404.skin.core.SkinRenderVec3
import top.e404.skin.renderer.opengl.OpenGlSkinPngRenderer
import java.io.File
import javax.imageio.ImageIO

class TestRender {
    private val renderer = OpenGlSkinPngRenderer()
    private val files = listOf(
        "alex_skin.png" to true,
        "steve_skin.png" to false
    )
    private val gifFrameCount = 12
    private val gifFrameDurationMs = 80
    private val shadowGridYaws = listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)
    private val shadowGridPitches = listOf(30f, 10f, -10f)

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
                        shadows = false
                    )
                )
                outputDir.resolve("rendered_$fileName").writeBytes(bytes)
            }
        } finally {
            renderer.close()
        }
    }

    @Test
    fun testRenderWithShadowPlatform() {
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
    fun testRenderShadow3DOverlayYawPitchGrid() {
        val outputDir = File("manual-test-output/opengl/render").apply { mkdirs() }
        renderer.startup()
        try {
            for ((fileName, isSlim) in files) {
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
    fun testRenderGifWith3DOverlayAndShadows() {
        val outputDir = File("manual-test-output/opengl/render").apply { mkdirs() }
        renderer.startup()
        try {
            for ((fileName, isSlim) in files) {
                val file = File(fileName)
                assertTrue(file.isFile, "Put $fileName in the run directory first.")
                val frames = (0 until gifFrameCount).map { index ->
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
                            modelYaw = 360f * index / gifFrameCount
                        )
                    )
                }
                writeGif(
                    frames = frames,
                    outputFile = outputDir.resolve("overlay3d_shadow_${file.nameWithoutExtension}.gif"),
                    durationMs = gifFrameDurationMs
                )
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
