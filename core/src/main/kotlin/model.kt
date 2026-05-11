package top.e404.skin.core

import org.jetbrains.skia.Rect
import top.e404.tavolo.draw.render3d.FaceDirection
import top.e404.tavolo.draw.render3d.Vec3

/**
 * 封装 Minecraft 皮肤模型的一个立方体部件。
 *
 * @param u 该部件在皮肤贴图上的起始 U 坐标（左）。
 * @param v 该部件在皮肤贴图上的起始 V 坐标（上）。
 * @param dims 部件的尺寸 (宽度, 高度, 深度)，用于计算UV。
 * @param pos 部件在模型坐标系中的位置。
 * @param pivot 部件进行旋转和缩放时的轴心点（相对于部件几何中心）。
 * @param mirrored 如果为 true，则交换左右两个面的UV贴图，以适应Minecraft左侧肢体的布局。
 */
class SkinCube(
    u: Float, v: Float,
    dims: Vec3,
    val pos: Vec3,
    val pivot: Vec3 = Vec3(0f, 0f, 0f),
    mirrored: Boolean = false
) {
    val uvs: Map<FaceDirection, Rect>

    init {
        val (w, h, d) = dims
        val standardRight = Rect.makeXYWH(u, v + d, d, h)
        val standardLeft = Rect.makeXYWH(u + d + w, v + d, d, h)

        uvs = mapOf(
            FaceDirection.RIGHT to if (mirrored) standardLeft else standardRight,
            FaceDirection.LEFT to if (mirrored) standardRight else standardLeft,
            FaceDirection.TOP to Rect.makeXYWH(u + d, v, w, d),
            FaceDirection.BOTTOM to Rect.makeXYWH(u + d + w, v, w, d),
            FaceDirection.FRONT to Rect.makeXYWH(u + d, v + d, w, h),
            FaceDirection.BACK to Rect.makeXYWH(u + d + w + d, v + d, w, h)
        )
    }
}

/**
 * 身体部位的标识符，现在包含尺寸信息。
 *
 * @param classicDims 经典模型（Steve）的尺寸。
 * @param slimDims 纤细模型（Alex）的尺寸，如果与经典模型相同则为 null。
 */
enum class BodyPart(
    val classicDims: Vec3,
    val slimDims: Vec3? = null
) {
    HEAD(Vec3(8f, 8f, 8f)),
    BODY(Vec3(8f, 12f, 4f)),
    RIGHT_ARM(classicDims = Vec3(4f, 12f, 4f), slimDims = Vec3(3f, 12f, 4f)),
    LEFT_ARM(classicDims = Vec3(4f, 12f, 4f), slimDims = Vec3(3f, 12f, 4f)),
    RIGHT_LEG(Vec3(4f, 12f, 4f)),
    LEFT_LEG(Vec3(4f, 12f, 4f));

    /**
     * 根据模型类型获取身体部位的尺寸。
     * @param isSlim 如果为 true，返回纤细尺寸；否则返回经典尺寸。
     */
    fun getDims(isSlim: Boolean): Vec3 {
        return if (isSlim) slimDims ?: classicDims else classicDims
    }
}

/**
 * 定义和存储一个完整的 Minecraft 玩家模型
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
                BodyPart.HEAD to SkinCube(0f, 0f, BodyPart.HEAD.getDims(true), Vec3(0f, 20f, 0f), mirrored = true),
                BodyPart.BODY to SkinCube(16f, 16f, BodyPart.BODY.getDims(true), Vec3(0f, 10f, 0f)),
                BodyPart.RIGHT_ARM to SkinCube(40f, 16f, BodyPart.RIGHT_ARM.getDims(true), Vec3(-5.5f, 10f, 0f), Vec3(0f, 6f, 0f)),
                BodyPart.LEFT_ARM to SkinCube(32f, 48f, BodyPart.LEFT_ARM.getDims(true), Vec3(5.5f, 10f, 0f), Vec3(0f, 6f, 0f), mirrored = true),
                BodyPart.RIGHT_LEG to SkinCube(0f, 16f, BodyPart.RIGHT_LEG.getDims(true), Vec3(-2f, -2f, 0f), Vec3(0f, 6f, 0f), mirrored = true),
                BodyPart.LEFT_LEG to SkinCube(16f, 48f, BodyPart.LEFT_LEG.getDims(true), Vec3(2f, -2f, 0f), Vec3(0f, 6f, 0f), mirrored = true)
            )
            overlays = mapOf(
                BodyPart.HEAD to SkinCube(32f, 0f, BodyPart.HEAD.getDims(true), Vec3(0f, 20f, 0f), mirrored = true),
                BodyPart.BODY to SkinCube(16f, 32f, BodyPart.BODY.getDims(true), Vec3(0f, 10f, 0f)),
                BodyPart.RIGHT_ARM to SkinCube(40f, 32f, BodyPart.RIGHT_ARM.getDims(true), Vec3(-5.5f, 10f, 0f), Vec3(0f, 6f, 0f)),
                BodyPart.LEFT_ARM to SkinCube(48f, 48f, BodyPart.LEFT_ARM.getDims(true), Vec3(5.5f, 10f, 0f), Vec3(0f, 6f, 0f), mirrored = true),
                BodyPart.RIGHT_LEG to SkinCube(0f, 32f, BodyPart.RIGHT_LEG.getDims(true), Vec3(-2f, -2f, 0f), Vec3(0f, 6f, 0f), mirrored = true),
                BodyPart.LEFT_LEG to SkinCube(0f, 48f, BodyPart.LEFT_LEG.getDims(true), Vec3(2f, -2f, 0f), Vec3(0f, 6f, 0f), mirrored = true)
            )
        } else {
            // Steve (Classic) 模型定义
            parts = mapOf(
                BodyPart.HEAD to SkinCube(0f, 0f, BodyPart.HEAD.getDims(false), Vec3(0f, 20f, 0f), mirrored = true),
                BodyPart.BODY to SkinCube(16f, 16f, BodyPart.BODY.getDims(false), Vec3(0f, 10f, 0f)),
                BodyPart.RIGHT_ARM to SkinCube(40f, 16f, BodyPart.RIGHT_ARM.getDims(false), Vec3(-6f, 10f, 0f), Vec3(0f, 6f, 0f)),
                BodyPart.LEFT_ARM to SkinCube(32f, 48f, BodyPart.LEFT_ARM.getDims(false), Vec3(6f, 10f, 0f), Vec3(0f, 6f, 0f), mirrored = true),
                BodyPart.RIGHT_LEG to SkinCube(0f, 16f, BodyPart.RIGHT_LEG.getDims(false), Vec3(-2f, -2f, 0f), Vec3(0f, 6f, 0f), mirrored = true),
                BodyPart.LEFT_LEG to SkinCube(16f, 48f, BodyPart.LEFT_LEG.getDims(false), Vec3(2f, -2f, 0f), Vec3(0f, 6f, 0f), mirrored = true)
            )
            overlays = mapOf(
                BodyPart.HEAD to SkinCube(32f, 0f, BodyPart.HEAD.getDims(false), Vec3(0f, 20f, 0f), mirrored = true),
                BodyPart.BODY to SkinCube(16f, 32f, BodyPart.BODY.getDims(false), Vec3(0f, 10f, 0f)),
                BodyPart.RIGHT_ARM to SkinCube(40f, 32f, BodyPart.RIGHT_ARM.getDims(false), Vec3(-6f, 10f, 0f), Vec3(0f, 6f, 0f)),
                BodyPart.LEFT_ARM to SkinCube(48f, 48f, BodyPart.LEFT_ARM.getDims(false), Vec3(6f, 10f, 0f), Vec3(0f, 6f, 0f), mirrored = true),
                BodyPart.RIGHT_LEG to SkinCube(0f, 32f, BodyPart.RIGHT_LEG.getDims(false), Vec3(-2f, -2f, 0f), Vec3(0f, 6f, 0f), mirrored = true),
                BodyPart.LEFT_LEG to SkinCube(0f, 48f, BodyPart.LEFT_LEG.getDims(false), Vec3(2f, -2f, 0f), Vec3(0f, 6f, 0f), mirrored = true)
            )
        }
    }
}
