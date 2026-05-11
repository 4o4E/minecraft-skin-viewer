package top.e404.skin.core.test

import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Color
import org.jetbrains.skia.IRect
import org.jetbrains.skia.Rect
import top.e404.skin.core.create3DOverlay
import top.e404.tavolo.draw.render3d.FaceDirection
import top.e404.tavolo.draw.render3d.Vec3

class Overlay3DTest {
    @Test
    fun `透明像素不会生成体素`() {
        val skin = Bitmap().apply {
            allocN32Pixels(1, 1)
            erase(Color.TRANSPARENT)
        }

        val mesh = create3DOverlay(
            skin = skin,
            dims = Vec3(1f, 1f, 1f),
            overlayDepth = 0.25f,
            faceUVs = mapOf(FaceDirection.FRONT to Rect.makeXYWH(0f, 0f, 1f, 1f)),
            textureWidth = 1f,
            textureHeight = 1f
        )

        assertEquals(0, mesh.vertices.size)
        assertEquals(0, mesh.faces.size)
    }

    @Test
    fun `单个不透明像素生成一个体素`() {
        val skin = Bitmap().apply {
            allocN32Pixels(1, 1)
            erase(Color.TRANSPARENT)
            erase(Color.WHITE, IRect.makeXYWH(0, 0, 1, 1))
        }

        val mesh = create3DOverlay(
            skin = skin,
            dims = Vec3(1f, 1f, 1f),
            overlayDepth = 0.25f,
            faceUVs = mapOf(FaceDirection.FRONT to Rect.makeXYWH(0f, 0f, 1f, 1f)),
            textureWidth = 1f,
            textureHeight = 1f
        )

        assertEquals(8, mesh.vertices.size)
        assertEquals(6, mesh.faces.size)
        assertEquals(Color.WHITE, mesh.faces.first().baseColor)
    }

    @Test
    fun `体素面颜色来自对应皮肤像素`() {
        val expectedColor = Color.makeRGB(120, 30, 220)
        val skin = Bitmap().apply {
            allocN32Pixels(1, 1)
            erase(expectedColor)
        }

        val mesh = create3DOverlay(
            skin = skin,
            dims = Vec3(1f, 1f, 1f),
            overlayDepth = 0.25f,
            faceUVs = mapOf(FaceDirection.FRONT to Rect.makeXYWH(0f, 0f, 1f, 1f)),
            textureWidth = 1f,
            textureHeight = 1f
        )

        assertEquals(6, mesh.faces.size)
        mesh.faces.forEach { face -> assertEquals(expectedColor, face.baseColor) }
    }
}
