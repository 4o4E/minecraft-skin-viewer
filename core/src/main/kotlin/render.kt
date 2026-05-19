package top.e404.mcsk.core

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Color

/**
 * Builds the complete Minecraft player model from a skin bitmap.
 */
fun createMinecraftPlayer(
    skin: Bitmap,
    isSlim: Boolean,
    pose: Map<BodyPart, List<SkinTransform>> = emptyMap(),
    use3DOverlay: Boolean = false
): SkinMesh = combineSkinMeshes(createMinecraftPlayerMeshes(skin, isSlim, pose, use3DOverlay), skin)

fun createMinecraftPlayerMeshes(
    skin: Bitmap,
    isSlim: Boolean,
    pose: Map<BodyPart, List<SkinTransform>> = emptyMap(),
    use3DOverlay: Boolean = false
): List<SkinMesh> {
    val texW = skin.width.toFloat()
    val texH = skin.height.toFloat()
    val texturedComponentMeshes = mutableListOf<SkinMesh>()
    val solidComponentMeshes = mutableListOf<SkinMesh>()
    val playerModel = PlayerModel(isSlim)

    for (partId in BodyPart.entries) {
        val partCube = playerModel.parts[partId] ?: continue
        val overlayCube = playerModel.overlays[partId]
        val transformations = pose[partId]
        val partDims = partId.getDims(isSlim)
        val partUvs = partCube.uvs.toGeometryFaceUvs()

        val transform: (SkinVec3) -> SkinVec3 = { vertexPos ->
            if (transformations.isNullOrEmpty()) {
                vertexPos
            } else {
                transformations.fold(vertexPos) { currentPos, transformation ->
                    when (transformation) {
                        is SkinTransform.Rotate -> (currentPos - partCube.pivot).rotate(transformation) + partCube.pivot
                        is SkinTransform.Scale -> (currentPos - partCube.pivot).scale(transformation) + partCube.pivot
                        is SkinTransform.Translate -> currentPos.translate(transformation)
                    }
                }
            }
        }

        val baseMesh = createMinecraftUVCuboid(partDims, partUvs, texW, texH)
        texturedComponentMeshes.add(baseMesh.copy(vertices = baseMesh.vertices.map {
            SkinVertex(transform(it.position) + partCube.pos, it.uv)
        }))

        overlayCube?.let {
            val overlayUvs = it.uvs.toGeometryFaceUvs()
            val overlayMesh = if (use3DOverlay) {
                val overlayDepth = if (partId == BodyPart.HEAD) 0.5f else 0.25f
                create3DOverlay(skin, partDims, overlayDepth, overlayUvs, texW, texH)
            } else {
                val overlaySize = if (partId == BodyPart.HEAD) 1.0f else 0.5f
                createMinecraftUVCuboid(partDims + SkinVec3(overlaySize, overlaySize, overlaySize), overlayUvs, texW, texH)
            }
            val transformedOverlayMesh = overlayMesh.copy(vertices = overlayMesh.vertices.map { vertex ->
                SkinVertex(transform(vertex.position) + partCube.pos, vertex.uv)
            })
            if (use3DOverlay) solidComponentMeshes.add(transformedOverlayMesh)
            else texturedComponentMeshes.add(transformedOverlayMesh)
        }
    }
    return listOf(combineSkinMeshes(texturedComponentMeshes, skin)) + solidComponentMeshes
}

fun createSkinPlatform(topY: Float = -8.2f, thickness: Float = 2f): SkinMesh =
    createSolidCuboid(
        dimensions = SkinVec3(24f, thickness, 24f),
        baseColor = Color.makeRGB(90, 105, 125)
    ).copy(castsShadow = false, receivesShadow = true)
        .translate(SkinVec3(0f, topY - thickness / 2f, 0f))

fun SkinMesh.rotateY(angle: Float): SkinMesh =
    copy(vertices = vertices.map { vertex ->
        vertex.copy(position = vertex.position.rotate(SkinTransform.Rotate(y = angle)))
    })

fun SkinMesh.translate(offset: SkinVec3): SkinMesh =
    copy(vertices = vertices.map { vertex ->
        vertex.copy(position = vertex.position + offset)
    })

private fun Map<SkinFace, SkinUvRect>.toGeometryFaceUvs(): Map<SkinFaceDirection, SkinUvRect> = mapOf(
    // Geometry RIGHT/LEFT are +X/-X; Minecraft skin RIGHT/LEFT are player right/left.
    SkinFaceDirection.RIGHT to getValue(SkinFace.LEFT),
    SkinFaceDirection.LEFT to getValue(SkinFace.RIGHT),
    SkinFaceDirection.TOP to getValue(SkinFace.TOP),
    SkinFaceDirection.BOTTOM to getValue(SkinFace.BOTTOM),
    SkinFaceDirection.FRONT to getValue(SkinFace.FRONT),
    SkinFaceDirection.BACK to getValue(SkinFace.BACK),
)

private fun createSolidCuboid(dimensions: SkinVec3, baseColor: Int): SkinMesh {
    val (w, h, d) = dimensions
    val zeroUv = SkinVec2(0f, 0f)
    val vertices = listOf(
        SkinVertex(SkinVec3(-w / 2, -h / 2, -d / 2), zeroUv),
        SkinVertex(SkinVec3(w / 2, -h / 2, -d / 2), zeroUv),
        SkinVertex(SkinVec3(w / 2, h / 2, -d / 2), zeroUv),
        SkinVertex(SkinVec3(-w / 2, h / 2, -d / 2), zeroUv),
        SkinVertex(SkinVec3(-w / 2, -h / 2, d / 2), zeroUv),
        SkinVertex(SkinVec3(w / 2, -h / 2, d / 2), zeroUv),
        SkinVertex(SkinVec3(w / 2, h / 2, d / 2), zeroUv),
        SkinVertex(SkinVec3(-w / 2, h / 2, d / 2), zeroUv)
    )
    val faces = listOf(
        SkinMeshFace(listOf(0, 3, 2, 1), baseColor),
        SkinMeshFace(listOf(1, 2, 6, 5), baseColor),
        SkinMeshFace(listOf(5, 6, 7, 4), baseColor),
        SkinMeshFace(listOf(4, 7, 3, 0), baseColor),
        SkinMeshFace(listOf(3, 7, 6, 2), baseColor),
        SkinMeshFace(listOf(4, 0, 1, 5), baseColor)
    )
    return SkinMesh(vertices, faces)
}

private fun createMinecraftUVCuboid(
    dims: SkinVec3,
    faceUVs: Map<SkinFaceDirection, SkinUvRect>,
    textureWidth: Float,
    textureHeight: Float,
): SkinMesh {
    val (w, h, d) = dims
    val vertices = mutableListOf<SkinVertex>()
    val faces = mutableListOf<SkinMeshFace>()
    val v = listOf(
        SkinVec3(-w / 2, -h / 2, d / 2),
        SkinVec3(w / 2, -h / 2, d / 2),
        SkinVec3(w / 2, h / 2, d / 2),
        SkinVec3(-w / 2, h / 2, d / 2),
        SkinVec3(w / 2, -h / 2, -d / 2),
        SkinVec3(-w / 2, -h / 2, -d / 2),
        SkinVec3(-w / 2, h / 2, -d / 2),
        SkinVec3(w / 2, h / 2, -d / 2)
    )

    fun u(px: Float) = px / textureWidth
    fun v(py: Float) = py / textureHeight

    fun addFace(direction: SkinFaceDirection, vIndices: List<Int>, uvRect: SkinUvRect) {
        val texelCenterOffset = 0.5f
        val centeredRect = SkinUvRect.makeLTRB(
            uvRect.left + texelCenterOffset,
            uvRect.top + texelCenterOffset,
            uvRect.right - texelCenterOffset,
            uvRect.bottom - texelCenterOffset
        )
        val bottomLeft = SkinVec2(u(centeredRect.left), v(centeredRect.bottom))
        val bottomRight = SkinVec2(u(centeredRect.right), v(centeredRect.bottom))
        val topRight = SkinVec2(u(centeredRect.right), v(centeredRect.top))
        val topLeft = SkinVec2(u(centeredRect.left), v(centeredRect.top))
        val uvs = when {
            direction == SkinFaceDirection.BOTTOM -> listOf(topLeft, topRight, bottomRight, bottomLeft)
            else -> listOf(bottomLeft, bottomRight, topRight, topLeft)
        }
        val faceIndices = mutableListOf<Int>()
        for (i in vIndices.indices) {
            vertices.add(SkinVertex(v[vIndices[i]], uvs[i]))
            faceIndices.add(vertices.size - 1)
        }
        faces.add(SkinMeshFace(faceIndices, Color.WHITE))
    }

    faceUVs[SkinFaceDirection.FRONT]?.let { addFace(SkinFaceDirection.FRONT, listOf(0, 1, 2, 3), it) }
    faceUVs[SkinFaceDirection.BACK]?.let { addFace(SkinFaceDirection.BACK, listOf(4, 5, 6, 7), it) }
    faceUVs[SkinFaceDirection.RIGHT]?.let { addFace(SkinFaceDirection.RIGHT, listOf(1, 4, 7, 2), it) }
    faceUVs[SkinFaceDirection.LEFT]?.let { addFace(SkinFaceDirection.LEFT, listOf(5, 0, 3, 6), it) }
    faceUVs[SkinFaceDirection.TOP]?.let { addFace(SkinFaceDirection.TOP, listOf(3, 2, 7, 6), it) }
    faceUVs[SkinFaceDirection.BOTTOM]?.let { addFace(SkinFaceDirection.BOTTOM, listOf(5, 4, 1, 0), it) }
    return SkinMesh(vertices, faces)
}
