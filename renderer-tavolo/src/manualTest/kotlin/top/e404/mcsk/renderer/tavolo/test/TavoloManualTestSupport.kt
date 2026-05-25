package top.e404.mcsk.renderer.tavolo.test

import kotlin.test.assertTrue
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import top.e404.mcsk.core.BodyPart
import top.e404.mcsk.core.SkinTransform
import top.e404.mcsk.core.createSkinPlatform
import top.e404.mcsk.renderer.tavolo.renderMinecraftViewTavolo
import top.e404.tavolo.draw.render3d.OrbitCamera
import top.e404.tavolo.draw.render3d.RenderConfig
import top.e404.tavolo.draw.render3d.Vec3
import top.e404.tavolo.util.Colors
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
internal val tavoloRenderOutputDir = File("manual-test-output/tavolo/render")
internal val shadowGridYaws = listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)
internal val shadowGridPitches = listOf(30f, 10f, -10f)
internal const val GIF_FRAME_COUNT = 20
internal const val GIF_FRAME_DURATION_MS = 40

internal fun renderTavoloFile(
    file: File,
    isSlim: Boolean,
    width: Int,
    height: Int,
    camera: OrbitCamera,
    lightDirection: Vec3 = Vec3(1f, 1.0f, 1f).normalized(),
    lightIntensity: Float = .8f,
    pose: Map<BodyPart, List<SkinTransform>> = emptyMap(),
    shadows: Boolean = false,
    showPlatform: Boolean = false,
    modelYaw: Float = 0f,
    showCape: Boolean = true,
): Image {
    assertTrue(file.isFile, "Put ${file.name} in the run directory first.")
    val skin = Image.makeFromEncoded(file.readBytes())
    val cape = if (showCape) Image.makeFromEncoded(syntheticManualCapePng()) else null
    return renderMinecraftViewTavolo(
        skin = skin,
        isSlim = isSlim,
        renderConfig = RenderConfig(
            width = width,
            height = height,
            camera = camera,
            backgroundColor = Colors.BG.argb,
            lightDirection = lightDirection,
            lightIntensity = lightIntensity,
            antiAliasingLevel = 4,
            enableShadows = shadows
        ),
        backgroundMeshes = if (showPlatform) listOf(createSkinPlatform()) else emptyList(),
        pose = pose,
        use3DOverlay = true,
        modelYaw = modelYaw,
        cape = cape
    )
}

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

internal fun writeGif(
    frames: List<Image>,
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
                    val image = requireNotNull(
                        ImageIO.read(ByteArrayInputStream(frame.encodeToData()!!.bytes))
                    ).toArgb()
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

internal fun stitchImages(images: List<Image>, columns: Int): BufferedImage {
    require(images.isNotEmpty()) { "No images to stitch" }
    val decoded = images.map { image ->
        ImageIO.read(ByteArrayInputStream(image.encodeToData(EncodedImageFormat.PNG)!!.bytes))
    }
    val tileWidth = decoded.maxOf { it.width }
    val tileHeight = decoded.maxOf { it.height }
    val rows = (decoded.size + columns - 1) / columns
    val output = BufferedImage(tileWidth * columns, tileHeight * rows, BufferedImage.TYPE_INT_ARGB)
    val graphics = output.createGraphics()
    decoded.forEachIndexed { index, image ->
        graphics.drawImage(image, (index % columns) * tileWidth, (index / columns) * tileHeight, null)
    }
    graphics.dispose()
    return output
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
