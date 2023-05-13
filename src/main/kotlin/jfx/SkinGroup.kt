package top.e404.skin.jfx

import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.transform.Rotate

class SkinGroup(
    xRotate: Rotate,
    yRotate: Rotate,
    zRotate: Rotate,
    vararg nodes: Node,
) : Group() {
    init {
        children.addAll(*nodes)
        transforms.addAll(xRotate, yRotate, zRotate)
    }
}
