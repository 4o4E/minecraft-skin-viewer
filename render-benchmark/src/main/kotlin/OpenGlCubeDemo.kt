package top.e404.mcsk.benchmark

import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR
import org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR
import org.lwjgl.glfw.GLFW.GLFW_FALSE
import org.lwjgl.glfw.GLFW.GLFW_VISIBLE
import org.lwjgl.glfw.GLFW.glfwCreateWindow
import org.lwjgl.glfw.GLFW.glfwDefaultWindowHints
import org.lwjgl.glfw.GLFW.glfwDestroyWindow
import org.lwjgl.glfw.GLFW.glfwInit
import org.lwjgl.glfw.GLFW.glfwMakeContextCurrent
import org.lwjgl.glfw.GLFW.glfwTerminate
import org.lwjgl.glfw.GLFW.glfwWindowHint
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.GL_BACK
import org.lwjgl.opengl.GL11.GL_BLEND
import org.lwjgl.opengl.GL11.GL_CLAMP
import org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL11.GL_CULL_FACE
import org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT
import org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT
import org.lwjgl.opengl.GL11.GL_DEPTH_TEST
import org.lwjgl.opengl.GL11.GL_FLOAT
import org.lwjgl.opengl.GL11.GL_FRONT
import org.lwjgl.opengl.GL11.GL_LEQUAL
import org.lwjgl.opengl.GL11.GL_MODELVIEW
import org.lwjgl.opengl.GL11.GL_NEAREST
import org.lwjgl.opengl.GL11.GL_NONE
import org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA
import org.lwjgl.opengl.GL11.GL_POLYGON_OFFSET_FILL
import org.lwjgl.opengl.GL11.GL_PROJECTION
import org.lwjgl.opengl.GL11.GL_QUADS
import org.lwjgl.opengl.GL11.GL_RGBA
import org.lwjgl.opengl.GL11.GL_SRC_ALPHA
import org.lwjgl.opengl.GL11.GL_TEXTURE_2D
import org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER
import org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER
import org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S
import org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T
import org.lwjgl.opengl.GL11.GL_TRIANGLE_FAN
import org.lwjgl.opengl.GL11.GL_TRIANGLES
import org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL11.glBegin
import org.lwjgl.opengl.GL11.glBindTexture
import org.lwjgl.opengl.GL11.glBlendFunc
import org.lwjgl.opengl.GL11.glClear
import org.lwjgl.opengl.GL11.glClearColor
import org.lwjgl.opengl.GL11.glColor4f
import org.lwjgl.opengl.GL11.glCullFace
import org.lwjgl.opengl.GL11.glDepthFunc
import org.lwjgl.opengl.GL11.glDepthMask
import org.lwjgl.opengl.GL11.glDisable
import org.lwjgl.opengl.GL11.glEnable
import org.lwjgl.opengl.GL11.glEnd
import org.lwjgl.opengl.GL11.glGenTextures
import org.lwjgl.opengl.GL11.glGetInteger
import org.lwjgl.opengl.GL11.glLoadMatrixf
import org.lwjgl.opengl.GL11.glMatrixMode
import org.lwjgl.opengl.GL11.glPolygonOffset
import org.lwjgl.opengl.GL11.glNormal3f
import org.lwjgl.opengl.GL11.glReadBuffer
import org.lwjgl.opengl.GL11.glReadPixels
import org.lwjgl.opengl.GL11.glTexCoord2f
import org.lwjgl.opengl.GL11.glTexImage2D
import org.lwjgl.opengl.GL11.glTexParameteri
import org.lwjgl.opengl.GL11.glVertex3f
import org.lwjgl.opengl.GL11.glViewport
import org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.GL_TEXTURE1
import org.lwjgl.opengl.GL13.glActiveTexture
import org.lwjgl.opengl.GL20.GL_COMPILE_STATUS
import org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER
import org.lwjgl.opengl.GL20.GL_LINK_STATUS
import org.lwjgl.opengl.GL20.GL_VERTEX_SHADER
import org.lwjgl.opengl.GL20.glAttachShader
import org.lwjgl.opengl.GL20.glCompileShader
import org.lwjgl.opengl.GL20.glCreateProgram
import org.lwjgl.opengl.GL20.glCreateShader
import org.lwjgl.opengl.GL20.glGetProgramInfoLog
import org.lwjgl.opengl.GL20.glGetProgrami
import org.lwjgl.opengl.GL20.glGetShaderInfoLog
import org.lwjgl.opengl.GL20.glGetShaderi
import org.lwjgl.opengl.GL20.glGetUniformLocation
import org.lwjgl.opengl.GL20.glLinkProgram
import org.lwjgl.opengl.GL20.glShaderSource
import org.lwjgl.opengl.GL20.glUniform1i
import org.lwjgl.opengl.GL20.glUniform3f
import org.lwjgl.opengl.GL20.glUniformMatrix4fv
import org.lwjgl.opengl.GL20.glUseProgram
import org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT
import org.lwjgl.opengl.GL30.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING
import org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE
import org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL30.GL_RENDERBUFFER
import org.lwjgl.opengl.GL30.glBindFramebuffer
import org.lwjgl.opengl.GL30.glBindRenderbuffer
import org.lwjgl.opengl.GL30.glCheckFramebufferStatus
import org.lwjgl.opengl.GL30.glFramebufferTexture2D
import org.lwjgl.opengl.GL30.glFramebufferRenderbuffer
import org.lwjgl.opengl.GL30.glGenFramebuffers
import org.lwjgl.opengl.GL30.glGenRenderbuffers
import org.lwjgl.opengl.GL30.glRenderbufferStorage
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Color as SkiaColor
import org.jetbrains.skia.Image as SkiaImage
import top.e404.mcsk.core.SkinMesh
import top.e404.mcsk.core.SkinMeshFace
import top.e404.mcsk.core.createMinecraftPlayerMeshes
import java.awt.image.BufferedImage
import java.io.File
import java.nio.ByteBuffer
import javax.imageio.ImageIO
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val GL_DEMO_WIDTH = 600
private const val GL_DEMO_HEIGHT = 900
private const val COLOR_SSAA = 2
private const val SHADOW_SIZE = 4096

object OpenGlCubeDemo {
    fun run() {
        val reportDir = File(System.getProperty("skin.openglDemo.reportDir", "build/reports/opengl-cube-demo"))
        reportDir.mkdirs()
        val cubeImageFile = reportDir.resolve("opengl-cube-transparent-shadow.png")
        val alexAngles = listOf(0, 45, 90, 135, 180, 225, 270, 315)

        renderToPng(cubeImageFile) { drawCubeScene() }
        alexAngles.forEach { yaw ->
            renderToPng(reportDir.resolve("opengl-alex-skin-yaw-$yaw.png")) {
                drawAlexScene(File("alex_skin.png"), yaw.toFloat())
            }
        }

        reportDir.resolve("summary.md").writeText(
            """
            |# OpenGL cube transparency and shadow demo
            |
            |Generated images:
            |
            |![OpenGL cube demo](opengl-cube-transparent-shadow.png)
            |
            |${alexAngles.joinToString("\n|\n|") { "![OpenGL Alex skin yaw $it](opengl-alex-skin-yaw-$it.png)" }}
            |
            |This is a first native OpenGL spike using LWJGL + GLFW. It creates a hidden local OpenGL context, renders opaque cubes, a translucent outer shell, and a real depth-texture shadow map, then reads pixels back to PNG.
            |
            |The Alex image reads `run/alex_skin.png` and reuses `core.createMinecraftPlayerMeshes` for the Minecraft model geometry.
            |
            |The Alex render uses a full-model shadow map for the floor and a second detail shadow map containing only the 3D overlay voxels, so small outer-layer blocks can cast visible contact shadows on the skin without base-mesh self-acne.
            |
            |This is not the final EGL/OSMesa server backend yet. The next step would be replacing GLFW with EGL/OSMesa and moving the compatibility-profile immediate drawing into VBO/shader batches.
            |""".trimMargin()
        )
        println("OpenGL demos written to ${reportDir.absolutePath}")
    }

    private fun renderToPng(file: File, draw: () -> Unit) {
        check(glfwInit()) { "Failed to initialize GLFW" }
        var window = 0L
        try {
            glfwDefaultWindowHints()
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0)
            window = glfwCreateWindow(GL_DEMO_WIDTH, GL_DEMO_HEIGHT, "OpenGL Cube Demo", 0, 0)
            check(window != 0L) { "Failed to create GLFW window/OpenGL context" }

            glfwMakeContextCurrent(window)
            GL.createCapabilities()
            val colorTarget = createColorTarget(GL_DEMO_WIDTH * COLOR_SSAA, GL_DEMO_HEIGHT * COLOR_SSAA)
            glBindFramebuffer(GL_FRAMEBUFFER, colorTarget.framebuffer)
            draw()
            ImageIO.write(readFramebuffer(colorTarget.width, colorTarget.height, GL_DEMO_WIDTH, GL_DEMO_HEIGHT), "png", file)
            glBindFramebuffer(GL_FRAMEBUFFER, 0)
        } finally {
            if (window != 0L) glfwDestroyWindow(window)
            glfwTerminate()
        }
    }

    private fun beginScene(eye: Vec3, target: Vec3, floorY: Float) {
        glViewport(0, 0, GL_DEMO_WIDTH * COLOR_SSAA, GL_DEMO_HEIGHT * COLOR_SSAA)
        glClearColor(28f / 255f, 32f / 255f, 38f / 255f, 1f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_LEQUAL)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glMatrixMode(GL_PROJECTION)
        glLoadMatrixf(perspective(45f, GL_DEMO_WIDTH.toFloat() / GL_DEMO_HEIGHT, 0.1f, 100f))
        glMatrixMode(GL_MODELVIEW)
        glLoadMatrixf(lookAt(eye, target, Vec3(0f, 1f, 0f)))
    }

    private fun drawCubeScene() {
        beginScene(Vec3(5.4f, 4.0f, 7.2f), Vec3(0f, 1.2f, 0f), 0f)
        val lightDir = Vec3(-0.45f, 0.85f, -0.55f).normalized()
        val light = ShadowCamera(
            projection = orthographic(-6f, 6f, -6f, 6f, 0.1f, 30f),
            view = lookAt(Vec3(-8f, 11f, -9f), Vec3(0f, 1.8f, 0f), Vec3(0f, 1f, 0f))
        )
        val shadow = createShadowResources()
        renderShadowMap(shadow, light) {
            drawCube(Vec3(0f, 1.1f, 0f), Vec3(1.8f, 2.2f, 1.2f), Color4(1f, 1f, 1f, 1f))
            drawCube(Vec3(-0.62f, 2.7f, 0.02f), Vec3(0.55f, 0.55f, 1.3f), Color4(1f, 1f, 1f, 1f))
            drawCube(Vec3(0f, 3.4f, 0f), Vec3(1.55f, 1.25f, 1.35f), Color4(1f, 1f, 1f, 1f))
        }

        beginScene(Vec3(5.4f, 4.0f, 7.2f), Vec3(0f, 1.2f, 0f), 0f)
        val shader = ShadowShader.create(light.shadowMatrix(), shadow.depthTexture, lightDir)
        shader.use(receiveShadow = true, useTexture = false)
        drawFloor(0f)
        shader.use(receiveShadow = true, useTexture = false)

        drawCube(Vec3(0f, 1.1f, 0f), Vec3(1.8f, 2.2f, 1.2f), Color4(0.16f, 0.44f, 0.86f, 1f))
        drawCube(Vec3(-0.62f, 2.7f, 0.02f), Vec3(0.55f, 0.55f, 1.3f), Color4(0.94f, 0.66f, 0.42f, 1f))
        drawCube(Vec3(0f, 3.4f, 0f), Vec3(1.55f, 1.25f, 1.35f), Color4(0.30f, 0.55f, 0.86f, 1f))

        glDepthMask(false)
        glEnable(GL_CULL_FACE)
        val shell = Color4(0.12f, 0.88f, 1.0f, 0.28f)
        shader.use(receiveShadow = false, useTexture = false)
        glCullFace(GL_FRONT)
        drawCube(Vec3(0f, 1.18f, 0f), Vec3(2.05f, 2.45f, 1.45f), shell)
        drawCube(Vec3(0f, 3.4f, 0f), Vec3(1.85f, 1.55f, 1.65f), shell)
        glCullFace(GL_BACK)
        drawCube(Vec3(0f, 1.18f, 0f), Vec3(2.05f, 2.45f, 1.45f), shell)
        drawCube(Vec3(0f, 3.4f, 0f), Vec3(1.85f, 1.55f, 1.65f), shell)
        glDisable(GL_CULL_FACE)
        glDepthMask(true)
        glUseProgram(0)
    }

    private fun drawAlexScene(skinFile: File, yaw: Float) {
        check(skinFile.isFile) { "Missing ${skinFile.absolutePath}; run this task with workingDir=run and alex_skin.png present" }
        val skinImage = SkiaImage.makeFromEncoded(skinFile.readBytes())
        val skinBitmap = Bitmap.makeFromImage(skinImage)
        val meshes = createMinecraftPlayerMeshes(skinBitmap, isSlim = true, use3DOverlay = true)

        val light = ShadowCamera(
            projection = orthographic(-24f, 24f, -24f, 24f, 0.1f, 90f),
            view = lookAt(Vec3(-24f, 46f, -28f), Vec3(0f, 8f, 0f), Vec3(0f, 1f, 0f))
        )
        val lightDir = Vec3(-0.45f, 0.85f, -0.55f).normalized()
        val floorShadow = createShadowResources()
        renderShadowMap(floorShadow, light) {
            meshes.filter { it.texture != null }.forEach { drawTexturedMesh(it, correctNormalByFaceCenter = true) }
            glDisable(GL_CULL_FACE)
            meshes.filter { it.texture == null }.forEach {
                drawSolidMesh(it, transparentPass = false, correctNormalByVoxelCenter = true)
            }
            meshes.filter { it.texture == null }.forEach {
                drawSolidMesh(it, transparentPass = true, correctNormalByVoxelCenter = true)
            }
            glEnable(GL_CULL_FACE)
            glCullFace(GL_FRONT)
        }
        val overlayDetailShadow = createShadowResources()
        renderShadowMap(overlayDetailShadow, light, cullFrontFaces = false, polygonOffset = null) {
            meshes.filter { it.texture == null }.forEach {
                drawSolidMesh(it, transparentPass = false, correctNormalByVoxelCenter = true)
            }
            meshes.filter { it.texture == null }.forEach {
                drawSolidMesh(it, transparentPass = true, correctNormalByVoxelCenter = true)
            }
        }

        beginScene(orbitEye(yaw, target = Vec3(0f, 8f, 0f), distance = 43f, height = 18f), Vec3(0f, 8f, 0f), -8.2f)
        val floorShader = ShadowShader.create(light.shadowMatrix(), floorShadow.depthTexture, lightDir, ShadowStyle.FLOOR)
        floorShader.use(receiveShadow = true, useTexture = false)
        drawFloor(-8.2f)

        val detailShader = ShadowShader.create(light.shadowMatrix(), overlayDetailShadow.depthTexture, lightDir, ShadowStyle.MODEL_DETAIL)
        val textureId = uploadTexture(skinBitmap)
        floorShader.use(receiveShadow = true, useTexture = true, skinTexture = textureId)
        glEnable(GL_TEXTURE_2D)
        glBindTexture(GL_TEXTURE_2D, textureId)
        meshes.filter { it.texture != null }.forEach { drawTexturedMesh(it, correctNormalByFaceCenter = true) }
        glDisable(GL_TEXTURE_2D)

        detailShader.use(receiveShadow = true, useTexture = false)
        meshes.filter { it.texture == null }.forEach {
            drawSolidMesh(it, transparentPass = false, correctNormalByVoxelCenter = true)
        }

        detailShader.use(receiveShadow = true, useTexture = false)
        glDepthMask(false)
        meshes.filter { it.texture == null }.forEach {
            drawSolidMesh(it, transparentPass = true, correctNormalByVoxelCenter = true)
        }
        glDepthMask(true)
        glUseProgram(0)
    }

    private fun drawFloor(y: Float) {
        glColor4f(0.44f, 0.53f, 0.62f, 1f)
        glNormal3f(0f, 1f, 0f)
        glBegin(GL_QUADS)
        glVertex3f(-14f, y, -14f)
        glVertex3f(14f, y, -14f)
        glVertex3f(14f, y, 14f)
        glVertex3f(-14f, y, 14f)
        glEnd()
    }

    private fun drawCube(center: Vec3, size: Vec3, color: Color4) {
        val x0 = center.x - size.x / 2f
        val x1 = center.x + size.x / 2f
        val y0 = center.y - size.y / 2f
        val y1 = center.y + size.y / 2f
        val z0 = center.z - size.z / 2f
        val z1 = center.z + size.z / 2f
        glColor4f(color.r, color.g, color.b, color.a)
        glBegin(GL_QUADS)
        glNormal3f(0f, 0f, 1f)
        quad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1)
        glNormal3f(0f, 0f, -1f)
        quad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0)
        glNormal3f(-1f, 0f, 0f)
        quad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0)
        glNormal3f(1f, 0f, 0f)
        quad(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1)
        glNormal3f(0f, 1f, 0f)
        quad(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0)
        glNormal3f(0f, -1f, 0f)
        quad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1)
        glEnd()
    }

    private fun uploadTexture(bitmap: Bitmap): Int {
        val textureId = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, textureId)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP)

        val pixels = BufferUtils.createByteBuffer(bitmap.width * bitmap.height * 4)
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val color = bitmap.getColor(x, y)
                pixels.put(SkiaColor.getR(color).toByte())
                pixels.put(SkiaColor.getG(color).toByte())
                pixels.put(SkiaColor.getB(color).toByte())
                pixels.put(SkiaColor.getA(color).toByte())
            }
        }
        pixels.flip()
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, bitmap.width, bitmap.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels)
        return textureId
    }

    private fun drawTexturedMesh(mesh: SkinMesh, correctNormalByFaceCenter: Boolean = false) {
        glColor4f(1f, 1f, 1f, 1f)
        glBegin(GL_TRIANGLES)
        mesh.faces.forEach { face ->
            if (face.indices.size < 3) return@forEach
            val faceCenter = if (correctNormalByFaceCenter) face.boundsCenter(mesh) else null
            for (i in 1 until face.indices.size - 1) {
                setTriangleNormal(mesh, face.indices[0], face.indices[i], face.indices[i + 1], faceCenter)
                drawTexturedVertex(mesh, face.indices[0])
                drawTexturedVertex(mesh, face.indices[i])
                drawTexturedVertex(mesh, face.indices[i + 1])
            }
        }
        glEnd()
    }

    private fun drawTexturedVertex(mesh: SkinMesh, index: Int) {
        val vertex = mesh.vertices[index]
        glTexCoord2f(vertex.uv.u, vertex.uv.v)
        glVertex3f(vertex.position.x, vertex.position.y, vertex.position.z)
    }

    private fun drawSolidMesh(mesh: SkinMesh, transparentPass: Boolean, correctNormalByVoxelCenter: Boolean = false) {
        glBegin(GL_TRIANGLES)
        mesh.faces.forEach { face ->
            val alpha = SkiaColor.getA(face.baseColor)
            if (transparentPass != (alpha < 255)) return@forEach
            glColor4f(
                SkiaColor.getR(face.baseColor) / 255f,
                SkiaColor.getG(face.baseColor) / 255f,
                SkiaColor.getB(face.baseColor) / 255f,
                alpha / 255f
            )
            if (face.indices.size < 3) return@forEach
            val normalCenter = if (correctNormalByVoxelCenter) face.voxelBoundsCenter(mesh) else null
            for (i in 1 until face.indices.size - 1) {
                setTriangleNormal(mesh, face.indices[0], face.indices[i], face.indices[i + 1], normalCenter)
                drawSolidVertex(mesh, face.indices[0])
                drawSolidVertex(mesh, face.indices[i])
                drawSolidVertex(mesh, face.indices[i + 1])
            }
        }
        glEnd()
    }

    private fun drawSolidVertex(mesh: SkinMesh, index: Int) {
        val position = mesh.vertices[index].position
        glVertex3f(position.x, position.y, position.z)
    }

    private fun setTriangleNormal(mesh: SkinMesh, a: Int, b: Int, c: Int, meshCenter: Vec3? = null) {
        val p0 = mesh.vertices[a].position
        val p1 = mesh.vertices[b].position
        val p2 = mesh.vertices[c].position
        var normal = (Vec3(p1.x - p0.x, p1.y - p0.y, p1.z - p0.z)
            .cross(Vec3(p2.x - p0.x, p2.y - p0.y, p2.z - p0.z)))
            .normalized()
        if (meshCenter != null) {
            val faceCenter = Vec3(
                (p0.x + p1.x + p2.x) / 3f,
                (p0.y + p1.y + p2.y) / 3f,
                (p0.z + p1.z + p2.z) / 3f
            )
            if (normal.dot(faceCenter - meshCenter) < 0f) {
                normal = -normal
            }
        }
        glNormal3f(normal.x, normal.y, normal.z)
    }

    private fun SkinMesh.boundsCenter(): Vec3 {
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        vertices.forEach { vertex ->
            minX = minOf(minX, vertex.position.x)
            minY = minOf(minY, vertex.position.y)
            minZ = minOf(minZ, vertex.position.z)
            maxX = maxOf(maxX, vertex.position.x)
            maxY = maxOf(maxY, vertex.position.y)
            maxZ = maxOf(maxZ, vertex.position.z)
        }
        return Vec3((minX + maxX) / 2f, (minY + maxY) / 2f, (minZ + maxZ) / 2f)
    }

    private fun SkinMeshFace.boundsCenter(mesh: SkinMesh): Vec3 {
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        indices.forEach { index ->
            val position = mesh.vertices[index].position
            minX = minOf(minX, position.x)
            minY = minOf(minY, position.y)
            minZ = minOf(minZ, position.z)
            maxX = maxOf(maxX, position.x)
            maxY = maxOf(maxY, position.y)
            maxZ = maxOf(maxZ, position.z)
        }
        return Vec3((minX + maxX) / 2f, (minY + maxY) / 2f, (minZ + maxZ) / 2f)
    }

    private fun SkinMeshFace.voxelBoundsCenter(mesh: SkinMesh): Vec3 {
        val firstIndex = indices.minOrNull() ?: return boundsCenter(mesh)
        val voxelBaseIndex = (firstIndex / 8) * 8
        if (voxelBaseIndex + 7 >= mesh.vertices.size) return boundsCenter(mesh)

        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        for (index in voxelBaseIndex until voxelBaseIndex + 8) {
            val position = mesh.vertices[index].position
            minX = minOf(minX, position.x)
            minY = minOf(minY, position.y)
            minZ = minOf(minZ, position.z)
            maxX = maxOf(maxX, position.x)
            maxY = maxOf(maxY, position.y)
            maxZ = maxOf(maxZ, position.z)
        }
        return Vec3((minX + maxX) / 2f, (minY + maxY) / 2f, (minZ + maxZ) / 2f)
    }

    private fun createShadowResources(): ShadowResources {
        val previousFramebuffer = glGetInteger(GL_FRAMEBUFFER_BINDING)
        val depthTexture = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, depthTexture)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, SHADOW_SIZE, SHADOW_SIZE, 0, GL_DEPTH_COMPONENT, GL_FLOAT, null as ByteBuffer?)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

        val framebuffer = glGenFramebuffers()
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTexture, 0)
        org.lwjgl.opengl.GL11.glDrawBuffer(GL_NONE)
        glReadBuffer(GL_NONE)
        check(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE) { "Shadow framebuffer is incomplete" }
        glBindFramebuffer(GL_FRAMEBUFFER, previousFramebuffer)
        return ShadowResources(framebuffer, depthTexture)
    }

    private fun createColorTarget(width: Int, height: Int): ColorTarget {
        val previousFramebuffer = glGetInteger(GL_FRAMEBUFFER_BINDING)
        val framebuffer = glGenFramebuffers()
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer)

        val colorTexture = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, colorTexture)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null as ByteBuffer?)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTexture, 0)

        val depthBuffer = glGenRenderbuffers()
        glBindRenderbuffer(GL_RENDERBUFFER, depthBuffer)
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, width, height)
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthBuffer)

        check(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE) { "Color framebuffer is incomplete" }
        glBindFramebuffer(GL_FRAMEBUFFER, previousFramebuffer)
        return ColorTarget(framebuffer, width, height)
    }

    private fun renderShadowMap(
        resources: ShadowResources,
        camera: ShadowCamera,
        cullFrontFaces: Boolean = true,
        polygonOffset: PolygonOffset? = PolygonOffset(0.1f, 0.25f),
        drawCasters: () -> Unit
    ) {
        val previousFramebuffer = glGetInteger(GL_FRAMEBUFFER_BINDING)
        glBindFramebuffer(GL_FRAMEBUFFER, resources.framebuffer)
        glViewport(0, 0, SHADOW_SIZE, SHADOW_SIZE)
        glClear(GL_DEPTH_BUFFER_BIT)
        glUseProgram(0)
        glDisable(GL_BLEND)
        glEnable(GL_DEPTH_TEST)
        if (cullFrontFaces) {
            glEnable(GL_CULL_FACE)
            glCullFace(GL_FRONT)
        } else {
            glDisable(GL_CULL_FACE)
        }
        if (polygonOffset != null) {
            glEnable(GL_POLYGON_OFFSET_FILL)
            glPolygonOffset(polygonOffset.factor, polygonOffset.units)
        } else {
            glDisable(GL_POLYGON_OFFSET_FILL)
        }
        glMatrixMode(GL_PROJECTION)
        glLoadMatrixf(camera.projection)
        glMatrixMode(GL_MODELVIEW)
        glLoadMatrixf(camera.view)
        drawCasters()
        glDisable(GL_POLYGON_OFFSET_FILL)
        glCullFace(GL_BACK)
        glDisable(GL_CULL_FACE)
        glBindFramebuffer(GL_FRAMEBUFFER, previousFramebuffer)
        glEnable(GL_BLEND)
    }

    private fun quad(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
    ) {
        glVertex3f(x0, y0, z0)
        glVertex3f(x1, y1, z1)
        glVertex3f(x2, y2, z2)
        glVertex3f(x3, y3, z3)
    }

    private fun readFramebuffer(sourceWidth: Int, sourceHeight: Int, outputWidth: Int, outputHeight: Int): BufferedImage {
        val pixels = BufferUtils.createByteBuffer(sourceWidth * sourceHeight * 4)
        glReadPixels(0, 0, sourceWidth, sourceHeight, GL_RGBA, GL_UNSIGNED_BYTE, pixels)

        val image = BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_ARGB)
        val scaleX = sourceWidth / outputWidth
        val scaleY = sourceHeight / outputHeight
        for (y in 0 until outputHeight) {
            for (x in 0 until outputWidth) {
                var r = 0
                var g = 0
                var b = 0
                var a = 0
                for (sy in 0 until scaleY) {
                    for (sx in 0 until scaleX) {
                        val sourceX = x * scaleX + sx
                        val sourceY = y * scaleY + sy
                        val i = (sourceY * sourceWidth + sourceX) * 4
                        r += pixels.get(i).toInt() and 0xff
                        g += pixels.get(i + 1).toInt() and 0xff
                        b += pixels.get(i + 2).toInt() and 0xff
                        a += pixels.get(i + 3).toInt() and 0xff
                    }
                }
                val samples = scaleX * scaleY
                image.setRGB(
                    x,
                    outputHeight - 1 - y,
                    ((a / samples) shl 24) or ((r / samples) shl 16) or ((g / samples) shl 8) or (b / samples)
                )
            }
        }
        return image
    }
}

fun main() {
    OpenGlCubeDemo.run()
}

private data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun minus(other: Vec3): Vec3 = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun unaryMinus(): Vec3 = Vec3(-x, -y, -z)
    fun cross(other: Vec3): Vec3 =
        Vec3(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x)

    fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z

    fun normalized(): Vec3 {
        val length = sqrt(x * x + y * y + z * z)
        return if (length > 0f) Vec3(x / length, y / length, z / length) else this
    }
}

private data class Color4(val r: Float, val g: Float, val b: Float, val a: Float)

private data class PolygonOffset(val factor: Float, val units: Float)

private data class ShadowStyle(
    val biasMin: Float,
    val biasSlope: Float,
    val strength: Float,
    val pcfRadius: Float,
    val occlusionDivisor: Float,
) {
    companion object {
        val FLOOR = ShadowStyle(
            biasMin = 0.0008f,
            biasSlope = 0.0030f,
            strength = 0.62f,
            pcfRadius = 1.0f,
            occlusionDivisor = 4.0f,
        )
        val MODEL_DETAIL = ShadowStyle(
            biasMin = 0.00018f,
            biasSlope = 0.00055f,
            strength = 0.58f,
            pcfRadius = 0.20f,
            occlusionDivisor = 1.8f,
        )
    }
}

private data class ShadowResources(val framebuffer: Int, val depthTexture: Int)

private data class ColorTarget(val framebuffer: Int, val width: Int, val height: Int)

private data class ShadowCamera(val projection: FloatArray, val view: FloatArray) {
    fun shadowMatrix(): FloatArray = multiply(multiply(biasMatrix(), projection), view)
}

private class ShadowShader private constructor(
    private val program: Int,
    private val useTextureLocation: Int,
    private val receiveShadowLocation: Int,
) {
    fun use(receiveShadow: Boolean, useTexture: Boolean, skinTexture: Int = 0) {
        glUseProgram(program)
        glUniform1i(useTextureLocation, if (useTexture) 1 else 0)
        glUniform1i(receiveShadowLocation, if (receiveShadow) 1 else 0)
        glActiveTexture(GL_TEXTURE0)
        if (skinTexture != 0) glBindTexture(GL_TEXTURE_2D, skinTexture)
    }

    companion object {
        fun create(
            shadowMatrix: FloatArray,
            shadowTexture: Int,
            lightDir: Vec3,
            style: ShadowStyle = ShadowStyle.FLOOR
        ): ShadowShader {
            val vertexShader = compileShader(
                GL_VERTEX_SHADER,
                """
                #version 120
                uniform mat4 uShadowMatrix;
                uniform vec3 uLightDir;
                varying vec4 vShadowCoord;
                varying vec4 vColor;
                varying vec2 vUv;
                varying float vLight;
                varying float vNdotL;

                void main() {
                    gl_Position = ftransform();
                    vShadowCoord = uShadowMatrix * gl_Vertex;
                    vColor = gl_Color;
                    vUv = gl_MultiTexCoord0.st;
                    vec3 normal = normalize(gl_Normal);
                    vNdotL = max(dot(normal, normalize(uLightDir)), 0.0);
                    float diffuse = vNdotL;
                    vLight = 0.46 + diffuse * 0.54;
                }
                """.trimIndent()
            )
            val fragmentShader = compileShader(
                GL_FRAGMENT_SHADER,
                """
                #version 120
                uniform sampler2D uSkinTexture;
                uniform sampler2D uShadowMap;
                uniform int uUseTexture;
                uniform int uReceiveShadow;
                varying vec4 vShadowCoord;
                varying vec4 vColor;
                varying vec2 vUv;
                varying float vLight;
                varying float vNdotL;

                void main() {
                    vec4 color = uUseTexture == 1 ? texture2D(uSkinTexture, vUv) : vColor;
                    if (color.a < 0.01) {
                        discard;
                    }

                    float shadowFactor = 1.0;
                    vec3 shadowCoord = vShadowCoord.xyz / vShadowCoord.w;
                    if (
                        uReceiveShadow == 1 &&
                        vNdotL > 0.02 &&
                        shadowCoord.x >= 0.0 && shadowCoord.x <= 1.0 &&
                        shadowCoord.y >= 0.0 && shadowCoord.y <= 1.0 &&
                        shadowCoord.z >= 0.0 && shadowCoord.z <= 1.0
                    ) {
                        float bias = max(${style.biasMin}, ${style.biasSlope} * (1.0 - vNdotL));
                        vec2 texelSize = vec2(1.0 / ${SHADOW_SIZE}.0, 1.0 / ${SHADOW_SIZE}.0);
                        float occlusion = 0.0;
                        for (int y = -1; y <= 1; y++) {
                            for (int x = -1; x <= 1; x++) {
                                float closestDepth = texture2D(uShadowMap, shadowCoord.xy + vec2(float(x), float(y)) * texelSize * ${style.pcfRadius}).r;
                                if (shadowCoord.z - bias > closestDepth) {
                                    occlusion += 1.0;
                                }
                            }
                        }
                        shadowFactor = 1.0 - ${style.strength} * min(1.0, occlusion / ${style.occlusionDivisor});
                    }

                    color.rgb *= vLight * shadowFactor;
                    gl_FragColor = color;
                }
                """.trimIndent()
            )
            val program = glCreateProgram()
            glAttachShader(program, vertexShader)
            glAttachShader(program, fragmentShader)
            glLinkProgram(program)
            check(glGetProgrami(program, GL_LINK_STATUS) != 0) { glGetProgramInfoLog(program) }

            glUseProgram(program)
            glUniformMatrix4fv(glGetUniformLocation(program, "uShadowMatrix"), false, shadowMatrix)
            glUniform3f(glGetUniformLocation(program, "uLightDir"), lightDir.x, lightDir.y, lightDir.z)
            glUniform1i(glGetUniformLocation(program, "uSkinTexture"), 0)
            glUniform1i(glGetUniformLocation(program, "uShadowMap"), 1)
            glActiveTexture(GL_TEXTURE1)
            glBindTexture(GL_TEXTURE_2D, shadowTexture)
            glActiveTexture(GL_TEXTURE0)

            return ShadowShader(
                program = program,
                useTextureLocation = glGetUniformLocation(program, "uUseTexture"),
                receiveShadowLocation = glGetUniformLocation(program, "uReceiveShadow")
            )
        }

        private fun compileShader(type: Int, source: String): Int {
            val shader = glCreateShader(type)
            glShaderSource(shader, source)
            glCompileShader(shader)
            check(glGetShaderi(shader, GL_COMPILE_STATUS) != 0) { glGetShaderInfoLog(shader) }
            return shader
        }
    }
}

private fun perspective(fovDegrees: Float, aspect: Float, near: Float, far: Float): FloatArray {
    val f = (1.0 / kotlin.math.tan(Math.toRadians((fovDegrees / 2f).toDouble()))).toFloat()
    return floatArrayOf(
        f / aspect, 0f, 0f, 0f,
        0f, f, 0f, 0f,
        0f, 0f, (far + near) / (near - far), -1f,
        0f, 0f, (2f * far * near) / (near - far), 0f
    )
}

private fun lookAt(eye: Vec3, center: Vec3, up: Vec3): FloatArray {
    val f = (center - eye).normalized()
    val s = f.cross(up).normalized()
    val u = s.cross(f)
    return floatArrayOf(
        s.x, u.x, -f.x, 0f,
        s.y, u.y, -f.y, 0f,
        s.z, u.z, -f.z, 0f,
        -s.dot(eye), -u.dot(eye), f.dot(eye), 1f
    )
}

private fun orbitEye(yawDegrees: Float, target: Vec3, distance: Float, height: Float): Vec3 {
    val yaw = Math.toRadians(yawDegrees.toDouble())
    return Vec3(
        target.x + (sin(yaw) * distance).toFloat(),
        target.y + height,
        target.z + (cos(yaw) * distance).toFloat()
    )
}

private fun orthographic(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float): FloatArray =
    floatArrayOf(
        2f / (right - left), 0f, 0f, 0f,
        0f, 2f / (top - bottom), 0f, 0f,
        0f, 0f, -2f / (far - near), 0f,
        -(right + left) / (right - left),
        -(top + bottom) / (top - bottom),
        -(far + near) / (far - near),
        1f
    )

private fun biasMatrix(): FloatArray =
    floatArrayOf(
        0.5f, 0f, 0f, 0f,
        0f, 0.5f, 0f, 0f,
        0f, 0f, 0.5f, 0f,
        0.5f, 0.5f, 0.5f, 1f
    )

private fun multiply(a: FloatArray, b: FloatArray): FloatArray {
    val result = FloatArray(16)
    for (col in 0 until 4) {
        for (row in 0 until 4) {
            var sum = 0f
            for (k in 0 until 4) {
                sum += a[k * 4 + row] * b[col * 4 + k]
            }
            result[col * 4 + row] = sum
        }
    }
    return result
}
