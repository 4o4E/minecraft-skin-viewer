package top.e404.skin.core

import top.e404.tavolo.draw.render3d.Transformation

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
    val HEAD_ONLY = mapOf(
        BodyPart.HEAD to listOf(),
        BodyPart.BODY to listOf(Transformation.Scale(0f, 0f, 0f)),
        BodyPart.RIGHT_ARM to listOf(Transformation.Scale(0f, 0f, 0f)),
        BodyPart.LEFT_ARM to listOf(Transformation.Scale(0f, 0f, 0f)),
        BodyPart.RIGHT_LEG to listOf(Transformation.Scale(0f, 0f, 0f)),
        BodyPart.LEFT_LEG to listOf(Transformation.Scale(0f, 0f, 0f)),
    )
    fun withScale(
        isSlim: Boolean,
        headScale: Float = 1f,
        laScale: Float = 1f,
        raScale: Float = 1f,
        llScale: Float = 1f,
        rlScale: Float = 1f,
    ) = mapOf(
        BodyPart.HEAD to listOf(
            Transformation.Rotate(y = -30f),
            Transformation.Scale(headScale, headScale, headScale),
            Transformation.Translate(y = BodyPart.HEAD.getDims(isSlim).y.let { it * (headScale - 1) / 2 }),
        ),
        BodyPart.RIGHT_ARM to listOf(
            Transformation.Rotate(x = -30f),
            Transformation.Scale(raScale, raScale, raScale),
            Transformation.Translate(x = -BodyPart.LEFT_ARM.getDims(isSlim).x.let { it * (raScale - 1) / 2 }),
        ),
        BodyPart.LEFT_ARM to listOf(
            Transformation.Rotate(x = -30f),
            Transformation.Scale(laScale, laScale, laScale),
            Transformation.Translate(x = BodyPart.LEFT_ARM.getDims(isSlim).x.let { it * (laScale - 1) / 2 }),
        ),
        BodyPart.RIGHT_LEG to listOf(
            Transformation.Rotate(x = -80f, z = -20f),
            Transformation.Scale(rlScale, rlScale, rlScale),
            Transformation.Translate(y = -BodyPart.LEFT_LEG.getDims(isSlim).y.let { it * (rlScale - 1) / 2 }),
        ),
        BodyPart.LEFT_LEG to listOf(
            Transformation.Rotate(x = -80f, z = 20f),
            Transformation.Scale(llScale, llScale, llScale),
            Transformation.Translate(y = -BodyPart.LEFT_LEG.getDims(isSlim).y.let { it * (llScale - 1) / 2 }),
        ),
    )
}
