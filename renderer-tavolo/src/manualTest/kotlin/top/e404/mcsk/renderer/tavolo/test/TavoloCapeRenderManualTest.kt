package top.e404.mcsk.renderer.tavolo.test

import top.e404.mcsk.core.SkinLightingMode
import top.e404.mcsk.core.SkinOverlayMode
import top.e404.mcsk.core.SkinRenderRequest
import top.e404.mcsk.core.SkinRenderSettings
import top.e404.mcsk.core.SkinRenderVec3
import top.e404.mcsk.renderer.tavolo.TavoloSkinPngRenderer
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

class TavoloCapeRenderManualTest {
    @Test
    fun renderWikiCapeComparison() {
        val outputDir = File("manual-test-output/tavolo/render").apply { mkdirs() }
        val output = outputDir.resolve("cape_wiki_compare.png")

        TavoloSkinPngRenderer().use { renderer ->
            val withoutCape = renderer.renderPng(syntheticRequest(capePng = null))
            val withCape = renderer.renderPng(syntheticRequest(capePng = wikiManualCapePng()))
            output.writeBytes(stitchComparison(withoutCape, withCape))
        }

        assertTrue(output.isFile && output.length() > 0, "披风对比人工测试图片应输出到 ${output.absolutePath}")
    }
}

private fun syntheticRequest(capePng: ByteArray?): SkinRenderRequest =
    SkinRenderRequest(
        skinPng = syntheticSkinPng(),
        isSlim = false,
        yaw = 205f,
        settings = SkinRenderSettings(
            width = 480,
            height = 720,
            target = SkinRenderVec3(0f, 10f, 0f),
            pitch = 12f,
            distance = 65f,
            backgroundColor = DEFAULT_BG,
            lightDirection = SkinRenderVec3(-0.4f, 0.9f, -0.3f).normalized(),
            platformTopY = -8.2f,
            platformThickness = 2f,
            antiAliasingLevel = 2
        ),
        overlayMode = SkinOverlayMode.THREE_D,
        lightingMode = SkinLightingMode.DIRECTIONAL,
        shadows = false,
        showPlatform = true,
        capePng = capePng
    )

private fun syntheticSkinPng(): ByteArray {
    val image = BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    try {
        graphics.color = java.awt.Color(0xFF4AA3FF.toInt(), true)
        graphics.fillRect(0, 0, 64, 64)
        graphics.color = java.awt.Color(0xFFFFD08A.toInt(), true)
        graphics.fillRect(8, 8, 8, 8)
        graphics.color = java.awt.Color(0xFF2D6CDF.toInt(), true)
        graphics.fillRect(20, 20, 8, 12)
    } finally {
        graphics.dispose()
    }
    return encodePng(image)
}

private fun stitchComparison(withoutCape: ByteArray, withCape: ByteArray): ByteArray {
    val left = ImageIO.read(ByteArrayInputStream(withoutCape))
    val right = ImageIO.read(ByteArrayInputStream(withCape))
    val output = BufferedImage(left.width + right.width, maxOf(left.height, right.height), BufferedImage.TYPE_INT_ARGB)
    val graphics = output.createGraphics()
    try {
        graphics.drawImage(left, 0, 0, null)
        graphics.drawImage(right, left.width, 0, null)
    } finally {
        graphics.dispose()
    }
    return encodePng(output)
}

private fun encodePng(image: BufferedImage): ByteArray =
    ByteArrayOutputStream().use {
        ImageIO.write(image, "png", it)
        it.toByteArray()
    }
