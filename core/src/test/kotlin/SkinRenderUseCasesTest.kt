package top.e404.mcsk.core.test

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.imageio.metadata.IIOMetadataNode
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.e404.mcsk.core.BodyPart
import top.e404.mcsk.core.SkinLightingMode
import top.e404.mcsk.core.SkinOverlayMode
import top.e404.mcsk.core.SkinPngRenderer
import top.e404.mcsk.core.SkinRenderOptions
import top.e404.mcsk.core.SkinRenderRequest
import top.e404.mcsk.core.SkinTransform
import top.e404.mcsk.core.SkinRenderUseCases
import top.e404.mcsk.core.SkinRenderVec3

class SkinRenderUseCasesTest {
    @Test
    fun `render options are forwarded to renderer request`() {
        val renderer = RecordingRenderer()
        val options = SkinRenderOptions(
            width = 320,
            height = 480,
            target = SkinRenderVec3(1f, 2f, 3f),
            yaw = 90f,
            pitch = 25f,
            distance = 42f,
            backgroundColor = 0xFF112233.toInt(),
            lightIntensity = 0.5f,
            lightDirection = SkinRenderVec3(0.2f, 0.8f, 0.4f),
            platformTopY = -7f,
            platformThickness = 1.5f,
            antiAliasingLevel = 4,
            overlayMode = SkinOverlayMode.FLAT,
            lightingMode = SkinLightingMode.DIRECTIONAL,
            shadows = true,
            showPlatform = true,
            modelYaw = 45f,
            pose = mapOf(BodyPart.BODY to listOf(SkinTransform.Translate(z = 2f)))
        )
        val capeBytes = testPng(width = 64, height = 32)

        SkinRenderUseCases.renderSkin(
            renderer = renderer,
            bytes = testPng(width = 64, height = 64),
            slim = true,
            headScale = 1.0,
            options = options,
            capeBytes = capeBytes
        )

        val request = renderer.requests.single()
        assertEquals(320, request.settings.width)
        assertEquals(480, request.settings.height)
        assertEquals(SkinRenderVec3(1f, 2f, 3f), request.settings.target)
        assertEquals(90f, request.yaw)
        assertEquals(25f, request.settings.pitch)
        assertEquals(42f, request.settings.distance)
        assertEquals(0xFF112233.toInt(), request.settings.backgroundColor)
        assertEquals(0.5f, request.settings.lightIntensity)
        assertEquals(SkinRenderVec3(0.2f, 0.8f, 0.4f).normalized(), request.settings.lightDirection)
        assertEquals(-7f, request.settings.platformTopY)
        assertEquals(1.5f, request.settings.platformThickness)
        assertEquals(4, request.settings.antiAliasingLevel)
        assertEquals(SkinOverlayMode.FLAT, request.overlayMode)
        assertEquals(SkinLightingMode.DIRECTIONAL, request.lightingMode)
        assertEquals(true, request.shadows)
        assertEquals(true, request.showPlatform)
        assertTrue(request.capePng?.contentEquals(capeBytes) == true)
        assertEquals(45f, request.modelYaw)
        assertEquals(listOf(SkinTransform.Translate(z = 2f)), request.pose.getValue(BodyPart.BODY))
    }

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
        assertTrue(renderer.requests.all { it.lightingMode == SkinLightingMode.AMBIENT })
        assertTrue(gif.isNotEmpty())
    }

    @Test
    fun `sneak pose matches legacy jfx offsets`() {
        val renderer = RecordingRenderer()

        val gif = runSuspend {
            SkinRenderUseCases.renderSneak(
                renderer = renderer,
                bytes = testPng(width = 64, height = 64),
                slim = true,
                backgroundColor = 0xFF1F1B1D.toInt(),
                lightIntensity = null,
                headScale = 1.0
            )
        }

        assertTrue(renderer.requests.all { it.modelYaw == 270f })
        val sneakPose = renderer.requests[1].pose
        assertEquals(listOf(SkinTransform.Translate(y = -3f, z = 1.8f)), sneakPose.getValue(BodyPart.HEAD))
        assertEquals(
            listOf(SkinTransform.Rotate(x = 30f), SkinTransform.Translate(y = -0.8f, z = -1f)),
            sneakPose.getValue(BodyPart.BODY)
        )
        assertEquals(
            listOf(SkinTransform.Rotate(x = 30f), SkinTransform.Translate(y = -1.6f)),
            sneakPose.getValue(BodyPart.RIGHT_ARM)
        )
        assertEquals(
            listOf(SkinTransform.Rotate(x = 30f), SkinTransform.Translate(y = -1.6f)),
            sneakPose.getValue(BodyPart.LEFT_ARM)
        )
        assertEquals(listOf(SkinTransform.Translate(z = -3f)), sneakPose.getValue(BodyPart.RIGHT_LEG))
        assertEquals(listOf(SkinTransform.Translate(z = -3f)), sneakPose.getValue(BodyPart.LEFT_LEG))
        assertEquals(
            listOf(SkinRenderUseCases.SNEAK_FRAME_DURATION_MS, SkinRenderUseCases.SNEAK_FRAME_DURATION_MS),
            gifFrameDurationsMs(gif)
        )
        assertTrue(gif.isNotEmpty())
    }
}

private class RecordingRenderer : SkinPngRenderer {
    val batchSizes = mutableListOf<Int>()
    val requests = mutableListOf<SkinRenderRequest>()

    override val name = "recording"

    override fun startup() = Unit

    override fun renderPng(request: SkinRenderRequest): ByteArray {
        requests += request
        return testPng(request.settings.width, request.settings.height)
    }

    override fun renderPngBatch(requests: List<SkinRenderRequest>): List<ByteArray> {
        batchSizes += requests.size
        this.requests += requests
        return requests.map { testPng(it.settings.width, it.settings.height) }
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

private fun gifFrameDurationsMs(bytes: ByteArray): List<Int> {
    val reader = ImageIO.getImageReadersByFormatName("gif").asSequence().first()
    try {
        ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { input ->
            reader.input = input
            return (0 until reader.getNumImages(true)).map { index ->
                val metadata = reader.getImageMetadata(index)
                val root = metadata.getAsTree(metadata.nativeMetadataFormatName) as IIOMetadataNode
                val gce = root.child("GraphicControlExtension")
                gce.getAttribute("delayTime").toInt() * 10
            }
        }
    } finally {
        reader.dispose()
    }
}

private fun IIOMetadataNode.child(name: String): IIOMetadataNode {
    for (i in 0 until length) {
        val node = item(i)
        if (node.nodeName == name) return node as IIOMetadataNode
    }
    throw IllegalArgumentException("Missing GIF metadata node: $name")
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
