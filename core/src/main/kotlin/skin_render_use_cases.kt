package top.e404.skin.core

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Color
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.IRect
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.MemoryCacheImageOutputStream
import kotlin.math.cos
import kotlin.math.sin

object SkinRenderUseCases {
    private const val FULL_WIDTH = 600
    private const val FULL_HEIGHT = 900
    private const val HEAD_SIZE = 400
    private const val HOMO_WIDTH = 1024
    private const val HOMO_HEIGHT = 768
    private const val DEFAULT_LIGHT_INTENSITY = 0.8f

    fun renderSkin(
        renderer: SkinPngRenderer,
        bytes: ByteArray,
        slim: Boolean,
        backgroundColor: Int,
        lightColor: Int?,
        headScale: Double,
        showPlatform: Boolean = false,
    ): ByteArray =
        renderer.renderPng(
            request = request(
                skinPng = bytes.formatSkinPng(),
                slim = slim,
                width = FULL_WIDTH,
                height = FULL_HEIGHT,
                backgroundColor = backgroundColor,
                lightColor = lightColor,
                camera = SkinCamera(SkinRenderVec3(0f, 10f, 0f), yaw = 45f, pitch = 15f, distance = 65f),
                pose = headScalePose(slim, headScale),
                antiAliasingLevel = 2,
                showPlatform = showPlatform
            )
        )

    suspend fun renderSkinRotate(
        renderer: SkinPngRenderer,
        bytes: ByteArray,
        slim: Boolean,
        backgroundColor: Int,
        frameCount: Int,
        pitchAmplitude: Int,
        lightColor: Int?,
        headScale: Double,
        duration: Int,
        showPlatform: Boolean = true,
    ): ByteArray =
        renderRotate(
            renderer = renderer,
            skinPng = bytes.formatSkinPng(),
            slim = slim,
            width = FULL_WIDTH,
            height = FULL_HEIGHT,
            backgroundColor = backgroundColor,
            frameCount = frameCount,
            pitch = pitchAmplitude.toFloat(),
            lightColor = lightColor,
            duration = duration,
            target = SkinRenderVec3(0f, 10f, 0f),
            distance = 65f,
            showPlatform = showPlatform,
            pose = headScalePose(slim, headScale),
            antiAliasingLevel = 1
        )

    fun renderHead(
        renderer: SkinPngRenderer,
        bytes: ByteArray,
        backgroundColor: Int,
        lightColor: Int?,
        showPlatform: Boolean = false,
    ): ByteArray =
        renderer.renderPng(
            request = request(
                skinPng = bytes.formatSkinPng(),
                slim = false,
                width = HEAD_SIZE,
                height = HEAD_SIZE,
                backgroundColor = backgroundColor,
                lightColor = lightColor,
                camera = SkinCamera(SkinRenderVec3(0f, 20f, 0f), yaw = 45f, pitch = 15f, distance = 30f),
                pose = PosePresets.HEAD_ONLY,
                antiAliasingLevel = 2,
                showPlatform = showPlatform
            )
        )

    suspend fun renderHeadRotate(
        renderer: SkinPngRenderer,
        bytes: ByteArray,
        backgroundColor: Int,
        frameCount: Int,
        pitchAmplitude: Int,
        lightColor: Int?,
        duration: Int,
        showPlatform: Boolean = false,
    ): ByteArray =
        renderRotate(
            renderer = renderer,
            skinPng = bytes.formatSkinPng(),
            slim = false,
            width = HEAD_SIZE,
            height = HEAD_SIZE,
            backgroundColor = backgroundColor,
            frameCount = frameCount,
            pitch = pitchAmplitude.toFloat(),
            lightColor = lightColor,
            duration = duration,
            target = SkinRenderVec3(0f, 20f, 0f),
            distance = 30f,
            showPlatform = showPlatform,
            pose = PosePresets.HEAD_ONLY,
            antiAliasingLevel = 1
        )

    suspend fun renderSneak(
        renderer: SkinPngRenderer,
        bytes: ByteArray,
        slim: Boolean,
        backgroundColor: Int,
        lightColor: Int?,
        headScale: Double,
        duration: Int,
        showPlatform: Boolean = false,
    ): ByteArray {
        val skinPng = bytes.formatSkinPng()
        val normalPose = headScalePose(slim, headScale)
        val sneakPose = mergePose(normalPose, sneakPose())
        val camera = SkinCamera(SkinRenderVec3(0f, 10f, 0f), yaw = 315f, pitch = 10f, distance = 65f)
        val pngs = renderer.renderPngBatch(
            listOf(
                request(skinPng, slim, FULL_WIDTH, FULL_HEIGHT, backgroundColor, lightColor, camera, normalPose, 1, showPlatform),
                request(skinPng, slim, FULL_WIDTH, FULL_HEIGHT, backgroundColor, lightColor, camera, sneakPose, 1, showPlatform)
            )
        )
        return encodeGif(
            listOf(
                SkinAnimationFrame(
                    durationMs = duration,
                    png = pngs[0]
                ),
                SkinAnimationFrame(
                    durationMs = duration,
                    png = pngs[1]
                )
            )
        )
    }

    fun renderHomo(
        renderer: SkinPngRenderer,
        bytes: ByteArray,
        slim: Boolean,
        backgroundColor: Int,
        lightColor: Int?,
        headScale: Double,
        showPlatform: Boolean = false,
    ): ByteArray =
        renderer.renderPng(
            request = request(
                skinPng = bytes.formatSkinPng(),
                slim = slim,
                width = HOMO_WIDTH,
                height = HOMO_HEIGHT,
                backgroundColor = backgroundColor,
                lightColor = lightColor,
                camera = SkinCamera(SkinRenderVec3(0f, 8f, 0f), yaw = 30f, pitch = 0f, distance = 80f),
                pose = PosePresets.withScale(slim, headScale = headScale.toFloat()),
                antiAliasingLevel = 2,
                showPlatform = showPlatform
            )
        )

    private suspend fun renderRotate(
        renderer: SkinPngRenderer,
        skinPng: ByteArray,
        slim: Boolean,
        width: Int,
        height: Int,
        backgroundColor: Int,
        frameCount: Int,
        pitch: Float,
        lightColor: Int?,
        duration: Int,
        target: SkinRenderVec3,
        distance: Float,
        showPlatform: Boolean,
        pose: Map<BodyPart, List<SkinTransform>>,
        antiAliasingLevel: Int,
    ): ByteArray {
        val frames = frameCount.coerceAtLeast(1)
        val camera = SkinCamera(target, yaw = 45f, pitch = pitch, distance = distance)
        val requests = (0 until frames).map { index ->
            val modelYaw = 360f * index / frames
            request(
                skinPng = skinPng,
                slim = slim,
                width = width,
                height = height,
                backgroundColor = backgroundColor,
                lightColor = lightColor,
                camera = camera,
                pose = pose,
                antiAliasingLevel = antiAliasingLevel,
                showPlatform = showPlatform,
                modelYaw = modelYaw
            )
        }
        val pngs = renderer.renderPngBatch(requests)
        return encodeGif(
            pngs.map {
                SkinAnimationFrame(
                    durationMs = duration,
                    png = it
                )
            }
        )
    }

    private fun request(
        skinPng: ByteArray,
        slim: Boolean,
        width: Int,
        height: Int,
        backgroundColor: Int,
        lightColor: Int?,
        camera: SkinCamera,
        pose: Map<BodyPart, List<SkinTransform>>,
        antiAliasingLevel: Int,
        showPlatform: Boolean = false,
        modelYaw: Float = 0f,
    ): SkinRenderRequest =
        SkinRenderRequest(
            skinPng = skinPng,
            isSlim = slim,
            yaw = camera.yaw,
            settings = SkinRenderSettings(
                width = width,
                height = height,
                target = camera.target,
                pitch = camera.pitch,
                distance = camera.distance,
                backgroundColor = backgroundColor,
                lightDirection = camera.relativeUpperLeftLight(),
                lightIntensity = lightIntensity(lightColor),
                antiAliasingLevel = antiAliasingLevel,
                platformTopY = -8.2f,
                platformThickness = 2f
            ),
            overlayMode = SkinOverlayMode.THREE_D,
            lightingMode = SkinLightingMode.DIRECTIONAL,
            shadows = false,
            showPlatform = showPlatform,
            pose = pose,
            modelYaw = modelYaw
        )

    private fun headScalePose(
        slim: Boolean,
        headScale: Double,
    ): Map<BodyPart, List<SkinTransform>> {
        val scale = headScale.toFloat()
        if (scale == 1f) return emptyMap()
        return mapOf(
            BodyPart.HEAD to listOf(
                SkinTransform.Scale(scale, scale, scale),
                SkinTransform.Translate(y = BodyPart.HEAD.getDims(slim).y * (scale - 1f) / 2f)
            )
        )
    }

    private fun sneakPose(): Map<BodyPart, List<SkinTransform>> = mapOf(
        BodyPart.HEAD to listOf(SkinTransform.Translate(y = 3f, z = -4.8f)),
        BodyPart.BODY to listOf(SkinTransform.Rotate(x = 30f), SkinTransform.Translate(y = 0.8f, z = -2f)),
        BodyPart.RIGHT_ARM to listOf(SkinTransform.Rotate(x = 30f), SkinTransform.Translate(y = 1.6f, z = -3f)),
        BodyPart.LEFT_ARM to listOf(SkinTransform.Rotate(x = 30f), SkinTransform.Translate(y = 1.6f, z = -3f))
    )

    private fun mergePose(
        first: Map<BodyPart, List<SkinTransform>>,
        second: Map<BodyPart, List<SkinTransform>>,
    ): Map<BodyPart, List<SkinTransform>> {
        if (first.isEmpty()) return second
        if (second.isEmpty()) return first
        return (first.keys + second.keys).associateWith { part ->
            first[part].orEmpty() + second[part].orEmpty()
        }
    }

    private fun lightIntensity(color: Int?): Float {
        if (color == null) return DEFAULT_LIGHT_INTENSITY
        val luminance = (0.2126f * Color.getR(color) + 0.7152f * Color.getG(color) + 0.0722f * Color.getB(color)) / 255f
        return luminance.coerceIn(0.2f, 1f)
    }
}

private data class SkinCamera(
    val target: SkinRenderVec3,
    val yaw: Float,
    val pitch: Float,
    val distance: Float,
) {
    fun relativeUpperLeftLight(): SkinRenderVec3 {
        val forward = (target - eye()).normalized()
        val right = forward.cross(SkinRenderVec3(0f, 1f, 0f)).normalized()
        val up = right.cross(forward).normalized()
        return ((-right * 0.9f) + (up * 1.1f) + (-forward * 0.45f)).normalized()
    }

    private fun eye(): SkinRenderVec3 {
        val yawRad = Math.toRadians(yaw.toDouble())
        val pitchRad = Math.toRadians(pitch.toDouble())
        val horizontalDistance = (cos(pitchRad) * distance).toFloat()
        return SkinRenderVec3(
            target.x + (sin(yawRad) * horizontalDistance).toFloat(),
            target.y + (sin(pitchRad) * distance).toFloat(),
            target.z + (cos(yawRad) * horizontalDistance).toFloat()
        )
    }
}

private data class SkinAnimationFrame(val durationMs: Int, val png: ByteArray)

private fun ByteArray.formatSkinPng(): ByteArray {
    val image = Image.makeFromEncoded(this)
    if (image.width == image.height) return this

    val source = Bitmap.makeFromImage(image)
    val multiple = (image.width / 64).coerceAtLeast(1)
    val target = Bitmap().apply {
        allocN32Pixels(image.width, image.height * 2)
        erase(Color.TRANSPARENT)
    }

    fun copy(srcX: Int, srcY: Int, w: Int, h: Int, dstX: Int, dstY: Int) {
        for (x in 0 until w) {
            for (y in 0 until h) {
                target.erase(source.getColor(srcX + x, srcY + y), IRect.makeXYWH(dstX + x, dstY + y, 1, 1))
            }
        }
    }

    copy(0, 0, image.width, image.height, 0, 0)
    copy(0, 16 * multiple, 16 * multiple, 16 * multiple, 16 * multiple, 48 * multiple)
    copy(40 * multiple, 16 * multiple, 16 * multiple, 16 * multiple, 32 * multiple, 48 * multiple)
    return Image.makeFromBitmap(target).encodeToData(EncodedImageFormat.PNG)!!.bytes
}

private fun encodeGif(frames: List<SkinAnimationFrame>): ByteArray {
    require(frames.isNotEmpty()) { "GIF must contain at least one frame" }
    val writer = ImageIO.getImageWritersBySuffix("gif").asSequence().first()
    val outputBytes = ByteArrayOutputStream()
    MemoryCacheImageOutputStream(outputBytes).use { output ->
        writer.output = output
        writer.prepareWriteSequence(null)
        frames.forEachIndexed { index, frame ->
            val image = ImageIO.read(ByteArrayInputStream(frame.png)).toArgb()
            val params = writer.defaultWriteParam
            val imageType = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB)
            val metadata = writer.getDefaultImageMetadata(imageType, params)
            configureGifMetadata(metadata, frame.durationMs, loop = index == 0)
            writer.writeToSequence(IIOImage(image, null, metadata), params)
        }
        writer.endWriteSequence()
    }
    writer.dispose()
    return outputBytes.toByteArray()
}

private fun configureGifMetadata(metadata: javax.imageio.metadata.IIOMetadata, durationMs: Int, loop: Boolean) {
    val format = metadata.nativeMetadataFormatName
    val root = metadata.getAsTree(format) as IIOMetadataNode
    val gce = root.child("GraphicControlExtension")
    gce.setAttribute("disposalMethod", "none")
    gce.setAttribute("userInputFlag", "FALSE")
    gce.setAttribute("transparentColorFlag", "FALSE")
    gce.setAttribute("delayTime", (durationMs / 10).coerceAtLeast(1).toString())
    gce.setAttribute("transparentColorIndex", "0")

    if (loop) {
        val appExtensions = root.child("ApplicationExtensions")
        val appExtension = IIOMetadataNode("ApplicationExtension")
        appExtension.setAttribute("applicationID", "NETSCAPE")
        appExtension.setAttribute("authenticationCode", "2.0")
        appExtension.userObject = byteArrayOf(1, 0, 0)
        appExtensions.appendChild(appExtension)
    }

    metadata.setFromTree(format, root)
}

private fun IIOMetadataNode.child(name: String): IIOMetadataNode {
    for (i in 0 until length) {
        val node = item(i)
        if (node.nodeName == name) return node as IIOMetadataNode
    }
    return IIOMetadataNode(name).also { appendChild(it) }
}

private fun BufferedImage.toArgb(): BufferedImage {
    if (type == BufferedImage.TYPE_INT_ARGB) return this
    val converted = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val graphics = converted.createGraphics()
    graphics.drawImage(this, 0, 0, null)
    graphics.dispose()
    return converted
}
