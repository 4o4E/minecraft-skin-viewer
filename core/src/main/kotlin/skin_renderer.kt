package top.e404.skin.core

data class SkinRenderVec3(val x: Float, val y: Float, val z: Float) {
    fun normalized(): SkinRenderVec3 {
        val length = kotlin.math.sqrt(x * x + y * y + z * z)
        return if (length > 0f) SkinRenderVec3(x / length, y / length, z / length) else this
    }
}

enum class SkinOverlayMode {
    NONE,
    FLAT,
    THREE_D
}

enum class SkinLightingMode {
    AMBIENT,
    DIRECTIONAL
}

data class SkinRenderSettings(
    val width: Int,
    val height: Int,
    val target: SkinRenderVec3,
    val pitch: Float,
    val distance: Float,
    val backgroundColor: Int,
    val lightDirection: SkinRenderVec3,
    val platformTopY: Float,
    val platformThickness: Float,
)

data class SkinRenderRequest(
    val skinPng: ByteArray,
    val isSlim: Boolean,
    val yaw: Float,
    val settings: SkinRenderSettings,
    val overlayMode: SkinOverlayMode,
    val lightingMode: SkinLightingMode,
    val shadows: Boolean,
)

interface SkinPngRenderer : AutoCloseable {
    val name: String

    fun startup()

    fun renderPng(request: SkinRenderRequest): ByteArray

    override fun close() = Unit
}
