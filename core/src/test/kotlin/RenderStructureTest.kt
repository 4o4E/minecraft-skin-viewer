package top.e404.mcsk.core.test

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Color
import top.e404.mcsk.core.SkinVec2
import top.e404.mcsk.core.createMinecraftPlayerMeshes
import top.e404.mcsk.core.createSkinPlatform

class RenderStructureTest {
    @Test
    fun `platform receives shadows but does not cast itself`() {
        val platform = createSkinPlatform(topY = -8.2f, thickness = 2f)
        val ys = platform.vertices.map { it.position.y }

        assertFalse(platform.castsShadow)
        assertTrue(platform.receivesShadow)
        assertEquals(-8.2f, ys.maxOrNull()!!, 0.0001f)
        assertEquals(-10.2f, ys.minOrNull()!!, 0.0001f)
    }

    @Test
    fun `3D overlay skin is split into solid color meshes`() {
        val skin = Bitmap().apply {
            allocN32Pixels(64, 64)
            erase(Color.TRANSPARENT)
        }

        val meshes = createMinecraftPlayerMeshes(skin, isSlim = true, use3DOverlay = true)

        assertEquals(7, meshes.size)
        assertNotNull(meshes.first().texture)
        meshes.drop(1).forEach { mesh ->
            assertNull(mesh.texture)
        }
    }

    @Test
    fun `cape is emitted as a separate textured mesh`() {
        val skin = Bitmap().apply {
            allocN32Pixels(64, 64)
            erase(Color.TRANSPARENT)
        }
        val cape = Bitmap().apply {
            allocN32Pixels(64, 32)
            erase(Color.WHITE)
        }

        val meshes = createMinecraftPlayerMeshes(skin, isSlim = false, use3DOverlay = false, cape = cape)

        assertEquals(2, meshes.size)
        assertNotNull(meshes[0].texture)
        assertEquals(skin, meshes[0].texture)
        assertEquals(cape, meshes[1].texture)
        assertTrue(meshes[1].vertices.minOf { it.position.z } < -4f)
        assertEquals(-2.2527f, meshes[1].vertices.maxOf { it.position.z }, 0.0001f)
    }

    @Test
    fun `cape mesh uses Wiki outside texture area on back face`() {
        val skin = Bitmap().apply {
            allocN32Pixels(64, 64)
            erase(Color.TRANSPARENT)
        }
        val cape = Bitmap().apply {
            allocN32Pixels(64, 32)
            erase(Color.WHITE)
        }

        val capeMesh = createMinecraftPlayerMeshes(skin, isSlim = false, cape = cape)[1]
        val frontFaceUvs = capeMesh.faces[0].indices.map { capeMesh.vertices[it].uv }
        val backFaceUvs = capeMesh.faces[1].indices.map { capeMesh.vertices[it].uv }

        assertUvBounds(12.5f / 64f, 21.5f / 64f, 1.5f / 32f, 16.5f / 32f, frontFaceUvs)
        assertUvBounds(1.5f / 64f, 10.5f / 64f, 1.5f / 32f, 16.5f / 32f, backFaceUvs)
    }

    private fun assertUvBounds(
        minU: Float,
        maxU: Float,
        minV: Float,
        maxV: Float,
        actual: List<SkinVec2>,
    ) {
        assertEquals(minU, actual.minOf { it.u }, 0.0001f)
        assertEquals(maxU, actual.maxOf { it.u }, 0.0001f)
        assertEquals(minV, actual.minOf { it.v }, 0.0001f)
        assertEquals(maxV, actual.maxOf { it.v }, 0.0001f)
    }
}
