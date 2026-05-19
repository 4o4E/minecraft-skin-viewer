package top.e404.mcsk.core

import org.jetbrains.skia.Bitmap
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class SkinVec2(val u: Float, val v: Float)

data class SkinVec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: SkinVec3): SkinVec3 = SkinVec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: SkinVec3): SkinVec3 = SkinVec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scale: Float): SkinVec3 = SkinVec3(x * scale, y * scale, z * scale)
    operator fun unaryMinus(): SkinVec3 = SkinVec3(-x, -y, -z)

    fun cross(other: SkinVec3): SkinVec3 =
        SkinVec3(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x)

    fun dot(other: SkinVec3): Float = x * other.x + y * other.y + z * other.z

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun normalized(): SkinVec3 {
        val length = length()
        return if (length > 0f) SkinVec3(x / length, y / length, z / length) else this
    }

    fun rotate(rotation: SkinTransform.Rotate): SkinVec3 =
        rotateZ(Math.toRadians(rotation.z.toDouble()).toFloat())
            .rotateY(Math.toRadians(rotation.y.toDouble()).toFloat())
            .rotateX(Math.toRadians(rotation.x.toDouble()).toFloat())

    fun scale(scale: SkinTransform.Scale): SkinVec3 = SkinVec3(x * scale.x, y * scale.y, z * scale.z)

    fun translate(translate: SkinTransform.Translate): SkinVec3 =
        SkinVec3(x + translate.x, y + translate.y, z + translate.z)

    private fun rotateX(angle: Float): SkinVec3 {
        val c = cos(angle)
        val s = sin(angle)
        return SkinVec3(x, y * c - z * s, y * s + z * c)
    }

    private fun rotateY(angle: Float): SkinVec3 {
        val c = cos(angle)
        val s = sin(angle)
        return SkinVec3(x * c + z * s, y, -x * s + z * c)
    }

    private fun rotateZ(angle: Float): SkinVec3 {
        val c = cos(angle)
        val s = sin(angle)
        return SkinVec3(x * c - y * s, x * s + y * c, z)
    }
}

data class SkinUvRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    companion object {
        fun makeXYWH(x: Float, y: Float, width: Float, height: Float): SkinUvRect =
            SkinUvRect(x, y, x + width, y + height)

        fun makeLTRB(left: Float, top: Float, right: Float, bottom: Float): SkinUvRect =
            SkinUvRect(left, top, right, bottom)
    }
}

enum class SkinFaceDirection {
    RIGHT,
    LEFT,
    TOP,
    BOTTOM,
    FRONT,
    BACK
}

sealed interface SkinTransform {
    data class Rotate(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) : SkinTransform
    data class Scale(val x: Float = 1f, val y: Float = 1f, val z: Float = 1f) : SkinTransform
    data class Translate(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) : SkinTransform
}

data class SkinVertex(val position: SkinVec3, val uv: SkinVec2)

data class SkinMeshFace(val indices: List<Int>, val baseColor: Int)

data class SkinMesh(
    val vertices: List<SkinVertex>,
    val faces: List<SkinMeshFace>,
    val texture: Bitmap? = null,
    val castsShadow: Boolean = true,
    val receivesShadow: Boolean = true,
)

fun combineSkinMeshes(meshes: List<SkinMesh>, texture: Bitmap? = null): SkinMesh {
    val vertices = mutableListOf<SkinVertex>()
    val faces = mutableListOf<SkinMeshFace>()
    meshes.forEach { mesh ->
        val offset = vertices.size
        vertices.addAll(mesh.vertices)
        faces.addAll(mesh.faces.map { face -> face.copy(indices = face.indices.map { it + offset }) })
    }
    return SkinMesh(
        vertices = vertices,
        faces = faces,
        texture = texture ?: meshes.firstOrNull()?.texture,
        castsShadow = meshes.any { it.castsShadow },
        receivesShadow = meshes.any { it.receivesShadow }
    )
}
