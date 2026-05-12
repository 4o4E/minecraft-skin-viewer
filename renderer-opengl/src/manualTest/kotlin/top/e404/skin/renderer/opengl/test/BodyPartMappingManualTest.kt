package top.e404.skin.renderer.opengl.test

import kotlin.test.Test
import kotlin.test.assertTrue
import top.e404.skin.core.SkinOverlayMode
import top.e404.skin.core.SkinRenderVec3
import top.e404.skin.renderer.opengl.OpenGlSkinPngRenderer
import java.io.File
import javax.imageio.ImageIO

class BodyPartMappingManualTest {
    private val outputDir = File("manual-test-output/opengl/body-part-mapping")
    private val tileSize = 720
    private val yaws = listOf(45f, 135f, 225f, 315f)
    private val pitches = listOf(25f, -25f)

    @Test
    fun renderExplodedAlexBodyParts() {
        val skinFile = File("alex_skin.png")
        assertTrue(skinFile.isFile, "Put alex_skin.png in the run directory first.")
        outputDir.mkdirs()

        val renderer = OpenGlSkinPngRenderer()
        renderer.startup()
        try {
            val pose = explodedPose(isSlim = true, gap = 4f)
            val renderedViews = pitches.flatMap { pitch ->
                yaws.map { yaw ->
                    renderer.renderPng(
                        renderRequest(
                            skinFile = skinFile,
                            slim = true,
                            width = tileSize,
                            height = tileSize,
                            target = SkinRenderVec3(0f, 10f, 0f),
                            yaw = yaw,
                            pitch = pitch,
                            distance = 58f,
                            lightDirection = SkinRenderVec3(0.4f, 0.8f, 0.6f).normalized(),
                            lightIntensity = 0.95f,
                            pose = pose,
                            overlayMode = SkinOverlayMode.FLAT,
                            shadows = false
                        )
                    )
                }
            }

            ImageIO.write(
                stitchPngs(renderedViews, columns = yaws.size),
                "png",
                outputDir.resolve("body_parts_alex_exploded.png")
            )
        } finally {
            renderer.close()
        }
    }
}
