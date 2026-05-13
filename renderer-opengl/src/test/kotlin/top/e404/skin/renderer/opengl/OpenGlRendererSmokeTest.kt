package top.e404.skin.renderer.opengl

import org.junit.jupiter.api.Assumptions.assumeTrue
import top.e404.skin.core.SkinLightingMode
import top.e404.skin.core.SkinOverlayMode
import top.e404.skin.core.SkinRenderRequest
import top.e404.skin.core.SkinRenderSettings
import top.e404.skin.core.SkinRenderVec3
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OpenGlRendererSmokeTest {
    @Test
    fun `renders a non-empty PNG`() {
        assumeTrue(
            System.getProperty("skin.openglSmoke") == "true" ||
                System.getenv("SKIN_OPENGL_SMOKE") == "true",
            "OpenGL smoke test requires an explicit Xvfb/OpenGL environment"
        )
        val renderer = OpenGlSkinPngRenderer()
        try {
            renderer.startup()
            val bytes = renderer.renderPng(smokeRequest())
            assertRenderedPng(bytes, width = 160, height = 240)
        } finally {
            renderer.close()
        }
    }
}

private fun smokeRequest(): SkinRenderRequest =
    SkinRenderRequest(
        skinPng = smokeSkinPng(),
        isSlim = true,
        yaw = 45f,
        settings = SkinRenderSettings(
            width = 160,
            height = 240,
            target = SkinRenderVec3(0f, 10f, 0f),
            pitch = 15f,
            distance = 65f,
            backgroundColor = 0xFF1F1B1D.toInt(),
            lightDirection = SkinRenderVec3(-0.5f, 1f, -0.5f).normalized(),
            platformTopY = -8.2f,
            platformThickness = 2f,
            antiAliasingLevel = 1
        ),
        overlayMode = SkinOverlayMode.THREE_D,
        lightingMode = SkinLightingMode.DIRECTIONAL,
        shadows = false,
        showPlatform = true
    )

private fun smokeSkinPng(): ByteArray {
    val image = BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    try {
        graphics.color = java.awt.Color(0xFF55AAFF.toInt(), true)
        graphics.fillRect(0, 0, 64, 64)
        graphics.color = java.awt.Color(0xFFFFCC66.toInt(), true)
        graphics.fillRect(8, 8, 8, 8)
        graphics.color = java.awt.Color(0xFFCC3333.toInt(), true)
        graphics.fillRect(20, 20, 8, 12)
        graphics.color = java.awt.Color(0xFF33AA55.toInt(), true)
        graphics.fillRect(44, 20, 4, 12)
    } finally {
        graphics.dispose()
    }
    return ByteArrayOutputStream().use {
        ImageIO.write(image, "png", it)
        it.toByteArray()
    }
}

private fun assertRenderedPng(bytes: ByteArray, width: Int, height: Int) {
    assertTrue(bytes.isNotEmpty(), "Renderer should return PNG bytes")
    val image = assertNotNull(ImageIO.read(ByteArrayInputStream(bytes)), "Renderer output should decode as an image")
    assertEquals(width, image.width)
    assertEquals(height, image.height)
    val background = image.getRGB(0, 0)
    var foregroundPixels = 0
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            if (image.getRGB(x, y) != background) {
                foregroundPixels++
                if (foregroundPixels > 100) return
            }
        }
    }
    assertTrue(false, "Rendered image should contain foreground pixels")
}
