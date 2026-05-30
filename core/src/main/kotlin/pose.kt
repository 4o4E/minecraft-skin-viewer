package top.e404.mcsk.core

/**
 * 预设的玩家姿态
 */
object PosePresets {
    val WALKING = mapOf(
        BodyPart.RIGHT_ARM to listOf(SkinTransform.Rotate(x = -30f)),
        BodyPart.LEFT_ARM to listOf(SkinTransform.Rotate(x = 30f)),
        BodyPart.RIGHT_LEG to listOf(SkinTransform.Rotate(x = 30f)),
        BodyPart.LEFT_LEG to listOf(SkinTransform.Rotate(x = -30f)),
    )
    val SIT = mapOf(
        BodyPart.RIGHT_ARM to listOf(SkinTransform.Rotate(x = -30f)),
        BodyPart.LEFT_ARM to listOf(SkinTransform.Rotate(x = -30f)),
        BodyPart.RIGHT_LEG to listOf(SkinTransform.Rotate(x = -80f, z = -20f)),
        BodyPart.LEFT_LEG to listOf(SkinTransform.Rotate(x = -80f, z = 20f)),
    )
    val HOMO = mapOf(
        BodyPart.HEAD to listOf(SkinTransform.Rotate(y = 30f)),
        BodyPart.RIGHT_ARM to listOf(SkinTransform.Rotate(x = -15f), SkinTransform.Translate(z = -2f)),
        BodyPart.LEFT_ARM to listOf(SkinTransform.Rotate(x = -15f), SkinTransform.Translate(z = -2f)),
        BodyPart.RIGHT_LEG to listOf(
            SkinTransform.Rotate(x = -80f, z = -15f),
            SkinTransform.Translate(x = -1.6f, y = -1.5f)
        ),
        BodyPart.LEFT_LEG to listOf(
            SkinTransform.Rotate(x = -80f, z = 15f),
            SkinTransform.Translate(x = 1.6f, y = -1.5f)
        ),
    )
    val HEAD_ONLY = mapOf(
        BodyPart.HEAD to listOf(),
        BodyPart.BODY to listOf(SkinTransform.Scale(0f, 0f, 0f)),
        BodyPart.RIGHT_ARM to listOf(SkinTransform.Scale(0f, 0f, 0f)),
        BodyPart.LEFT_ARM to listOf(SkinTransform.Scale(0f, 0f, 0f)),
        BodyPart.RIGHT_LEG to listOf(SkinTransform.Scale(0f, 0f, 0f)),
        BodyPart.LEFT_LEG to listOf(SkinTransform.Scale(0f, 0f, 0f)),
        BodyPart.CAPE to listOf(SkinTransform.Scale(0f, 0f, 0f)),
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
            SkinTransform.Rotate(y = -30f),
            SkinTransform.Scale(headScale, headScale, headScale),
            SkinTransform.Translate(y = BodyPart.HEAD.getDims(isSlim).y.let { it * (headScale - 1) / 2 }),
        ),
        BodyPart.RIGHT_ARM to listOf(
            SkinTransform.Rotate(x = -30f),
            SkinTransform.Scale(raScale, raScale, raScale),
            SkinTransform.Translate(x = -BodyPart.LEFT_ARM.getDims(isSlim).x.let { it * (raScale - 1) / 2 }),
        ),
        BodyPart.LEFT_ARM to listOf(
            SkinTransform.Rotate(x = -30f),
            SkinTransform.Scale(laScale, laScale, laScale),
            SkinTransform.Translate(x = BodyPart.LEFT_ARM.getDims(isSlim).x.let { it * (laScale - 1) / 2 }),
        ),
        BodyPart.RIGHT_LEG to listOf(
            SkinTransform.Rotate(x = -80f, z = -20f),
            SkinTransform.Scale(rlScale, rlScale, rlScale),
            SkinTransform.Translate(y = -BodyPart.LEFT_LEG.getDims(isSlim).y.let { it * (rlScale - 1) / 2 }),
        ),
        BodyPart.LEFT_LEG to listOf(
            SkinTransform.Rotate(x = -80f, z = 20f),
            SkinTransform.Scale(llScale, llScale, llScale),
            SkinTransform.Translate(y = -BodyPart.LEFT_LEG.getDims(isSlim).y.let { it * (llScale - 1) / 2 }),
        ),
    )

    fun homo(isSlim: Boolean, headScale: Float = 1f): Map<BodyPart, List<SkinTransform>> =
        HOMO + mapOf(
            BodyPart.HEAD to HOMO.getValue(BodyPart.HEAD) + listOf(
                SkinTransform.Scale(headScale, headScale, headScale),
                SkinTransform.Translate(y = BodyPart.HEAD.getDims(isSlim).y.let { it * (headScale - 1) / 2 }),
            )
        )
}
