package top.e404.skin.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Color
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import top.e404.tavolo.draw.render3d.*
import top.e404.tavolo.frame.Frame
import top.e404.tavolo.frame.encodeToBytes

/**
 * 根据皮肤贴图和模型类型创建完整的Minecraft玩家模型。
 */
fun createMinecraftPlayer(
    skin: Bitmap,
    isSlim: Boolean,
    pose: Map<BodyPart, List<Transformation>> = emptyMap(),
    use3DOverlay: Boolean = false
): Mesh {
    val texW = skin.width.toFloat()
    val texH = skin.height.toFloat()
    val componentMeshes = mutableListOf<Mesh>()
    val playerModel = PlayerModel(isSlim)

    for (partId in BodyPart.entries) {
        val partCube = playerModel.parts[partId] ?: continue
        val overlayCube = playerModel.overlays[partId]
        val transformations = pose[partId]
        val partDims = partId.getDims(isSlim)
        val partUvs = partCube.uvs.toGeometryFaceUvs()

        val transform: (Vec3) -> Vec3 = { vertexPos ->
            if (transformations.isNullOrEmpty()) {
                vertexPos
            } else {
                transformations.fold(vertexPos) { currentPos, transformation ->
                    when (transformation) {
                        is Transformation.Rotate -> (currentPos - partCube.pivot).rotate(transformation) + partCube.pivot
                        is Transformation.Scale -> (currentPos - partCube.pivot).scale(transformation) + partCube.pivot
                        is Transformation.Translate -> currentPos.translate(transformation)
                    }
                }
            }
        }

        val baseMesh = createMinecraftUVCuboid(partDims, partUvs, texW, texH)
        componentMeshes.add(Mesh(baseMesh.vertices.map {
            Vertex(transform(it.position) + partCube.pos, it.uv)
        }, baseMesh.faces))

        overlayCube?.let {
            val overlayUvs = it.uvs.toGeometryFaceUvs()
            val overlayMesh = if (use3DOverlay) {
                val overlayDepth = if (partId == BodyPart.HEAD) 0.5f else 0.25f
                create3DOverlay(skin, partDims, overlayDepth, overlayUvs, texW, texH)
            } else {
                val overlaySize = if (partId == BodyPart.HEAD) 1.0f else 0.5f
                createMinecraftUVCuboid(partDims + Vec3(overlaySize, overlaySize, overlaySize), overlayUvs, texW, texH)
            }
            componentMeshes.add(Mesh(overlayMesh.vertices.map { vertex ->
                Vertex(transform(vertex.position) + partCube.pos, vertex.uv)
            }, overlayMesh.faces))
        }
    }
    return combineMeshes(componentMeshes, skin)
}

/**
 * 渲染Minecraft皮肤为图像的函数 (API无变化)
 */
fun renderMinecraftView(
    skin: Image,
    isSlim: Boolean,
    renderConfig: RenderConfig,
    backgroundMeshes: List<Mesh> = emptyList(),
    pose: Map<BodyPart, List<Transformation>> = emptyMap(),
    use3DOverlay: Boolean = true,
): Image {
    val skinBitmap = Bitmap.makeFromImage(skin)
    val playerMesh = createMinecraftPlayer(skinBitmap, isSlim, pose, use3DOverlay)
    val scene = Scene(listOf(playerMesh) + backgroundMeshes)
    return renderSceneToImage(scene, renderConfig)
}

/**
 * 渲染旋转动画的函数 (API无变化)
 */
suspend fun renderRotate(
    skin: Image,
    isSlim: Boolean,
    config: RenderConfig,
    frameCount: Int,
    frameDuration: Int,
    pose: Map<BodyPart, List<Transformation>> = emptyMap(),
    use3DOverlay: Boolean = true
): ByteArray {
    val unitAngel = 360f / frameCount
    val skinBitmap = Bitmap.makeFromImage(skin)
    val playerMesh = createMinecraftPlayer(skinBitmap, isSlim, pose, use3DOverlay)
    val ground = createPlane(
        center = Vec3(0f, -15f, 0f), // 脚下
        size = Vec2(100f, 100f),
        color = Color.makeRGB(200, 200, 200), // 灰色地面
        normalDirection = Vec3(0f, 1f, 0f)
    )
    val wall = createPlane(
        center = Vec3(0f, 10f, -30f), // 身后背景墙
        size = Vec2(100f, 100f),
        color = Color.makeRGB(220, 220, 255),
        normalDirection = Vec3(0f, 0f, 1f) // 面向 Z 轴正向
    )
    val scene = Scene(listOf(playerMesh, ground, wall))
    return coroutineScope {
        withContext(Dispatchers.Default) {
            (0 until frameCount).map { i ->
                async {
                    val angle = i * unitAngel
                    val rotatedCamera = config.camera.copy(yaw = config.camera.yaw + angle)
                    renderSceneToImage(
                        scene,
                        config.copy(camera = rotatedCamera)
                    ).let { Frame(frameDuration, it) }
                }
            }.awaitAll()
        }.encodeToBytes()
    }
}

private fun Map<SkinFace, Rect>.toGeometryFaceUvs(): Map<FaceDirection, Rect> = mapOf(
    // Tavolo 的 RIGHT/LEFT 是几何 +X/-X；Minecraft 语义 RIGHT/LEFT 是玩家右/左侧。
    FaceDirection.RIGHT to getValue(SkinFace.LEFT),
    FaceDirection.LEFT to getValue(SkinFace.RIGHT),
    FaceDirection.TOP to getValue(SkinFace.TOP),
    FaceDirection.BOTTOM to getValue(SkinFace.BOTTOM),
    FaceDirection.FRONT to getValue(SkinFace.FRONT),
    FaceDirection.BACK to getValue(SkinFace.BACK),
)

private fun createMinecraftUVCuboid(
    dims: Vec3,
    faceUVs: Map<FaceDirection, Rect>,
    textureWidth: Float,
    textureHeight: Float,
): Mesh {
    val (w, h, d) = dims
    val vertices = mutableListOf<Vertex>()
    val faces = mutableListOf<Face>()
    val v = listOf(
        Vec3(-w / 2, -h / 2, d / 2),
        Vec3(w / 2, -h / 2, d / 2),
        Vec3(w / 2, h / 2, d / 2),
        Vec3(-w / 2, h / 2, d / 2),
        Vec3(w / 2, -h / 2, -d / 2),
        Vec3(-w / 2, -h / 2, -d / 2),
        Vec3(-w / 2, h / 2, -d / 2),
        Vec3(w / 2, h / 2, -d / 2)
    )

    fun u(px: Float) = px / textureWidth
    fun v(py: Float) = py / textureHeight

    fun addFace(direction: FaceDirection, vIndices: List<Int>, uvRect: Rect) {
        val texelCenterOffset = 0.5f
        val centeredRect = Rect.makeLTRB(
            uvRect.left + texelCenterOffset,
            uvRect.top + texelCenterOffset,
            uvRect.right - texelCenterOffset,
            uvRect.bottom - texelCenterOffset
        )
        val bottomLeft = Vec2(u(centeredRect.left), v(centeredRect.bottom))
        val bottomRight = Vec2(u(centeredRect.right), v(centeredRect.bottom))
        val topRight = Vec2(u(centeredRect.right), v(centeredRect.top))
        val topLeft = Vec2(u(centeredRect.left), v(centeredRect.top))
        val uvs = when {
            direction == FaceDirection.BOTTOM -> listOf(topLeft, topRight, bottomRight, bottomLeft)
            else -> listOf(bottomLeft, bottomRight, topRight, topLeft)
        }
        val faceIndices = mutableListOf<Int>()
        for (i in vIndices.indices) {
            vertices.add(Vertex(v[vIndices[i]], uvs[i]))
            faceIndices.add(vertices.size - 1)
        }
        faces.add(Face(faceIndices, Color.WHITE))
    }

    faceUVs[FaceDirection.FRONT]?.let { addFace(FaceDirection.FRONT, listOf(0, 1, 2, 3), it) }
    faceUVs[FaceDirection.BACK]?.let { addFace(FaceDirection.BACK, listOf(4, 5, 6, 7), it) }
    faceUVs[FaceDirection.RIGHT]?.let { addFace(FaceDirection.RIGHT, listOf(1, 4, 7, 2), it) }
    faceUVs[FaceDirection.LEFT]?.let { addFace(FaceDirection.LEFT, listOf(5, 0, 3, 6), it) }
    faceUVs[FaceDirection.TOP]?.let { addFace(FaceDirection.TOP, listOf(3, 2, 7, 6), it) }
    faceUVs[FaceDirection.BOTTOM]?.let { addFace(FaceDirection.BOTTOM, listOf(5, 4, 1, 0), it) }
    return Mesh(vertices, faces)
}
