package top.e404.skin.renderer.tavolo

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import top.e404.skin.core.BodyPart
import top.e404.skin.core.SkinMesh
import top.e404.skin.core.SkinMeshFace
import top.e404.skin.core.SkinTransform
import top.e404.skin.core.SkinVec2
import top.e404.skin.core.SkinVec3
import top.e404.skin.core.SkinVertex
import top.e404.skin.core.createMinecraftPlayerMeshes
import top.e404.skin.core.rotateY
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
): Image {
    val skinBitmap = Bitmap.makeFromImage(skin)
    val playerMeshes = createMinecraftPlayerMeshes(skinBitmap, isSlim, pose, use3DOverlay)
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
): List<SkinMesh> =
    createMinecraftPlayerMeshes(Bitmap.makeFromImage(skin), isSlim, pose, use3DOverlay)

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
        faces = faces.map { it.toTavoloFace() },
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

private fun SkinMeshFace.toTavoloFace(): Face = Face(indices, baseColor)

private fun SkinVec3.toTavoloVec3(): Vec3 = Vec3(x, y, z)

private fun SkinVec2.toTavoloVec2(): Vec2 = Vec2(u, v)
