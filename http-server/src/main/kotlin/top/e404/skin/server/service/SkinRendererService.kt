package top.e404.skin.server.service

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import top.e404.skin.core.SkinPngRenderer
import top.e404.skin.core.SkinRenderUseCases
import top.e404.skin.server.appLog
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object SkinRendererService {
    private const val TAVOLO_RENDERER_CLASS = "top.e404.skin.renderer.tavolo.TavoloSkinPngRenderer"
    private const val OPENGL_RENDERER_CLASS = "top.e404.skin.renderer.opengl.OpenGlSkinPngRenderer"

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "skin-renderer").apply { isDaemon = true }
    }
    private val dispatcher = executor.asCoroutineDispatcher()
    private val rendererLazy = lazy { createRenderer() }

    val rendererId: String
        get() = configuredRendererId()
            ?: configuredRendererClass()?.substringAfterLast('.')
            ?: autoRendererId()

    suspend fun renderSkin(
        bytes: ByteArray,
        slim: Boolean,
        backgroundColor: Int,
        lightColor: Int?,
        headScale: Double,
        showPlatform: Boolean = false,
    ): ByteArray = withRenderer {
        SkinRenderUseCases.renderSkin(it, bytes, slim, backgroundColor, lightColor, headScale, showPlatform)
    }

    suspend fun renderSkinRotate(
        bytes: ByteArray,
        slim: Boolean,
        backgroundColor: Int,
        frameCount: Int,
        pitchAmplitude: Int,
        lightColor: Int?,
        headScale: Double,
        duration: Int,
        showPlatform: Boolean = true,
    ): ByteArray = withRenderer {
        SkinRenderUseCases.renderSkinRotate(
            renderer = it,
            bytes = bytes,
            slim = slim,
            backgroundColor = backgroundColor,
            frameCount = frameCount,
            pitchAmplitude = pitchAmplitude,
            lightColor = lightColor,
            headScale = headScale,
            duration = duration,
            showPlatform = showPlatform
        )
    }

    suspend fun renderHead(
        bytes: ByteArray,
        backgroundColor: Int,
        lightColor: Int?,
        showPlatform: Boolean = false,
    ): ByteArray = withRenderer {
        SkinRenderUseCases.renderHead(it, bytes, backgroundColor, lightColor, showPlatform)
    }

    suspend fun renderHeadRotate(
        bytes: ByteArray,
        backgroundColor: Int,
        frameCount: Int,
        pitchAmplitude: Int,
        lightColor: Int?,
        duration: Int,
        showPlatform: Boolean = false,
    ): ByteArray = withRenderer {
        SkinRenderUseCases.renderHeadRotate(
            renderer = it,
            bytes = bytes,
            backgroundColor = backgroundColor,
            frameCount = frameCount,
            pitchAmplitude = pitchAmplitude,
            lightColor = lightColor,
            duration = duration,
            showPlatform = showPlatform
        )
    }

    suspend fun renderSneak(
        bytes: ByteArray,
        slim: Boolean,
        backgroundColor: Int,
        lightColor: Int?,
        headScale: Double,
        duration: Int,
        showPlatform: Boolean = false,
    ): ByteArray = withRenderer {
        SkinRenderUseCases.renderSneak(it, bytes, slim, backgroundColor, lightColor, headScale, duration, showPlatform)
    }

    suspend fun renderHomo(
        bytes: ByteArray,
        slim: Boolean,
        backgroundColor: Int,
        lightColor: Int?,
        headScale: Double,
        showPlatform: Boolean = false,
    ): ByteArray = withRenderer {
        SkinRenderUseCases.renderHomo(it, bytes, slim, backgroundColor, lightColor, headScale, showPlatform)
    }

    fun shutdown() {
        try {
            if (rendererLazy.isInitialized()) {
                executor.submit { rendererLazy.value.close() }.get(5, TimeUnit.SECONDS)
            }
        } finally {
            dispatcher.close()
            executor.shutdown()
        }
    }

    private suspend fun <T> withRenderer(block: suspend (SkinPngRenderer) -> T): T =
        withContext(dispatcher) { block(rendererLazy.value) }

    private fun createRenderer(): SkinPngRenderer {
        val configuredClass = configuredRendererClass()
        val classNames = if (configuredClass != null) {
            listOf(configuredClass)
        } else {
            listOf(TAVOLO_RENDERER_CLASS, OPENGL_RENDERER_CLASS)
        }
        val failures = mutableListOf<String>()
        for (className in classNames) {
            try {
                val renderer = Class.forName(className)
                    .getDeclaredConstructor()
                    .newInstance() as SkinPngRenderer
                renderer.startup()
                appLog.info("Using skin renderer ${renderer.name} ($className)")
                return renderer
            } catch (e: ClassNotFoundException) {
                failures += "$className: class not found"
            } catch (e: Throwable) {
                failures += "$className: ${e.message ?: e::class.java.name}"
            }
        }
        error("No skin renderer is available. Tried: ${failures.joinToString("; ")}")
    }

    private fun configuredRendererClass(): String? =
        System.getProperty("skin.renderer.class")
            ?: System.getenv("SKIN_RENDERER_CLASS")

    private fun configuredRendererId(): String? =
        System.getProperty("skin.renderer.id")
            ?: System.getenv("SKIN_RENDERER_ID")

    private fun autoRendererId(): String =
        when {
            isClassAvailable(TAVOLO_RENDERER_CLASS) -> "tavolo-cpu-v1"
            isClassAvailable(OPENGL_RENDERER_CLASS) -> "opengl-lwjgl-fbo"
            else -> "unconfigured-renderer"
        }

    private fun isClassAvailable(className: String): Boolean =
        try {
            Class.forName(className, false, SkinRendererService::class.java.classLoader)
            true
        } catch (_: ClassNotFoundException) {
            false
        }
}
