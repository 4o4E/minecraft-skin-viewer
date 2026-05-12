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
    ): ByteArray =
        SkinRenderUseCases.renderSkin(renderer, bytes, slim, backgroundColor, lightColor, headScale)

    suspend fun renderSkinRotate(
        bytes: ByteArray,
        slim: Boolean,
        backgroundColor: Int,
        frameCount: Int,
        pitchAmplitude: Int,
        lightColor: Int?,
        headScale: Double,
        duration: Int,
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
            duration = duration
        )

    fun renderHead(
        bytes: ByteArray,
        backgroundColor: Int,
        lightColor: Int?,
    ): ByteArray =
        SkinRenderUseCases.renderHead(renderer, bytes, backgroundColor, lightColor)

    suspend fun renderHeadRotate(
        bytes: ByteArray,
        backgroundColor: Int,
        frameCount: Int,
        pitchAmplitude: Int,
        lightColor: Int?,
        duration: Int,
    ): ByteArray =
        SkinRenderUseCases.renderHeadRotate(
            renderer = renderer,
            bytes = bytes,
            backgroundColor = backgroundColor,
            frameCount = frameCount,
            pitchAmplitude = pitchAmplitude,
            lightColor = lightColor,
            duration = duration
        )

    suspend fun renderSneak(
        bytes: ByteArray,
        slim: Boolean,
        backgroundColor: Int,
        lightColor: Int?,
        headScale: Double,
        duration: Int,
    ): ByteArray =
        SkinRenderUseCases.renderSneak(renderer, bytes, slim, backgroundColor, lightColor, headScale, duration)

    fun renderHomo(
        bytes: ByteArray,
        slim: Boolean,
        backgroundColor: Int,
        lightColor: Int?,
        headScale: Double,
    ): ByteArray =
        SkinRenderUseCases.renderHomo(renderer, bytes, slim, backgroundColor, lightColor, headScale)
}
