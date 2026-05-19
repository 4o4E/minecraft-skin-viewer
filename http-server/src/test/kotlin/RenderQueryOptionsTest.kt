package top.e404.mcsk.test

import io.ktor.http.parametersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import top.e404.mcsk.core.BodyPart
import top.e404.mcsk.core.SkinLightingMode
import top.e404.mcsk.core.SkinOverlayMode
import top.e404.mcsk.core.SkinRenderUseCases
import top.e404.mcsk.core.SkinRenderVec3
import top.e404.mcsk.core.SkinTransform
import top.e404.mcsk.server.plugin.renderOptions

class RenderQueryOptionsTest {
    @Test
    fun `render query exposes renderer request options`() {
        val params = parametersOf(
            "width" to listOf("320"),
            "height" to listOf("480"),
            "target" to listOf("1,2,3"),
            "yaw" to listOf("90"),
            "pitch" to listOf("25"),
            "distance" to listOf("42"),
            "bg" to listOf("#112233"),
            "light" to listOf("0.5"),
            "lightDirection" to listOf("0.2,0.8,0.4"),
            "platformTopY" to listOf("-7"),
            "platformThickness" to listOf("1.5"),
            "aa" to listOf("4"),
            "overlay" to listOf("flat"),
            "lighting" to listOf("directional"),
            "shadow" to listOf("true"),
            "platform" to listOf("true"),
            "modelYaw" to listOf("45"),
            "pose" to listOf("""{"body":[{"type":"translate","z":2}]}""")
        )

        val options = params.renderOptions(SkinRenderUseCases.skinOptions(0))

        assertEquals(320, options.width)
        assertEquals(480, options.height)
        assertEquals(SkinRenderVec3(1f, 2f, 3f), options.target)
        assertEquals(90f, options.yaw)
        assertEquals(25f, options.pitch)
        assertEquals(42f, options.distance)
        assertEquals(0xFF112233.toInt(), options.backgroundColor)
        assertEquals(0.5f, options.lightIntensity)
        assertEquals(SkinRenderVec3(0.2f, 0.8f, 0.4f), options.lightDirection)
        assertEquals(-7f, options.platformTopY)
        assertEquals(1.5f, options.platformThickness)
        assertEquals(4, options.antiAliasingLevel)
        assertEquals(SkinOverlayMode.FLAT, options.overlayMode)
        assertEquals(SkinLightingMode.DIRECTIONAL, options.lightingMode)
        assertTrue(options.shadows)
        assertTrue(options.showPlatform)
        assertEquals(45f, options.modelYaw)
        assertEquals(listOf(SkinTransform.Translate(z = 2f)), options.pose.getValue(BodyPart.BODY))
    }

    @Test
    fun `invalid render query values fail instead of falling back to defaults`() {
        assertFailsWith<IllegalArgumentException> {
            parametersOf("width" to listOf("abc")).renderOptions(SkinRenderUseCases.skinOptions(0))
        }
        assertFailsWith<IllegalArgumentException> {
            parametersOf("targetX" to listOf("abc")).renderOptions(SkinRenderUseCases.skinOptions(0))
        }
        assertFailsWith<IllegalArgumentException> {
            parametersOf("lightX" to listOf("1")).renderOptions(SkinRenderUseCases.skinOptions(0))
        }
        assertFailsWith<IllegalArgumentException> {
            parametersOf("pose" to listOf("""{"body":[{"type":"rotate","x":"bad"}]}"""))
                .renderOptions(SkinRenderUseCases.skinOptions(0))
        }
    }

    @Test
    fun `shadow query enables directional lighting and visible platform by default`() {
        val options = parametersOf("shadow" to listOf("true"))
            .renderOptions(SkinRenderUseCases.skinRotateOptions(0))

        assertTrue(options.shadows)
        assertTrue(options.showPlatform)
        assertEquals(SkinLightingMode.DIRECTIONAL, options.lightingMode)
    }

    @Test
    fun `shadow query rejects ambient lighting`() {
        assertFailsWith<IllegalArgumentException> {
            parametersOf(
                "shadow" to listOf("true"),
                "lighting" to listOf("ambient")
            ).renderOptions(SkinRenderUseCases.skinOptions(0))
        }
    }
}
