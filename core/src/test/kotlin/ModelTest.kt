package top.e404.mcsk.core.test

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import top.e404.mcsk.core.BodyPart
import top.e404.mcsk.core.PlayerModel
import top.e404.mcsk.core.SkinCube
import top.e404.mcsk.core.SkinFace
import top.e404.mcsk.core.SkinUvRect
import top.e404.mcsk.core.SkinVec3

class ModelTest {
    @Test
    fun `BodyPart returns dimensions by model type`() {
        assertEquals(SkinVec3(4f, 12f, 4f), BodyPart.RIGHT_ARM.getDims(false))
        assertEquals(SkinVec3(3f, 12f, 4f), BodyPart.RIGHT_ARM.getDims(true))
        assertEquals(SkinVec3(8f, 8f, 8f), BodyPart.HEAD.getDims(false))
        assertEquals(SkinVec3(8f, 8f, 8f), BodyPart.HEAD.getDims(true))
        assertEquals(SkinVec3(10f, 16f, 1f), BodyPart.CAPE.getDims(false))
    }

    @Test
    fun `SkinCube generates Minecraft face UVs`() {
        val standard = SkinCube(0f, 0f, SkinVec3(8f, 8f, 8f), SkinVec3(0f, 0f, 0f))

        assertRect(0f, 8f, 8f, 8f, standard.uvs.getValue(SkinFace.RIGHT))
        assertRect(16f, 8f, 8f, 8f, standard.uvs.getValue(SkinFace.LEFT))
        assertRect(8f, 0f, 8f, 8f, standard.uvs.getValue(SkinFace.TOP))
        assertRect(16f, 0f, 8f, 8f, standard.uvs.getValue(SkinFace.BOTTOM))
        assertRect(8f, 8f, 8f, 8f, standard.uvs.getValue(SkinFace.FRONT))
        assertRect(24f, 8f, 8f, 8f, standard.uvs.getValue(SkinFace.BACK))
    }

    @Test
    fun `PlayerModel contains all base and overlay parts`() {
        val classic = PlayerModel(isSlim = false)
        val slim = PlayerModel(isSlim = true)

        BodyPart.entries.filter { it != BodyPart.CAPE }.forEach { part ->
            assertNotNull(classic.parts[part])
            assertNotNull(classic.overlays[part])
            assertNotNull(slim.parts[part])
            assertNotNull(slim.overlays[part])
        }

        assertNotNull(classic.cape)
        assertNull(classic.parts[BodyPart.CAPE])
        assertNull(classic.overlays[BodyPart.CAPE])
        assertEquals(SkinVec3(-6f, 10f, 0f), classic.parts.getValue(BodyPart.RIGHT_ARM).pos)
        assertEquals(SkinVec3(-5.5f, 10f, 0f), slim.parts.getValue(BodyPart.RIGHT_ARM).pos)
        assertRect(0f, 20f, 4f, 12f, classic.parts.getValue(BodyPart.RIGHT_LEG).uvs.getValue(SkinFace.RIGHT))
        assertRect(8f, 20f, 4f, 12f, classic.parts.getValue(BodyPart.RIGHT_LEG).uvs.getValue(SkinFace.LEFT))
        assertNull(BodyPart.HEAD.slimDims)
    }

    @Test
    fun `cape uses Minecraft cuboid UV layout`() {
        val cape = PlayerModel(isSlim = false).cape

        assertRect(0f, 1f, 1f, 16f, cape.uvs.getValue(SkinFace.RIGHT))
        assertRect(11f, 1f, 1f, 16f, cape.uvs.getValue(SkinFace.LEFT))
        assertRect(1f, 0f, 10f, 1f, cape.uvs.getValue(SkinFace.TOP))
        assertRect(11f, 0f, 10f, 1f, cape.uvs.getValue(SkinFace.BOTTOM))
        assertRect(1f, 1f, 10f, 16f, cape.uvs.getValue(SkinFace.FRONT))
        assertRect(12f, 1f, 10f, 16f, cape.uvs.getValue(SkinFace.BACK))
    }

    private fun assertRect(
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        actual: SkinUvRect,
    ) {
        assertEquals(left, actual.left)
        assertEquals(top, actual.top)
        assertEquals(width, actual.width)
        assertEquals(height, actual.height)
    }
}
