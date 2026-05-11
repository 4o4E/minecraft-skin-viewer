package top.e404.skin.core

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Color
import org.jetbrains.skia.Rect
import top.e404.tavolo.draw.render3d.Face
import top.e404.tavolo.draw.render3d.FaceDirection
import top.e404.tavolo.draw.render3d.Mesh
import top.e404.tavolo.draw.render3d.Vec2
import top.e404.tavolo.draw.render3d.Vec3
import top.e404.tavolo.draw.render3d.Vertex

private enum class PixelPosition {
    INNER, TOP, BOTTOM, LEFT, RIGHT, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

private val FACE_ADJACENCY: Map<Pair<FaceDirection, PixelPosition>, FaceDirection> = mapOf(
    (FaceDirection.TOP to PixelPosition.TOP) to FaceDirection.BACK,
    (FaceDirection.TOP to PixelPosition.BOTTOM) to FaceDirection.FRONT,
    (FaceDirection.TOP to PixelPosition.LEFT) to FaceDirection.LEFT,
    (FaceDirection.TOP to PixelPosition.RIGHT) to FaceDirection.RIGHT,

    (FaceDirection.BOTTOM to PixelPosition.TOP) to FaceDirection.FRONT,
    (FaceDirection.BOTTOM to PixelPosition.BOTTOM) to FaceDirection.BACK,
    (FaceDirection.BOTTOM to PixelPosition.LEFT) to FaceDirection.LEFT,
    (FaceDirection.BOTTOM to PixelPosition.RIGHT) to FaceDirection.RIGHT,

    (FaceDirection.FRONT to PixelPosition.TOP) to FaceDirection.TOP,
    (FaceDirection.FRONT to PixelPosition.BOTTOM) to FaceDirection.BOTTOM,
    (FaceDirection.FRONT to PixelPosition.LEFT) to FaceDirection.LEFT,
    (FaceDirection.FRONT to PixelPosition.RIGHT) to FaceDirection.RIGHT,

    (FaceDirection.BACK to PixelPosition.TOP) to FaceDirection.TOP,
    (FaceDirection.BACK to PixelPosition.BOTTOM) to FaceDirection.BOTTOM,
    (FaceDirection.BACK to PixelPosition.LEFT) to FaceDirection.RIGHT,
    (FaceDirection.BACK to PixelPosition.RIGHT) to FaceDirection.LEFT,

    (FaceDirection.LEFT to PixelPosition.TOP) to FaceDirection.TOP,
    (FaceDirection.LEFT to PixelPosition.BOTTOM) to FaceDirection.BOTTOM,
    (FaceDirection.LEFT to PixelPosition.LEFT) to FaceDirection.BACK,
    (FaceDirection.LEFT to PixelPosition.RIGHT) to FaceDirection.FRONT,

    (FaceDirection.RIGHT to PixelPosition.TOP) to FaceDirection.TOP,
    (FaceDirection.RIGHT to PixelPosition.BOTTOM) to FaceDirection.BOTTOM,
    (FaceDirection.RIGHT to PixelPosition.LEFT) to FaceDirection.FRONT,
    (FaceDirection.RIGHT to PixelPosition.RIGHT) to FaceDirection.BACK
)

private val ADJACENT_UV_MAPPING: Map<Pair<FaceDirection, PixelPosition>, (px: Int, py: Int, adjW: Int, adjH: Int) -> Pair<Int, Int>> = mapOf(
    FaceDirection.FRONT to PixelPosition.TOP to { px, _, _, adjH -> px to adjH - 1 },
    FaceDirection.FRONT to PixelPosition.BOTTOM to { px, _, _, _ -> px to 0 },
    FaceDirection.FRONT to PixelPosition.LEFT to { _, py, adjW, _ -> adjW - 1 to py },
    FaceDirection.FRONT to PixelPosition.RIGHT to { _, py, _, _ -> 0 to py },

    FaceDirection.BACK to PixelPosition.TOP to { px, _, adjW, _ -> adjW - 1 - px to 0 },
    FaceDirection.BACK to PixelPosition.BOTTOM to { px, _, adjW, adjH -> adjW - 1 - px to adjH - 1 },
    FaceDirection.BACK to PixelPosition.LEFT to { _, py, adjW, _ -> adjW - 1 to py },
    FaceDirection.BACK to PixelPosition.RIGHT to { _, py, _, _ -> 0 to py },

    FaceDirection.TOP to PixelPosition.TOP to { px, _, adjW, _ -> adjW - 1 - px to 0 },
    FaceDirection.TOP to PixelPosition.BOTTOM to { px, _, _, _ -> px to 0 },
    FaceDirection.TOP to PixelPosition.LEFT to { _, py, _, _ -> py to 0 },
    FaceDirection.TOP to PixelPosition.RIGHT to { _, py, adjW, _ -> adjW - 1 - py to 0 },

    FaceDirection.BOTTOM to PixelPosition.TOP to { px, _, _, adjH -> px to adjH - 1 },
    FaceDirection.BOTTOM to PixelPosition.BOTTOM to { px, _, adjW, adjH -> adjW - 1 - px to adjH - 1 },
    FaceDirection.BOTTOM to PixelPosition.LEFT to { _, py, adjW, adjH -> adjW - 1 - py to adjH - 1 },
    FaceDirection.BOTTOM to PixelPosition.RIGHT to { _, py, _, adjH -> py to adjH - 1 },

    FaceDirection.LEFT to PixelPosition.TOP to { px, _, _, _ -> 0 to px },
    FaceDirection.LEFT to PixelPosition.BOTTOM to { px, _, _, adjH -> 0 to adjH - 1 - px },
    FaceDirection.LEFT to PixelPosition.LEFT to { _, py, _, _ -> 0 to py },
    FaceDirection.LEFT to PixelPosition.RIGHT to { _, py, _, _ -> 0 to py },

    FaceDirection.RIGHT to PixelPosition.TOP to { px, _, adjW, adjH -> adjW - 1 to adjH - 1 - px },
    FaceDirection.RIGHT to PixelPosition.BOTTOM to { px, _, adjW, _ -> adjW - 1 to px },
    FaceDirection.RIGHT to PixelPosition.LEFT to { _, py, _, _ -> 0 to py },
    FaceDirection.RIGHT to PixelPosition.RIGHT to { _, py, adjW, _ -> adjW - 1 to py }
)

private fun getPixelPosition(px: Int, py: Int, width: Int, height: Int): PixelPosition {
    val isTop = py == 0
    val isBottom = py == height - 1
    val isLeft = px == 0
    val isRight = px == width - 1

    return when {
        isTop && isLeft -> PixelPosition.TOP_LEFT
        isTop && isRight -> PixelPosition.TOP_RIGHT
        isBottom && isLeft -> PixelPosition.BOTTOM_LEFT
        isBottom && isRight -> PixelPosition.BOTTOM_RIGHT
        isTop -> PixelPosition.TOP
        isBottom -> PixelPosition.BOTTOM
        isLeft -> PixelPosition.LEFT
        isRight -> PixelPosition.RIGHT
        else -> PixelPosition.INNER
    }
}

private fun isAdjacentTransparent(
    skin: Bitmap,
    currentDirection: FaceDirection,
    edge: PixelPosition,
    px: Int, py: Int,
    faceUVs: Map<FaceDirection, Rect>
): Boolean {
    val adjFaceDir = FACE_ADJACENCY[currentDirection to edge] ?: return true
    val adjUvRect = faceUVs[adjFaceDir] ?: return true
    val adjUvW = adjUvRect.width.toInt()
    val adjUvH = adjUvRect.height.toInt()

    val uvMappingFunc = ADJACENT_UV_MAPPING[currentDirection to edge] ?: return true
    val (adj_local_px, adj_local_py) = uvMappingFunc(px, py, adjUvW, adjUvH)

    val final_adj_px = adjUvRect.left.toInt() + adj_local_px
    val final_adj_py = adjUvRect.top.toInt() + adj_local_py

    if (final_adj_px < 0 || final_adj_px >= skin.width || final_adj_py < 0 || final_adj_py >= skin.height) {
        return true
    }
    return Color.getA(skin.getColor(final_adj_px, final_adj_py)) == 0
}

/**
 * 创建一个3D覆盖层网格，根据皮肤贴图的透明像素生成体素化的覆盖层效果
 *
 * @param skin 皮肤贴图
 * @param dims 覆盖层对应的基础模型尺寸
 * @param overlayDepth 覆盖层的厚度
 * @param faceUVs 每个面对应的UV坐标矩形区域
 * @param textureWidth 皮肤贴图的总宽度
 * @param textureHeight 皮肤贴图的总高度
 * @return 返回生成的Mesh对象，包含顶点和面信息
 */
fun create3DOverlay(
    skin: Bitmap,
    dims: Vec3,
    overlayDepth: Float,
    faceUVs: Map<FaceDirection, Rect>,
    textureWidth: Float,
    textureHeight: Float
): Mesh {
    val vertices = mutableListOf<Vertex>()
    val faces = mutableListOf<Face>()
    val effectiveDims = dims + Vec3(2 * overlayDepth, 2 * overlayDepth, 2 * overlayDepth)

    for ((direction, uvRect) in faceUVs) {
        val uvW = uvRect.width.toInt()
        val uvH = uvRect.height.toInt()

        for (px in 0 until uvW) {
            for (py in 0 until uvH) {
                val pixelColor = skin.getColor(uvRect.left.toInt() + px, uvRect.top.toInt() + py)
                if (Color.getA(pixelColor) <= 0) continue

                val pixelUV = Vec2(
                    (uvRect.left + px + 0.5f) / textureWidth,
                    (uvRect.top + py + 0.5f) / textureHeight
                )

                val (voxelW, voxelH) = when (direction) {
                    FaceDirection.RIGHT, FaceDirection.LEFT -> Pair(effectiveDims.z / uvW, effectiveDims.y / uvH)
                    FaceDirection.TOP, FaceDirection.BOTTOM -> Pair(effectiveDims.x / uvW, effectiveDims.z / uvH)
                    else -> Pair(effectiveDims.x / uvW, effectiveDims.y / uvH)
                }
                val voxelD = voxelW
                val retractionDepth = voxelD - overlayDepth
                val w = voxelW / 2; val h = voxelH / 2; val d = voxelD / 2

                val v = mutableListOf(
                    Vec3(-w, -h, -d), Vec3(w, -h, -d), Vec3(w, h, -d), Vec3(-w, h, -d),
                    Vec3(-w, -h, d), Vec3(w, -h, d), Vec3(w, h, d), Vec3(-w, h, d)
                )

                val pixelPosition = getPixelPosition(px, py, uvW, uvH)
                if (pixelPosition != PixelPosition.INNER) {
                    val shrinkTop = pixelPosition in listOf(PixelPosition.TOP, PixelPosition.TOP_LEFT, PixelPosition.TOP_RIGHT) && !isAdjacentTransparent(skin, direction, PixelPosition.TOP, px, py, faceUVs)
                    val shrinkBottom = pixelPosition in listOf(PixelPosition.BOTTOM, PixelPosition.BOTTOM_LEFT, PixelPosition.BOTTOM_RIGHT) && !isAdjacentTransparent(skin, direction, PixelPosition.BOTTOM, px, py, faceUVs)
                    val shrinkLeft = pixelPosition in listOf(PixelPosition.LEFT, PixelPosition.TOP_LEFT, PixelPosition.BOTTOM_LEFT) && !isAdjacentTransparent(skin, direction, PixelPosition.LEFT, px, py, faceUVs)
                    val shrinkRight = pixelPosition in listOf(PixelPosition.RIGHT, PixelPosition.TOP_RIGHT, PixelPosition.BOTTOM_RIGHT) && !isAdjacentTransparent(skin, direction, PixelPosition.RIGHT, px, py, faceUVs)

                    if (shrinkTop) { v[2] = v[2].copy(y = v[2].y - voxelD); v[3] = v[3].copy(y = v[3].y - voxelD) }
                    if (shrinkBottom) { v[0] = v[0].copy(y = v[0].y + voxelD); v[1] = v[1].copy(y = v[1].y + voxelD) }
                    if (shrinkLeft) { v[0] = v[0].copy(x = v[0].x + voxelD); v[3] = v[3].copy(x = v[3].x + voxelD) }
                    if (shrinkRight) { v[1] = v[1].copy(x = v[1].x - voxelD); v[2] = v[2].copy(x = v[2].x - voxelD) }
                }

                val finalVoxelVertices = v.map { p ->
                    val rotatedP = when (direction) {
                        FaceDirection.FRONT -> p
                        FaceDirection.BACK -> Vec3(-p.x, p.y, -p.z)
                        FaceDirection.RIGHT -> Vec3(p.z, p.y, -p.x)
                        FaceDirection.LEFT -> Vec3(-p.z, p.y, p.x)
                        FaceDirection.TOP -> Vec3(p.x, p.z, -p.y)
                        FaceDirection.BOTTOM -> Vec3(p.x, -p.z, p.y)
                    }
                    val voxelCenter = when (direction) {
                        FaceDirection.FRONT -> Vec3(-effectiveDims.x / 2 + (px + 0.5f) * voxelW, effectiveDims.y / 2 - (py + 0.5f) * voxelH, dims.z / 2 + d - retractionDepth)
                        FaceDirection.BACK -> Vec3(effectiveDims.x / 2 - (px + 0.5f) * voxelW, effectiveDims.y / 2 - (py + 0.5f) * voxelH, -dims.z / 2 - d + retractionDepth)
                        FaceDirection.RIGHT -> Vec3(dims.x / 2 + d - retractionDepth, effectiveDims.y / 2 - (py + 0.5f) * voxelH, effectiveDims.z / 2 - (px + 0.5f) * voxelW)
                        FaceDirection.LEFT -> Vec3(-dims.x / 2 - d + retractionDepth, effectiveDims.y / 2 - (py + 0.5f) * voxelH, -effectiveDims.z / 2 + (px + 0.5f) * voxelW)
                        FaceDirection.TOP -> Vec3(-effectiveDims.x / 2 + (px + 0.5f) * voxelW, dims.y / 2 + d - retractionDepth, -effectiveDims.z / 2 + (py + 0.5f) * voxelH)
                        FaceDirection.BOTTOM -> Vec3(-effectiveDims.x / 2 + (px + 0.5f) * voxelW, -dims.y / 2 - d + retractionDepth, effectiveDims.z / 2 - (py + 0.5f) * voxelH)
                    }
                    rotatedP + voxelCenter
                }

                val baseIndex = vertices.size
                vertices.addAll(finalVoxelVertices.map { Vertex(it, pixelUV) })

                faces.addAll(listOf(
                    Face(listOf(baseIndex + 4, baseIndex + 7, baseIndex + 6, baseIndex + 5), pixelColor),
                    Face(listOf(baseIndex + 0, baseIndex + 3, baseIndex + 2, baseIndex + 1), pixelColor),
                    Face(listOf(baseIndex + 5, baseIndex + 6, baseIndex + 2, baseIndex + 1), pixelColor),
                    Face(listOf(baseIndex + 4, baseIndex + 0, baseIndex + 3, baseIndex + 7), pixelColor),
                    Face(listOf(baseIndex + 7, baseIndex + 3, baseIndex + 2, baseIndex + 6), pixelColor),
                    Face(listOf(baseIndex + 4, baseIndex + 5, baseIndex + 1, baseIndex + 0), pixelColor)
                ))
            }
        }
    }
    return Mesh(vertices, faces)
}
