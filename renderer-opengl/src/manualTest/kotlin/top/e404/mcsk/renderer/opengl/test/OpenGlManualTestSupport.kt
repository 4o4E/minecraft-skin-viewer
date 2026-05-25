package top.e404.mcsk.renderer.opengl.test

import top.e404.mcsk.core.BodyPart
import top.e404.mcsk.core.PlayerModel
import top.e404.mcsk.core.SkinLightingMode
import top.e404.mcsk.core.SkinOverlayMode
import top.e404.mcsk.core.SkinRenderRequest
import top.e404.mcsk.core.SkinRenderSettings
import top.e404.mcsk.core.SkinRenderVec3
import top.e404.mcsk.core.SkinTransform
import top.e404.mcsk.core.SkinVec3
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.MemoryCacheImageOutputStream

internal const val DEFAULT_BG: Int = 0xFF1F1B1D.toInt()
internal val manualSkinFiles = listOf(
    "alex_skin.png" to true,
    "steve_skin.png" to false
)
internal val openGlRenderOutputDir = File("manual-test-output/opengl/render")
internal val shadowGridYaws = listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)
internal val shadowGridPitches = listOf(30f, 10f, -10f)
internal const val GIF_FRAME_COUNT = 20
internal const val GIF_FRAME_DURATION_MS = 40

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
    showPlatform: Boolean = false,
    modelYaw: Float = 0f,
    showCape: Boolean = true,
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
        pose = pose,
        modelYaw = modelYaw,
        capePng = syntheticManualCapePng().takeIf { showCape }
    )

internal fun syntheticManualCapePng(): ByteArray {
    val image = BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    try {
        // 按 Minecraft 披风 cuboid 的 uv=[0,0]、size=[10,16,1] 布局填满六个面。
        graphics.color = java.awt.Color(0xFF5B21B6.toInt(), true)
        graphics.fillRect(0, 1, 1, 16)
        graphics.color = java.awt.Color(0xFF7B2CBF.toInt(), true)
        graphics.fillRect(1, 1, 10, 16)
        graphics.color = java.awt.Color(0xFF6D28D9.toInt(), true)
        graphics.fillRect(11, 1, 1, 16)
        graphics.color = java.awt.Color(0xFF4C1D95.toInt(), true)
        graphics.fillRect(12, 1, 10, 16)
        graphics.color = java.awt.Color(0xFF8B5CF6.toInt(), true)
        graphics.fillRect(1, 0, 10, 1)
        graphics.color = java.awt.Color(0xFF2E1065.toInt(), true)
        graphics.fillRect(11, 0, 10, 1)
        graphics.color = java.awt.Color(0xFFFFD166.toInt(), true)
        graphics.fillRect(4, 3, 4, 10)
        graphics.fillRect(15, 3, 4, 10)
    } finally {
        graphics.dispose()
    }
    return ByteArrayOutputStream().use {
        ImageIO.write(image, "png", it)
        it.toByteArray()
    }
}

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
        BodyPart.CAPE to SkinVec3(0f, 10f, -(bodyDims.z / 2 + gap + BodyPart.CAPE.getDims(isSlim).z / 2)),
    )

    return BodyPart.entries.associateWith { part ->
        val current = if (part == BodyPart.CAPE) model.cape.pos else model.parts.getValue(part).pos
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

internal fun writeGif(
    frames: List<ByteArray>,
    outputFile: File,
    durationMs: Int,
) {
    require(frames.isNotEmpty()) { "GIF must contain at least one frame" }
    outputFile.parentFile?.mkdirs()
    val writer = ImageIO.getImageWritersBySuffix("gif").asSequence().first()
    try {
        outputFile.outputStream().use { fileOutput ->
            MemoryCacheImageOutputStream(fileOutput).use { output ->
                writer.output = output
                writer.prepareWriteSequence(null)
                frames.forEachIndexed { index, frame ->
                    val image = requireNotNull(ImageIO.read(ByteArrayInputStream(frame))).toArgb()
                    val params = writer.defaultWriteParam
                    val imageType = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB)
                    val metadata = writer.getDefaultImageMetadata(imageType, params)
                    configureGifMetadata(metadata, durationMs, loop = index == 0)
                    writer.writeToSequence(IIOImage(image, null, metadata), params)
                }
                writer.endWriteSequence()
            }
        }
    } finally {
        writer.dispose()
    }
}

private fun configureGifMetadata(
    metadata: javax.imageio.metadata.IIOMetadata,
    durationMs: Int,
    loop: Boolean,
) {
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
