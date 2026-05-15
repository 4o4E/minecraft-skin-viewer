package top.e404.skin.core

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Color
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.IRect
import top.e404.tavolo.frame.Frame
import top.e404.tavolo.frame.encodeToBytes
import kotlin.math.cos
import kotlin.math.sin

object SkinRenderUseCases {
    private const val FULL_WIDTH = 600
    private const val FULL_HEIGHT = 900
    private const val HEAD_SIZE = 400
    private const val HOMO_WIDTH = 1024
    private const val HOMO_HEIGHT = 768
    private const val DEFAULT_LIGHT_INTENSITY = 0.8f
    private const val SNEAK_MODEL_YAW = 90f

    fun renderSkin(
        renderer: SkinPngRenderer,
        bytes: ByteArray,
        slim: Boolean,
        backgroundColor: Int,
        lightIntensity: Float?,
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
                lightIntensity = lightIntensity,
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
        lightIntensity: Float?,
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
            lightIntensity = lightIntensity,
            duration = duration,
            target = SkinRenderVec3(0f, 10f, 0f),
            distance = 65f,
            showPlatform = showPlatform,
            pose = headScalePose(slim, headScale),
            antiAliasingLevel = 1,
            lightingMode = SkinLightingMode.AMBIENT
        )

    fun renderHead(
        renderer: SkinPngRenderer,
        bytes: ByteArray,
        backgroundColor: Int,
        lightIntensity: Float?,
        showPlatform: Boolean = false,
    ): ByteArray =
        renderer.renderPng(
            request = request(
                skinPng = bytes.formatSkinPng(),
                slim = false,
                width = HEAD_SIZE,
                height = HEAD_SIZE,
                backgroundColor = backgroundColor,
                lightIntensity = lightIntensity,
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
        lightIntensity: Float?,
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
            lightIntensity = lightIntensity,
            duration = duration,
            target = SkinRenderVec3(0f, 20f, 0f),
            distance = 30f,
            showPlatform = showPlatform,
            pose = PosePresets.HEAD_ONLY,
            antiAliasingLevel = 1,
            lightingMode = SkinLightingMode.AMBIENT
        )

    suspend fun renderSneak(
        renderer: SkinPngRenderer,
        bytes: ByteArray,
        slim: Boolean,
        backgroundColor: Int,
        lightIntensity: Float?,
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
                request(
                    skinPng,
                    slim,
                    FULL_WIDTH,
                    FULL_HEIGHT,
                    backgroundColor,
                    lightIntensity,
                    camera,
                    normalPose,
                    1,
                    showPlatform,
                    modelYaw = SNEAK_MODEL_YAW
                ),
                request(
                    skinPng,
                    slim,
                    FULL_WIDTH,
                    FULL_HEIGHT,
                    backgroundColor,
                    lightIntensity,
                    camera,
                    sneakPose,
                    1,
                    showPlatform,
                    modelYaw = SNEAK_MODEL_YAW
                )
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
        lightIntensity: Float?,
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
                lightIntensity = lightIntensity,
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
        lightIntensity: Float?,
        duration: Int,
        target: SkinRenderVec3,
        distance: Float,
        showPlatform: Boolean,
        pose: Map<BodyPart, List<SkinTransform>>,
        antiAliasingLevel: Int,
        lightingMode: SkinLightingMode = SkinLightingMode.DIRECTIONAL,
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
                lightIntensity = lightIntensity,
                camera = camera,
                pose = pose,
                antiAliasingLevel = antiAliasingLevel,
                showPlatform = showPlatform,
                modelYaw = modelYaw,
                lightingMode = lightingMode
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
        lightIntensity: Float?,
        camera: SkinCamera,
        pose: Map<BodyPart, List<SkinTransform>>,
        antiAliasingLevel: Int,
        showPlatform: Boolean = false,
        modelYaw: Float = 0f,
        lightingMode: SkinLightingMode = SkinLightingMode.DIRECTIONAL,
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
                lightIntensity = lightIntensity ?: DEFAULT_LIGHT_INTENSITY,
                antiAliasingLevel = antiAliasingLevel,
                platformTopY = -8.2f,
                platformThickness = 2f
            ),
            overlayMode = SkinOverlayMode.THREE_D,
            lightingMode = lightingMode,
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

    private fun sneakPose(): Map<BodyPart, List<SkinTransform>> {
        val rootOffset = SkinTransform.Translate(z = -3f)
        return mapOf(
            // 对齐迁移前 JFX sneak：整个人前移，头/身体/手臂局部调整，腿保持站立支撑。
            BodyPart.BODY to listOf(
                SkinTransform.Rotate(x = 30f),
                SkinTransform.Translate(y = -0.8f, z = -1f)
            ),
            BodyPart.HEAD to listOf(SkinTransform.Translate(y = -3f, z = 1.8f)),
            BodyPart.RIGHT_ARM to listOf(SkinTransform.Rotate(x = 30f), SkinTransform.Translate(y = -1.6f)),
            BodyPart.LEFT_ARM to listOf(SkinTransform.Rotate(x = 30f), SkinTransform.Translate(y = -1.6f)),
            BodyPart.RIGHT_LEG to listOf(rootOffset),
            BodyPart.LEFT_LEG to listOf(rootOffset)
        )
    }

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
    return frames.map { frame ->
        // 使用迁移前的 GIF 编码器，避免 ImageIO 逐帧调色板差异。
        Frame(frame.durationMs, Image.makeFromEncoded(frame.png))
    }.encodeToBytes()
}
