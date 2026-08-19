package top.e404.mcsk.renderer.opengl

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Color as SkiaColor
import org.jetbrains.skia.Image as SkiaImage
import org.lwjgl.BufferUtils
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
import org.lwjgl.opengl.GL11.GL_ONE
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
import org.lwjgl.opengl.GL11.GL_TRIANGLES
import org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL11.GL_VENDOR
import org.lwjgl.opengl.GL11.GL_RENDERER
import org.lwjgl.opengl.GL11.GL_VERSION
import org.lwjgl.opengl.GL11.glBegin
import org.lwjgl.opengl.GL11.glBindTexture
import org.lwjgl.opengl.GL11.glClear
import org.lwjgl.opengl.GL11.glClearColor
import org.lwjgl.opengl.GL11.glColor4f
import org.lwjgl.opengl.GL11.glCullFace
import org.lwjgl.opengl.GL11.glDeleteTextures
import org.lwjgl.opengl.GL11.glDepthFunc
import org.lwjgl.opengl.GL11.glDepthMask
import org.lwjgl.opengl.GL11.glDisable
import org.lwjgl.opengl.GL11.glDrawBuffer
import org.lwjgl.opengl.GL11.glEnable
import org.lwjgl.opengl.GL11.glEnd
import org.lwjgl.opengl.GL11.glGenTextures
import org.lwjgl.opengl.GL11.glGetInteger
import org.lwjgl.opengl.GL11.glGetString
import org.lwjgl.opengl.GL11.glLoadMatrixf
import org.lwjgl.opengl.GL11.glMatrixMode
import org.lwjgl.opengl.GL11.glNormal3f
import org.lwjgl.opengl.GL11.glPolygonOffset
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
import org.lwjgl.opengl.GL14.glBlendFuncSeparate
import org.lwjgl.opengl.GL20.GL_COMPILE_STATUS
import org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER
import org.lwjgl.opengl.GL20.GL_LINK_STATUS
import org.lwjgl.opengl.GL20.GL_VERTEX_SHADER
import org.lwjgl.opengl.GL20.glAttachShader
import org.lwjgl.opengl.GL20.glCompileShader
import org.lwjgl.opengl.GL20.glCreateProgram
import org.lwjgl.opengl.GL20.glCreateShader
import org.lwjgl.opengl.GL20.glDeleteProgram
import org.lwjgl.opengl.GL20.glDeleteShader
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
import org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT
import org.lwjgl.opengl.GL30.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING
import org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE
import org.lwjgl.opengl.GL30.GL_RENDERBUFFER
import org.lwjgl.opengl.GL30.glBindFramebuffer
import org.lwjgl.opengl.GL30.glBindRenderbuffer
import org.lwjgl.opengl.GL30.glCheckFramebufferStatus
import org.lwjgl.opengl.GL30.glDeleteFramebuffers
import org.lwjgl.opengl.GL30.glDeleteRenderbuffers
import org.lwjgl.opengl.GL30.glFramebufferRenderbuffer
import org.lwjgl.opengl.GL30.glFramebufferTexture2D
import org.lwjgl.opengl.GL30.glGenFramebuffers
import org.lwjgl.opengl.GL30.glGenRenderbuffers
import org.lwjgl.opengl.GL30.glRenderbufferStorage
import top.e404.mcsk.core.SkinLightingMode
import top.e404.mcsk.core.SkinOverlayMode
import top.e404.mcsk.core.SkinPngRenderer
import top.e404.mcsk.core.SkinRenderRequest
import top.e404.mcsk.core.SkinRenderSettings
import top.e404.mcsk.core.SkinRenderVec3
import top.e404.mcsk.core.SkinMesh
import top.e404.mcsk.core.SkinMeshFace
import top.e404.mcsk.core.createMinecraftPlayerMeshes
import top.e404.mcsk.core.rotateY
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.IdentityHashMap
import javax.imageio.ImageIO
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

private const val GL_SHADOW_SIZE = 4096

class OpenGlSkinPngRenderer : SkinPngRenderer {
    override val name: String = "opengl-lwjgl-fbo"

    private val renderer = OpenGlSkinRenderer()

    override fun startup() {
        renderer.startup()
    }

    override fun renderPng(request: SkinRenderRequest): ByteArray =
        renderer.renderPng(
            request = request
        )

    override fun renderPngBatch(requests: List<SkinRenderRequest>): List<ByteArray> =
        renderer.renderPngBatch(requests)

    override fun close() {
        renderer.close()
    }
}

private class OpenGlSkinRenderer {
    private var context: GlContext? = null
    private var colorTarget: GlColorTarget? = null
    private val shaders = mutableMapOf<GlShaderKey, GlShadowShader>()

    fun startup() {
        if (context != null) return
        val createdContext = GlContext.createConfigured()
        try {
            createdContext.makeCurrent()
            GL.createCapabilities()
            context = createdContext
            val vendor = glGetString(GL_VENDOR).orEmpty()
            val renderer = glGetString(GL_RENDERER).orEmpty()
            val version = glGetString(GL_VERSION).orEmpty()
            verifyConfiguredGlDriver(vendor, renderer)
            println(
                "OpenGL initialized: backend=${createdContext.backendName}, " +
                    "vendor=$vendor, renderer=$renderer, version=$version"
            )
        } catch (error: Throwable) {
            createdContext.close()
            throw error
        }
    }

    private fun verifyConfiguredGlDriver(vendor: String, renderer: String) {
        val softwareRenderer = listOf("llvmpipe", "softpipe", "swrast").any {
            renderer.contains(it, ignoreCase = true)
        }
        if (System.getenv("MCSK_GL_REQUIRE_HARDWARE").toBoolean() && softwareRenderer) {
            error("Hardware OpenGL is required, but software renderer was selected: $renderer")
        }
        System.getenv("MCSK_GL_EXPECT_VENDOR")
            ?.takeIf { it.isNotBlank() }
            ?.let { expected ->
                check(vendor.contains(expected, ignoreCase = true)) {
                    "Expected OpenGL vendor $expected, but selected $vendor ($renderer)"
                }
            }
    }

    fun renderPng(
        request: SkinRenderRequest,
    ): ByteArray {
        startup()
        val prepared = prepareSkinScene(request)
        val textureIds = uploadTextures(prepared.meshes)
        try {
            return renderPngPrepared(request, prepared.meshes, textureIds)
        } finally {
            textureIds.values.forEach { glDeleteTextures(it) }
        }
    }

    fun renderPngBatch(requests: List<SkinRenderRequest>): List<ByteArray> {
        if (requests.isEmpty()) return emptyList()
        startup()
        if (!requests.canReusePreparedScene()) return requests.map(::renderPng)

        val prepared = prepareSkinScene(requests.first())
        val textureIds = uploadTextures(prepared.meshes)
        try {
            return requests.map { renderPngPrepared(it, prepared.meshes, textureIds) }
        } finally {
            textureIds.values.forEach { glDeleteTextures(it) }
        }
    }

    private fun renderPngPrepared(
        request: SkinRenderRequest,
        baseMeshes: List<SkinMesh>,
        textureIds: Map<Bitmap, Int>,
    ): ByteArray {
        val settings = request.settings
        val colorSsaa = settings.colorSsaa()
        val targetWidth = settings.width * colorSsaa
        val targetHeight = settings.height * colorSsaa
        val target = colorTarget
        if (target == null || target.width != targetWidth || target.height != targetHeight) {
            target?.close()
            colorTarget = createColorTarget(targetWidth, targetHeight)
        }

        glBindFramebuffer(GL_FRAMEBUFFER, colorTarget!!.framebuffer)
        renderSkinScene(
            request = request,
            meshes = baseMeshes.map { if (request.modelYaw == 0f) it else it.rotateY(request.modelYaw) },
            textureIds = textureIds
        )
        val image = readFramebuffer(targetWidth, targetHeight, settings.width, settings.height)
        glBindFramebuffer(GL_FRAMEBUFFER, 0)

        return ByteArrayOutputStream().use { output ->
            ImageIO.write(image, "png", output)
            output.toByteArray()
        }
    }

    fun close() {
        colorTarget?.close()
        colorTarget = null
        shaders.values.forEach { it.close() }
        shaders.clear()
        context?.let {
            GL.setCapabilities(null)
            it.close()
        }
        context = null
    }

    private fun prepareSkinScene(request: SkinRenderRequest): PreparedSkinScene {
        startup()
        val skinImage = SkiaImage.makeFromEncoded(request.skinPng)
        val skinBitmap = Bitmap.makeFromImage(skinImage)
        val capeBitmap = request.capePng?.let { Bitmap.makeFromImage(SkiaImage.makeFromEncoded(it)) }
        val use3DOverlay = request.overlayMode == SkinOverlayMode.THREE_D
        return PreparedSkinScene(
            meshes = createMinecraftPlayerMeshes(
                skin = skinBitmap,
                isSlim = request.isSlim,
                pose = request.pose,
                use3DOverlay = use3DOverlay,
                cape = capeBitmap
            )
        )
    }

    private fun renderSkinScene(
        request: SkinRenderRequest,
        meshes: List<SkinMesh>,
        textureIds: Map<Bitmap, Int>,
    ) {
        val settings = request.settings
        val use3DOverlay = request.overlayMode == SkinOverlayMode.THREE_D
        val lightDir = settings.lightDirection.toGlVec3().normalized()
        val shadowCamera = createShadowCamera(settings, lightDir)

        var floorShadow: GlShadowResources? = null
        try {
            if (request.shadows) {
                floorShadow = createShadowResources()
                renderShadowMap(floorShadow, shadowCamera) {
                    drawAllMeshesForShadow(meshes, useVoxelNormals = use3DOverlay)
                }
            }

            beginScene(settings, request.yaw)
            val shadowMatrix = shadowCamera.shadowMatrix()
            val floorShader = shadowShader(
                style = GlShadowStyle.FLOOR,
                lightingMode = request.lightingMode
            )
            val detailShader = shadowShader(
                style = GlShadowStyle.MODEL_DETAIL,
                lightingMode = request.lightingMode
            )

            if (request.showPlatform) {
                floorShader.use(
                    shadowMatrix = shadowMatrix,
                    shadowTexture = floorShadow?.depthTexture ?: 0,
                    lightDir = lightDir,
                    receiveShadow = request.shadows,
                    useTexture = false
                )
                drawFloor(settings.platformTopY)
            }

            floorShader.use(
                shadowMatrix = shadowMatrix,
                shadowTexture = floorShadow?.depthTexture ?: 0,
                lightDir = lightDir,
                receiveShadow = false,
                useTexture = true
            )
            glEnable(GL_TEXTURE_2D)
            drawTexturedMeshes(meshes, textureIds)
            glDisable(GL_TEXTURE_2D)

            // 角色本体只负责投影，避免 shadow map 自采样在模型表面形成旋转闪烁的三角色块。
            val solidReceiveDetailShadow = false
            detailShader.use(
                shadowMatrix = shadowMatrix,
                shadowTexture = floorShadow?.depthTexture ?: 0,
                lightDir = lightDir,
                receiveShadow = solidReceiveDetailShadow,
                useTexture = false
            )
            drawSolidMeshes(meshes, transparentPass = false, correctNormalByVoxelCenter = use3DOverlay)

            detailShader.use(
                shadowMatrix = shadowMatrix,
                shadowTexture = floorShadow?.depthTexture ?: 0,
                lightDir = lightDir,
                receiveShadow = solidReceiveDetailShadow,
                useTexture = false
            )
            glDepthMask(false)
            drawSolidMeshes(meshes, transparentPass = true, correctNormalByVoxelCenter = use3DOverlay)
            glDepthMask(true)
            glUseProgram(0)
        } finally {
            floorShadow?.close()
        }
    }

    private fun beginScene(settings: SkinRenderSettings, yaw: Float) {
        val colorSsaa = settings.colorSsaa()
        glViewport(0, 0, settings.width * colorSsaa, settings.height * colorSsaa)
        glClearColor(
            SkiaColor.getR(settings.backgroundColor) / 255f,
            SkiaColor.getG(settings.backgroundColor) / 255f,
            SkiaColor.getB(settings.backgroundColor) / 255f,
            SkiaColor.getA(settings.backgroundColor) / 255f
        )
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_LEQUAL)
        glEnable(GL_BLEND)
        // 透明底图合成需要保留覆盖后的真实 alpha，不能让 alpha 再乘一次自身。
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
        glDisable(GL_CULL_FACE)

        glMatrixMode(GL_PROJECTION)
        glLoadMatrixf(perspective(45f, settings.width.toFloat() / settings.height, 0.1f, 200f))
        glMatrixMode(GL_MODELVIEW)
        glLoadMatrixf(lookAt(orbitEye(yaw, settings), settings.target.toGlVec3(), GlVec3(0f, 1f, 0f)))
    }

    private fun createShadowCamera(settings: SkinRenderSettings, lightDir: GlVec3): GlShadowCamera {
        val target = settings.target.toGlVec3()
        val radius = max(24f, settings.distance * 0.45f)
        val lightDistance = max(70f, settings.distance * 1.25f)
        return GlShadowCamera(
            projection = orthographic(-radius, radius, -radius, radius, 0.1f, lightDistance * 2.4f),
            view = lookAt(target + (lightDir * lightDistance), target, GlVec3(0f, 1f, 0f))
        )
    }

    private fun shadowShader(style: GlShadowStyle, lightingMode: SkinLightingMode): GlShadowShader =
        shaders.getOrPut(GlShaderKey(style, lightingMode)) {
            GlShadowShader.create(style, lightingMode)
        }

    private fun drawAllMeshesForShadow(meshes: List<SkinMesh>, useVoxelNormals: Boolean) {
        meshes.filter { it.texture != null }.forEach { drawTexturedMesh(it) }
        glDisable(GL_CULL_FACE)
        drawSolidMeshes(meshes, transparentPass = false, correctNormalByVoxelCenter = useVoxelNormals)
        drawSolidMeshes(meshes, transparentPass = true, correctNormalByVoxelCenter = useVoxelNormals)
        glEnable(GL_CULL_FACE)
        glCullFace(GL_FRONT)
    }

    private fun drawSolidMeshes(
        meshes: List<SkinMesh>,
        transparentPass: Boolean,
        correctNormalByVoxelCenter: Boolean
    ) {
        meshes.filter { it.texture == null }.forEach {
            drawSolidMesh(it, transparentPass, correctNormalByVoxelCenter)
        }
    }

    private fun drawFloor(y: Float) {
        val halfSize = 12f
        glColor4f(0.44f, 0.53f, 0.62f, 1f)
        glNormal3f(0f, 1f, 0f)
        glBegin(GL_QUADS)
        glVertex3f(-halfSize, y, -halfSize)
        glVertex3f(halfSize, y, -halfSize)
        glVertex3f(halfSize, y, halfSize)
        glVertex3f(-halfSize, y, halfSize)
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

    private fun uploadTextures(meshes: List<SkinMesh>): Map<Bitmap, Int> {
        val textureIds = IdentityHashMap<Bitmap, Int>()
        meshes.mapNotNull { it.texture }.forEach { texture ->
            if (!textureIds.containsKey(texture)) textureIds[texture] = uploadTexture(texture)
        }
        return textureIds
    }

    private fun drawTexturedMeshes(meshes: List<SkinMesh>, textureIds: Map<Bitmap, Int>) {
        meshes.filter { it.texture != null }.forEach { mesh ->
            val texture = mesh.texture ?: return@forEach
            glBindTexture(GL_TEXTURE_2D, textureIds.getValue(texture))
            drawTexturedMesh(mesh)
        }
    }

    private fun drawTexturedMesh(mesh: SkinMesh) {
        glColor4f(1f, 1f, 1f, 1f)
        glBegin(GL_TRIANGLES)
        mesh.faces.forEach { face ->
            if (face.indices.size < 3) return@forEach
            val normal = face.stableNormal(mesh)
            for (i in 1 until face.indices.size - 1) {
                glNormal3f(normal.x, normal.y, normal.z)
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

    private fun drawSolidMesh(mesh: SkinMesh, transparentPass: Boolean, correctNormalByVoxelCenter: Boolean) {
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
            val normal = face.stableNormal(mesh, normalCenter)
            for (i in 1 until face.indices.size - 1) {
                glNormal3f(normal.x, normal.y, normal.z)
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

    private fun SkinMeshFace.stableNormal(mesh: SkinMesh, normalCenter: GlVec3? = null): GlVec3 {
        if (normalCenter != null) {
            // 体素边缘可能收缩成非平面四边形，用整面中心保证拆分出的两个三角形亮度一致。
            val center = center(mesh)
            val outward = (center - normalCenter).normalized()
            if (outward.length() > 0f) return outward
        }

        val p0 = mesh.vertices[indices[0]].position
        val p1 = mesh.vertices[indices[1]].position
        val p2 = mesh.vertices[indices[2]].position
        var normal = (GlVec3(p1.x - p0.x, p1.y - p0.y, p1.z - p0.z)
            .cross(GlVec3(p2.x - p0.x, p2.y - p0.y, p2.z - p0.z)))
            .normalized()
        if (normalCenter != null && normal.dot(center(mesh) - normalCenter) < 0f) {
            normal = -normal
        }
        return normal
    }

    private fun SkinMeshFace.center(mesh: SkinMesh): GlVec3 {
        var x = 0f
        var y = 0f
        var z = 0f
        indices.forEach { index ->
            val position = mesh.vertices[index].position
            x += position.x
            y += position.y
            z += position.z
        }
        val count = indices.size.toFloat()
        return GlVec3(x / count, y / count, z / count)
    }

    private fun SkinMeshFace.boundsCenter(mesh: SkinMesh): GlVec3 {
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
        return GlVec3((minX + maxX) / 2f, (minY + maxY) / 2f, (minZ + maxZ) / 2f)
    }

    private fun SkinMeshFace.voxelBoundsCenter(mesh: SkinMesh): GlVec3 {
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
        return GlVec3((minX + maxX) / 2f, (minY + maxY) / 2f, (minZ + maxZ) / 2f)
    }
}

private fun createShadowResources(): GlShadowResources {
    val previousFramebuffer = glGetInteger(GL_FRAMEBUFFER_BINDING)
    val depthTexture = glGenTextures()
    glBindTexture(GL_TEXTURE_2D, depthTexture)
    glTexImage2D(
        GL_TEXTURE_2D,
        0,
        GL_DEPTH_COMPONENT,
        GL_SHADOW_SIZE,
        GL_SHADOW_SIZE,
        0,
        GL_DEPTH_COMPONENT,
        GL_FLOAT,
        null as ByteBuffer?
    )
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

    val framebuffer = glGenFramebuffers()
    glBindFramebuffer(GL_FRAMEBUFFER, framebuffer)
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTexture, 0)
    glDrawBuffer(GL_NONE)
    glReadBuffer(GL_NONE)
    check(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE) { "Shadow framebuffer is incomplete" }
    glBindFramebuffer(GL_FRAMEBUFFER, previousFramebuffer)
    return GlShadowResources(framebuffer, depthTexture)
}

private fun createColorTarget(width: Int, height: Int): GlColorTarget {
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
    return GlColorTarget(framebuffer, colorTexture, depthBuffer, width, height)
}

private fun renderShadowMap(
    resources: GlShadowResources,
    camera: GlShadowCamera,
    cullFrontFaces: Boolean = true,
    polygonOffset: GlPolygonOffset? = GlPolygonOffset(0.1f, 0.25f),
    drawCasters: () -> Unit
) {
    val previousFramebuffer = glGetInteger(GL_FRAMEBUFFER_BINDING)
    glBindFramebuffer(GL_FRAMEBUFFER, resources.framebuffer)
    glViewport(0, 0, GL_SHADOW_SIZE, GL_SHADOW_SIZE)
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

private data class PreparedSkinScene(
    val meshes: List<SkinMesh>,
)

private data class GlShaderKey(
    val style: GlShadowStyle,
    val lightingMode: SkinLightingMode,
)

private data class GlVec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: GlVec3): GlVec3 = GlVec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: GlVec3): GlVec3 = GlVec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scale: Float): GlVec3 = GlVec3(x * scale, y * scale, z * scale)
    operator fun unaryMinus(): GlVec3 = GlVec3(-x, -y, -z)

    fun cross(other: GlVec3): GlVec3 =
        GlVec3(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x)

    fun dot(other: GlVec3): Float = x * other.x + y * other.y + z * other.z

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun normalized(): GlVec3 {
        val length = length()
        return if (length > 0f) GlVec3(x / length, y / length, z / length) else this
    }
}

private data class GlPolygonOffset(val factor: Float, val units: Float)

private data class GlShadowResources(val framebuffer: Int, val depthTexture: Int) {
    fun close() {
        glDeleteFramebuffers(framebuffer)
        glDeleteTextures(depthTexture)
    }
}

private data class GlColorTarget(
    val framebuffer: Int,
    val colorTexture: Int,
    val depthBuffer: Int,
    val width: Int,
    val height: Int,
) {
    fun close() {
        glDeleteFramebuffers(framebuffer)
        glDeleteTextures(colorTexture)
        glDeleteRenderbuffers(depthBuffer)
    }
}

private data class GlShadowCamera(val projection: FloatArray, val view: FloatArray) {
    fun shadowMatrix(): FloatArray = multiply(multiply(biasMatrix(), projection), view)
}

private data class GlShadowStyle(
    val biasMin: Float,
    val biasSlope: Float,
    val strength: Float,
    val pcfRadius: Float,
    val occlusionDivisor: Float,
) {
    companion object {
        val FLOOR = GlShadowStyle(0.0008f, 0.0030f, 0.62f, 1.0f, 4.0f)
        val MODEL_DETAIL = GlShadowStyle(0.00018f, 0.00055f, 0.58f, 0.20f, 1.8f)
    }
}

private class GlShadowShader private constructor(
    private val program: Int,
    private val shadowMatrixLocation: Int,
    private val lightDirLocation: Int,
    private val useTextureLocation: Int,
    private val receiveShadowLocation: Int,
) {
    fun use(
        shadowMatrix: FloatArray,
        shadowTexture: Int,
        lightDir: GlVec3,
        receiveShadow: Boolean,
        useTexture: Boolean,
        skinTexture: Int = 0,
    ) {
        glUseProgram(program)
        glUniformMatrix4fv(shadowMatrixLocation, false, shadowMatrix)
        glUniform3f(lightDirLocation, lightDir.x, lightDir.y, lightDir.z)
        glUniform1i(useTextureLocation, if (useTexture) 1 else 0)
        glUniform1i(receiveShadowLocation, if (receiveShadow) 1 else 0)
        glActiveTexture(GL_TEXTURE1)
        glBindTexture(GL_TEXTURE_2D, shadowTexture)
        glActiveTexture(GL_TEXTURE0)
        if (skinTexture != 0) glBindTexture(GL_TEXTURE_2D, skinTexture)
    }

    fun close() {
        glDeleteProgram(program)
    }

    companion object {
        fun create(
            style: GlShadowStyle,
            lightingMode: SkinLightingMode,
        ): GlShadowShader {
            val ambientOnly = lightingMode == SkinLightingMode.AMBIENT
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
                    vLight = ${if (ambientOnly) "1.0" else "0.46 + diffuse * 0.54"};
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
                        vec2 texelSize = vec2(1.0 / ${GL_SHADOW_SIZE}.0, 1.0 / ${GL_SHADOW_SIZE}.0);
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
            glDeleteShader(vertexShader)
            glDeleteShader(fragmentShader)
            check(glGetProgrami(program, GL_LINK_STATUS) != 0) { glGetProgramInfoLog(program) }

            glUseProgram(program)
            glUniform1i(glGetUniformLocation(program, "uSkinTexture"), 0)
            glUniform1i(glGetUniformLocation(program, "uShadowMap"), 1)

            return GlShadowShader(
                program = program,
                shadowMatrixLocation = glGetUniformLocation(program, "uShadowMatrix"),
                lightDirLocation = glGetUniformLocation(program, "uLightDir"),
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

private fun SkinRenderVec3.toGlVec3(): GlVec3 = GlVec3(x, y, z)

private fun SkinRenderSettings.colorSsaa(): Int =
    antiAliasingLevel.coerceAtLeast(1)

private fun List<SkinRenderRequest>.canReusePreparedScene(): Boolean {
    val first = first()
    return all {
            it.skinPng.contentEquals(first.skinPng) &&
            it.capePng.contentEqualsNullable(first.capePng) &&
            it.isSlim == first.isSlim &&
            it.overlayMode == first.overlayMode &&
            it.pose == first.pose
    }
}

private fun ByteArray?.contentEqualsNullable(other: ByteArray?): Boolean =
    if (this == null || other == null) this == other else contentEquals(other)

private fun orbitEye(yawDegrees: Float, settings: SkinRenderSettings): GlVec3 {
    val yaw = Math.toRadians(yawDegrees.toDouble())
    val pitch = Math.toRadians(settings.pitch.toDouble())
    val horizontalDistance = (cos(pitch) * settings.distance).toFloat()
    val target = settings.target.toGlVec3()
    return GlVec3(
        target.x + (sin(yaw) * horizontalDistance).toFloat(),
        target.y + (sin(pitch) * settings.distance).toFloat(),
        target.z + (cos(yaw) * horizontalDistance).toFloat()
    )
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

private fun lookAt(eye: GlVec3, center: GlVec3, up: GlVec3): FloatArray {
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
