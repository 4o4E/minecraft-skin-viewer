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
import org.jetbrains.skia.Color as SkiaColor
import org.jetbrains.skia.Image as SkiaImage
import top.e404.mcsk.core.SkinMesh
import top.e404.mcsk.core.SkinMeshFace
import top.e404.mcsk.core.createMinecraftPlayerMeshes
import top.e404.mcsk.core.createSkinPlatform
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal object JfxToolkit {
    @Volatile
    private var started = false

    fun startup() {
        if (started) return
        synchronized(this) {
            if (started) return
            val latch = CountDownLatch(1)
            Platform.startup {
                Platform.setImplicitExit(false)
                latch.countDown()
            }
            latch.await()
            started = true
        }
    }

    fun <T> runAndWait(block: () -> T): T {
        startup()
        val latch = CountDownLatch(1)
        val result = AtomicReference<T>()
        val error = AtomicReference<Throwable>()
        Platform.runLater {
            runCatching { block() }
                .onSuccess(result::set)
                .onFailure(error::set)
            latch.countDown()
        }
        latch.await()
        error.get()?.let { throw it }
        return result.get()
    }

    fun shutdown() {
        if (!started) return
        synchronized(this) {
            if (!started) return
            val latch = CountDownLatch(1)
            Platform.runLater {
                Platform.exit()
                latch.countDown()
            }
            latch.await()
            started = false
        }
    }
}

internal class LegacyJfxSkinRenderer(
    private val skinBytes: ByteArray,
    private val slim: Boolean,
    private val settings: BenchmarkRenderSettings,
    private val overlayMode: OverlayMode,
    private val lightingMode: LightingMode,
    private val shadows: Boolean,
) {
    fun startup() {
        JfxToolkit.startup()
    }

    fun renderPng(yaw: Double): ByteArray = JfxToolkit.runAndWait {
        val image = Image(skinBytes.inputStream())
        val width = settings.width.toDouble()
        val height = settings.height.toDouble()
        val pane = StackPane()
        Scene(pane, width, height)
        pane.resize(width, height)
        pane.children.setAll(
            JfxSkinCanvas(
                skinBytes = skinBytes,
                skinImage = image,
                slim = slim,
                settings = settings,
                overlayMode = overlayMode,
                lightingMode = lightingMode,
                shadows = shadows,
                yaw = yaw
            )
        )
        pane.layout()
        snapshot(Color.rgb(28, 32, 38), pane).png()
    }
}

private class JfxSkinCanvas(
    skinBytes: ByteArray,
    skinImage: Image,
    slim: Boolean,
    private val settings: BenchmarkRenderSettings,
    overlayMode: OverlayMode,
    lightingMode: LightingMode,
    shadows: Boolean,
    yaw: Double,
) : Group() {
    private val width = settings.width.toDouble()
    private val height = settings.height.toDouble()
    private val skin = skinImage
    private val skinBitmap = Bitmap.makeFromImage(SkiaImage.makeFromEncoded(skinBytes))
    private val camera = PerspectiveCamera(true).apply {
        fieldOfView = 45.0
        isVerticalFieldOfView = true
        nearClip = 0.1
        farClip = 200.0
        transforms.setAll(jfxCameraTransform(settings, yaw))
    }

    init {
        val playerMeshes = createMinecraftPlayerMeshes(
            skin = skinBitmap,
            isSlim = slim,
            use3DOverlay = overlayMode == OverlayMode.THREE_D
        )

        val world = Group().apply {
            if (shadows) {
                children.addAll(meshToJfxNodes(createSkinPlatform(settings.platformTopY, settings.platformThickness), null))
                children.add(createProjectedShadow(settings))
            }
            playerMeshes.flatMapTo(children) { meshToJfxNodes(it, skin) }
            children.addAll(createLights(lightingMode, settings))
        }

        val subScene = SubScene(world, width, height, true, SceneAntialiasing.BALANCED).apply {
            fill = Color.TRANSPARENT
            camera = this@JfxSkinCanvas.camera
        }
        children.add(subScene)
    }
}

private fun tavoloCameraEye(settings: BenchmarkRenderSettings, yaw: Double): Vec3d {
    val yawRad = Math.toRadians(yaw)
    val pitchRad = Math.toRadians(settings.pitch.toDouble())
    val target = Vec3d(
        settings.target.x.toDouble(),
        settings.target.y.toDouble(),
        settings.target.z.toDouble()
    )
    return Vec3d(
        target.x + settings.distance * cos(pitchRad) * sin(yawRad),
        target.y + settings.distance * sin(pitchRad),
        target.z + settings.distance * cos(pitchRad) * cos(yawRad)
    )
}

private fun jfxCameraTransform(settings: BenchmarkRenderSettings, yaw: Double): Affine {
    val eye = tavoloToJfx(tavoloCameraEye(settings, yaw))
    val target = tavoloToJfx(Vec3d(settings.target.x.toDouble(), settings.target.y.toDouble(), settings.target.z.toDouble()))
    val forward = (target - eye).normalized()
    val worldUp = Vec3d(0.0, -1.0, 0.0)
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

private fun meshToJfxNodes(mesh: SkinMesh, texture: Image?): List<Node> {
    if (mesh.texture != null && texture != null) {
        return listOf(
            MeshView(mesh.toTriangleMesh(mesh.faces)).apply {
                material = PhongMaterial().apply { diffuseMap = texture }
                cullFace = CullFace.NONE
            }
        )
    }
    return mesh.faces.groupBy { it.baseColor }.map { (color, faces) ->
        MeshView(mesh.toTriangleMesh(faces)).apply {
            material = PhongMaterial(color.toJfxColor())
            cullFace = CullFace.NONE
        }
    }
}

private fun SkinMesh.toTriangleMesh(selectedFaces: List<SkinMeshFace>): TriangleMesh =
    TriangleMesh().apply {
        vertices.forEach { vertex ->
            val p = tavoloToJfx(Vec3d(vertex.position.x.toDouble(), vertex.position.y.toDouble(), vertex.position.z.toDouble()))
            points.addAll(p.x.toFloat(), p.y.toFloat(), p.z.toFloat())
        }
        vertices.forEach { vertex ->
            texCoords.addAll(vertex.uv.u, vertex.uv.v)
        }
        if (vertices.isEmpty()) {
            texCoords.addAll(0f, 0f)
        }
        selectedFaces.forEach { face ->
            if (face.indices.size < 3) return@forEach
            for (i in 1 until face.indices.size - 1) {
                addTriangle(face.indices[0], face.indices[i + 1], face.indices[i])
            }
        }
    }

private fun TriangleMesh.addTriangle(a: Int, b: Int, c: Int) {
    faces.addAll(a, a, b, b, c, c)
}

private fun Int.toJfxColor(): Color =
    Color.rgb(SkiaColor.getR(this), SkiaColor.getG(this), SkiaColor.getB(this), SkiaColor.getA(this) / 255.0)

private fun createLights(lightingMode: LightingMode, settings: BenchmarkRenderSettings): List<Node> = when (lightingMode) {
    LightingMode.AMBIENT -> listOf(AmbientLight(Color.WHITE))
    LightingMode.DIRECTIONAL -> listOf(
        AmbientLight(Color.gray(0.65)),
        PointLight(Color.WHITE).apply {
            val scale = 80.0
            translateX = settings.lightDirection.x * scale
            translateY = -settings.lightDirection.y * scale
            translateZ = settings.lightDirection.z * scale
        }
    )
}

private fun createProjectedShadow(settings: BenchmarkRenderSettings): Node {
    val floorY = tavoloYToJfx(settings.platformTopY)
    val lightRay = Vec3d(
        settings.lightDirection.x.toDouble(),
        -settings.lightDirection.y.toDouble(),
        settings.lightDirection.z.toDouble()
    )
    val parts = listOf(
        BoxSpec(0.0, tavoloYToJfx(20f), 0.0, 8.8, 8.8, 8.8),
        BoxSpec(0.0, tavoloYToJfx(10f), 0.0, 8.4, 12.4, 4.4),
        BoxSpec(5.5, tavoloYToJfx(10f), 0.0, 3.4, 12.4, 4.4),
        BoxSpec(-5.5, tavoloYToJfx(10f), 0.0, 3.4, 12.4, 4.4),
        BoxSpec(2.0, tavoloYToJfx(-2f), 0.0, 4.4, 12.4, 4.4),
        BoxSpec(-2.0, tavoloYToJfx(-2f), 0.0, 4.4, 12.4, 4.4)
    )
    return Group().apply {
        children.addAll(parts.map { JfxShadowQuad(it, floorY, lightRay) })
    }
}

private fun tavoloToJfx(point: Vec3d): Vec3d = Vec3d(point.x, -point.y, point.z)

private fun tavoloYToJfx(y: Float): Double = -y.toDouble()

private data class Vec3d(val x: Double, val y: Double, val z: Double) {
    operator fun minus(other: Vec3d): Vec3d = Vec3d(x - other.x, y - other.y, z - other.z)
    operator fun unaryMinus(): Vec3d = Vec3d(-x, -y, -z)

    fun cross(other: Vec3d): Vec3d =
        Vec3d(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x)

    fun normalized(): Vec3d {
        val length = sqrt(x * x + y * y + z * z)
        return if (length > 0.0) Vec3d(x / length, y / length, z / length) else this
    }
}

private data class BoxSpec(
    val centerX: Double,
    val centerY: Double,
    val centerZ: Double,
    val width: Double,
    val height: Double,
    val depth: Double,
)

private class JfxShadowQuad(box: BoxSpec, floorY: Double, lightRay: Vec3d) : MeshView() {
    init {
        val corners = listOf(
            Vec3d(box.centerX - box.width / 2, box.centerY - box.height / 2, box.centerZ - box.depth / 2),
            Vec3d(box.centerX + box.width / 2, box.centerY - box.height / 2, box.centerZ - box.depth / 2),
            Vec3d(box.centerX - box.width / 2, box.centerY + box.height / 2, box.centerZ - box.depth / 2),
            Vec3d(box.centerX + box.width / 2, box.centerY + box.height / 2, box.centerZ - box.depth / 2),
            Vec3d(box.centerX - box.width / 2, box.centerY - box.height / 2, box.centerZ + box.depth / 2),
            Vec3d(box.centerX + box.width / 2, box.centerY - box.height / 2, box.centerZ + box.depth / 2),
            Vec3d(box.centerX - box.width / 2, box.centerY + box.height / 2, box.centerZ + box.depth / 2),
            Vec3d(box.centerX + box.width / 2, box.centerY + box.height / 2, box.centerZ + box.depth / 2)
        ).map { corner ->
            val t = (floorY - corner.y) / lightRay.y
            Vec3d(corner.x + lightRay.x * t, floorY, corner.z + lightRay.z * t)
        }
        val minX = corners.minOf { it.x }
        val maxX = corners.maxOf { it.x }
        val minZ = corners.minOf { it.z }
        val maxZ = corners.maxOf { it.z }
        mesh = quadMesh(minX, maxX, floorY, minZ, maxZ)
        material = PhongMaterial(Color.rgb(0, 0, 0, 0.28))
        cullFace = CullFace.NONE
    }
}

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

private fun Image.png(): ByteArray {
    val w = width.toInt()
    val h = height.toInt()
    val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val reader = pixelReader
    for (x in 0 until w) {
        for (y in 0 until h) {
            img.setRGB(x, y, reader.getArgb(x, y))
        }
    }
    return ByteArrayOutputStream().use {
        ImageIO.write(img, "png", it)
        it.toByteArray()
    }
}

private fun snapshot(bg: Color, pane: StackPane): WritableImage =
    WritableImage(pane.width.toInt(), pane.height.toInt()).also {
        pane.snapshot(SnapshotParameters().apply { fill = bg }, it)
    }
