package top.e404.skin.core.test

import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Color
import org.jetbrains.skia.IRect
import top.e404.skin.core.SkinFaceDirection
import top.e404.skin.core.SkinUvRect
import top.e404.skin.core.SkinVec3
import top.e404.skin.core.create3DOverlay

class Overlay3DTest {
    @Test
    fun `transparent pixels do not create voxels`() {
        val skin = Bitmap().apply {
            allocN32Pixels(1, 1)
            erase(Color.TRANSPARENT)
        }

        val mesh = create3DOverlay(
            skin = skin,
            dims = SkinVec3(1f, 1f, 1f),
            overlayDepth = 0.25f,
            faceUVs = mapOf(SkinFaceDirection.FRONT to SkinUvRect.makeXYWH(0f, 0f, 1f, 1f)),
            textureWidth = 1f,
            textureHeight = 1f
        )

        assertEquals(0, mesh.vertices.size)
        assertEquals(0, mesh.faces.size)
    }

    @Test
    fun `one opaque pixel creates one voxel`() {
        val skin = Bitmap().apply {
            allocN32Pixels(1, 1)
            erase(Color.TRANSPARENT)
            erase(Color.WHITE, IRect.makeXYWH(0, 0, 1, 1))
        }

        val mesh = create3DOverlay(
            skin = skin,
            dims = SkinVec3(1f, 1f, 1f),
            overlayDepth = 0.25f,
            faceUVs = mapOf(SkinFaceDirection.FRONT to SkinUvRect.makeXYWH(0f, 0f, 1f, 1f)),
            textureWidth = 1f,
            textureHeight = 1f
        )

        assertEquals(8, mesh.vertices.size)
        assertEquals(6, mesh.faces.size)
        assertEquals(Color.WHITE, mesh.faces.first().baseColor)
    }

    @Test
    fun `voxel face color comes from skin pixel`() {
        val expectedColor = Color.makeRGB(120, 30, 220)
        val skin = Bitmap().apply {
            allocN32Pixels(1, 1)
            erase(expectedColor)
        }

        val mesh = create3DOverlay(
            skin = skin,
            dims = SkinVec3(1f, 1f, 1f),
            overlayDepth = 0.25f,
            faceUVs = mapOf(SkinFaceDirection.FRONT to SkinUvRect.makeXYWH(0f, 0f, 1f, 1f)),
            textureWidth = 1f,
            textureHeight = 1f
        )

        assertEquals(6, mesh.faces.size)
        mesh.faces.forEach { face -> assertEquals(expectedColor, face.baseColor) }
    }
}
