package top.e404.mcsk.server.service

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import top.e404.mcsk.core.SkinPngRenderer
import top.e404.mcsk.core.SkinRenderOptions
import top.e404.mcsk.core.SkinRenderUseCases
import top.e404.mcsk.server.appLog
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

object SkinRendererService {
    private const val TAVOLO_RENDERER_CLASS = "top.e404.mcsk.renderer.tavolo.TavoloSkinPngRenderer"
    private const val OPENGL_RENDERER_CLASS = "top.e404.mcsk.renderer.opengl.OpenGlSkinPngRenderer"

    private val backendLazy = lazy { createBackend() }

    val rendererId: String
        get() = configuredRendererId()
            ?: configuredRendererClass()?.substringAfterLast('.')
            ?: autoRendererId()

    suspend fun renderSkin(
        bytes: ByteArray,
        slim: Boolean,
        headScale: Double,
        options: SkinRenderOptions,
        capeBytes: ByteArray? = null,
    ): ByteArray = withRenderer {
        SkinRenderUseCases.renderSkin(it, bytes, slim, headScale, options, capeBytes)
    }

    suspend fun renderSkinRotate(
        bytes: ByteArray,
        slim: Boolean,
        frameCount: Int,
        headScale: Double,
        duration: Int,
        options: SkinRenderOptions,
        capeBytes: ByteArray? = null,
    ): ByteArray = withRenderer {
        SkinRenderUseCases.renderSkinRotate(
            renderer = it,
            bytes = bytes,
            capeBytes = capeBytes,
            slim = slim,
            frameCount = frameCount,
            headScale = headScale,
            duration = duration,
            options = options
        )
    }

    suspend fun renderHead(
        bytes: ByteArray,
        options: SkinRenderOptions,
    ): ByteArray = withRenderer {
        SkinRenderUseCases.renderHead(it, bytes, options)
    }

    suspend fun renderHeadRotate(
        bytes: ByteArray,
        frameCount: Int,
        duration: Int,
        options: SkinRenderOptions,
    ): ByteArray = withRenderer {
        SkinRenderUseCases.renderHeadRotate(
            renderer = it,
            bytes = bytes,
            frameCount = frameCount,
            duration = duration,
            options = options
        )
    }

    suspend fun renderSneak(
        bytes: ByteArray,
        slim: Boolean,
        headScale: Double,
        duration: Int = SkinRenderUseCases.SNEAK_FRAME_DURATION_MS,
        options: SkinRenderOptions,
        capeBytes: ByteArray? = null,
    ): ByteArray = withRenderer {
        SkinRenderUseCases.renderSneak(it, bytes, slim, headScale, duration, options, capeBytes)
    }

    suspend fun renderHomo(
        bytes: ByteArray,
        slim: Boolean,
        headScale: Double,
        options: SkinRenderOptions,
        capeBytes: ByteArray? = null,
    ): ByteArray = withRenderer {
        SkinRenderUseCases.renderHomo(it, bytes, slim, headScale, options, capeBytes)
    }

    fun shutdown() {
        if (backendLazy.isInitialized()) backendLazy.value.shutdown()
    }

    private suspend fun <T> withRenderer(block: suspend (SkinPngRenderer) -> T): T =
        backendLazy.value.withRenderer(block)

    private fun createBackend(): RendererBackend {
        configuredRendererClass()?.let { return createBackendOrThrow(it) }

        val classNames = listOf(TAVOLO_RENDERER_CLASS, OPENGL_RENDERER_CLASS)
        val failures = mutableListOf<String>()
        for (className in classNames) {
            try {
                if (!isClassAvailable(className)) {
                    failures += "$className: class not found"
                    continue
                }
                return createBackendFor(className)
            } catch (e: ClassNotFoundException) {
                failures += "$className: class not found"
            } catch (e: Throwable) {
                failures += "$className: ${e.message ?: e::class.java.name}"
            }
        }
        error("No skin renderer is available. Tried: ${failures.joinToString("; ")}")
    }

    private fun createBackendOrThrow(className: String): RendererBackend =
        try {
            createBackendFor(className)
        } catch (e: Throwable) {
            error("Failed to initialize skin renderer $className: ${e.message ?: e::class.java.name}")
        }

    private fun createBackendFor(className: String): RendererBackend =
        if (className == OPENGL_RENDERER_CLASS) {
            val executor = Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "mcsk-renderer-opengl").apply { isDaemon = true }
            }
            createSingleRendererBackend(className, executor)
        } else if (className == TAVOLO_RENDERER_CLASS) {
            val threads = configuredRendererThreads() ?: defaultTavoloThreads()
            val executor = Executors.newFixedThreadPool(threads.coerceAtLeast(1)) { runnable ->
                Thread(runnable, "mcsk-renderer-tavolo").apply { isDaemon = true }
            }
            createPooledRendererBackend(className, executor)
        } else {
            val executor = Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "mcsk-renderer-custom").apply { isDaemon = true }
            }
            createSingleRendererBackend(className, executor)
        }

    private fun createSingleRendererBackend(className: String, executor: ExecutorService): RendererBackend =
        try {
            SingleRendererBackend(className, executor).also { it.startup() }
        } catch (e: Throwable) {
            executor.shutdown()
            throw e
        }

    private fun createPooledRendererBackend(className: String, executor: ExecutorService): RendererBackend =
        try {
            PooledRendererBackend(className, executor).also { it.startup() }
        } catch (e: Throwable) {
            executor.shutdown()
            throw e
        }

    private fun createRenderer(className: String): SkinPngRenderer {
        val renderer = Class.forName(className)
            .getDeclaredConstructor()
            .newInstance() as SkinPngRenderer
        renderer.startup()
        return renderer
    }

    private fun configuredRendererClass(): String? =
        System.getProperty("mcsk.renderer.class")
            ?: System.getenv("MCSK_RENDERER_CLASS")

    private fun configuredRendererThreads(): Int? =
        System.getProperty("mcsk.renderer.threads")
            ?.toIntOrNull()
            ?: System.getenv("MCSK_RENDERER_THREADS")?.toIntOrNull()

    private fun defaultTavoloThreads(): Int =
        Runtime.getRuntime().availableProcessors().coerceIn(1, 4)

    private fun configuredRendererId(): String? =
        System.getProperty("mcsk.renderer.id")
            ?: System.getenv("MCSK_RENDERER_ID")

    private fun rendererIdFor(className: String): String =
        when {
            className == TAVOLO_RENDERER_CLASS -> "tavolo-cpu-v1"
            className == OPENGL_RENDERER_CLASS -> "opengl-lwjgl-fbo"
            else -> className.substringAfterLast('.')
        }

    private fun autoRendererId(): String =
        when {
            isClassAvailable(TAVOLO_RENDERER_CLASS) -> rendererIdFor(TAVOLO_RENDERER_CLASS)
            isClassAvailable(OPENGL_RENDERER_CLASS) -> rendererIdFor(OPENGL_RENDERER_CLASS)
            else -> "unconfigured-renderer"
        }

    private fun isClassAvailable(className: String): Boolean =
        try {
            Class.forName(className, false, SkinRendererService::class.java.classLoader)
            true
        } catch (_: ClassNotFoundException) {
            false
        }

    private interface RendererBackend {
        val rendererId: String

        fun startup()

        suspend fun <T> withRenderer(block: suspend (SkinPngRenderer) -> T): T

        fun shutdown()
    }

    private class SingleRendererBackend(
        private val className: String,
        private val executor: ExecutorService,
    ) : RendererBackend {
        private val dispatcher = executor.asCoroutineDispatcher()
        private val renderer = executor.submit<SkinPngRenderer> {
            createRenderer(className)
        }.get()

        override val rendererId = rendererIdFor(className)

        override fun startup() {
            appLog.info("Using skin renderer ${renderer.name} ($className)")
        }

        override suspend fun <T> withRenderer(block: suspend (SkinPngRenderer) -> T): T =
            withContext(dispatcher) { block(renderer) }

        override fun shutdown() {
            try {
                executor.submit { renderer.close() }.get(5, TimeUnit.SECONDS)
            } finally {
                dispatcher.close()
                executor.shutdown()
            }
        }
    }

    private class PooledRendererBackend(
        private val className: String,
        private val executor: ExecutorService,
    ) : RendererBackend {
        private val dispatcher = executor.asCoroutineDispatcher()
        private val renderers = ConcurrentLinkedQueue<SkinPngRenderer>()
        private val threadRenderer = ThreadLocal.withInitial {
            createRenderer(className).also { renderers.add(it) }
        }
        private val startupRenderer = createRenderer(className)

        override val rendererId = rendererIdFor(className)

        override fun startup() {
            appLog.info("Using skin renderer ${startupRenderer.name} ($className)")
            startupRenderer.close()
        }

        override suspend fun <T> withRenderer(block: suspend (SkinPngRenderer) -> T): T =
            withContext(dispatcher) { block(threadRenderer.get()) }

        override fun shutdown() {
            try {
                renderers.forEach { renderer -> renderer.close() }
            } finally {
                dispatcher.close()
                executor.shutdown()
            }
        }
    }
}
