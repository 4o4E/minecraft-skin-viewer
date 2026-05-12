package top.e404.skin.core

data class SkinRenderVec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: SkinRenderVec3): SkinRenderVec3 = SkinRenderVec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: SkinRenderVec3): SkinRenderVec3 = SkinRenderVec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scale: Float): SkinRenderVec3 = SkinRenderVec3(x * scale, y * scale, z * scale)
    operator fun unaryMinus(): SkinRenderVec3 = SkinRenderVec3(-x, -y, -z)

    fun cross(other: SkinRenderVec3): SkinRenderVec3 =
        SkinRenderVec3(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x)

    fun dot(other: SkinRenderVec3): Float = x * other.x + y * other.y + z * other.z

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
    val lightIntensity: Float = 0.8f,
    val antiAliasingLevel: Int = 2,
)

data class SkinRenderRequest(
    val skinPng: ByteArray,
    val isSlim: Boolean,
    val yaw: Float,
    val settings: SkinRenderSettings,
    val overlayMode: SkinOverlayMode,
    val lightingMode: SkinLightingMode,
    val shadows: Boolean,
    val showPlatform: Boolean = false,
    val pose: Map<BodyPart, List<SkinTransform>> = emptyMap(),
    val modelYaw: Float = 0f,
)

interface SkinPngRenderer : AutoCloseable {
    val name: String

    fun startup()

    fun renderPng(request: SkinRenderRequest): ByteArray

    override fun close() = Unit
}
