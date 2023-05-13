package top.e404.skin.jfx

import javafx.scene.*
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.transform.Rotate
import javafx.scene.transform.Scale
import javafx.scene.transform.Translate
import kotlin.math.max

/**
 * 代表一个皮肤预览
 *
 * @property skin 皮肤
 * @param width 画布宽度
 * @param height 画布高度
 * @param slim 皮肤格式
 */
class SkinCanvas(
    skinImage: Image,
    val slim: Boolean,
    width: Double,
    height: Double,
    light: Color?,
    val headScale: Double,
) : Group() {
    // skin
    private val skin = skinImage.run {
        val ts = skinImage.format()
        val multiple = max((1024 / ts.width).toInt(), 1)
        if (multiple > 1) ts.enlarge(multiple) else ts
    }

    private val zoom = 1F
    private val outerZoom = 1.09F
    private val outerZoomLarge = 1.1F
    private val arm = if (slim) 3 else 4
    private val headInner = SkinCube(8, 8, 8, zoom, 0, 0, skin)
    private val headOuter = SkinCube(8, 8, 8, outerZoom, 32, 0, skin)
    private val bodyInner = SkinCube(8, 12, 4, zoom, 16, 16, skin)
    private val bodyOuter = SkinCube(8, 12, 4, outerZoomLarge, 16, 32, skin)
    private val lArmInner = SkinCube(arm, 12, 4, zoom, 32, 48, skin)
    private val lArmOuter = SkinCube(arm, 12, 4, outerZoom, 48, 48, skin)
    private val rArmInner = SkinCube(arm, 12, 4, zoom, 40, 16, skin)
    private val rArmOuter = SkinCube(arm, 12, 4, outerZoom, 40, 32, skin)
    private val lLegInner = SkinCube(4, 12, 4, zoom, 16, 48, skin)
    private val lLegOuter = SkinCube(4, 12, 4, outerZoom, 0, 48, skin)
    private val rLegInner = SkinCube(4, 12, 4, zoom, 0, 16, skin)
    private val rLegOuter = SkinCube(4, 12, 4, outerZoom, 0, 32, skin)

    private val head = SkinGroup(
        Rotate(0.0, 0.0, headInner.height / 2.0, 0.0, Rotate.X_AXIS),
        Rotate(0.0, Rotate.Y_AXIS),
        Rotate(0.0, 0.0, headInner.height / 2.0, 0.0, Rotate.Z_AXIS),
        headInner, headOuter
    ).apply {
        children.forEach {
            it.scaleX *= headScale
            it.scaleY *= headScale
            it.scaleZ *= headScale
        }
    }
    private val body = SkinGroup(
        Rotate(0.0, Rotate.X_AXIS),
        Rotate(0.0, Rotate.Y_AXIS),
        Rotate(0.0, Rotate.Z_AXIS),
        bodyInner, bodyOuter
    )
    private val lArm = SkinGroup(
        Rotate(0.0, 0.0, -lArmInner.height / 2.0, 0.0, Rotate.X_AXIS),
        Rotate(0.0, Rotate.Y_AXIS),
        Rotate(0.0, +lArmInner.width / 2.0, -lArmInner.height / 2.0, 0.0, Rotate.Z_AXIS),
        lArmInner, lArmOuter
    )
    private val rArm = SkinGroup(
        Rotate(0.0, 0.0, -rArmInner.height / 2.0, 0.0, Rotate.X_AXIS),
        Rotate(0.0, Rotate.Y_AXIS),
        Rotate(0.0, -rArmInner.width / 2.0, -rArmInner.height / 2.0, 0.0, Rotate.Z_AXIS),
        rArmInner, rArmOuter
    )
    private val lLeg = SkinGroup(
        Rotate(0.0, 0.0, -lLegInner.height / 2.0, 0.0, Rotate.X_AXIS),
        Rotate(0.0, Rotate.Y_AXIS),
        Rotate(0.0, 0.0, -lLegInner.height / 2.0, 0.0, Rotate.Z_AXIS),
        lLegInner, lLegOuter
    )
    private val rLeg = SkinGroup(
        Rotate(0.0, 0.0, -rLegInner.height / 2.0, 0.0, Rotate.X_AXIS),
        Rotate(0.0, Rotate.Y_AXIS),
        Rotate(0.0, 0.0, -rLegInner.height / 2.0, 0.0, Rotate.Z_AXIS),
        rLegInner, rLegOuter
    )

    var xRotate = Rotate(15.0, Rotate.X_AXIS)
    var yRotate = Rotate(45.0, Rotate.Y_AXIS)
    var zRotate = Rotate(-15.0, Rotate.Z_AXIS)
    var translate = Translate(0.0, 0.0, -80.0)
    var scale = Scale(1.0, 1.0)

    var subScene: SubScene
    var root = Group()
    var camera = PerspectiveCamera(true).apply {
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
            camera = this@SkinCanvas.camera
            children.add(this)
        }
    }

    private fun createPlayerModel() {
        head.translateY = -(bodyInner.height + headInner.height * headScale) / 2.0
        lArm.translateX = +(bodyInner.width + lArmInner.width) / 2.0
        rArm.translateX = -(bodyInner.width + rArmInner.width) / 2.0
        lLeg.translateX = +(bodyInner.width - lLegInner.width) / 2.0
        rLeg.translateX = -(bodyInner.width - rLegInner.width) / 2.0
        lLeg.translateY = +(bodyInner.height + lLegInner.height) / 2.0
        rLeg.translateY = +(bodyInner.height + rLegInner.height) / 2.0
        root.transforms.addAll(xRotate)
        root.children.addAll(head, body, lArm, rArm, lLeg, rLeg)
    }

    fun homo() {
        xRotate.angle = .0
        yRotate.angle = 30.0
        zRotate.angle = .0
        translate.x += 5
        scale.apply {
            x = 0.9
            y = 0.9
        }
        head.apply {
            transforms.add(Rotate(30.0, Rotate.Y_AXIS))
        }
        lArm.apply {
            transforms.add(Rotate(-15.0, Rotate.X_AXIS))
            translateZ -= lArmInner.depth / 2
        }
        rArm.apply {
            transforms.add(Rotate(-15.0, Rotate.X_AXIS))
            translateZ -= rArmInner.depth / 2
        }
        lLeg.apply {
            transforms.addAll(
                Rotate(-80.0, Rotate.X_AXIS),
                Rotate(-15.0, Rotate.Z_AXIS)
            )
            translateX += lLegOuter.width / 2.5
            translateZ -= lLegInner.height / 2
            translateY -= lLegInner.height / 2.7
        }
        rLeg.apply {
            transforms.addAll(
                Rotate(-80.0, Rotate.X_AXIS),
                Rotate(15.0, Rotate.Z_AXIS)
            )
            translateX -= lLegOuter.width / 2.5
            translateZ -= lLegInner.height / 2
            translateY -= lLegInner.height / 2.7
        }
    }

    fun sneak() {
        root.translateZ += 3
        xRotate.angle = 10.0
        yRotate.angle = 315.0
        zRotate.angle = 10.0
        head.apply {
            translateZ -= 4.8
            translateY += 3
        }
        lArm.apply {
            transforms.add(Rotate(30.0, Rotate.X_AXIS))
            translateZ -= lArmInner.depth / 2 + 1
            translateY += 1.6
        }
        rArm.apply {
            transforms.add(Rotate(30.0, Rotate.X_AXIS))
            translateZ -= rArmInner.depth / 2 + 1
            translateY += 1.6
        }
        body.apply {
            transforms.add(Rotate(30.0, Rotate.X_AXIS))
            translateZ -= bodyInner.depth / 2
            translateY += 0.8
        }
    }

    fun unsneak() {
        root.translateZ -= 3
        xRotate.angle = 10.0
        yRotate.angle = 315.0
        zRotate.angle = 10.0
        head.apply {
            translateZ += 4.8
            translateY -= 3
        }
        lArm.apply {
            transforms.add(Rotate(-30.0, Rotate.X_AXIS))
            translateZ += lArmInner.depth / 2 + 1
            translateY -= 1.6
        }
        rArm.apply {
            transforms.add(Rotate(-30.0, Rotate.X_AXIS))
            translateZ += rArmInner.depth / 2 + 1
            translateY -= 1.6
        }
        body.apply {
            transforms.add(Rotate(-30.0, Rotate.X_AXIS))
            translateZ += bodyInner.depth / 2
            translateY -= 0.8
        }
    }
}
