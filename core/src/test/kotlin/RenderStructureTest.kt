package top.e404.skin.core.test

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Color
import top.e404.skin.core.cameraRelativeUpperLeftLight
import top.e404.skin.core.createMinecraftPlayerMeshes
import top.e404.skin.core.createSkinPlatform
import top.e404.tavolo.draw.render3d.OrbitCamera
import top.e404.tavolo.draw.render3d.Vec3

class RenderStructureTest {
    @Test
    fun `地台接收阴影但不投射自身阴影`() {
        val platform = createSkinPlatform(topY = -8.2f, thickness = 2f)
        val ys = platform.vertices.map { it.position.y }

        assertFalse(platform.castsShadow)
        assertTrue(platform.receivesShadow)
        assertEquals(-8.2f, ys.maxOrNull()!!, 0.0001f)
        assertEquals(-10.2f, ys.minOrNull()!!, 0.0001f)
    }

    @Test
    fun `相机相对光源来自画面左上方`() {
        val camera = OrbitCamera(Vec3(0f, 10f, 0f), yaw = 45f, pitch = 15f, distance = 65f)
        val (_, cameraForward) = camera.createViewMatrix()
        val cameraRight = cameraForward.cross(camera.upVector).normalized()
        val cameraUp = cameraRight.cross(cameraForward).normalized()

        val light = cameraRelativeUpperLeftLight(camera)

        assertEquals(1f, light.length(), 0.0001f)
        assertTrue(light.dot(cameraUp) > 0.5f)
        assertTrue(light.dot(cameraRight) < -0.4f)
        assertTrue(light.dot(-cameraForward) > 0.2f)
    }

    @Test
    fun `3D 外层皮肤会拆成独立纯色 Mesh`() {
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
}
