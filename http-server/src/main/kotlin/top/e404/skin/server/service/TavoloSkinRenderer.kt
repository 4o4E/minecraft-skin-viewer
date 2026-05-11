package top.e404.skin.server.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Color
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import top.e404.skin.core.BodyPart
import top.e404.skin.core.PosePresets
import top.e404.skin.core.createMinecraftPlayer
import top.e404.skin.core.renderMinecraftView
import top.e404.tavolo.draw.render3d.OrbitCamera
import top.e404.tavolo.draw.render3d.RenderConfig
import top.e404.tavolo.draw.render3d.Scene
import top.e404.tavolo.draw.render3d.Transformation
import top.e404.tavolo.draw.render3d.Vec3
import top.e404.tavolo.draw.render3d.renderSceneToImage
import top.e404.tavolo.frame.Frame
import top.e404.tavolo.frame.encodeToBytes
import top.e404.tavolo.util.bytes
import top.e404.tavolo.util.toBitmap
import kotlin.math.PI
import kotlin.math.cos

object TavoloSkinRenderer {
    private const val FULL_WIDTH = 600
    private const val FULL_HEIGHT = 900
    private const val HEAD_SIZE = 400
    private const val HOMO_WIDTH = 1024
    private const val HOMO_HEIGHT = 768
    private const val DEFAULT_LIGHT_INTENSITY = 0.8f

    fun renderSkin(
        bytes: ByteArray,
        slim: Boolean,
        backgroundColor: Int,
        lightColor: Int?,
        headScale: Double,
    ): ByteArray {
        val image = bytes.decodeSkin()
        val pose = headScalePose(slim, headScale)
        return render(
            skin = image,
            slim = slim,
            width = FULL_WIDTH,
            height = FULL_HEIGHT,
            backgroundColor = backgroundColor,
            lightColor = lightColor,
            camera = fullBodyCamera(),
            pose = pose,
            antiAliasingLevel = 2
        ).bytes(EncodedImageFormat.PNG)
    }

    suspend fun renderSkinRotate(
        bytes: ByteArray,
        slim: Boolean,
        backgroundColor: Int,
        frameCount: Int,
        pitchAmplitude: Int,
        lightColor: Int?,
        headScale: Double,
        duration: Int,
    ): ByteArray {
        val image = bytes.decodeSkin()
        val pose = headScalePose(slim, headScale)
        return renderRotate(
            skin = image,
            slim = slim,
            width = FULL_WIDTH,
            height = FULL_HEIGHT,
            backgroundColor = backgroundColor,
            frameCount = frameCount,
            pitchAmplitude = pitchAmplitude,
            lightColor = lightColor,
            duration = duration,
            target = Vec3(0f, 10f, 0f),
            distance = 65f,
            pose = pose,
            antiAliasingLevel = 1
        )
    }

    fun renderHead(
        bytes: ByteArray,
        backgroundColor: Int,
        lightColor: Int?,
    ): ByteArray {
        val image = bytes.decodeSkin()
        return render(
            skin = image,
            slim = false,
            width = HEAD_SIZE,
            height = HEAD_SIZE,
            backgroundColor = backgroundColor,
            lightColor = lightColor,
            camera = headCamera(),
            pose = PosePresets.HEAD_ONLY,
            antiAliasingLevel = 2
        ).bytes(EncodedImageFormat.PNG)
    }

    suspend fun renderHeadRotate(
        bytes: ByteArray,
        backgroundColor: Int,
        frameCount: Int,
        pitchAmplitude: Int,
        lightColor: Int?,
        duration: Int,
    ): ByteArray {
        val image = bytes.decodeSkin()
        return renderRotate(
            skin = image,
            slim = false,
            width = HEAD_SIZE,
            height = HEAD_SIZE,
            backgroundColor = backgroundColor,
            frameCount = frameCount,
            pitchAmplitude = pitchAmplitude,
            lightColor = lightColor,
            duration = duration,
            target = Vec3(0f, 20f, 0f),
            distance = 30f,
            pose = PosePresets.HEAD_ONLY,
            antiAliasingLevel = 1
        )
    }

    suspend fun renderSneak(
        bytes: ByteArray,
        slim: Boolean,
        backgroundColor: Int,
        lightColor: Int?,
        headScale: Double,
        duration: Int,
    ): ByteArray {
        val image = bytes.decodeSkin()
        val normalPose = headScalePose(slim, headScale)
        val sneakPose = mergePose(normalPose, sneakPose())
        return listOf(
            Frame(duration, render(image, slim, FULL_WIDTH, FULL_HEIGHT, backgroundColor, lightColor, sneakCamera(), normalPose, antiAliasingLevel = 1)),
            Frame(duration, render(image, slim, FULL_WIDTH, FULL_HEIGHT, backgroundColor, lightColor, sneakCamera(), sneakPose, antiAliasingLevel = 1))
        ).encodeToBytes()
    }

    fun renderHomo(
        bytes: ByteArray,
        slim: Boolean,
        backgroundColor: Int,
        lightColor: Int?,
        headScale: Double,
    ): ByteArray {
        val image = bytes.decodeSkin()
        return render(
            skin = image,
            slim = slim,
            width = HOMO_WIDTH,
            height = HOMO_HEIGHT,
            backgroundColor = backgroundColor,
            lightColor = lightColor,
            camera = OrbitCamera(Vec3(0f, 8f, 0f), yaw = 30f, pitch = 0f, distance = 80f),
            pose = PosePresets.withScale(slim, headScale = headScale.toFloat()),
            antiAliasingLevel = 2
        ).bytes(EncodedImageFormat.PNG)
    }

    private fun render(
        skin: Image,
        slim: Boolean,
        width: Int,
        height: Int,
        backgroundColor: Int,
        lightColor: Int?,
        camera: OrbitCamera,
        pose: Map<BodyPart, List<Transformation>>,
        antiAliasingLevel: Int,
    ): Image = renderMinecraftView(
        skin = skin,
        isSlim = slim,
        renderConfig = RenderConfig(
            width = width,
            height = height,
            camera = camera,
            backgroundColor = backgroundColor,
            lightDirection = Vec3(0.5f, 1f, 1f).normalized(),
            lightIntensity = lightIntensity(lightColor),
            antiAliasingLevel = antiAliasingLevel
        ),
        pose = pose,
        use3DOverlay = true
    )

    private suspend fun renderRotate(
        skin: Image,
        slim: Boolean,
        width: Int,
        height: Int,
        backgroundColor: Int,
        frameCount: Int,
        pitchAmplitude: Int,
        lightColor: Int?,
        duration: Int,
        target: Vec3,
        distance: Float,
        pose: Map<BodyPart, List<Transformation>>,
        antiAliasingLevel: Int,
    ): ByteArray {
        val frames = frameCount.coerceAtLeast(1)
        val scene = Scene(listOf(createMinecraftPlayer(skin.toBitmap(), slim, pose, use3DOverlay = true)))
        return coroutineScope {
            withContext(Dispatchers.Default) {
                (0 until frames).map { index ->
                    async {
                        val yaw = 360f * index / frames
                        val pitch = (cos(yaw * PI / 180.0) * pitchAmplitude).toFloat()
                        Frame(
                            duration,
                            renderScene(
                                scene = scene,
                                width = width,
                                height = height,
                                backgroundColor = backgroundColor,
                                lightColor = lightColor,
                                camera = OrbitCamera(target, yaw = -yaw, pitch = pitch, distance = distance),
                                antiAliasingLevel = antiAliasingLevel
                            )
                        )
                    }
                }.awaitAll()
            }
        }.encodeToBytes()
    }

    private fun renderScene(
        scene: Scene,
        width: Int,
        height: Int,
        backgroundColor: Int,
        lightColor: Int?,
        camera: OrbitCamera,
        antiAliasingLevel: Int,
    ): Image = renderSceneToImage(
        scene = scene,
        config = RenderConfig(
            width = width,
            height = height,
            camera = camera,
            backgroundColor = backgroundColor,
            lightDirection = Vec3(0.5f, 1f, 1f).normalized(),
            lightIntensity = lightIntensity(lightColor),
            antiAliasingLevel = antiAliasingLevel
        )
    )

    private fun fullBodyCamera() =
        OrbitCamera(Vec3(0f, 10f, 0f), yaw = 45f, pitch = 15f, distance = 65f)

    private fun headCamera() =
        OrbitCamera(Vec3(0f, 20f, 0f), yaw = 45f, pitch = 15f, distance = 30f)

    private fun sneakCamera() =
        OrbitCamera(Vec3(0f, 10f, 0f), yaw = 315f, pitch = 10f, distance = 65f)

    private fun headScalePose(
        slim: Boolean,
        headScale: Double,
    ): Map<BodyPart, List<Transformation>> {
        val scale = headScale.toFloat()
        if (scale == 1f) return emptyMap()
        return mapOf(
            BodyPart.HEAD to listOf(
                Transformation.Scale(scale, scale, scale),
                Transformation.Translate(y = BodyPart.HEAD.getDims(slim).y * (scale - 1f) / 2f)
            )
        )
    }

    private fun sneakPose(): Map<BodyPart, List<Transformation>> = mapOf(
        BodyPart.HEAD to listOf(Transformation.Translate(y = 3f, z = -4.8f)),
        BodyPart.BODY to listOf(Transformation.Rotate(x = 30f), Transformation.Translate(y = 0.8f, z = -2f)),
        BodyPart.RIGHT_ARM to listOf(Transformation.Rotate(x = 30f), Transformation.Translate(y = 1.6f, z = -3f)),
        BodyPart.LEFT_ARM to listOf(Transformation.Rotate(x = 30f), Transformation.Translate(y = 1.6f, z = -3f))
    )

    private fun mergePose(
        first: Map<BodyPart, List<Transformation>>,
        second: Map<BodyPart, List<Transformation>>,
    ): Map<BodyPart, List<Transformation>> {
        if (first.isEmpty()) return second
        if (second.isEmpty()) return first
        return (first.keys + second.keys).associateWith { part ->
            first.orEmpty(part) + second.orEmpty(part)
        }
    }

    private fun Map<BodyPart, List<Transformation>>.orEmpty(part: BodyPart): List<Transformation> =
        this[part].orEmpty()

    private fun lightIntensity(color: Int?): Float {
        if (color == null) return DEFAULT_LIGHT_INTENSITY
        val luminance = (0.2126f * Color.getR(color) + 0.7152f * Color.getG(color) + 0.0722f * Color.getB(color)) / 255f
        return luminance.coerceIn(0.2f, 1f)
    }

    private fun ByteArray.decodeSkin(): Image =
        Image.makeFromEncoded(this).formatSkin()

    private fun Image.formatSkin(): Image {
        if (width == height) return this

        val source = toBitmap()
        val multiple = (width / 64).coerceAtLeast(1)
        val target = Bitmap().apply {
            allocN32Pixels(width, height * 2)
            erase(Color.TRANSPARENT)
        }

        fun copy(srcX: Int, srcY: Int, w: Int, h: Int, dstX: Int, dstY: Int) {
            for (x in 0 until w) {
                for (y in 0 until h) {
                    target.erase(source.getColor(srcX + x, srcY + y), org.jetbrains.skia.IRect.makeXYWH(dstX + x, dstY + y, 1, 1))
                }
            }
        }

        copy(0, 0, width, height, 0, 0)
        copy(0, 16 * multiple, 16 * multiple, 16 * multiple, 16 * multiple, 48 * multiple)
        copy(40 * multiple, 16 * multiple, 16 * multiple, 16 * multiple, 32 * multiple, 48 * multiple)
        return Image.makeFromBitmap(target)
    }
}
