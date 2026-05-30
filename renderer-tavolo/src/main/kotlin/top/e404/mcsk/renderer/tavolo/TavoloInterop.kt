package top.e404.mcsk.renderer.tavolo

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import top.e404.mcsk.core.BodyPart
import top.e404.mcsk.core.SkinMesh
import top.e404.mcsk.core.SkinMeshFace
import top.e404.mcsk.core.SkinTransform
import top.e404.mcsk.core.SkinVec2
import top.e404.mcsk.core.SkinVec3
import top.e404.mcsk.core.SkinVertex
import top.e404.mcsk.core.createMinecraftPlayerMeshes
import top.e404.mcsk.core.rotateY
import top.e404.tavolo.draw.render3d.Face
import top.e404.tavolo.draw.render3d.Mesh
import top.e404.tavolo.draw.render3d.OrbitCamera
import top.e404.tavolo.draw.render3d.RenderConfig
import top.e404.tavolo.draw.render3d.Scene
import top.e404.tavolo.draw.render3d.Vec2
import top.e404.tavolo.draw.render3d.Vec3
import top.e404.tavolo.draw.render3d.Vertex
import top.e404.tavolo.draw.render3d.renderSceneToImage

fun renderMinecraftViewTavolo(
    skin: Image,
    isSlim: Boolean,
    renderConfig: RenderConfig,
    backgroundMeshes: List<SkinMesh> = emptyList(),
    pose: Map<BodyPart, List<SkinTransform>> = emptyMap(),
    use3DOverlay: Boolean = true,
    modelYaw: Float = 0f,
    cape: Image? = null,
): Image {
    val skinBitmap = Bitmap.makeFromImage(skin)
    val capeBitmap = cape?.let { Bitmap.makeFromImage(it) }
    val playerMeshes = createMinecraftPlayerMeshes(skinBitmap, isSlim, pose, use3DOverlay, capeBitmap)
    return renderMinecraftViewTavolo(
        playerMeshes = playerMeshes,
        renderConfig = renderConfig,
        backgroundMeshes = backgroundMeshes,
        modelYaw = modelYaw
    )
}

fun prepareMinecraftPlayerMeshesTavolo(
    skin: Image,
    isSlim: Boolean,
    pose: Map<BodyPart, List<SkinTransform>> = emptyMap(),
    use3DOverlay: Boolean = true,
    cape: Image? = null,
): List<SkinMesh> =
    createMinecraftPlayerMeshes(Bitmap.makeFromImage(skin), isSlim, pose, use3DOverlay, cape?.let { Bitmap.makeFromImage(it) })

fun renderMinecraftViewTavolo(
    playerMeshes: List<SkinMesh>,
    renderConfig: RenderConfig,
    backgroundMeshes: List<SkinMesh> = emptyList(),
    modelYaw: Float = 0f,
): Image {
    val renderedPlayerMeshes = playerMeshes
        .map { if (modelYaw == 0f) it else it.rotateY(modelYaw) }
    return renderSceneToImage(
        scene = Scene((renderedPlayerMeshes + backgroundMeshes).map { it.toTavoloMesh() }),
        config = renderConfig
    )
}

fun SkinMesh.toTavoloMesh(): Mesh =
    Mesh(
        vertices = vertices.map { it.toTavoloVertex() },
        faces = faces.map { it.toTavoloFace(this) },
        texture = texture,
        castsShadow = castsShadow,
        receivesShadow = receivesShadow
    )

fun OrbitCamera.cameraRelativeUpperLeftLight(): Vec3 {
    val (_, cameraForward) = createViewMatrix()
    val cameraRight = cameraForward.cross(upVector).normalized()
    val cameraUp = cameraRight.cross(cameraForward).normalized()
    return ((-cameraRight * 0.9f) + (cameraUp * 1.1f) + (-cameraForward * 0.45f)).normalized()
}

private fun SkinVertex.toTavoloVertex(): Vertex = Vertex(position.toTavoloVec3(), uv.toTavoloVec2())

private fun SkinMeshFace.toTavoloFace(mesh: SkinMesh): Face =
    Face(stableTavoloIndices(mesh), baseColor)

private fun SkinMeshFace.stableTavoloIndices(mesh: SkinMesh): List<Int> {
    if (!mesh.shouldUseVoxelStableNormal() || indices.size < 4) return indices

    val targetNormal = voxelStableNormal(mesh)
    if (targetNormal.length() <= 0f) return indices

    // Tavolo 只能从面的前三个点推导法线，旋转索引可避免非平面体素面选到错误三角形。
    return sequence {
        for (offset in indices.indices) yield(indices.rotate(offset))
        val reversed = indices.asReversed()
        for (offset in reversed.indices) yield(reversed.rotate(offset))
    }.maxByOrNull { candidate ->
        candidate.faceNormal(mesh).dot(targetNormal)
    } ?: indices
}

private fun SkinMesh.shouldUseVoxelStableNormal(): Boolean =
    texture == null && vertices.size >= 8 && vertices.size % 8 == 0 && faces.size >= 6

private fun SkinMeshFace.voxelStableNormal(mesh: SkinMesh): SkinVec3 {
    val center = center(mesh)
    val normalCenter = voxelBoundsCenter(mesh)
    val outward = (center - normalCenter).normalized()
    return if (outward.length() > 0f) outward else indices.faceNormal(mesh)
}

private fun SkinMeshFace.center(mesh: SkinMesh): SkinVec3 {
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
    return SkinVec3(x / count, y / count, z / count)
}

private fun SkinMeshFace.voxelBoundsCenter(mesh: SkinMesh): SkinVec3 {
    val firstIndex = indices.minOrNull() ?: return center(mesh)
    val voxelBaseIndex = (firstIndex / 8) * 8
    if (voxelBaseIndex + 7 >= mesh.vertices.size) return center(mesh)

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
    return SkinVec3((minX + maxX) / 2f, (minY + maxY) / 2f, (minZ + maxZ) / 2f)
}

private fun List<Int>.rotate(offset: Int): List<Int> =
    drop(offset) + take(offset)

private fun List<Int>.faceNormal(mesh: SkinMesh): SkinVec3 {
    if (size < 3) return SkinVec3(0f, 0f, 0f)
    val p0 = mesh.vertices[this[0]].position
    val p1 = mesh.vertices[this[1]].position
    val p2 = mesh.vertices[this[2]].position
    return (p1 - p0).cross(p2 - p0).normalized()
}

private fun SkinVec3.toTavoloVec3(): Vec3 = Vec3(x, y, z)

private fun SkinVec2.toTavoloVec2(): Vec2 = Vec2(u, v)
