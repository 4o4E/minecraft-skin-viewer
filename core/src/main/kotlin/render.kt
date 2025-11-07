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

/**
 * 封装所有可能的变换操作
 */
sealed class Transformation {
    /**
     * 旋转操作
     * @param x 绕X轴旋转的角度
     * @param y 绕Y轴旋转的角度
     * @param z 绕Z轴旋转的角度
     */
    data class Rotate(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) : Transformation()

    /**
     * 缩放操作
     * @param x X轴的缩放比例
     * @param y Y轴的缩放比例
     * @param z Z轴的缩放比例
     */
    data class Scale(val x: Float = 1f, val y: Float = 1f, val z: Float = 1f) : Transformation()

    /**
     * 平移操作
     * @param x X轴的平移量
     * @param y Y轴的平移量
     * @param z Z轴的平移量
     */
    data class Translate(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) : Transformation()
}

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
    fun withScale(headScale: Float = 1f, laScale: Float = 1f, raScale: Float = 1f, llScale: Float = 1f, rlScale: Float = 1f, ) = mapOf(
        BodyPart.HEAD to listOf(
            Transformation.Rotate(y = -30f),
            Transformation.Scale(headScale, headScale, headScale),
            Transformation.Translate(y = BodyPart.HEAD.getDims(false).y.let { it * (headScale - 1) / 2 }),
        ),
        BodyPart.RIGHT_ARM to listOf(
            Transformation.Rotate(x = -30f),
            Transformation.Scale(raScale, raScale, raScale),
            Transformation.Translate(x = -BodyPart.LEFT_ARM.getDims(false).x.let { it * (raScale - 1) / 2 }),
        ),
        BodyPart.LEFT_ARM to listOf(
            Transformation.Rotate(x = -30f),
            Transformation.Scale(laScale, laScale, laScale),
            Transformation.Translate(x = BodyPart.LEFT_ARM.getDims(false).x.let { it * (laScale - 1) / 2 }),
        ),
        BodyPart.RIGHT_LEG to listOf(
            Transformation.Rotate(x = -80f, z = -20f),
            Transformation.Scale(rlScale, rlScale, rlScale),
            Transformation.Translate(y = -BodyPart.LEFT_LEG.getDims(false).y.let { it * (rlScale - 1) / 2 }),
        ),
        BodyPart.LEFT_LEG to listOf(
            Transformation.Rotate(x = -80f, z = 20f),
            Transformation.Scale(llScale, llScale, llScale),
            Transformation.Translate(y = -BodyPart.LEFT_LEG.getDims(false).y.let { it * (llScale - 1) / 2 }),
        ),
    )
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

/**
 * 按照 Z -> Y -> X 的顺序旋转向量
 */
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

/**
 * 将Minecraft模型所有复杂的、硬编码的数据（尺寸、位置、UV坐标）封装起来
 *
 * 实现了数据与逻辑的完全分离，极大地提高了代码的可读性、可维护性和可扩展性
 */
enum class BodyPart {
    HEAD {
        override fun getDims(isSlim: Boolean) = Vec3(8f, 8f, 8f)
        override fun getPos(isSlim: Boolean) = Vec3(0f, 20f, 0f)
        override fun getBaseUVs(isSlim: Boolean) = mapOf(
            FaceDirection.RIGHT to Rect.makeXYWH(16f, 8f, 8f, 8f),
            FaceDirection.LEFT to Rect.makeXYWH(0f, 8f, 8f, 8f),
            FaceDirection.TOP to Rect.makeXYWH(8f, 0f, 8f, 8f),
            FaceDirection.BOTTOM to Rect.makeXYWH(16f, 0f, 8f, 8f),
            FaceDirection.FRONT to Rect.makeXYWH(8f, 8f, 8f, 8f),
            FaceDirection.BACK to Rect.makeXYWH(24f, 8f, 8f, 8f)
        )

        override fun getOverlayUVs(isSlim: Boolean) = mapOf(
            FaceDirection.RIGHT to Rect.makeXYWH(48f, 8f, 8f, 8f),
            FaceDirection.LEFT to Rect.makeXYWH(32f, 8f, 8f, 8f),
            FaceDirection.TOP to Rect.makeXYWH(40f, 0f, 8f, 8f),
            FaceDirection.BOTTOM to Rect.makeXYWH(48f, 0f, 8f, 8f),
            FaceDirection.FRONT to Rect.makeXYWH(40f, 8f, 8f, 8f),
            FaceDirection.BACK to Rect.makeXYWH(56f, 8f, 8f, 8f)
        )

        override fun getPivot(isSlim: Boolean) = Vec3(0f, 0f, 0f)
    },
    BODY {
        override fun getDims(isSlim: Boolean) = Vec3(8f, 12f, 4f)
        override fun getPos(isSlim: Boolean) = Vec3(0f, 10f, 0f)
        override fun getBaseUVs(isSlim: Boolean) = mapOf(
            FaceDirection.RIGHT to Rect.makeXYWH(16f, 20f, 4f, 12f),
            FaceDirection.LEFT to Rect.makeXYWH(28f, 20f, 4f, 12f),
            FaceDirection.TOP to Rect.makeXYWH(20f, 16f, 8f, 4f),
            FaceDirection.BOTTOM to Rect.makeXYWH(28f, 16f, 8f, 4f),
            FaceDirection.FRONT to Rect.makeXYWH(20f, 20f, 8f, 12f),
            FaceDirection.BACK to Rect.makeXYWH(32f, 20f, 8f, 12f)
        )

        override fun getOverlayUVs(isSlim: Boolean) = mapOf(
            FaceDirection.RIGHT to Rect.makeXYWH(16f, 36f, 4f, 12f),
            FaceDirection.LEFT to Rect.makeXYWH(28f, 36f, 4f, 12f),
            FaceDirection.TOP to Rect.makeXYWH(20f, 32f, 8f, 4f),
            FaceDirection.BOTTOM to Rect.makeXYWH(28f, 32f, 8f, 4f),
            FaceDirection.FRONT to Rect.makeXYWH(20f, 36f, 8f, 12f),
            FaceDirection.BACK to Rect.makeXYWH(32f, 36f, 8f, 12f)
        )

        override fun getPivot(isSlim: Boolean) = Vec3(0f, 0f, 0f)
    },
    RIGHT_ARM {
        override fun getDims(isSlim: Boolean) = if (isSlim) Vec3(3f, 12f, 4f) else Vec3(4f, 12f, 4f)
        override fun getPos(isSlim: Boolean) = if (isSlim) Vec3(-5.5f, 10f, 0f) else Vec3(-6f, 10f, 0f)
        override fun getBaseUVs(isSlim: Boolean) = if (isSlim) mapOf(
            FaceDirection.RIGHT to Rect.makeXYWH(40f, 20f, 4f, 12f),
            FaceDirection.LEFT to Rect.makeXYWH(47f, 20f, 4f, 12f),
            FaceDirection.TOP to Rect.makeXYWH(44f, 16f, 3f, 4f),
            FaceDirection.BOTTOM to Rect.makeXYWH(47f, 16f, 3f, 4f),
            FaceDirection.FRONT to Rect.makeXYWH(44f, 20f, 3f, 12f),
            FaceDirection.BACK to Rect.makeXYWH(51f, 20f, 3f, 12f)
        ) else mapOf(
            FaceDirection.RIGHT to Rect.makeXYWH(40f, 20f, 4f, 12f),
            FaceDirection.LEFT to Rect.makeXYWH(48f, 20f, 4f, 12f),
            FaceDirection.TOP to Rect.makeXYWH(44f, 16f, 4f, 4f),
            FaceDirection.BOTTOM to Rect.makeXYWH(48f, 16f, 4f, 4f),
            FaceDirection.FRONT to Rect.makeXYWH(44f, 20f, 4f, 12f),
            FaceDirection.BACK to Rect.makeXYWH(52f, 20f, 4f, 12f)
        )

        override fun getOverlayUVs(isSlim: Boolean) = if (isSlim) mapOf(
            FaceDirection.RIGHT to Rect.makeXYWH(47f, 36f, 4f, 12f),
            FaceDirection.LEFT to Rect.makeXYWH(40f, 36f, 4f, 12f),
            FaceDirection.TOP to Rect.makeXYWH(44f, 32f, 3f, 4f),
            FaceDirection.BOTTOM to Rect.makeXYWH(47f, 32f, 3f, 4f),
            FaceDirection.FRONT to Rect.makeXYWH(44f, 36f, 3f, 12f),
            FaceDirection.BACK to Rect.makeXYWH(51f, 36f, 3f, 12f)
        ) else mapOf(
            FaceDirection.RIGHT to Rect.makeXYWH(48f, 36f, 4f, 12f),
            FaceDirection.LEFT to Rect.makeXYWH(40f, 36f, 4f, 12f),
            FaceDirection.TOP to Rect.makeXYWH(44f, 32f, 4f, 4f),
            FaceDirection.BOTTOM to Rect.makeXYWH(48f, 32f, 4f, 4f),
            FaceDirection.FRONT to Rect.makeXYWH(44f, 36f, 4f, 12f),
            FaceDirection.BACK to Rect.makeXYWH(52f, 36f, 4f, 12f)
        )

        override fun getPivot(isSlim: Boolean) = Vec3(0f, 6f, 0f)
    },
    LEFT_ARM {
        override fun getDims(isSlim: Boolean) = if (isSlim) Vec3(3f, 12f, 4f) else Vec3(4f, 12f, 4f)
        override fun getPos(isSlim: Boolean) = if (isSlim) Vec3(5.5f, 10f, 0f) else Vec3(6f, 10f, 0f)
        override fun getBaseUVs(isSlim: Boolean) = if (isSlim) mapOf(
            FaceDirection.RIGHT to Rect.makeXYWH(39f, 52f, 4f, 12f),
            FaceDirection.LEFT to Rect.makeXYWH(32f, 52f, 4f, 12f),
            FaceDirection.TOP to Rect.makeXYWH(36f, 48f, 3f, 4f),
            FaceDirection.BOTTOM to Rect.makeXYWH(39f, 48f, 3f, 4f),
            FaceDirection.FRONT to Rect.makeXYWH(36f, 52f, 3f, 12f),
            FaceDirection.BACK to Rect.makeXYWH(43f, 52f, 3f, 12f)
        ) else mapOf(
            FaceDirection.RIGHT to Rect.makeXYWH(40f, 52f, 4f, 12f),
            FaceDirection.LEFT to Rect.makeXYWH(32f, 52f, 4f, 12f),
            FaceDirection.TOP to Rect.makeXYWH(36f, 48f, 4f, 4f),
            FaceDirection.BOTTOM to Rect.makeXYWH(40f, 48f, 4f, 4f),
            FaceDirection.FRONT to Rect.makeXYWH(36f, 52f, 4f, 12f),
            FaceDirection.BACK to Rect.makeXYWH(44f, 52f, 4f, 12f)
        )

        override fun getOverlayUVs(isSlim: Boolean) = if (isSlim) mapOf(
            FaceDirection.RIGHT to Rect.makeXYWH(55f, 52f, 4f, 12f),
            FaceDirection.LEFT to Rect.makeXYWH(48f, 52f, 4f, 12f),
            FaceDirection.TOP to Rect.makeXYWH(52f, 48f, 3f, 4f),
            FaceDirection.BOTTOM to Rect.makeXYWH(55f, 48f, 3f, 4f),
            FaceDirection.FRONT to Rect.makeXYWH(52f, 52f, 3f, 12f),
            FaceDirection.BACK to Rect.makeXYWH(59f, 52f, 3f, 12f)
        ) else mapOf(
            FaceDirection.RIGHT to Rect.makeXYWH(56f, 52f, 4f, 12f),
            FaceDirection.LEFT to Rect.makeXYWH(48f, 52f, 4f, 12f),
            FaceDirection.TOP to Rect.makeXYWH(52f, 48f, 4f, 4f),
            FaceDirection.BOTTOM to Rect.makeXYWH(56f, 48f, 4f, 4f),
            FaceDirection.FRONT to Rect.makeXYWH(52f, 52f, 4f, 12f),
            FaceDirection.BACK to Rect.makeXYWH(60f, 52f, 4f, 12f)
        )

        override fun getPivot(isSlim: Boolean) = Vec3(0f, 6f, 0f)
    },
    RIGHT_LEG {
        override fun getDims(isSlim: Boolean) = Vec3(4f, 12f, 4f)
        override fun getPos(isSlim: Boolean) = Vec3(-2f, -2f, 0f)
        override fun getBaseUVs(isSlim: Boolean) = mapOf(
            FaceDirection.RIGHT to Rect.makeXYWH(0f, 20f, 4f, 12f),
            FaceDirection.LEFT to Rect.makeXYWH(8f, 20f, 4f, 12f),
            FaceDirection.TOP to Rect.makeXYWH(4f, 16f, 4f, 4f),
            FaceDirection.BOTTOM to Rect.makeXYWH(8f, 16f, 4f, 4f),
            FaceDirection.FRONT to Rect.makeXYWH(4f, 20f, 4f, 12f),
            FaceDirection.BACK to Rect.makeXYWH(12f, 20f, 4f, 12f)
        )

        override fun getOverlayUVs(isSlim: Boolean) = mapOf(
            FaceDirection.RIGHT to Rect.makeXYWH(8f, 36f, 4f, 12f),
            FaceDirection.LEFT to Rect.makeXYWH(0f, 36f, 4f, 12f),
            FaceDirection.TOP to Rect.makeXYWH(4f, 32f, 4f, 4f),
            FaceDirection.BOTTOM to Rect.makeXYWH(8f, 32f, 4f, 4f),
            FaceDirection.FRONT to Rect.makeXYWH(4f, 36f, 4f, 12f),
            FaceDirection.BACK to Rect.makeXYWH(12f, 36f, 4f, 12f)
        )

        override fun getPivot(isSlim: Boolean) = Vec3(0f, 6f, 0f)
    },
    LEFT_LEG {
        override fun getDims(isSlim: Boolean) = Vec3(4f, 12f, 4f)
        override fun getPos(isSlim: Boolean) = Vec3(2f, -2f, 0f)
        override fun getBaseUVs(isSlim: Boolean) = mapOf(
            FaceDirection.RIGHT to Rect.makeXYWH(24f, 52f, 4f, 12f),
            FaceDirection.LEFT to Rect.makeXYWH(16f, 52f, 4f, 12f),
            FaceDirection.TOP to Rect.makeXYWH(20f, 48f, 4f, 4f),
            FaceDirection.BOTTOM to Rect.makeXYWH(24f, 48f, 4f, 4f),
            FaceDirection.FRONT to Rect.makeXYWH(20f, 52f, 4f, 12f),
            FaceDirection.BACK to Rect.makeXYWH(28f, 52f, 4f, 12f)
        )

        override fun getOverlayUVs(isSlim: Boolean) = mapOf(
            FaceDirection.RIGHT to Rect.makeXYWH(8f, 52f, 4f, 12f),
            FaceDirection.LEFT to Rect.makeXYWH(0f, 52f, 4f, 12f),
            FaceDirection.TOP to Rect.makeXYWH(4f, 48f, 4f, 4f),
            FaceDirection.BOTTOM to Rect.makeXYWH(8f, 48f, 4f, 4f),
            FaceDirection.FRONT to Rect.makeXYWH(4f, 52f, 4f, 12f),
            FaceDirection.BACK to Rect.makeXYWH(12f, 52f, 4f, 12f)
        )

        override fun getPivot(isSlim: Boolean) = Vec3(0f, 6f, 0f)
    };

    /** 每个身体部件都必须提供自己的尺寸、位置和UV数据 */
    abstract fun getDims(isSlim: Boolean): Vec3
    /** 获取身体部件相对于模型中心点的位置 */
    abstract fun getPos(isSlim: Boolean): Vec3
    /** 获取身体部件的基础层UV坐标 */
    abstract fun getBaseUVs(isSlim: Boolean): Map<FaceDirection, Rect>
    /** 获取身体部件的外层UV坐标，若无外层则返回null */
    abstract fun getOverlayUVs(isSlim: Boolean): Map<FaceDirection, Rect>?

    /**
     * 获取身体部件的旋转轴心点（局部坐标系，即相对于部件几何中心）
     */
    abstract fun getPivot(isSlim: Boolean): Vec3
}

/**
 * [新增] 用于标识像素在UV区域中的位置（内部、边缘、角落）
 */
private enum class PixelPosition {
    INNER,
    TOP, BOTTOM, LEFT, RIGHT,
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

/**
 * 根据像素坐标和UV区域尺寸，判断像素的位置类型
 */
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

/**
 * 创建一个立体的、基于像素的外层皮肤网格。
 * 此版本新增了边缘检测逻辑：如果一个边缘像素所连接的相邻面上的对应像素是透明的，
 * 那么这个边缘将不会被内缩，从而形成一个平整的侧面，而不是斜角。
 *
 * @param skin 皮肤位图，用于检查像素透明度。
 * @param dims 内部核心部件的尺寸。
 * @param overlayDepth 立体外层的厚度（挤出距离）。
 * @param faceUVs 定义了外层每个面在皮肤贴图上的UV区域。
 * @param textureWidth 皮肤贴图的总宽度。
 * @param textureHeight 皮肤贴图的总高度。
 * @return 返回一个由许多小方块（体素）组成的网格（Mesh）。
 */
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

    // 定义身体部件各个面在3D空间中的邻接关系
    // Key: (当前面, 当前边) -> Value: (邻接面, 邻接边)
    val faceAdjacency = mapOf(
        (FaceDirection.TOP to PixelPosition.TOP) to (FaceDirection.BACK to PixelPosition.TOP),
        (FaceDirection.TOP to PixelPosition.BOTTOM) to (FaceDirection.FRONT to PixelPosition.TOP),
        (FaceDirection.TOP to PixelPosition.LEFT) to (FaceDirection.LEFT to PixelPosition.TOP),
        (FaceDirection.TOP to PixelPosition.RIGHT) to (FaceDirection.RIGHT to PixelPosition.TOP),

        (FaceDirection.BOTTOM to PixelPosition.TOP) to (FaceDirection.FRONT to PixelPosition.BOTTOM),
        (FaceDirection.BOTTOM to PixelPosition.BOTTOM) to (FaceDirection.BACK to PixelPosition.BOTTOM),
        (FaceDirection.BOTTOM to PixelPosition.LEFT) to (FaceDirection.LEFT to PixelPosition.BOTTOM),
        (FaceDirection.BOTTOM to PixelPosition.RIGHT) to (FaceDirection.RIGHT to PixelPosition.BOTTOM),

        (FaceDirection.FRONT to PixelPosition.TOP) to (FaceDirection.TOP to PixelPosition.BOTTOM),
        (FaceDirection.FRONT to PixelPosition.BOTTOM) to (FaceDirection.BOTTOM to PixelPosition.TOP),
        (FaceDirection.FRONT to PixelPosition.LEFT) to (FaceDirection.LEFT to PixelPosition.RIGHT),
        (FaceDirection.FRONT to PixelPosition.RIGHT) to (FaceDirection.RIGHT to PixelPosition.LEFT),

        (FaceDirection.BACK to PixelPosition.TOP) to (FaceDirection.TOP to PixelPosition.TOP),
        (FaceDirection.BACK to PixelPosition.BOTTOM) to (FaceDirection.BOTTOM to PixelPosition.BOTTOM),
        (FaceDirection.BACK to PixelPosition.LEFT) to (FaceDirection.RIGHT to PixelPosition.RIGHT),
        (FaceDirection.BACK to PixelPosition.RIGHT) to (FaceDirection.LEFT to PixelPosition.LEFT),

        (FaceDirection.LEFT to PixelPosition.TOP) to (FaceDirection.TOP to PixelPosition.LEFT),
        (FaceDirection.LEFT to PixelPosition.BOTTOM) to (FaceDirection.BOTTOM to PixelPosition.LEFT),
        (FaceDirection.LEFT to PixelPosition.LEFT) to (FaceDirection.BACK to PixelPosition.RIGHT),
        (FaceDirection.LEFT to PixelPosition.RIGHT) to (FaceDirection.FRONT to PixelPosition.LEFT),

        (FaceDirection.RIGHT to PixelPosition.TOP) to (FaceDirection.TOP to PixelPosition.RIGHT),
        (FaceDirection.RIGHT to PixelPosition.BOTTOM) to (FaceDirection.BOTTOM to PixelPosition.RIGHT),
        (FaceDirection.RIGHT to PixelPosition.LEFT) to (FaceDirection.FRONT to PixelPosition.RIGHT),
        (FaceDirection.RIGHT to PixelPosition.RIGHT) to (FaceDirection.BACK to PixelPosition.LEFT)
    )

    for ((direction, uvRect) in faceUVs) {
        val uvW = uvRect.width.toInt()
        val uvH = uvRect.height.toInt()

        for (px in 0 until uvW) {
            for (py in 0 until uvH) {
                val color = skin.getColor(uvRect.left.toInt() + px, uvRect.top.toInt() + py)
                if (Color.getA(color) <= 0) continue

                val pixelUV = Vec2(
                    (uvRect.left + px + 0.5f) / textureWidth,
                    (uvRect.top + py + 0.5f) / textureHeight
                )

                val (voxelW, voxelH) = when (direction) {
                    FaceDirection.RIGHT, FaceDirection.LEFT -> Pair(effectiveDims.z / uvW, effectiveDims.y / uvH)
                    FaceDirection.TOP, FaceDirection.BOTTOM -> Pair(effectiveDims.x / uvW, effectiveDims.z / uvH)
                    else -> Pair(effectiveDims.x / uvW, effectiveDims.y / uvH)
                }
                // voxelW 和 voxelH 一致 都是单个像素尺寸
                val voxelD = voxelW
                // 外层方块向内缩的距离
                val retractionDepth = voxelD - overlayDepth

                val w = voxelW / 2; val h = voxelH / 2; val d = voxelD / 2

                val v = mutableListOf(
                    Vec3(-w, -h, -d), Vec3(w, -h, -d), Vec3(w, h, -d), Vec3(-w, h, -d),
                    Vec3(-w, -h, d), Vec3(w, -h, d), Vec3(w, h, d), Vec3(-w, h, d)
                )

                val pixelPosition = getPixelPosition(px, py, uvW, uvH)
                if (pixelPosition != PixelPosition.INNER) {
                    // 辅助函数，检查指定边缘的相邻像素是否透明
                    fun isAdjacentTransparent(edge: PixelPosition): Boolean {
                        val (adjFaceDir, _) = faceAdjacency[direction to edge] ?: return true
                        val adjUvRect = faceUVs[adjFaceDir] ?: return true
                        val adjUvW = adjUvRect.width.toInt()
                        val adjUvH = adjUvRect.height.toInt()

                        // [FIXED] 重新、完整地推导了所有24种边缘连接的UV坐标映射关系
                        val (adj_local_px, adj_local_py) = when (direction to edge) {
                            // --- Connections FROM FRONT ---
                            FaceDirection.FRONT to PixelPosition.TOP -> px to adjUvH - 1 // To TOP's BOTTOM edge
                            FaceDirection.FRONT to PixelPosition.BOTTOM -> px to 0 // To BOTTOM's TOP edge
                            FaceDirection.FRONT to PixelPosition.LEFT -> adjUvW - 1 to py // To LEFT's RIGHT edge
                            FaceDirection.FRONT to PixelPosition.RIGHT -> 0 to py // To RIGHT's LEFT edge

                            // --- Connections FROM BACK ---
                            FaceDirection.BACK to PixelPosition.TOP -> adjUvW - 1 - px to 0 // To TOP's TOP edge (Reversed X)
                            FaceDirection.BACK to PixelPosition.BOTTOM -> adjUvW - 1 - px to adjUvH - 1 // To BOTTOM's BOTTOM edge (Reversed X)
                            FaceDirection.BACK to PixelPosition.LEFT -> adjUvW - 1 to py // To RIGHT's RIGHT edge
                            FaceDirection.BACK to PixelPosition.RIGHT -> 0 to py // To LEFT's LEFT edge

                            // --- Connections FROM TOP ---
                            FaceDirection.TOP to PixelPosition.TOP -> adjUvW - 1 - px to 0 // To BACK's TOP edge (Reversed X)
                            FaceDirection.TOP to PixelPosition.BOTTOM -> px to 0 // To FRONT's TOP edge
                            FaceDirection.TOP to PixelPosition.LEFT -> py to 0 // To LEFT's TOP edge (Z -> Z)
                            FaceDirection.TOP to PixelPosition.RIGHT -> adjUvW - 1 - py to 0 // To RIGHT's TOP edge (Z -> -Z, Reversed)

                            // --- Connections FROM BOTTOM ---
                            FaceDirection.BOTTOM to PixelPosition.TOP -> px to adjUvH - 1 // To FRONT's BOTTOM edge
                            FaceDirection.BOTTOM to PixelPosition.BOTTOM -> adjUvW - 1 - px to adjUvH - 1 // To BACK's BOTTOM edge (Reversed X)
                            FaceDirection.BOTTOM to PixelPosition.LEFT -> adjUvW - 1 - py to adjUvH - 1 // To LEFT's BOTTOM edge (-Z -> Z, Reversed)
                            FaceDirection.BOTTOM to PixelPosition.RIGHT -> py to adjUvH - 1 // To RIGHT's BOTTOM edge (-Z -> -Z)

                            // --- Connections FROM LEFT ---
                            FaceDirection.LEFT to PixelPosition.TOP -> 0 to px // To TOP's LEFT edge (Z -> Z)
                            FaceDirection.LEFT to PixelPosition.BOTTOM -> 0 to adjUvH - 1 - px // To BOTTOM's LEFT edge (Z -> -Z, Reversed)
                            FaceDirection.LEFT to PixelPosition.LEFT -> 0 to py // To BACK's RIGHT edge
                            FaceDirection.LEFT to PixelPosition.RIGHT -> 0 to py // To FRONT's LEFT edge

                            // --- Connections FROM RIGHT ---
                            FaceDirection.RIGHT to PixelPosition.TOP -> adjUvW - 1 to adjUvH - 1 - px // To TOP's RIGHT edge (-Z -> Z, Reversed)
                            FaceDirection.RIGHT to PixelPosition.BOTTOM -> adjUvW - 1 to px // To BOTTOM's RIGHT edge (-Z -> -Z)
                            FaceDirection.RIGHT to PixelPosition.LEFT -> 0 to py // To FRONT's RIGHT edge
                            FaceDirection.RIGHT to PixelPosition.RIGHT -> adjUvW - 1 to py // To BACK's LEFT edge

                            else -> return true // Should not happen
                        }

                        val final_adj_px = adjUvRect.left.toInt() + adj_local_px
                        val final_adj_py = adjUvRect.top.toInt() + adj_local_py

                        if (final_adj_px < 0 || final_adj_px >= skin.width || final_adj_py < 0 || final_adj_py >= skin.height) {
                            return true // Out of bounds is considered transparent
                        }
                        return Color.getA(skin.getColor(final_adj_px, final_adj_py)) == 0
                    }

                    // 根据邻接像素的透明度决定是否收缩边缘
                    val shrinkTop = pixelPosition in listOf(PixelPosition.TOP, PixelPosition.TOP_LEFT, PixelPosition.TOP_RIGHT) && !isAdjacentTransparent(PixelPosition.TOP)
                    val shrinkBottom = pixelPosition in listOf(PixelPosition.BOTTOM, PixelPosition.BOTTOM_LEFT, PixelPosition.BOTTOM_RIGHT) && !isAdjacentTransparent(PixelPosition.BOTTOM)
                    val shrinkLeft = pixelPosition in listOf(PixelPosition.LEFT, PixelPosition.TOP_LEFT, PixelPosition.BOTTOM_LEFT) && !isAdjacentTransparent(PixelPosition.LEFT)
                    val shrinkRight = pixelPosition in listOf(PixelPosition.RIGHT, PixelPosition.TOP_RIGHT, PixelPosition.BOTTOM_RIGHT) && !isAdjacentTransparent(PixelPosition.RIGHT)

                    if (shrinkTop) {
                        v[2] = v[2].copy(y = v[2].y - voxelD)
                        v[3] = v[3].copy(y = v[3].y - voxelD)
                    }
                    if (shrinkBottom) {
                        v[0] = v[0].copy(y = v[0].y + voxelD)
                        v[1] = v[1].copy(y = v[1].y + voxelD)
                    }
                    if (shrinkLeft) {
                        v[0] = v[0].copy(x = v[0].x + voxelD)
                        v[3] = v[3].copy(x = v[3].x + voxelD)
                    }
                    if (shrinkRight) {
                        v[1] = v[1].copy(x = v[1].x - voxelD)
                        v[2] = v[2].copy(x = v[2].x - voxelD)
                    }
                }

                // 旋转和定位体素的逻辑保持不变
                val finalVoxelVertices = v.map { p ->
                    val rotatedP = when (direction) {
                        FaceDirection.FRONT  -> p
                        FaceDirection.BACK   -> Vec3(-p.x, p.y, -p.z)
                        FaceDirection.RIGHT  -> Vec3(p.z, p.y, -p.x)
                        FaceDirection.LEFT   -> Vec3(-p.z, p.y, p.x)
                        FaceDirection.TOP    -> Vec3(p.x, p.z, -p.y)
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

                val voxelFaces = listOf(
                    Face(listOf(baseIndex + 4, baseIndex + 7, baseIndex + 6, baseIndex + 5), Color.WHITE), // Outer
                    Face(listOf(baseIndex + 0, baseIndex + 3, baseIndex + 2, baseIndex + 1), Color.WHITE), // Inner
                    Face(listOf(baseIndex + 5, baseIndex + 6, baseIndex + 2, baseIndex + 1), Color.WHITE), // Right
                    Face(listOf(baseIndex + 4, baseIndex + 0, baseIndex + 3, baseIndex + 7), Color.WHITE), // Left
                    Face(listOf(baseIndex + 7, baseIndex + 3, baseIndex + 2, baseIndex + 6), Color.WHITE), // Top
                    Face(listOf(baseIndex + 4, baseIndex + 5, baseIndex + 1, baseIndex + 0), Color.WHITE)  // Bottom
                )
                faces.addAll(voxelFaces)
            }
        }
    }
    return Mesh(vertices, faces)
}

/**
 * 根据皮肤贴图和模型类型（标准/Slim）创建完整的Minecraft玩家模型。
 * @param pose 一个映射，定义了特定身体部件的一系列有序变换
 * @param use3DOverlay [新增] 是否启用立体外层皮肤效果
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

    for (part in BodyPart.entries) {
        val dims = part.getDims(isSlim)
        val pos = part.getPos(isSlim)
        val baseUVs = part.getBaseUVs(isSlim)
        val overlayUVs = part.getOverlayUVs(isSlim)
        val pivot = part.getPivot(isSlim)
        val transformations = pose[part]

        // 定义统一的变换函数
        val transform: (Vec3) -> Vec3 = { vertexPos ->
            if (transformations.isNullOrEmpty()) {
                vertexPos // 如果没有变换，直接返回原坐标
            } else {
                // 使用 fold 依次应用变换链中的每一个操作
                transformations.fold(vertexPos) { currentPos, transformation ->
                    when (transformation) {
                        // 旋转和缩放是围绕轴心点进行的
                        is Transformation.Rotate -> (currentPos - pivot).rotate(transformation) + pivot
                        is Transformation.Scale -> (currentPos - pivot).scale(transformation) + pivot
                        // 平移是绝对的
                        is Transformation.Translate -> currentPos.translate(transformation)
                    }
                }
            }
        }

        // 创建并添加基础层
        val baseCuboid = createUVCuboid(dims, baseUVs, texW, texH)
        componentMeshes.add(Mesh(baseCuboid.vertices.map {
            Vertex(transform(it.position) + pos, it.uv)
        }, baseCuboid.faces))

        // 创建并添加外层
        overlayUVs?.let { uvs ->
            val overlayMesh = if (use3DOverlay) {
                val overlayDepth = if (part == BodyPart.HEAD) .5f else 0.25f
                create3DOverlay(skin, dims, overlayDepth, uvs, texW, texH)
            } else {
                // 使用旧的平面外层方案
                val overlayAmount = 0.5f
                val headOverlayAmount = 1f
                val overlaySize = if (part == BodyPart.HEAD) headOverlayAmount else overlayAmount
                createUVCuboid(dims + Vec3(overlaySize, overlaySize, overlaySize), uvs, texW, texH)
            }
            componentMeshes.add(Mesh(overlayMesh.vertices.map {
                Vertex(transform(it.position) + pos, it.uv)
            }, overlayMesh.faces))
        }
    }
    return combineMeshes(componentMeshes, skin)
}

/**
 * 渲染Minecraft皮肤为图像的函数
 *
 * @param skin 皮肤图片
 * @param isSlim 是否为Slim模型
 * @param backgroundColor 背景颜色
 * @param camera 相机参数
 * @param pose 一个映射，定义了特定身体部件的一系列有序变换
 * @param use3DOverlay [新增] 是否启用立体外层皮肤效果
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
 * 渲染旋转动画的函数
 * @param skin 皮肤图片
 * @param isSlim 是否为Slim模型
 * @param width 画布宽度
 * @param height 画布高度
 * @param backgroundColor 背景颜色
 * @param camera 相机参数
 * @param frameCount 帧数
 * @param pose 一个映射，定义了特定身体部件的一系列有序变换，模型将以这个姿势进行旋转
 * @param use3DOverlay [新增] 是否启用立体外层皮肤效果
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