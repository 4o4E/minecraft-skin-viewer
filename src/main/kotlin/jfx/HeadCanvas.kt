package top.e404.skin.jfx

import javafx.scene.*
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.transform.Rotate
import javafx.scene.transform.Scale
import javafx.scene.transform.Translate
import kotlin.math.max

/**
 * 代表一个头颅预览
 *
 * @param skinImage 皮肤图片
 * @param width 画布宽度
 * @param height 画布高度
 * @param light 环境光颜色
 */
class HeadCanvas(
    skinImage: Image,
    width: Double,
    height: Double,
    light: Color?,
) : Group() {
    // skin
    private val skin = skinImage.run {
        val ts = skinImage.format()
        val multiple = max((1024 / ts.width).toInt(), 1)
        if (multiple > 1) ts.enlarge(multiple)
        else ts
    }

    private val headInner = SkinCube(8, 8, 8, 1F, 0, 0, skin)
    private val headOuter = SkinCube(8, 8, 8, 1.09F, 32, 0, skin)

    private val head = SkinGroup(
        Rotate(0.0, 0.0, headInner.height / 2.0, 0.0, Rotate.X_AXIS),
        Rotate(0.0, Rotate.Y_AXIS),
        Rotate(0.0, 0.0, headInner.height / 2.0, 0.0, Rotate.Z_AXIS),
        headInner, headOuter
    )

    val xRotate = Rotate(15.0, Rotate.X_AXIS)
    val yRotate = Rotate(45.0, Rotate.Y_AXIS)
    val zRotate = Rotate(-15.0, Rotate.Z_AXIS)
    val translate = Translate(0.0, 0.0, -80.0)
    val scale = Scale(.38, .38)

    val subScene: SubScene
    val root = Group()
    val camera = PerspectiveCamera(true).apply {
        transforms.addAll(yRotate, translate, scale)
    }

    init {
        createPlayerModel()
        subScene = SubScene(
            Group().apply {
                children.add(root)
                light?.let { children.add(AmbientLight(it)) }
                transforms.add(zRotate)
            }, width, height,
            true,
            SceneAntialiasing.BALANCED
        ).apply {
            fill = Color.valueOf("#00000000")
            camera = this@HeadCanvas.camera
            children.add(this)
        }
    }

    private fun createPlayerModel() {
        head.translateY
        root.transforms.addAll(xRotate)
        root.children.addAll(head)
    }
}
