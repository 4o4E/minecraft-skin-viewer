package top.e404.skin.core.test

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.e404.skin.core.SkinPngRenderer
import top.e404.skin.core.SkinRenderRequest
import top.e404.skin.core.SkinRenderUseCases

class SkinRenderUseCasesTest {
    @Test
    fun `rotate render uses renderer batch API`() {
        val renderer = RecordingRenderer()

        val gif = runSuspend {
            SkinRenderUseCases.renderSkinRotate(
                renderer = renderer,
                bytes = testPng(width = 64, height = 64),
                slim = true,
                backgroundColor = 0xFF1F1B1D.toInt(),
                frameCount = 3,
                pitchAmplitude = 20,
                lightIntensity = null,
                headScale = 1.0,
                duration = 40
            )
        }

        assertEquals(listOf(3), renderer.batchSizes)
        assertTrue(gif.isNotEmpty())
    }
}

private class RecordingRenderer : SkinPngRenderer {
    val batchSizes = mutableListOf<Int>()

    override val name = "recording"

    override fun startup() = Unit

    override fun renderPng(request: SkinRenderRequest): ByteArray =
        testPng(request.settings.width, request.settings.height)

    override fun renderPngBatch(requests: List<SkinRenderRequest>): List<ByteArray> {
        batchSizes += requests.size
        return requests.map(::renderPng)
    }
}

private fun testPng(width: Int, height: Int): ByteArray {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    try {
        graphics.color = java.awt.Color(0xFF55AAFF.toInt(), true)
        graphics.fillRect(0, 0, width, height)
    } finally {
        graphics.dispose()
    }
    return ByteArrayOutputStream().use {
        ImageIO.write(image, "png", it)
        it.toByteArray()
    }
}

private fun <T> runSuspend(block: suspend () -> T): T {
    var outcome: Result<T>? = null
    block.startCoroutine(
        object : Continuation<T> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(result: Result<T>) {
                outcome = result
            }
        }
    )
    return outcome!!.getOrThrow()
}
