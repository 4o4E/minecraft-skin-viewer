package top.e404.skin.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Color
import org.jetbrains.skia.Image
import top.e404.skiko.draw.render3d.*
import top.e404.skiko.frame.Frame
import top.e404.skiko.frame.encodeToBytes

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

        val baseMesh = createUVCuboid(partDims, partCube.uvs, texW, texH)
        componentMeshes.add(Mesh(baseMesh.vertices.map {
            Vertex(transform(it.position) + partCube.pos, it.uv)
        }, baseMesh.faces))

        overlayCube?.let {
            val overlayMesh = if (use3DOverlay) {
                val overlayDepth = if (partId == BodyPart.HEAD) 0.5f else 0.25f
                create3DOverlay(skin, partDims, overlayDepth, it.uvs, texW, texH)
            } else {
                val overlaySize = if (partId == BodyPart.HEAD) 1.0f else 0.5f
                createUVCuboid(partDims + Vec3(overlaySize, overlaySize, overlaySize), it.uvs, texW, texH)
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