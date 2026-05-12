package top.e404.skin.renderer.opengl.test

import top.e404.skin.core.BodyPart
import top.e404.skin.core.PlayerModel
import top.e404.skin.core.SkinLightingMode
import top.e404.skin.core.SkinOverlayMode
import top.e404.skin.core.SkinRenderRequest
import top.e404.skin.core.SkinRenderSettings
import top.e404.skin.core.SkinRenderVec3
import top.e404.skin.core.SkinTransform
import top.e404.skin.core.SkinVec3
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

internal const val DEFAULT_BG: Int = -15124186 // rgb(25, 30, 38)

internal fun renderRequest(
    skinFile: File,
    slim: Boolean,
    width: Int,
    height: Int,
    target: SkinRenderVec3,
    yaw: Float,
    pitch: Float,
    distance: Float,
    lightDirection: SkinRenderVec3 = SkinRenderVec3(0.45f, 0.85f, 0.35f).normalized(),
    lightIntensity: Float = 0.75f,
    pose: Map<BodyPart, List<SkinTransform>> = emptyMap(),
    overlayMode: SkinOverlayMode = SkinOverlayMode.THREE_D,
    shadows: Boolean = true,
    showPlatform: Boolean = shadows,
): SkinRenderRequest =
    SkinRenderRequest(
        skinPng = skinFile.readBytes(),
        isSlim = slim,
        yaw = yaw,
        settings = SkinRenderSettings(
            width = width,
            height = height,
            target = target,
            pitch = pitch,
            distance = distance,
            backgroundColor = DEFAULT_BG,
            lightDirection = lightDirection,
            platformTopY = -8.2f,
            platformThickness = 2f,
            lightIntensity = lightIntensity,
            antiAliasingLevel = 2
        ),
        overlayMode = overlayMode,
        lightingMode = SkinLightingMode.DIRECTIONAL,
        shadows = shadows,
        showPlatform = showPlatform,
        pose = pose
    )

internal fun explodedPose(isSlim: Boolean, gap: Float): Map<BodyPart, List<SkinTransform>> {
    val model = PlayerModel(isSlim)
    val bodyDims = BodyPart.BODY.getDims(isSlim)
    val headDims = BodyPart.HEAD.getDims(isSlim)
    val rightArmDims = BodyPart.RIGHT_ARM.getDims(isSlim)
    val leftArmDims = BodyPart.LEFT_ARM.getDims(isSlim)
    val rightLegDims = BodyPart.RIGHT_LEG.getDims(isSlim)
    val leftLegDims = BodyPart.LEFT_LEG.getDims(isSlim)

    val desired = mapOf(
        BodyPart.BODY to SkinVec3(0f, 10f, 0f),
        BodyPart.HEAD to SkinVec3(0f, 10f + bodyDims.y / 2 + gap + headDims.y / 2, 0f),
        BodyPart.RIGHT_ARM to SkinVec3(-(bodyDims.x / 2 + gap + rightArmDims.x / 2), 10f, 0f),
        BodyPart.LEFT_ARM to SkinVec3(bodyDims.x / 2 + gap + leftArmDims.x / 2, 10f, 0f),
        BodyPart.RIGHT_LEG to SkinVec3(-(gap / 2 + rightLegDims.x / 2), 10f - bodyDims.y / 2 - gap - rightLegDims.y / 2, 0f),
        BodyPart.LEFT_LEG to SkinVec3(gap / 2 + leftLegDims.x / 2, 10f - bodyDims.y / 2 - gap - leftLegDims.y / 2, 0f),
    )

    return BodyPart.entries.associateWith { part ->
        val current = model.parts.getValue(part).pos
        val target = desired.getValue(part)
        listOf(
            SkinTransform.Translate(
                x = target.x - current.x,
                y = target.y - current.y,
                z = target.z - current.z
            )
        )
    }
}

internal fun stitchPngs(images: List<ByteArray>, columns: Int): BufferedImage {
    require(images.isNotEmpty()) { "No images to stitch" }
    val decoded = images.map { ImageIO.read(ByteArrayInputStream(it)) }
    val tileWidth = decoded.maxOf { it.width }
    val tileHeight = decoded.maxOf { it.height }
    val rows = (decoded.size + columns - 1) / columns
    val output = BufferedImage(tileWidth * columns, tileHeight * rows, BufferedImage.TYPE_INT_ARGB)
    val graphics: Graphics2D = output.createGraphics()
    decoded.forEachIndexed { index, image ->
        graphics.drawImage(image, (index % columns) * tileWidth, (index / columns) * tileHeight, null)
    }
    graphics.dispose()
    return output
}
