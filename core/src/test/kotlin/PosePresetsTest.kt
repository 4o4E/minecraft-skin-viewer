package top.e404.skin.core.test

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.e404.skin.core.BodyPart
import top.e404.skin.core.PosePresets
import top.e404.tavolo.draw.render3d.Transformation

class PosePresetsTest {
    @Test
    fun `HEAD_ONLY 会隐藏非头部部件`() {
        assertTrue(PosePresets.HEAD_ONLY.getValue(BodyPart.HEAD).isEmpty())

        listOf(BodyPart.BODY, BodyPart.RIGHT_ARM, BodyPart.LEFT_ARM, BodyPart.RIGHT_LEG, BodyPart.LEFT_LEG)
            .forEach { part ->
                val transformations = PosePresets.HEAD_ONLY.getValue(part)
                assertEquals(listOf(Transformation.Scale(0f, 0f, 0f)), transformations)
            }
    }

    @Test
    fun `withScale 根据 slim 尺寸计算平移补偿`() {
        val classic = PosePresets.withScale(isSlim = false, headScale = 1.5f, laScale = 1.2f, raScale = 1.2f)
        val slim = PosePresets.withScale(isSlim = true, laScale = 1.2f, raScale = 1.2f)

        assertTrue(classic.getValue(BodyPart.HEAD).containsTranslate(y = 2f))
        assertTrue(classic.getValue(BodyPart.RIGHT_ARM).containsTranslate(x = -0.4f))
        assertTrue(slim.getValue(BodyPart.RIGHT_ARM).containsTranslate(x = -0.3f))
    }

    private fun List<Transformation>.containsTranslate(
        x: Float = 0f,
        y: Float = 0f,
        z: Float = 0f,
    ): Boolean = any {
        it is Transformation.Translate &&
            kotlin.math.abs(it.x - x) < 0.0001f &&
            kotlin.math.abs(it.y - y) < 0.0001f &&
            kotlin.math.abs(it.z - z) < 0.0001f
    }
}
