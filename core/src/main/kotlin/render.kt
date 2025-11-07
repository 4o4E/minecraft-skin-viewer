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
import top.e404.skiko.draw.render3d.*
import top.e404.skiko.frame.Frame
import top.e404.skiko.frame.encodeToBytes
import kotlin.math.cos
import kotlin.math.sin

// =================================================================================
// region 1. 核心数据结构与工具
// =================================================================================

/**
 * 封装所有可能的变换操作
 */
sealed class Transformation {
    data class Rotate(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) : Transformation()
    data class Scale(val x: Float = 1f, val y: Float = 1f, val z: Float = 1f) : Transformation()
    data class Translate(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) : Transformation()
}

/**
 * 预设的玩家姿势
 */
object PosePresets {
    val WALKING = mapOf(
        BodyPart.RIGHT_ARM to listOf(Transformation.Rotate(x = -30f)),
        BodyPart.LEFT_ARM to listOf(Transformation.Rotate(x = 30f)),
        BodyPart.RIGHT_LEG to listOf(Transformation.Rotate(x = 30f)),
        BodyPart.LEFT_LEG to listOf(Transformation.Rotate(x = -30f)),
    )
    val SIT = mapOf(
        BodyPart.RIGHT_ARM to listOf(Transformation.Rotate(x = -30f)),
        BodyPart.LEFT_ARM to listOf(Transformation.Rotate(x = -30f)),
        BodyPart.RIGHT_LEG to listOf(Transformation.Rotate(x = -80f, z = -20f)),
        BodyPart.LEFT_LEG to listOf(Transformation.Rotate(x = -80f, z = 20f)),
    )
    val HOMO = mapOf(
        BodyPart.HEAD to listOf(Transformation.Rotate(y = -30f)),
        BodyPart.RIGHT_ARM to listOf(Transformation.Rotate(x = -30f)),
        BodyPart.LEFT_ARM to listOf(Transformation.Rotate(x = -30f)),
        BodyPart.RIGHT_LEG to listOf(Transformation.Rotate(x = -80f, z = -20f)),
        BodyPart.LEFT_LEG to listOf(Transformation.Rotate(x = -80f, z = 20f)),
    )
    // fun withScale(headScale: Float = 1f, laScale: Float = 1f, raScale: Float = 1f, llScale: Float = 1f, rlScale: Float = 1f, ) = mapOf(
    //     BodyPart.HEAD to listOf(
    //         Transformation.Rotate(y = -30f),
    //         Transformation.Scale(headScale, headScale, headScale),
    //         Transformation.Translate(y = BodyPart.HEAD.getDims(false).y.let { it * (headScale - 1) / 2 }),
    //     ),
    //     BodyPart.RIGHT_ARM to listOf(
    //         Transformation.Rotate(x = -30f),
    //         Transformation.Scale(raScale, raScale, raScale),
    //         Transformation.Translate(x = -BodyPart.LEFT_ARM.getDims(false).x.let { it * (raScale - 1) / 2 }),
    //     ),
    //     BodyPart.LEFT_ARM to listOf(
    //         Transformation.Rotate(x = -30f),
    //         Transformation.Scale(laScale, laScale, laScale),
    //         Transformation.Translate(x = BodyPart.LEFT_ARM.getDims(false).x.let { it * (laScale - 1) / 2 }),
    //     ),
    //     BodyPart.RIGHT_LEG to listOf(
    //         Transformation.Rotate(x = -80f, z = -20f),
    //         Transformation.Scale(rlScale, rlScale, rlScale),
    //         Transformation.Translate(y = -BodyPart.LEFT_LEG.getDims(false).y.let { it * (rlScale - 1) / 2 }),
    //     ),
    //     BodyPart.LEFT_LEG to listOf(
    //         Transformation.Rotate(x = -80f, z = 20f),
    //         Transformation.Scale(llScale, llScale, llScale),
    //         Transformation.Translate(y = -BodyPart.LEFT_LEG.getDims(false).y.let { it * (llScale - 1) / 2 }),
    //     ),
    // )
}

// region 扩展函数
private fun Vec3.rotateX(angle: Float): Vec3 {
    val cos = cos(angle)
    val sin = sin(angle)
    return Vec3(x, y * cos - z * sin, y * sin + z * cos)
}

private fun Vec3.rotateY(angle: Float): Vec3 {
    val cos = cos(angle)
    val sin = sin(angle)
    return Vec3(x * cos + z * sin, y, -x * sin + z * cos)
}

private fun Vec3.rotateZ(angle: Float): Vec3 {
    val cos = cos(angle)
    val sin = sin(angle)
    return Vec3(x * cos - y * sin, x * sin + y * cos, z)
}

private fun Vec3.rotate(rotation: Transformation.Rotate): Vec3 {
    val radX = Math.toRadians(rotation.x.toDouble()).toFloat()
    val radY = Math.toRadians(rotation.y.toDouble()).toFloat()
    val radZ = Math.toRadians(rotation.z.toDouble()).toFloat()
    return this.rotateZ(radZ).rotateY(radY).rotateX(radX)
}

private fun Vec3.scale(scaling: Transformation.Scale): Vec3 {
    return Vec3(x * scaling.x, y * scaling.y, z * scaling.z)
}

private fun Vec3.translate(translation: Transformation.Translate): Vec3 {
    return this.plus(Vec3(translation.x, translation.y, translation.z))
}
// endregion

// =================================================================================
// region 2. 模型定义抽象
// =================================================================================

/**
 * [修正] 封装 Minecraft 皮肤模型的一个立方体部件。
 *
 * 新增了 `mirrored` 参数来处理左侧肢体贴图的镜像问题。
 *
 * @param u 该部件在皮肤贴图上的起始 U 坐标（左）。
 * @param v 该部件在皮肤贴图上的起始 V 坐标（上）。
 * @param dims 部件的尺寸 (宽度, 高度, 深度)。
 * @param pos 部件在模型坐标系中的位置。
 * @param pivot 部件进行旋转和缩放时的轴心点（相对于部件几何中心）。
 * @param mirrored 如果为 true，则交换左右两个面的UV贴图，以适应Minecraft左侧肢体的布局。
 */
class SkinCube(
    u: Float, v: Float,
    val dims: Vec3,
    val pos: Vec3,
    val pivot: Vec3 = Vec3(0f, 0f, 0f),
    mirrored: Boolean = false // [新增]
) {
    val uvs: Map<FaceDirection, Rect>

    init {
        val (w, h, d) = dims
        val standardRight = Rect.makeXYWH(u, v + d, d, h)
        val standardLeft = Rect.makeXYWH(u + d + w, v + d, d, h)

        uvs = mapOf(
            // [修改] 根据 mirrored 参数决定左右面的UV
            FaceDirection.RIGHT to if (mirrored) standardLeft else standardRight,
            FaceDirection.LEFT to if (mirrored) standardRight else standardLeft,
            // 其他面保持不变
            FaceDirection.TOP to Rect.makeXYWH(u + d, v, w, d),
            FaceDirection.BOTTOM to Rect.makeXYWH(u + d + w, v, w, d),
            FaceDirection.FRONT to Rect.makeXYWH(u + d, v + d, w, h),
            FaceDirection.BACK to Rect.makeXYWH(u + d + w + d, v + d, w, h)
        )
    }
}

/**
 * [重构] 身体部位的标识符。
 */
enum class BodyPart {
    HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG
}

/**
 * [修正] 定义和存储一个完整的 Minecraft 玩家模型。
 *
 * 为所有左侧肢体（LEFT_ARM, LEFT_LEG）的 SkinCube 定义添加 `mirrored = true`。
 *
 * @param isSlim 是否为 Alex (Slim) 模型。
 */
class PlayerModel(val isSlim: Boolean) {
    val parts: Map<BodyPart, SkinCube>
    val overlays: Map<BodyPart, SkinCube>

    init {
        if (isSlim) {
            // Alex (Slim) 模型定义
            parts = mapOf(
                BodyPart.HEAD to SkinCube(0f, 0f, Vec3(8f, 8f, 8f), Vec3(0f, 20f, 0f), mirrored = true),
                BodyPart.BODY to SkinCube(16f, 16f, Vec3(8f, 12f, 4f), Vec3(0f, 10f, 0f)),
                BodyPart.RIGHT_ARM to SkinCube(40f, 16f, Vec3(3f, 12f, 4f), Vec3(-5.5f, 10f, 0f), Vec3(0f, 6f, 0f)),
                BodyPart.LEFT_ARM to SkinCube(32f, 48f, Vec3(3f, 12f, 4f), Vec3(5.5f, 10f, 0f), Vec3(0f, 6f, 0f), mirrored = true), // [修正]
                BodyPart.RIGHT_LEG to SkinCube(0f, 16f, Vec3(4f, 12f, 4f), Vec3(-2f, -2f, 0f), Vec3(0f, 6f, 0f)),
                BodyPart.LEFT_LEG to SkinCube(16f, 48f, Vec3(4f, 12f, 4f), Vec3(2f, -2f, 0f), Vec3(0f, 6f, 0f), mirrored = true) // [修正]
            )
            overlays = mapOf(
                BodyPart.HEAD to SkinCube(32f, 0f, Vec3(8f, 8f, 8f), Vec3(0f, 20f, 0f), mirrored = true),
                BodyPart.BODY to SkinCube(16f, 32f, Vec3(8f, 12f, 4f), Vec3(0f, 10f, 0f)),
                BodyPart.RIGHT_ARM to SkinCube(40f, 32f, Vec3(3f, 12f, 4f), Vec3(-5.5f, 10f, 0f), Vec3(0f, 6f, 0f)),
                BodyPart.LEFT_ARM to SkinCube(48f, 48f, Vec3(3f, 12f, 4f), Vec3(5.5f, 10f, 0f), Vec3(0f, 6f, 0f), mirrored = true), // [修正]
                BodyPart.RIGHT_LEG to SkinCube(0f, 32f, Vec3(4f, 12f, 4f), Vec3(-2f, -2f, 0f), Vec3(0f, 6f, 0f)),
                BodyPart.LEFT_LEG to SkinCube(0f, 48f, Vec3(4f, 12f, 4f), Vec3(2f, -2f, 0f), Vec3(0f, 6f, 0f), mirrored = true) // [修正]
            )
        } else {
            // Steve (Classic) 模型定义
            parts = mapOf(
                BodyPart.HEAD to SkinCube(0f, 0f, Vec3(8f, 8f, 8f), Vec3(0f, 20f, 0f), mirrored = true),
                BodyPart.BODY to SkinCube(16f, 16f, Vec3(8f, 12f, 4f), Vec3(0f, 10f, 0f)),
                BodyPart.RIGHT_ARM to SkinCube(40f, 16f, Vec3(4f, 12f, 4f), Vec3(-6f, 10f, 0f), Vec3(0f, 6f, 0f)),
                BodyPart.LEFT_ARM to SkinCube(32f, 48f, Vec3(4f, 12f, 4f), Vec3(6f, 10f, 0f), Vec3(0f, 6f, 0f), mirrored = true), // [修正]
                BodyPart.RIGHT_LEG to SkinCube(0f, 16f, Vec3(4f, 12f, 4f), Vec3(-2f, -2f, 0f), Vec3(0f, 6f, 0f)),
                BodyPart.LEFT_LEG to SkinCube(16f, 48f, Vec3(4f, 12f, 4f), Vec3(2f, -2f, 0f), Vec3(0f, 6f, 0f), mirrored = true) // [修正]
            )
            overlays = mapOf(
                BodyPart.HEAD to SkinCube(32f, 0f, Vec3(8f, 8f, 8f), Vec3(0f, 20f, 0f), mirrored = true),
                BodyPart.BODY to SkinCube(16f, 32f, Vec3(8f, 12f, 4f), Vec3(0f, 10f, 0f)),
                BodyPart.RIGHT_ARM to SkinCube(40f, 32f, Vec3(4f, 12f, 4f), Vec3(-6f, 10f, 0f), Vec3(0f, 6f, 0f)),
                BodyPart.LEFT_ARM to SkinCube(48f, 48f, Vec3(4f, 12f, 4f), Vec3(6f, 10f, 0f), Vec3(0f, 6f, 0f), mirrored = true), // [修正]
                BodyPart.RIGHT_LEG to SkinCube(0f, 32f, Vec3(4f, 12f, 4f), Vec3(-2f, -2f, 0f), Vec3(0f, 6f, 0f)),
                BodyPart.LEFT_LEG to SkinCube(0f, 48f, Vec3(4f, 12f, 4f), Vec3(2f, -2f, 0f), Vec3(0f, 6f, 0f), mirrored = true) // [修正]
            )
        }
    }
}

// =================================================================================
// region 3. 3D 外层实现
// =================================================================================

private enum class PixelPosition {
    INNER, TOP, BOTTOM, LEFT, RIGHT, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

private val FACE_ADJACENCY: Map<Pair<FaceDirection, PixelPosition>, FaceDirection> = mapOf(
    (FaceDirection.TOP to PixelPosition.TOP) to FaceDirection.BACK, (FaceDirection.TOP to PixelPosition.BOTTOM) to FaceDirection.FRONT, (FaceDirection.TOP to PixelPosition.LEFT) to FaceDirection.LEFT, (FaceDirection.TOP to PixelPosition.RIGHT) to FaceDirection.RIGHT,
    (FaceDirection.BOTTOM to PixelPosition.TOP) to FaceDirection.FRONT, (FaceDirection.BOTTOM to PixelPosition.BOTTOM) to FaceDirection.BACK, (FaceDirection.BOTTOM to PixelPosition.LEFT) to FaceDirection.LEFT, (FaceDirection.BOTTOM to PixelPosition.RIGHT) to FaceDirection.RIGHT,
    (FaceDirection.FRONT to PixelPosition.TOP) to FaceDirection.TOP, (FaceDirection.FRONT to PixelPosition.BOTTOM) to FaceDirection.BOTTOM, (FaceDirection.FRONT to PixelPosition.LEFT) to FaceDirection.LEFT, (FaceDirection.FRONT to PixelPosition.RIGHT) to FaceDirection.RIGHT,
    (FaceDirection.BACK to PixelPosition.TOP) to FaceDirection.TOP, (FaceDirection.BACK to PixelPosition.BOTTOM) to FaceDirection.BOTTOM, (FaceDirection.BACK to PixelPosition.LEFT) to FaceDirection.RIGHT, (FaceDirection.BACK to PixelPosition.RIGHT) to FaceDirection.LEFT,
    (FaceDirection.LEFT to PixelPosition.TOP) to FaceDirection.TOP, (FaceDirection.LEFT to PixelPosition.BOTTOM) to FaceDirection.BOTTOM, (FaceDirection.LEFT to PixelPosition.LEFT) to FaceDirection.BACK, (FaceDirection.LEFT to PixelPosition.RIGHT) to FaceDirection.FRONT,
    (FaceDirection.RIGHT to PixelPosition.TOP) to FaceDirection.TOP, (FaceDirection.RIGHT to PixelPosition.BOTTOM) to FaceDirection.BOTTOM, (FaceDirection.RIGHT to PixelPosition.LEFT) to FaceDirection.FRONT, (FaceDirection.RIGHT to PixelPosition.RIGHT) to FaceDirection.BACK
)

private val ADJACENT_UV_MAPPING: Map<Pair<FaceDirection, PixelPosition>, (px: Int, py: Int, adjW: Int, adjH: Int) -> Pair<Int, Int>> = mapOf(
    FaceDirection.FRONT to PixelPosition.TOP to { px, _, _, adjH -> px to adjH - 1 },
    FaceDirection.FRONT to PixelPosition.BOTTOM to { px, _, _, _ -> px to 0 },
    FaceDirection.FRONT to PixelPosition.LEFT to { _, py, adjW, _ -> adjW - 1 to py },
    FaceDirection.FRONT to PixelPosition.RIGHT to { _, py, _, _ -> 0 to py },
    FaceDirection.BACK to PixelPosition.TOP to { px, _, adjW, _ -> adjW - 1 - px to 0 },
    FaceDirection.BACK to PixelPosition.BOTTOM to { px, _, adjW, adjH -> adjW - 1 - px to adjH - 1 },
    FaceDirection.BACK to PixelPosition.LEFT to { _, py, adjW, _ -> adjW - 1 to py },
    FaceDirection.BACK to PixelPosition.RIGHT to { _, py, _, _ -> 0 to py },
    FaceDirection.TOP to PixelPosition.TOP to { px, _, adjW, _ -> adjW - 1 - px to 0 },
    FaceDirection.TOP to PixelPosition.BOTTOM to { px, _, _, _ -> px to 0 },
    FaceDirection.TOP to PixelPosition.LEFT to { _, py, _, _ -> py to 0 },
    FaceDirection.TOP to PixelPosition.RIGHT to { _, py, adjW, _ -> adjW - 1 - py to 0 },
    FaceDirection.BOTTOM to PixelPosition.TOP to { px, _, _, adjH -> px to adjH - 1 },
    FaceDirection.BOTTOM to PixelPosition.BOTTOM to { px, _, adjW, adjH -> adjW - 1 - px to adjH - 1 },
    FaceDirection.BOTTOM to PixelPosition.LEFT to { _, py, adjW, adjH -> adjW - 1 - py to adjH - 1 },
    FaceDirection.BOTTOM to PixelPosition.RIGHT to { _, py, _, adjH -> py to adjH - 1 },
    FaceDirection.LEFT to PixelPosition.TOP to { px, _, _, _ -> 0 to px },
    FaceDirection.LEFT to PixelPosition.BOTTOM to { px, _, _, adjH -> 0 to adjH - 1 - px },
    FaceDirection.LEFT to PixelPosition.LEFT to { _, py, _, _ -> 0 to py },
    FaceDirection.LEFT to PixelPosition.RIGHT to { _, py, _, _ -> 0 to py },
    FaceDirection.RIGHT to PixelPosition.TOP to { px, _, adjW, adjH -> adjW - 1 to adjH - 1 - px },
    FaceDirection.RIGHT to PixelPosition.BOTTOM to { px, _, adjW, _ -> adjW - 1 to px },
    FaceDirection.RIGHT to PixelPosition.LEFT to { _, py, _, _ -> 0 to py },
    FaceDirection.RIGHT to PixelPosition.RIGHT to { _, py, adjW, _ -> adjW - 1 to py }
)

private fun getPixelPosition(px: Int, py: Int, width: Int, height: Int): PixelPosition {
    val isTop = py == 0
    val isBottom = py == height - 1
    val isLeft = px == 0
    val isRight = px == width - 1

    return when {
        isTop && isLeft -> PixelPosition.TOP_LEFT
        isTop && isRight -> PixelPosition.TOP_RIGHT
        isBottom && isLeft -> PixelPosition.BOTTOM_LEFT
        isBottom && isRight -> PixelPosition.BOTTOM_RIGHT
        isTop -> PixelPosition.TOP
        isBottom -> PixelPosition.BOTTOM
        isLeft -> PixelPosition.LEFT
        isRight -> PixelPosition.RIGHT
        else -> PixelPosition.INNER
    }
}

private fun isAdjacentTransparent(
    skin: Bitmap,
    currentDirection: FaceDirection,
    edge: PixelPosition,
    px: Int, py: Int,
    faceUVs: Map<FaceDirection, Rect>
): Boolean {
    val adjFaceDir = FACE_ADJACENCY[currentDirection to edge] ?: return true
    val adjUvRect = faceUVs[adjFaceDir] ?: return true
    val adjUvW = adjUvRect.width.toInt()
    val adjUvH = adjUvRect.height.toInt()

    val uvMappingFunc = ADJACENT_UV_MAPPING[currentDirection to edge] ?: return true
    val (adj_local_px, adj_local_py) = uvMappingFunc(px, py, adjUvW, adjUvH)

    val final_adj_px = adjUvRect.left.toInt() + adj_local_px
    val final_adj_py = adjUvRect.top.toInt() + adj_local_py

    if (final_adj_px < 0 || final_adj_px >= skin.width || final_adj_py < 0 || final_adj_py >= skin.height) {
        return true
    }
    return Color.getA(skin.getColor(final_adj_px, final_adj_py)) == 0
}

private fun create3DOverlay(
    skin: Bitmap,
    dims: Vec3,
    overlayDepth: Float,
    faceUVs: Map<FaceDirection, Rect>,
    textureWidth: Float,
    textureHeight: Float
): Mesh {
    val vertices = mutableListOf<Vertex>()
    val faces = mutableListOf<Face>()
    val effectiveDims = dims + Vec3(2 * overlayDepth, 2 * overlayDepth, 2 * overlayDepth)

    for ((direction, uvRect) in faceUVs) {
        val uvW = uvRect.width.toInt()
        val uvH = uvRect.height.toInt()

        for (px in 0 until uvW) {
            for (py in 0 until uvH) {
                if (Color.getA(skin.getColor(uvRect.left.toInt() + px, uvRect.top.toInt() + py)) <= 0) continue

                val pixelUV = Vec2(
                    (uvRect.left + px + 0.5f) / textureWidth,
                    (uvRect.top + py + 0.5f) / textureHeight
                )

                val (voxelW, voxelH) = when (direction) {
                    FaceDirection.RIGHT, FaceDirection.LEFT -> Pair(effectiveDims.z / uvW, effectiveDims.y / uvH)
                    FaceDirection.TOP, FaceDirection.BOTTOM -> Pair(effectiveDims.x / uvW, effectiveDims.z / uvH)
                    else -> Pair(effectiveDims.x / uvW, effectiveDims.y / uvH)
                }
                val voxelD = voxelW
                val retractionDepth = voxelD - overlayDepth
                val w = voxelW / 2; val h = voxelH / 2; val d = voxelD / 2

                val v = mutableListOf(
                    Vec3(-w, -h, -d), Vec3(w, -h, -d), Vec3(w, h, -d), Vec3(-w, h, -d),
                    Vec3(-w, -h, d), Vec3(w, -h, d), Vec3(w, h, d), Vec3(-w, h, d)
                )

                val pixelPosition = getPixelPosition(px, py, uvW, uvH)
                if (pixelPosition != PixelPosition.INNER) {
                    val shrinkTop = pixelPosition in listOf(PixelPosition.TOP, PixelPosition.TOP_LEFT, PixelPosition.TOP_RIGHT) && !isAdjacentTransparent(skin, direction, PixelPosition.TOP, px, py, faceUVs)
                    val shrinkBottom = pixelPosition in listOf(PixelPosition.BOTTOM, PixelPosition.BOTTOM_LEFT, PixelPosition.BOTTOM_RIGHT) && !isAdjacentTransparent(skin, direction, PixelPosition.BOTTOM, px, py, faceUVs)
                    val shrinkLeft = pixelPosition in listOf(PixelPosition.LEFT, PixelPosition.TOP_LEFT, PixelPosition.BOTTOM_LEFT) && !isAdjacentTransparent(skin, direction, PixelPosition.LEFT, px, py, faceUVs)
                    val shrinkRight = pixelPosition in listOf(PixelPosition.RIGHT, PixelPosition.TOP_RIGHT, PixelPosition.BOTTOM_RIGHT) && !isAdjacentTransparent(skin, direction, PixelPosition.RIGHT, px, py, faceUVs)

                    if (shrinkTop) { v[2] = v[2].copy(y = v[2].y - voxelD); v[3] = v[3].copy(y = v[3].y - voxelD) }
                    if (shrinkBottom) { v[0] = v[0].copy(y = v[0].y + voxelD); v[1] = v[1].copy(y = v[1].y + voxelD) }
                    if (shrinkLeft) { v[0] = v[0].copy(x = v[0].x + voxelD); v[3] = v[3].copy(x = v[3].x + voxelD) }
                    if (shrinkRight) { v[1] = v[1].copy(x = v[1].x - voxelD); v[2] = v[2].copy(x = v[2].x - voxelD) }
                }

                val finalVoxelVertices = v.map { p ->
                    val rotatedP = when (direction) {
                        FaceDirection.FRONT -> p
                        FaceDirection.BACK -> Vec3(-p.x, p.y, -p.z)
                        FaceDirection.RIGHT -> Vec3(p.z, p.y, -p.x)
                        FaceDirection.LEFT -> Vec3(-p.z, p.y, p.x)
                        FaceDirection.TOP -> Vec3(p.x, p.z, -p.y)
                        FaceDirection.BOTTOM -> Vec3(p.x, -p.z, p.y)
                    }
                    val voxelCenter = when (direction) {
                        FaceDirection.FRONT -> Vec3(-effectiveDims.x / 2 + (px + 0.5f) * voxelW, effectiveDims.y / 2 - (py + 0.5f) * voxelH, dims.z / 2 + d - retractionDepth)
                        FaceDirection.BACK -> Vec3(effectiveDims.x / 2 - (px + 0.5f) * voxelW, effectiveDims.y / 2 - (py + 0.5f) * voxelH, -dims.z / 2 - d + retractionDepth)
                        FaceDirection.RIGHT -> Vec3(dims.x / 2 + d - retractionDepth, effectiveDims.y / 2 - (py + 0.5f) * voxelH, effectiveDims.z / 2 - (px + 0.5f) * voxelW)
                        FaceDirection.LEFT -> Vec3(-dims.x / 2 - d + retractionDepth, effectiveDims.y / 2 - (py + 0.5f) * voxelH, -effectiveDims.z / 2 + (px + 0.5f) * voxelW)
                        FaceDirection.TOP -> Vec3(-effectiveDims.x / 2 + (px + 0.5f) * voxelW, dims.y / 2 + d - retractionDepth, -effectiveDims.z / 2 + (py + 0.5f) * voxelH)
                        FaceDirection.BOTTOM -> Vec3(-effectiveDims.x / 2 + (px + 0.5f) * voxelW, -dims.y / 2 - d + retractionDepth, effectiveDims.z / 2 - (py + 0.5f) * voxelH)
                    }
                    rotatedP + voxelCenter
                }

                val baseIndex = vertices.size
                vertices.addAll(finalVoxelVertices.map { Vertex(it, pixelUV) })

                faces.addAll(listOf(
                    Face(listOf(baseIndex + 4, baseIndex + 7, baseIndex + 6, baseIndex + 5), Color.WHITE),
                    Face(listOf(baseIndex + 0, baseIndex + 3, baseIndex + 2, baseIndex + 1), Color.WHITE),
                    Face(listOf(baseIndex + 5, baseIndex + 6, baseIndex + 2, baseIndex + 1), Color.WHITE),
                    Face(listOf(baseIndex + 4, baseIndex + 0, baseIndex + 3, baseIndex + 7), Color.WHITE),
                    Face(listOf(baseIndex + 7, baseIndex + 3, baseIndex + 2, baseIndex + 6), Color.WHITE),
                    Face(listOf(baseIndex + 4, baseIndex + 5, baseIndex + 1, baseIndex + 0), Color.WHITE)
                ))
            }
        }
    }
    return Mesh(vertices, faces)
}

// =================================================================================
// region 4. 核心渲染流程
// =================================================================================

/**
 * [重构] 根据皮肤贴图和模型类型创建完整的Minecraft玩家模型。
 */
internal fun createMinecraftPlayer(
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

        val baseMesh = createUVCuboid(partCube.dims, partCube.uvs, texW, texH)
        componentMeshes.add(Mesh(baseMesh.vertices.map {
            Vertex(transform(it.position) + partCube.pos, it.uv)
        }, baseMesh.faces))

        overlayCube?.let {
            val overlayMesh = if (use3DOverlay) {
                val overlayDepth = if (partId == BodyPart.HEAD) 0.5f else 0.25f
                create3DOverlay(skin, partCube.dims, overlayDepth, it.uvs, texW, texH)
            } else {
                val overlaySize = if (partId == BodyPart.HEAD) 1.0f else 0.5f
                createUVCuboid(partCube.dims + Vec3(overlaySize, overlaySize, overlaySize), it.uvs, texW, texH)
            }
            componentMeshes.add(Mesh(overlayMesh.vertices.map {
                Vertex(transform(it.position) + partCube.pos, it.uv)
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
    width: Int,
    height: Int,
    backgroundColor: Int,
    camera: OrbitCamera,
    lightDirection: Vec3 = Vec3(-0.7f, 1.0f, 0.5f).normalized(),
    pose: Map<BodyPart, List<Transformation>> = emptyMap(),
    use3DOverlay: Boolean = false
): Image {
    val skinBitmap = Bitmap.makeFromImage(skin)
    val playerMesh = createMinecraftPlayer(skinBitmap, isSlim, pose, use3DOverlay)
    val (viewMatrix, eyePosition) = createViewMatrix(camera)
    val cameraForward = (camera.target - eyePosition).normalized()
    return renderToImage(
        playerMesh, width, height, viewMatrix, cameraForward, camera.distance,
        true, true, backgroundColor, useBackFaceCulling = false, lightDirection = lightDirection
    )
}

/**
 * 渲染旋转动画的函数 (API无变化)
 */
suspend fun renderRotate(
    skin: Image,
    isSlim: Boolean,
    width: Int,
    height: Int,
    backgroundColor: Int,
    camera: OrbitCamera,
    frameCount: Int,
    frameDuration: Int,
    pose: Map<BodyPart, List<Transformation>> = emptyMap(),
    use3DOverlay: Boolean = false
): ByteArray {
    val unitAngel = 360f / frameCount
    val skinBitmap = Bitmap.makeFromImage(skin)
    val playerMesh = createMinecraftPlayer(skinBitmap, isSlim, pose, use3DOverlay)
    return coroutineScope {
        withContext(Dispatchers.Default) {
            (0 until frameCount).map { i ->
                async {
                    val angle = i * unitAngel
                    val rotatedCamera = camera.copy(azimuthDegrees = camera.azimuthDegrees + angle)
                    val (viewMatrix, eyePosition) = createViewMatrix(rotatedCamera)
                    val cameraForward = (rotatedCamera.target - eyePosition).normalized()
                    renderToImage(
                        playerMesh, width, height, viewMatrix, cameraForward, rotatedCamera.distance,
                        true, true, backgroundColor, useBackFaceCulling = false
                    ).let { Frame(frameDuration, it) }
                }
            }.awaitAll()
        }.encodeToBytes()
    }
}