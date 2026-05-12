package top.e404.skin.server.service

import top.e404.skin.core.SkinRenderUseCases
import top.e404.skin.renderer.tavolo.TavoloSkinPngRenderer

object TavoloSkinRenderer {
    private val renderer = TavoloSkinPngRenderer().also { it.startup() }

    fun renderSkin(
        bytes: ByteArray,
        slim: Boolean,
        backgroundColor: Int,
        lightColor: Int?,
        headScale: Double,
        showPlatform: Boolean = false,
    ): ByteArray =
        SkinRenderUseCases.renderSkin(renderer, bytes, slim, backgroundColor, lightColor, headScale, showPlatform)

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
    ): ByteArray =
        SkinRenderUseCases.renderSkinRotate(
            renderer = renderer,
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

    fun renderHead(
        bytes: ByteArray,
        backgroundColor: Int,
        lightColor: Int?,
        showPlatform: Boolean = false,
    ): ByteArray =
        SkinRenderUseCases.renderHead(renderer, bytes, backgroundColor, lightColor, showPlatform)

    suspend fun renderHeadRotate(
        bytes: ByteArray,
        backgroundColor: Int,
        frameCount: Int,
        pitchAmplitude: Int,
        lightColor: Int?,
        duration: Int,
        showPlatform: Boolean = false,
    ): ByteArray =
        SkinRenderUseCases.renderHeadRotate(
            renderer = renderer,
            bytes = bytes,
            backgroundColor = backgroundColor,
            frameCount = frameCount,
            pitchAmplitude = pitchAmplitude,
            lightColor = lightColor,
            duration = duration,
            showPlatform = showPlatform
        )

    suspend fun renderSneak(
        bytes: ByteArray,
        slim: Boolean,
        backgroundColor: Int,
        lightColor: Int?,
        headScale: Double,
        duration: Int,
        showPlatform: Boolean = false,
    ): ByteArray =
        SkinRenderUseCases.renderSneak(renderer, bytes, slim, backgroundColor, lightColor, headScale, duration, showPlatform)

    fun renderHomo(
        bytes: ByteArray,
        slim: Boolean,
        backgroundColor: Int,
        lightColor: Int?,
        headScale: Double,
        showPlatform: Boolean = false,
    ): ByteArray =
        SkinRenderUseCases.renderHomo(renderer, bytes, slim, backgroundColor, lightColor, headScale, showPlatform)
}
