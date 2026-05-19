package top.e404.mcsk.benchmark

import javafx.application.Platform
import javafx.scene.AmbientLight
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.PerspectiveCamera
import javafx.scene.PointLight
import javafx.scene.Scene
import javafx.scene.SceneAntialiasing
import javafx.scene.SnapshotParameters
import javafx.scene.SubScene
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.CullFace
import javafx.scene.shape.MeshView
import javafx.scene.shape.TriangleMesh
import javafx.scene.transform.Affine
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color as SkiaColor
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.IRect
import org.jetbrains.skia.Image as SkiaImage
import top.e404.mcsk.core.SkinMesh
import top.e404.mcsk.core.SkinMeshFace
import top.e404.mcsk.core.SkinRenderVec3
import top.e404.mcsk.core.createMinecraftPlayerMeshes
import top.e404.mcsk.core.createSkinPlatform
import top.e404.mcsk.renderer.tavolo.renderMinecraftViewTavolo
import top.e404.tavolo.draw.render3d.OrbitCamera
import top.e404.tavolo.draw.render3d.RenderConfig
import top.e404.tavolo.draw.render3d.Vec3
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val DEMO_WIDTH = 600
private const val DEMO_HEIGHT = 900
private const val DEMO_YAW = 45f

private enum class DemoShadowStyle(val label: String) {
    NONE("no-shadow"),
    PROJECTED_PARTS("projected-parts"),
    PROJECTED_UNION("projected-union")
}

private data class JavaFxQualityVariant(
    val id: String,
    val title: String,
    val specular: Boolean = true,
    val alphaCutout: Boolean = false,
    val shadowStyle: DemoShadowStyle = DemoShadowStyle.PROJECTED_PARTS,
    val shadowOpacity: Double = 0.28,
)

object JavaFxQualityDemo {
    private val settings = BenchmarkRenderSettings(
        width = DEMO_WIDTH,
        height = DEMO_HEIGHT,
        target = SkinRenderVec3(0f, 10f, 0f),
        pitch = 20f,
        distance = 58f,
        backgroundColor = SkiaColor.makeRGB(28, 32, 38),
        lightDirection = SkinRenderVec3(0.35f, 0.9f, 0.45f).normalized(),
        platformTopY = -8.2f,
        platformThickness = 2f,
    )

    private val variants = listOf(
        JavaFxQualityVariant(
            id = "javafx-baseline",
            title = "JavaFX baseline: semi-transparent material + current projected shadow"
        ),
        JavaFxQualityVariant(
            id = "javafx-no-specular",
            title = "JavaFX no specular: same alpha/shadow, specular disabled",
            specular = false
        ),
        JavaFxQualityVariant(
            id = "javafx-alpha-cutout",
            title = "JavaFX alpha cutout: translucent pixels promoted to opaque",
            specular = false,
            alphaCutout = true
        ),
        JavaFxQualityVariant(
            id = "javafx-union-shadow",
            title = "JavaFX union shadow: alpha cutout + one projected shadow quad",
            specular = false,
            alphaCutout = true,
            shadowStyle = DemoShadowStyle.PROJECTED_UNION,
            shadowOpacity = 0.20
        ),
        JavaFxQualityVariant(
            id = "javafx-no-shadow",
            title = "JavaFX no shadow: alpha cutout + no shadow",
            specular = false,
            alphaCutout = true,
            shadowStyle = DemoShadowStyle.NONE
        )
    )

    fun run() {
        val reportDir = File(System.getProperty("skin.qualityDemo.reportDir", "build/reports/javafx-quality-demo"))
        val imagesDir = reportDir.resolve("images")
        imagesDir.mkdirs()

        val skin = createQualityDemoSkinPng()
        val tavoloImage = renderTavolo(skin)
        imagesDir.resolve("tavolo-shadow-map.png").writeBytes(tavoloImage)

        val generated = mutableListOf("tavolo-shadow-map.png" to "Tavolo shadow map baseline")
        try {
            variants.forEach { variant ->
                val bytes = QualityDemoJavaFxRenderer(
                    skinBytes = if (variant.alphaCutout) alphaCutoutSkin(skin) else skin,
                    variant = variant,
                    settings = settings
                ).renderPng(DEMO_YAW.toDouble())
                val fileName = "${variant.id}.png"
                imagesDir.resolve(fileName).writeBytes(bytes)
                generated += fileName to variant.title
            }
        } finally {
            JfxToolkit.shutdown()
        }

        reportDir.resolve("summary.md").writeText(buildReport(generated))
        println("JavaFX quality demo written to ${reportDir.absolutePath}")
    }

    private fun renderTavolo(skinBytes: ByteArray): ByteArray {
        val image = renderMinecraftViewTavolo(
            skin = SkiaImage.makeFromEncoded(skinBytes),
            isSlim = true,
            renderConfig = RenderConfig(
                width = settings.width,
                height = settings.height,
                camera = OrbitCamera(
                    target = Vec3(settings.target.x, settings.target.y, settings.target.z),
                    yaw = DEMO_YAW,
                    pitch = settings.pitch,
                    distance = settings.distance
                ),
                backgroundColor = settings.backgroundColor,
                useBackFaceCulling = false,
                antiAliasingLevel = 2,
                lightDirection = Vec3(settings.lightDirection.x, settings.lightDirection.y, settings.lightDirection.z),
                lightIntensity = 0.65f,
                enableShadows = true
            ),
            backgroundMeshes = listOf(createSkinPlatform(settings.platformTopY, settings.platformThickness)),
            use3DOverlay = true
        )
        return image.encodeToData(EncodedImageFormat.PNG)!!.bytes
    }

    private fun buildReport(generated: List<Pair<String, String>>): String = buildString {
        appendLine("# JavaFX transparent and shadow quality demo")
        appendLine()
        appendLine("Scene: 600x900, yaw=$DEMO_YAW, 3D overlay enabled, directional light, platform enabled.")
        appendLine()
        appendLine("| Variant | Image |")
        appendLine("| --- | --- |")
        generated.forEach { (fileName, title) ->
            appendLine("| $title | ![$title](images/$fileName) |")
        }
        appendLine()
        appendLine("Interpretation:")
        appendLine("- If `javafx-no-specular` fixes bright grid artifacts, the issue is mostly material/light tuning.")
        appendLine("- If only `javafx-alpha-cutout` looks stable, JavaFX 3D translucent sorting is not reliable enough for translucent skin overlays.")
        appendLine("- If `javafx-union-shadow` looks acceptable but `javafx-baseline` does not, the current multi-quad projected shadow is the main shadow artifact source.")
        appendLine("- If Tavolo is the only acceptable shadow/alpha result, prefer a custom OpenGL/EGL backend over continuing to patch JavaFX.")
    }
}

fun main() {
    JavaFxQualityDemo.run()
}

private class QualityDemoJavaFxRenderer(
    private val skinBytes: ByteArray,
    private val variant: JavaFxQualityVariant,
    private val settings: BenchmarkRenderSettings,
) {
    fun renderPng(yaw: Double): ByteArray = JfxToolkit.runAndWait {
        val image = Image(skinBytes.inputStream())
        val pane = StackPane()
        Scene(pane, settings.width.toDouble(), settings.height.toDouble())
        pane.resize(settings.width.toDouble(), settings.height.toDouble())
        pane.children.setAll(QualityDemoCanvas(skinBytes, image, variant, settings, yaw))
        pane.layout()
        WritableImage(settings.width, settings.height).also {
            pane.snapshot(SnapshotParameters().apply { fill = Color.rgb(28, 32, 38) }, it)
        }.png()
    }
}

private class QualityDemoCanvas(
    skinBytes: ByteArray,
    skinImage: Image,
    private val variant: JavaFxQualityVariant,
    private val settings: BenchmarkRenderSettings,
    yaw: Double,
) : Group() {
    private val camera = PerspectiveCamera(true).apply {
        fieldOfView = 45.0
        isVerticalFieldOfView = true
        nearClip = 0.1
        farClip = 200.0
        transforms.setAll(jfxCameraTransform(settings, yaw))
    }

    init {
        val skinBitmap = Bitmap.makeFromImage(SkiaImage.makeFromEncoded(skinBytes))
        val playerMeshes = createMinecraftPlayerMeshes(
            skin = skinBitmap,
            isSlim = true,
            use3DOverlay = true
        )
        val world = Group().apply {
            if (variant.shadowStyle != DemoShadowStyle.NONE) {
                children.addAll(meshToJfxNodes(createSkinPlatform(settings.platformTopY, settings.platformThickness), null, variant))
                children.add(
                    when (variant.shadowStyle) {
                        DemoShadowStyle.NONE -> Group()
                        DemoShadowStyle.PROJECTED_PARTS -> createProjectedPartsShadow(settings, variant.shadowOpacity)
                        DemoShadowStyle.PROJECTED_UNION -> createProjectedUnionShadow(settings, variant.shadowOpacity)
                    }
                )
            }
            playerMeshes.flatMapTo(children) { meshToJfxNodes(it, skinImage, variant) }
            children.addAll(createDemoLights(settings))
        }

        children.add(
            SubScene(world, settings.width.toDouble(), settings.height.toDouble(), true, SceneAntialiasing.BALANCED).apply {
                fill = Color.TRANSPARENT
                camera = this@QualityDemoCanvas.camera
            }
        )
    }
}

private fun meshToJfxNodes(mesh: SkinMesh, texture: Image?, variant: JavaFxQualityVariant): List<Node> {
    if (mesh.texture != null && texture != null) {
        return listOf(
            MeshView(mesh.toTriangleMesh(mesh.faces)).apply {
                material = PhongMaterial().apply {
                    diffuseMap = texture
                    applyDemoMaterialTuning(variant)
                }
                cullFace = CullFace.NONE
            }
        )
    }
    return mesh.faces.groupBy { it.baseColor }.map { (color, faces) ->
        MeshView(mesh.toTriangleMesh(faces)).apply {
            material = PhongMaterial(color.toJfxColor()).apply {
                applyDemoMaterialTuning(variant)
            }
            cullFace = CullFace.NONE
        }
    }
}

private fun PhongMaterial.applyDemoMaterialTuning(variant: JavaFxQualityVariant) {
    if (!variant.specular) {
        specularColor = Color.TRANSPARENT
        specularPower = 1.0
    }
}

private fun SkinMesh.toTriangleMesh(selectedFaces: List<SkinMeshFace>): TriangleMesh =
    TriangleMesh().apply {
        vertices.forEach { vertex ->
            val p = tavoloToJfx(DemoVec3d(vertex.position.x.toDouble(), vertex.position.y.toDouble(), vertex.position.z.toDouble()))
            points.addAll(p.x.toFloat(), p.y.toFloat(), p.z.toFloat())
        }
        vertices.forEach { vertex ->
            texCoords.addAll(vertex.uv.u, vertex.uv.v)
        }
        if (vertices.isEmpty()) texCoords.addAll(0f, 0f)
        selectedFaces.forEach { face ->
            if (face.indices.size < 3) return@forEach
            for (i in 1 until face.indices.size - 1) {
                faces.addAll(face.indices[0], face.indices[0], face.indices[i + 1], face.indices[i + 1], face.indices[i], face.indices[i])
            }
        }
    }

private fun Int.toJfxColor(): Color =
    Color.rgb(SkiaColor.getR(this), SkiaColor.getG(this), SkiaColor.getB(this), SkiaColor.getA(this) / 255.0)

private fun createDemoLights(settings: BenchmarkRenderSettings): List<Node> = listOf(
    AmbientLight(Color.gray(0.68)),
    PointLight(Color.gray(0.80)).apply {
        val scale = 80.0
        translateX = settings.lightDirection.x * scale
        translateY = -settings.lightDirection.y * scale
        translateZ = settings.lightDirection.z * scale
    }
)

private fun createProjectedPartsShadow(settings: BenchmarkRenderSettings, opacity: Double): Node {
    val floorY = tavoloYToJfx(settings.platformTopY)
    val lightRay = demoLightRay(settings)
    return Group().apply {
        children.addAll(demoShadowBoxes().map { JfxDemoShadowQuad(it, floorY, lightRay, opacity) })
    }
}

private fun createProjectedUnionShadow(settings: BenchmarkRenderSettings, opacity: Double): Node {
    val floorY = tavoloYToJfx(settings.platformTopY)
    val lightRay = demoLightRay(settings)
    val projected = demoShadowBoxes().flatMap { box -> projectBoxCorners(box, floorY, lightRay) }
    val minX = projected.minOf { it.x }
    val maxX = projected.maxOf { it.x }
    val minZ = projected.minOf { it.z }
    val maxZ = projected.maxOf { it.z }
    return MeshView(quadMesh(minX, maxX, floorY - 0.02, minZ, maxZ)).apply {
        material = PhongMaterial(Color.rgb(0, 0, 0, opacity)).apply {
            specularColor = Color.TRANSPARENT
        }
        cullFace = CullFace.NONE
    }
}

private fun demoLightRay(settings: BenchmarkRenderSettings): DemoVec3d =
    DemoVec3d(settings.lightDirection.x.toDouble(), -settings.lightDirection.y.toDouble(), settings.lightDirection.z.toDouble())

private fun demoShadowBoxes(): List<DemoBoxSpec> = listOf(
    DemoBoxSpec(0.0, tavoloYToJfx(20f), 0.0, 8.8, 8.8, 8.8),
    DemoBoxSpec(0.0, tavoloYToJfx(10f), 0.0, 8.4, 12.4, 4.4),
    DemoBoxSpec(5.5, tavoloYToJfx(10f), 0.0, 3.4, 12.4, 4.4),
    DemoBoxSpec(-5.5, tavoloYToJfx(10f), 0.0, 3.4, 12.4, 4.4),
    DemoBoxSpec(2.0, tavoloYToJfx(-2f), 0.0, 4.4, 12.4, 4.4),
    DemoBoxSpec(-2.0, tavoloYToJfx(-2f), 0.0, 4.4, 12.4, 4.4)
)

private class JfxDemoShadowQuad(box: DemoBoxSpec, floorY: Double, lightRay: DemoVec3d, opacity: Double) : MeshView() {
    init {
        val projected = projectBoxCorners(box, floorY, lightRay)
        mesh = quadMesh(
            projected.minOf { it.x },
            projected.maxOf { it.x },
            floorY - 0.02,
            projected.minOf { it.z },
            projected.maxOf { it.z }
        )
        material = PhongMaterial(Color.rgb(0, 0, 0, opacity)).apply {
            specularColor = Color.TRANSPARENT
        }
        cullFace = CullFace.NONE
    }
}

private fun projectBoxCorners(box: DemoBoxSpec, floorY: Double, lightRay: DemoVec3d): List<DemoVec3d> =
    listOf(
        DemoVec3d(box.centerX - box.width / 2, box.centerY - box.height / 2, box.centerZ - box.depth / 2),
        DemoVec3d(box.centerX + box.width / 2, box.centerY - box.height / 2, box.centerZ - box.depth / 2),
        DemoVec3d(box.centerX - box.width / 2, box.centerY + box.height / 2, box.centerZ - box.depth / 2),
        DemoVec3d(box.centerX + box.width / 2, box.centerY + box.height / 2, box.centerZ - box.depth / 2),
        DemoVec3d(box.centerX - box.width / 2, box.centerY - box.height / 2, box.centerZ + box.depth / 2),
        DemoVec3d(box.centerX + box.width / 2, box.centerY - box.height / 2, box.centerZ + box.depth / 2),
        DemoVec3d(box.centerX - box.width / 2, box.centerY + box.height / 2, box.centerZ + box.depth / 2),
        DemoVec3d(box.centerX + box.width / 2, box.centerY + box.height / 2, box.centerZ + box.depth / 2)
    ).map { corner ->
        val t = (floorY - corner.y) / lightRay.y
        DemoVec3d(corner.x + lightRay.x * t, floorY, corner.z + lightRay.z * t)
    }

private fun jfxCameraTransform(settings: BenchmarkRenderSettings, yaw: Double): Affine {
    val eye = tavoloToJfx(tavoloCameraEye(settings, yaw))
    val target = tavoloToJfx(DemoVec3d(settings.target.x.toDouble(), settings.target.y.toDouble(), settings.target.z.toDouble()))
    val forward = (target - eye).normalized()
    val worldUp = DemoVec3d(0.0, -1.0, 0.0)
    val cameraRight = forward.cross(worldUp).normalized()
    val right = -cameraRight
    val screenUp = cameraRight.cross(forward).normalized()
    val down = -screenUp
    return Affine(
        right.x, down.x, forward.x, eye.x,
        right.y, down.y, forward.y, eye.y,
        right.z, down.z, forward.z, eye.z
    )
}

private fun tavoloCameraEye(settings: BenchmarkRenderSettings, yaw: Double): DemoVec3d {
    val yawRad = Math.toRadians(yaw)
    val pitchRad = Math.toRadians(settings.pitch.toDouble())
    val target = DemoVec3d(settings.target.x.toDouble(), settings.target.y.toDouble(), settings.target.z.toDouble())
    return DemoVec3d(
        target.x + settings.distance * cos(pitchRad) * sin(yawRad),
        target.y + settings.distance * sin(pitchRad),
        target.z + settings.distance * cos(pitchRad) * cos(yawRad)
    )
}

private fun tavoloToJfx(point: DemoVec3d): DemoVec3d = DemoVec3d(point.x, -point.y, point.z)

private fun tavoloYToJfx(y: Float): Double = -y.toDouble()

private data class DemoVec3d(val x: Double, val y: Double, val z: Double) {
    operator fun minus(other: DemoVec3d): DemoVec3d = DemoVec3d(x - other.x, y - other.y, z - other.z)
    operator fun unaryMinus(): DemoVec3d = DemoVec3d(-x, -y, -z)

    fun cross(other: DemoVec3d): DemoVec3d =
        DemoVec3d(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x)

    fun normalized(): DemoVec3d {
        val length = sqrt(x * x + y * y + z * z)
        return if (length > 0.0) DemoVec3d(x / length, y / length, z / length) else this
    }
}

private data class DemoBoxSpec(
    val centerX: Double,
    val centerY: Double,
    val centerZ: Double,
    val width: Double,
    val height: Double,
    val depth: Double,
)

private fun quadMesh(minX: Double, maxX: Double, y: Double, minZ: Double, maxZ: Double): TriangleMesh =
    TriangleMesh().apply {
        val x0 = min(minX, maxX).toFloat()
        val x1 = max(minX, maxX).toFloat()
        val z0 = min(minZ, maxZ).toFloat()
        val z1 = max(minZ, maxZ).toFloat()
        val fy = y.toFloat()
        points.addAll(x0, fy, z0, x1, fy, z0, x1, fy, z1, x0, fy, z1)
        texCoords.addAll(0f, 0f)
        faces.addAll(0, 0, 1, 0, 2, 0, 0, 0, 2, 0, 3, 0)
    }

private fun WritableImage.png(): ByteArray {
    val img = BufferedImage(width.toInt(), height.toInt(), BufferedImage.TYPE_INT_ARGB)
    val reader = pixelReader
    for (x in 0 until width.toInt()) {
        for (y in 0 until height.toInt()) {
            img.setRGB(x, y, reader.getArgb(x, y))
        }
    }
    return ByteArrayOutputStream().use {
        ImageIO.write(img, "png", it)
        it.toByteArray()
    }
}

private fun createQualityDemoSkinPng(): ByteArray {
    val bitmap = Bitmap().apply { allocN32Pixels(64, 64) }
    Canvas(bitmap).clear(SkiaColor.makeRGB(88, 132, 190))

    fun rect(x: Int, y: Int, w: Int, h: Int, color: Int) {
        bitmap.erase(color, IRect.makeXYWH(x, y, w, h))
    }

    rect(8, 8, 8, 8, SkiaColor.makeRGB(238, 183, 132))
    rect(20, 20, 8, 12, SkiaColor.makeRGB(45, 90, 170))
    rect(44, 20, 4, 12, SkiaColor.makeRGB(238, 183, 132))
    rect(4, 20, 4, 12, SkiaColor.makeRGB(70, 92, 130))
    rect(20, 52, 4, 12, SkiaColor.makeRGB(48, 62, 92))
    rect(4, 20, 4, 12, SkiaColor.makeRGB(48, 62, 92))

    rect(32, 0, 32, 16, SkiaColor.TRANSPARENT)
    rect(16, 32, 48, 32, SkiaColor.TRANSPARENT)

    rect(40, 8, 8, 8, SkiaColor.makeARGB(120, 20, 24, 34))
    rect(20, 36, 8, 12, SkiaColor.makeARGB(105, 35, 75, 145))
    rect(44, 36, 4, 12, SkiaColor.makeARGB(130, 255, 255, 255))
    rect(4, 36, 4, 12, SkiaColor.makeARGB(130, 10, 20, 40))
    rect(4, 52, 4, 12, SkiaColor.makeARGB(100, 255, 255, 255))
    rect(20, 52, 4, 12, SkiaColor.makeARGB(100, 255, 255, 255))

    return SkiaImage.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG)!!.bytes
}

private fun alphaCutoutSkin(bytes: ByteArray): ByteArray {
    val src = Bitmap.makeFromImage(SkiaImage.makeFromEncoded(bytes))
    val dst = Bitmap().apply { allocN32Pixels(src.width, src.height) }
    for (y in 0 until src.height) {
        for (x in 0 until src.width) {
            val color = src.getColor(x, y)
            val alpha = SkiaColor.getA(color)
            val out = when {
                alpha == 0 -> SkiaColor.TRANSPARENT
                else -> SkiaColor.makeARGB(255, SkiaColor.getR(color), SkiaColor.getG(color), SkiaColor.getB(color))
            }
            dst.erase(out, IRect.makeXYWH(x, y, 1, 1))
        }
    }
    return SkiaImage.makeFromBitmap(dst).encodeToData(EncodedImageFormat.PNG)!!.bytes
}
