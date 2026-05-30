package top.e404.mcsk.core

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
    const val SNEAK_FRAME_DURATION_MS = 40

    const val FULL_WIDTH = 600
    const val FULL_HEIGHT = 900
    const val HEAD_SIZE = 400
    const val HOMO_WIDTH = 1024
    const val HOMO_HEIGHT = 768
    const val DEFAULT_LIGHT_INTENSITY = 0.8f
    const val DEFAULT_PLATFORM_TOP_Y = -8.2f
    const val DEFAULT_PLATFORM_THICKNESS = 2f
    const val SNEAK_MODEL_YAW = 270f

    fun skinOptions(
        backgroundColor: Int,
        lightIntensity: Float = DEFAULT_LIGHT_INTENSITY,
        showPlatform: Boolean = true,
    ): SkinRenderOptions =
        SkinRenderOptions(
            width = FULL_WIDTH,
            height = FULL_HEIGHT,
            target = SkinRenderVec3(0f, 10f, 0f),
            yaw = 45f,
            pitch = 15f,
            distance = 65f,
            backgroundColor = backgroundColor,
            lightIntensity = lightIntensity,
            platformTopY = DEFAULT_PLATFORM_TOP_Y,
            platformThickness = DEFAULT_PLATFORM_THICKNESS,
            antiAliasingLevel = 2,
            overlayMode = SkinOverlayMode.THREE_D,
            lightingMode = SkinLightingMode.DIRECTIONAL,
            shadows = true,
            showPlatform = showPlatform
        )

    fun skinRotateOptions(
        backgroundColor: Int,
        lightIntensity: Float = DEFAULT_LIGHT_INTENSITY,
        showPlatform: Boolean = true,
    ): SkinRenderOptions =
        SkinRenderOptions(
            width = FULL_WIDTH,
            height = FULL_HEIGHT,
            target = SkinRenderVec3(0f, 10f, 0f),
            yaw = 45f,
            pitch = 20f,
            distance = 65f,
            backgroundColor = backgroundColor,
            lightIntensity = lightIntensity,
            platformTopY = DEFAULT_PLATFORM_TOP_Y,
            platformThickness = DEFAULT_PLATFORM_THICKNESS,
            antiAliasingLevel = 1,
            overlayMode = SkinOverlayMode.THREE_D,
            lightingMode = SkinLightingMode.DIRECTIONAL,
            shadows = true,
            showPlatform = showPlatform
        )

    fun headOptions(
        backgroundColor: Int,
        lightIntensity: Float = DEFAULT_LIGHT_INTENSITY,
        showPlatform: Boolean = true,
    ): SkinRenderOptions =
        SkinRenderOptions(
            width = HEAD_SIZE,
            height = HEAD_SIZE,
            target = SkinRenderVec3(0f, 20f, 0f),
            yaw = 45f,
            pitch = 15f,
            distance = 30f,
            backgroundColor = backgroundColor,
            lightIntensity = lightIntensity,
            platformTopY = DEFAULT_PLATFORM_TOP_Y,
            platformThickness = DEFAULT_PLATFORM_THICKNESS,
            antiAliasingLevel = 2,
            overlayMode = SkinOverlayMode.THREE_D,
            lightingMode = SkinLightingMode.DIRECTIONAL,
            shadows = true,
            showPlatform = showPlatform
        )

    fun headRotateOptions(
        backgroundColor: Int,
        lightIntensity: Float = DEFAULT_LIGHT_INTENSITY,
        showPlatform: Boolean = true,
    ): SkinRenderOptions =
        SkinRenderOptions(
            width = HEAD_SIZE,
            height = HEAD_SIZE,
            target = SkinRenderVec3(0f, 20f, 0f),
            yaw = 45f,
            pitch = 20f,
            distance = 30f,
            backgroundColor = backgroundColor,
            lightIntensity = lightIntensity,
            platformTopY = DEFAULT_PLATFORM_TOP_Y,
            platformThickness = DEFAULT_PLATFORM_THICKNESS,
            antiAliasingLevel = 1,
            overlayMode = SkinOverlayMode.THREE_D,
            lightingMode = SkinLightingMode.DIRECTIONAL,
            shadows = true,
            showPlatform = showPlatform
        )

    fun sneakOptions(
        backgroundColor: Int,
        lightIntensity: Float = DEFAULT_LIGHT_INTENSITY,
        showPlatform: Boolean = true,
    ): SkinRenderOptions =
        SkinRenderOptions(
            width = FULL_WIDTH,
            height = FULL_HEIGHT,
            target = SkinRenderVec3(0f, 10f, 0f),
            yaw = 315f,
            pitch = 10f,
            distance = 65f,
            backgroundColor = backgroundColor,
            lightIntensity = lightIntensity,
            platformTopY = DEFAULT_PLATFORM_TOP_Y,
            platformThickness = DEFAULT_PLATFORM_THICKNESS,
            antiAliasingLevel = 1,
            overlayMode = SkinOverlayMode.THREE_D,
            lightingMode = SkinLightingMode.DIRECTIONAL,
            shadows = true,
            showPlatform = showPlatform,
            modelYaw = SNEAK_MODEL_YAW
        )

    fun homoOptions(
        backgroundColor: Int,
        lightIntensity: Float = DEFAULT_LIGHT_INTENSITY,
        showPlatform: Boolean = true,
    ): SkinRenderOptions =
        SkinRenderOptions(
            width = HOMO_WIDTH,
            height = HOMO_HEIGHT,
            target = SkinRenderVec3(0f, 8f, 0f),
            yaw = 30f,
            pitch = 0f,
            distance = 80f,
            backgroundColor = backgroundColor,
            lightIntensity = lightIntensity,
            platformTopY = DEFAULT_PLATFORM_TOP_Y,
            platformThickness = DEFAULT_PLATFORM_THICKNESS,
            antiAliasingLevel = 2,
            overlayMode = SkinOverlayMode.THREE_D,
            lightingMode = SkinLightingMode.DIRECTIONAL,
            shadows = true,
            showPlatform = showPlatform
        )

    fun renderSkin(
        renderer: SkinPngRenderer,
        bytes: ByteArray,
        slim: Boolean,
        backgroundColor: Int,
        lightIntensity: Float?,
        headScale: Double,
        showPlatform: Boolean = true,
        capeBytes: ByteArray? = null,
    ): ByteArray =
        renderSkin(
            renderer = renderer,
            bytes = bytes,
            capeBytes = capeBytes,
            slim = slim,
            headScale = headScale,
            options = skinOptions(
                backgroundColor = backgroundColor,
                lightIntensity = lightIntensity ?: DEFAULT_LIGHT_INTENSITY,
                showPlatform = showPlatform
            )
        )

    fun renderSkin(
        renderer: SkinPngRenderer,
        bytes: ByteArray,
        slim: Boolean,
        headScale: Double,
        options: SkinRenderOptions,
        capeBytes: ByteArray? = null,
    ): ByteArray =
        renderer.renderPng(
            request = request(
                skinPng = bytes.formatSkinPng(),
                capePng = capeBytes?.takeIf { options.showCape },
                slim = slim,
                options = options,
                pose = headScalePose(slim, headScale),
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
        capeBytes: ByteArray? = null,
    ): ByteArray =
        renderSkinRotate(
            renderer = renderer,
            bytes = bytes,
            capeBytes = capeBytes,
            slim = slim,
            frameCount = frameCount,
            headScale = headScale,
            duration = duration,
            options = skinRotateOptions(
                backgroundColor = backgroundColor,
                lightIntensity = lightIntensity ?: DEFAULT_LIGHT_INTENSITY,
                showPlatform = showPlatform
            ).copy(pitch = pitchAmplitude.toFloat())
        )

    suspend fun renderSkinRotate(
        renderer: SkinPngRenderer,
        bytes: ByteArray,
        slim: Boolean,
        frameCount: Int,
        headScale: Double,
        duration: Int,
        options: SkinRenderOptions,
        capeBytes: ByteArray? = null,
    ): ByteArray =
        renderRotate(
            renderer = renderer,
            skinPng = bytes.formatSkinPng(),
            capePng = capeBytes?.takeIf { options.showCape },
            slim = slim,
            frameCount = frameCount,
            duration = duration,
            options = options,
            pose = headScalePose(slim, headScale),
        )

    fun renderHead(
        renderer: SkinPngRenderer,
        bytes: ByteArray,
        backgroundColor: Int,
        lightIntensity: Float?,
        showPlatform: Boolean = true,
    ): ByteArray =
        renderHead(
            renderer = renderer,
            bytes = bytes,
            options = headOptions(
                backgroundColor = backgroundColor,
                lightIntensity = lightIntensity ?: DEFAULT_LIGHT_INTENSITY,
                showPlatform = showPlatform
            )
        )

    fun renderHead(
        renderer: SkinPngRenderer,
        bytes: ByteArray,
        options: SkinRenderOptions,
    ): ByteArray =
        renderer.renderPng(
            request = request(
                skinPng = bytes.formatSkinPng(),
                slim = false,
                options = options,
                pose = PosePresets.HEAD_ONLY,
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
        showPlatform: Boolean = true,
    ): ByteArray =
        renderHeadRotate(
            renderer = renderer,
            bytes = bytes,
            frameCount = frameCount,
            duration = duration,
            options = headRotateOptions(
                backgroundColor = backgroundColor,
                lightIntensity = lightIntensity ?: DEFAULT_LIGHT_INTENSITY,
                showPlatform = showPlatform
            ).copy(pitch = pitchAmplitude.toFloat())
        )

    suspend fun renderHeadRotate(
        renderer: SkinPngRenderer,
        bytes: ByteArray,
        frameCount: Int,
        duration: Int,
        options: SkinRenderOptions,
    ): ByteArray =
        renderRotate(
            renderer = renderer,
            skinPng = bytes.formatSkinPng(),
            capePng = null,
            slim = false,
            frameCount = frameCount,
            duration = duration,
            options = options,
            pose = PosePresets.HEAD_ONLY,
        )

    suspend fun renderSneak(
        renderer: SkinPngRenderer,
        bytes: ByteArray,
        slim: Boolean,
        backgroundColor: Int,
        lightIntensity: Float?,
        headScale: Double,
        duration: Int = SNEAK_FRAME_DURATION_MS,
        showPlatform: Boolean = true,
        capeBytes: ByteArray? = null,
    ): ByteArray {
        return renderSneak(
            renderer = renderer,
            bytes = bytes,
            capeBytes = capeBytes,
            slim = slim,
            headScale = headScale,
            duration = duration,
            options = sneakOptions(
                backgroundColor = backgroundColor,
                lightIntensity = lightIntensity ?: DEFAULT_LIGHT_INTENSITY,
                showPlatform = showPlatform
            )
        )
    }

    suspend fun renderSneak(
        renderer: SkinPngRenderer,
        bytes: ByteArray,
        slim: Boolean,
        headScale: Double,
        duration: Int = SNEAK_FRAME_DURATION_MS,
        options: SkinRenderOptions,
        capeBytes: ByteArray? = null,
    ): ByteArray {
        val skinPng = bytes.formatSkinPng()
        val capePng = capeBytes?.takeIf { options.showCape }
        val normalPose = headScalePose(slim, headScale)
        val sneakPose = mergePose(normalPose, sneakPose())
        val pngs = renderer.renderPngBatch(
            listOf(
                request(
                    skinPng = skinPng,
                    capePng = capePng,
                    slim = slim,
                    options = options,
                    pose = normalPose
                ),
                request(
                    skinPng = skinPng,
                    capePng = capePng,
                    slim = slim,
                    options = options,
                    pose = sneakPose
                )
            )
        )
        return encodeGif(
            listOf(
                SkinAnimationFrame(
                    duration,
                    pngs[0]
                ),
                SkinAnimationFrame(
                    duration,
                    pngs[1]
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
        showPlatform: Boolean = true,
        capeBytes: ByteArray? = null,
    ): ByteArray =
        renderHomo(
            renderer = renderer,
            bytes = bytes,
            capeBytes = capeBytes,
            slim = slim,
            headScale = headScale,
            options = homoOptions(
                backgroundColor = backgroundColor,
                lightIntensity = lightIntensity ?: DEFAULT_LIGHT_INTENSITY,
                showPlatform = showPlatform
            )
        )

    fun renderHomo(
        renderer: SkinPngRenderer,
        bytes: ByteArray,
        slim: Boolean,
        headScale: Double,
        options: SkinRenderOptions,
        capeBytes: ByteArray? = null,
    ): ByteArray =
        renderer.renderPng(
            request = request(
                skinPng = bytes.formatSkinPng(),
                capePng = capeBytes?.takeIf { options.showCape },
                slim = slim,
                options = options,
                pose = PosePresets.withScale(slim, headScale = headScale.toFloat()),
            )
        )

    private suspend fun renderRotate(
        renderer: SkinPngRenderer,
        skinPng: ByteArray,
        capePng: ByteArray?,
        slim: Boolean,
        frameCount: Int,
        duration: Int,
        options: SkinRenderOptions,
        pose: Map<BodyPart, List<SkinTransform>>,
    ): ByteArray {
        val frames = frameCount.coerceAtLeast(1)
        val requests = (0 until frames).map { index ->
            val modelYaw = options.modelYaw + 360f * index / frames
            request(
                skinPng = skinPng,
                capePng = capePng,
                slim = slim,
                options = options,
                pose = pose,
                modelYaw = modelYaw,
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
        capePng: ByteArray? = null,
        slim: Boolean,
        options: SkinRenderOptions,
        pose: Map<BodyPart, List<SkinTransform>>,
        modelYaw: Float = options.modelYaw,
    ): SkinRenderRequest {
        val camera = SkinCamera(options.target, yaw = options.yaw, pitch = options.pitch, distance = options.distance)
        return SkinRenderRequest(
            skinPng = skinPng,
            capePng = capePng,
            isSlim = slim,
            yaw = camera.yaw,
            settings = SkinRenderSettings(
                width = options.width,
                height = options.height,
                target = camera.target,
                pitch = camera.pitch,
                distance = camera.distance,
                backgroundColor = options.backgroundColor,
                lightDirection = (options.lightDirection ?: camera.relativeUpperLeftLight()).normalized(),
                lightIntensity = options.lightIntensity,
                antiAliasingLevel = options.antiAliasingLevel,
                platformTopY = options.platformTopY,
                platformThickness = options.platformThickness
            ),
            overlayMode = options.overlayMode,
            lightingMode = options.lightingMode,
            shadows = options.shadows,
            showPlatform = options.showPlatform,
            pose = mergePose(pose, options.pose),
            modelYaw = modelYaw
        )
    }

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
