package top.e404.mcsk.core

/**
 * Minecraft 皮肤模板中的语义面方向。
 */
enum class SkinFace {
    RIGHT, LEFT, TOP, BOTTOM, FRONT, BACK
}

/**
 * 封装 Minecraft 皮肤模型的一个立方体部件。
 *
 * @param u 该部件在皮肤贴图上的起始 U 坐标（左）。
 * @param v 该部件在皮肤贴图上的起始 V 坐标（上）。
 * @param dims 部件的尺寸 (宽度, 高度, 深度)，用于计算UV。
 * @param pos 部件在模型坐标系中的位置。
 * @param pivot 部件进行旋转和缩放时的轴心点（相对于部件几何中心）。
 */
class SkinCube(
    u: Float, v: Float,
    dims: SkinVec3,
    val pos: SkinVec3,
    val pivot: SkinVec3 = SkinVec3(0f, 0f, 0f),
) {
    val uvs: Map<SkinFace, SkinUvRect>

    init {
        val (w, h, d) = dims
        uvs = mapOf(
            SkinFace.RIGHT to SkinUvRect.makeXYWH(u, v + d, d, h),
            SkinFace.LEFT to SkinUvRect.makeXYWH(u + d + w, v + d, d, h),
            SkinFace.TOP to SkinUvRect.makeXYWH(u + d, v, w, d),
            SkinFace.BOTTOM to SkinUvRect.makeXYWH(u + d + w, v, w, d),
            SkinFace.FRONT to SkinUvRect.makeXYWH(u + d, v + d, w, h),
            SkinFace.BACK to SkinUvRect.makeXYWH(u + d + w + d, v + d, w, h)
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
    val classicDims: SkinVec3,
    val slimDims: SkinVec3? = null
) {
    HEAD(SkinVec3(8f, 8f, 8f)),
    BODY(SkinVec3(8f, 12f, 4f)),
    RIGHT_ARM(classicDims = SkinVec3(4f, 12f, 4f), slimDims = SkinVec3(3f, 12f, 4f)),
    LEFT_ARM(classicDims = SkinVec3(4f, 12f, 4f), slimDims = SkinVec3(3f, 12f, 4f)),
    RIGHT_LEG(SkinVec3(4f, 12f, 4f)),
    LEFT_LEG(SkinVec3(4f, 12f, 4f)),
    CAPE(SkinVec3(10f, 16f, 1f));

    /**
     * 根据模型类型获取身体部位的尺寸。
     * @param isSlim 如果为 true，返回纤细尺寸；否则返回经典尺寸。
     */
    fun getDims(isSlim: Boolean): SkinVec3 {
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
    val cape: SkinCube = SkinCube(
        0f,
        0f,
        BodyPart.CAPE.getDims(isSlim),
        SkinVec3(0f, 8f, -2.75f),
        SkinVec3(0f, 8f, 0f)
    )

    init {
        if (isSlim) {
            // Alex (Slim) 模型定义
            parts = mapOf(
                BodyPart.HEAD to SkinCube(0f, 0f, BodyPart.HEAD.getDims(true), SkinVec3(0f, 20f, 0f)),
                BodyPart.BODY to SkinCube(16f, 16f, BodyPart.BODY.getDims(true), SkinVec3(0f, 10f, 0f)),
                BodyPart.RIGHT_ARM to SkinCube(40f, 16f, BodyPart.RIGHT_ARM.getDims(true), SkinVec3(-5.5f, 10f, 0f), SkinVec3(0f, 6f, 0f)),
                BodyPart.LEFT_ARM to SkinCube(32f, 48f, BodyPart.LEFT_ARM.getDims(true), SkinVec3(5.5f, 10f, 0f), SkinVec3(0f, 6f, 0f)),
                BodyPart.RIGHT_LEG to SkinCube(0f, 16f, BodyPart.RIGHT_LEG.getDims(true), SkinVec3(-2f, -2f, 0f), SkinVec3(0f, 6f, 0f)),
                BodyPart.LEFT_LEG to SkinCube(16f, 48f, BodyPart.LEFT_LEG.getDims(true), SkinVec3(2f, -2f, 0f), SkinVec3(0f, 6f, 0f))
            )
            overlays = mapOf(
                BodyPart.HEAD to SkinCube(32f, 0f, BodyPart.HEAD.getDims(true), SkinVec3(0f, 20f, 0f)),
                BodyPart.BODY to SkinCube(16f, 32f, BodyPart.BODY.getDims(true), SkinVec3(0f, 10f, 0f)),
                BodyPart.RIGHT_ARM to SkinCube(40f, 32f, BodyPart.RIGHT_ARM.getDims(true), SkinVec3(-5.5f, 10f, 0f), SkinVec3(0f, 6f, 0f)),
                BodyPart.LEFT_ARM to SkinCube(48f, 48f, BodyPart.LEFT_ARM.getDims(true), SkinVec3(5.5f, 10f, 0f), SkinVec3(0f, 6f, 0f)),
                BodyPart.RIGHT_LEG to SkinCube(0f, 32f, BodyPart.RIGHT_LEG.getDims(true), SkinVec3(-2f, -2f, 0f), SkinVec3(0f, 6f, 0f)),
                BodyPart.LEFT_LEG to SkinCube(0f, 48f, BodyPart.LEFT_LEG.getDims(true), SkinVec3(2f, -2f, 0f), SkinVec3(0f, 6f, 0f))
            )
        } else {
            // Steve (Classic) 模型定义
            parts = mapOf(
                BodyPart.HEAD to SkinCube(0f, 0f, BodyPart.HEAD.getDims(false), SkinVec3(0f, 20f, 0f)),
                BodyPart.BODY to SkinCube(16f, 16f, BodyPart.BODY.getDims(false), SkinVec3(0f, 10f, 0f)),
                BodyPart.RIGHT_ARM to SkinCube(40f, 16f, BodyPart.RIGHT_ARM.getDims(false), SkinVec3(-6f, 10f, 0f), SkinVec3(0f, 6f, 0f)),
                BodyPart.LEFT_ARM to SkinCube(32f, 48f, BodyPart.LEFT_ARM.getDims(false), SkinVec3(6f, 10f, 0f), SkinVec3(0f, 6f, 0f)),
                BodyPart.RIGHT_LEG to SkinCube(0f, 16f, BodyPart.RIGHT_LEG.getDims(false), SkinVec3(-2f, -2f, 0f), SkinVec3(0f, 6f, 0f)),
                BodyPart.LEFT_LEG to SkinCube(16f, 48f, BodyPart.LEFT_LEG.getDims(false), SkinVec3(2f, -2f, 0f), SkinVec3(0f, 6f, 0f))
            )
            overlays = mapOf(
                BodyPart.HEAD to SkinCube(32f, 0f, BodyPart.HEAD.getDims(false), SkinVec3(0f, 20f, 0f)),
                BodyPart.BODY to SkinCube(16f, 32f, BodyPart.BODY.getDims(false), SkinVec3(0f, 10f, 0f)),
                BodyPart.RIGHT_ARM to SkinCube(40f, 32f, BodyPart.RIGHT_ARM.getDims(false), SkinVec3(-6f, 10f, 0f), SkinVec3(0f, 6f, 0f)),
                BodyPart.LEFT_ARM to SkinCube(48f, 48f, BodyPart.LEFT_ARM.getDims(false), SkinVec3(6f, 10f, 0f), SkinVec3(0f, 6f, 0f)),
                BodyPart.RIGHT_LEG to SkinCube(0f, 32f, BodyPart.RIGHT_LEG.getDims(false), SkinVec3(-2f, -2f, 0f), SkinVec3(0f, 6f, 0f)),
                BodyPart.LEFT_LEG to SkinCube(0f, 48f, BodyPart.LEFT_LEG.getDims(false), SkinVec3(2f, -2f, 0f), SkinVec3(0f, 6f, 0f))
            )
        }
    }
}
