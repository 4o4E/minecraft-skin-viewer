package top.e404.skin.jfx

import javafx.scene.image.Image
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.MeshView
import javafx.scene.shape.TriangleMesh

/**
 * 代表一个方块的TriangleMesh, 一次性使用, 更换皮肤需要重新构造
 *
 * @property width 方块模型的宽度, 范围[0, 64)
 * @property height 方块模型的高度, 范围[0, 64)
 * @property depth 方块模型的深度, 范围[0, 64)
 * @property cubeZoom 方块模型的缩放尺寸
 * @param x 按64x皮肤计算时皮肤材质开始的X坐标
 * @param y 按64x皮肤计算时皮肤材质开始的Y坐标
 * @param image 材质
 */
class SkinCube(
    val width: Int,
    val height: Int,
    val depth: Int,
    val cubeZoom: Float,
    x: Int,
    y: Int,
    image: Image,
) : MeshView() {
    // 缩放
    private val sizeW = .5F / (width + depth)
    private val sizeH = 1F / (height + depth)
    private val zoom = image.width.toInt() / 64

    // 实际材质尺寸
    private val w = width * zoom
    private val h = height * zoom
    private val d = depth * zoom

    private val texture = image.run {
        val tx = x * zoom
        val ty = y * zoom
        subImage(tx, ty, 2 * (d + w), d + h)
    }

    init {
        mesh = TriangleMesh().apply {
            points.addAll(*createPoints())
            texCoords.addAll(*createTexCoords())
            faces.addAll(*createFaces())
        }
        material = PhongMaterial().apply {
            diffuseMap = texture
        }
    }

    /**
     * 创建模型顶点
     *
     * @return 模型顶点数组
     */
    private fun createPoints(): FloatArray {
        val w = width * cubeZoom / 2F
        val h = height * cubeZoom / 2F
        val d = depth * cubeZoom / 2F
        return floatArrayOf(
            -w, -h, -d,  // P0 左上 前
            w, -h, -d,   // P1 右上 前
            -w, h, -d,   // P2 左下 前
            w, h, -d,    // P3 右下 前
            -w, -h, d, // P4 左上 后
            w, -h, d,  // P5 右上 后
            -w, h, d,  // P6 左下 后
            w, h, d    // P7 右下 后
        )
    }

    /**
     * 创建材质顶点
     *
     * @return 材质顶点数组
     */
    private fun createTexCoords(): FloatArray {
        val x0 = 0F
        val x1 = depth * sizeW
        val x2 = (depth + width) * sizeW
        val x3 = (depth * 2 + width) * sizeW
        val x4 = (depth + width * 2) * sizeW
        val x5 = (depth + width) * 2 * sizeW
        val y0 = 0F
        val y1 = depth * sizeH
        val y2 = (depth + height) * sizeH
        return floatArrayOf(
            // L1
            x1, y0, // T0
            x2, y0, // T1
            x4, y0, // T2
            // L2
            x0, y1, // T3
            x1, y1, // T4
            x2, y1, // T5
            x3, y1, // T6
            x4, y1, // T7
            x5, y1, // T8
            // L3
            x0, y2, // T9
            x1, y2, // T10
            x2, y2, // T11
            x3, y2, // T12
            x5, y2, // T13
        )
    }

    /**
     * 创建材质顶点绑定
     *
     * @return 材质顶点顶点绑定
     */
    private fun createFaces(): IntArray {
        // p, t
        val faces = intArrayOf(
            // 上
            5, 1, 0, 4, 1, 5,
            5, 1, 0, 4, 4, 0,
            // 左
            4, 3, 2, 10, 0, 4,
            4, 3, 2, 10, 6, 9,
            // 前
            0, 4, 3, 11, 1, 5,
            0, 4, 3, 11, 2, 10,
            // 右
            1, 5, 7, 12, 5, 6,
            1, 5, 7, 12, 3, 11,
            // 后
            4, 8, 7, 12, 5, 6,
            4, 8, 7, 12, 6, 13,
            // 下
            6, 1, 3, 7, 2, 5,
            6, 1, 3, 7, 7, 2
        )
        val copy = faces.clone().reversedArray()
        for (i in copy.indices step 2) {
            val tmp = copy[i]
            copy[i] = copy[i + 1]
            copy[i + 1] = tmp
        }
        return faces.toMutableList().apply {
            addAll(copy.toList())
        }.toIntArray()
    }
}
