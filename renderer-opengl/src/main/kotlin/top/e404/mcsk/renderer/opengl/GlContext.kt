package top.e404.mcsk.renderer.opengl

import org.lwjgl.egl.EGL
import org.lwjgl.opengl.GL
import org.lwjgl.system.Configuration
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
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
import org.lwjgl.egl.EGL10.EGL_ALPHA_SIZE
import org.lwjgl.egl.EGL10.EGL_BLUE_SIZE
import org.lwjgl.egl.EGL10.EGL_DEPTH_SIZE
import org.lwjgl.egl.EGL10.EGL_GREEN_SIZE
import org.lwjgl.egl.EGL10.EGL_HEIGHT
import org.lwjgl.egl.EGL10.EGL_NONE
import org.lwjgl.egl.EGL10.EGL_NO_CONTEXT
import org.lwjgl.egl.EGL10.EGL_NO_DISPLAY
import org.lwjgl.egl.EGL10.EGL_NO_SURFACE
import org.lwjgl.egl.EGL10.EGL_PBUFFER_BIT
import org.lwjgl.egl.EGL10.EGL_RED_SIZE
import org.lwjgl.egl.EGL10.EGL_SURFACE_TYPE
import org.lwjgl.egl.EGL10.EGL_VENDOR
import org.lwjgl.egl.EGL10.EGL_WIDTH
import org.lwjgl.egl.EGL10.eglChooseConfig
import org.lwjgl.egl.EGL10.eglCreateContext
import org.lwjgl.egl.EGL10.eglCreatePbufferSurface
import org.lwjgl.egl.EGL10.eglDestroyContext
import org.lwjgl.egl.EGL10.eglDestroySurface
import org.lwjgl.egl.EGL10.eglGetDisplay
import org.lwjgl.egl.EGL10.eglGetError
import org.lwjgl.egl.EGL10.eglInitialize
import org.lwjgl.egl.EGL10.eglMakeCurrent
import org.lwjgl.egl.EGL10.eglQueryString
import org.lwjgl.egl.EGL10.eglTerminate
import org.lwjgl.egl.EGL12.eglBindAPI
import org.lwjgl.egl.EGL12.EGL_RENDERABLE_TYPE
import org.lwjgl.egl.EGL14.EGL_DEFAULT_DISPLAY
import org.lwjgl.egl.EGL14.EGL_OPENGL_API
import org.lwjgl.egl.EGL14.EGL_OPENGL_BIT
import org.lwjgl.egl.EXTDeviceEnumeration.eglQueryDevicesEXT
import org.lwjgl.egl.EXTPlatformBase.eglGetPlatformDisplayEXT
import org.lwjgl.egl.EXTPlatformDevice.EGL_PLATFORM_DEVICE_EXT

/**
 * 隔离窗口系统和无头 EGL 的上下文生命周期，使同一渲染器可按配置切换后端。
 */
internal interface GlContext : AutoCloseable {
    val backendName: String

    fun makeCurrent()

    companion object {
        fun createConfigured(): GlContext = when (
            (System.getenv("MCSK_GL_BACKEND") ?: System.getProperty("mcsk.gl.backend") ?: "glfw").lowercase()
        ) {
            "glfw", "xvfb" -> GlfwGlContext.create()
            "egl", "nvidia" -> EglGlContext.create()
            else -> error("Unsupported OpenGL backend. Expected glfw, xvfb, egl or nvidia")
        }
    }
}

private class GlfwGlContext(
    private var window: Long,
) : GlContext {
    override val backendName: String = "glfw"

    override fun makeCurrent() {
        glfwMakeContextCurrent(window)
    }

    override fun close() {
        if (window == 0L) return
        glfwDestroyWindow(window)
        window = 0L
        glfwTerminate()
    }

    companion object {
        fun create(): GlfwGlContext {
            check(glfwInit()) { "Failed to initialize GLFW" }
            glfwDefaultWindowHints()
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0)
            val window = glfwCreateWindow(1, 1, "OpenGL Skin Renderer", 0, 0)
            check(window != 0L) { "Failed to create GLFW window/OpenGL context" }
            return GlfwGlContext(window).also { it.makeCurrent() }
        }
    }
}

private class EglGlContext(
    private var display: Long,
    private var surface: Long,
    private var context: Long,
) : GlContext {
    override val backendName: String = "egl"

    override fun makeCurrent() {
        check(eglMakeCurrent(display, surface, surface, context)) {
            "Failed to make EGL context current: ${eglError()}"
        }
    }

    override fun close() {
        if (display == EGL_NO_DISPLAY) return
        GL.setCapabilities(null)
        eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT)
        if (context != EGL_NO_CONTEXT) eglDestroyContext(display, context)
        if (surface != EGL_NO_SURFACE) eglDestroySurface(display, surface)
        eglTerminate(display)
        context = EGL_NO_CONTEXT
        surface = EGL_NO_SURFACE
        display = EGL_NO_DISPLAY
        EGL.destroy()
    }

    companion object {
        fun create(): EglGlContext {
            Configuration.OPENGL_CONTEXT_API.set("EGL")
            stackPush().use { stack ->
                val initializedDisplay = initializeDisplay(stack)
                val display = initializedDisplay.display
                try {
                    EGL.createDisplayCapabilities(display, initializedDisplay.major, initializedDisplay.minor)
                    check(eglBindAPI(EGL_OPENGL_API)) { "Failed to bind desktop OpenGL API: ${eglError()}" }

                    val configAttributes = stack.ints(
                        EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
                        EGL_RENDERABLE_TYPE, EGL_OPENGL_BIT,
                        EGL_RED_SIZE, 8,
                        EGL_GREEN_SIZE, 8,
                        EGL_BLUE_SIZE, 8,
                        EGL_ALPHA_SIZE, 8,
                        EGL_DEPTH_SIZE, 24,
                        EGL_NONE,
                    )
                    val configs = stack.mallocPointer(1)
                    val configCount = stack.mallocInt(1)
                    check(eglChooseConfig(display, configAttributes, configs, configCount)) {
                        "Failed to choose EGL config: ${eglError()}"
                    }
                    check(configCount[0] > 0) { "No EGL config supports desktop OpenGL pbuffer rendering" }

                    val surfaceAttributes = stack.ints(EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE)
                    val surface = eglCreatePbufferSurface(display, configs[0], surfaceAttributes)
                    check(surface != EGL_NO_SURFACE) { "Failed to create EGL pbuffer surface: ${eglError()}" }
                    val context = eglCreateContext(display, configs[0], EGL_NO_CONTEXT, stack.ints(EGL_NONE))
                    check(context != EGL_NO_CONTEXT) { "Failed to create EGL OpenGL context: ${eglError()}" }

                    return EglGlContext(display, surface, context).also { it.makeCurrent() }
                } catch (error: Throwable) {
                    eglTerminate(display)
                    EGL.destroy()
                    throw error
                }
            }
        }

        /**
         * NVIDIA 无头环境没有默认显示器，优先通过 EGLDevice 选择显卡，再回退默认显示器。
         */
        private fun initializeDisplay(stack: MemoryStack): InitializedDisplay {
            val expectedVendor = System.getenv("MCSK_GL_EXPECT_VENDOR")?.takeIf { it.isNotBlank() }
            val clientCapabilities = EGL.getCapabilities()
            if (clientCapabilities.EGL_EXT_device_enumeration && clientCapabilities.EGL_EXT_platform_device) {
                val devices = stack.mallocPointer(16)
                val deviceCount = stack.mallocInt(1)
                if (eglQueryDevicesEXT(devices, deviceCount)) {
                    for (index in 0 until minOf(deviceCount[0], devices.capacity())) {
                        val display = eglGetPlatformDisplayEXT(EGL_PLATFORM_DEVICE_EXT, devices[index], null as IntArray?)
                        val initialized = tryInitializeDisplay(stack, display, expectedVendor)
                        if (initialized != null) return initialized
                    }
                }
            }

            val display = eglGetDisplay(EGL_DEFAULT_DISPLAY)
            return requireNotNull(tryInitializeDisplay(stack, display, expectedVendor)) {
                "Failed to initialize EGL display: ${eglError()}"
            }
        }

        private fun tryInitializeDisplay(
            stack: MemoryStack,
            display: Long,
            expectedVendor: String?,
        ): InitializedDisplay? {
            if (display == EGL_NO_DISPLAY) return null
            val major = stack.mallocInt(1)
            val minor = stack.mallocInt(1)
            if (!eglInitialize(display, major, minor)) return null
            val vendor = eglQueryString(display, EGL_VENDOR).orEmpty()
            if (expectedVendor != null && !vendor.contains(expectedVendor, ignoreCase = true)) {
                eglTerminate(display)
                return null
            }
            return InitializedDisplay(display, major[0], minor[0])
        }

        private data class InitializedDisplay(
            val display: Long,
            val major: Int,
            val minor: Int,
        )

        private fun eglError(): String = "0x${eglGetError().toString(16)}"
    }
}
