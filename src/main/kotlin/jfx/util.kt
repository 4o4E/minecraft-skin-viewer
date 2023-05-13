package top.e404.skin.jfx

import javafx.application.Platform
import javafx.scene.SnapshotParameters
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

fun Image.png(): ByteArray {
    val w = width.toInt()
    val h = height.toInt()
    val img = BufferedImage(w, h, 2)
    val reader = pixelReader
    for (x in 0 until w) for (y in 0 until h) {
        img.setRGB(x, y, reader.getArgb(x, y))
    }
    return ByteArrayOutputStream().run {
        ImageIO.write(img, "png", this)
        toByteArray()
    }
}

fun Image.subImage(x: Int, y: Int, w: Int, h: Int): Image {
    if (x + w > width) throw IllegalArgumentException("x($x) + w($w) > width($width)")
    if (y + h > height) throw IllegalArgumentException("y($y) + h($h) > height($height)")
    val img = WritableImage(w, h)
    val reader = pixelReader
    val writer = img.pixelWriter
    for (tx in 0 until w) for (ty in 0 until h) {
        writer.setArgb(
            tx, ty,
            reader.getArgb(tx + x, ty + y)
        )
    }
    return img
}

/**
 * 格式化材质, 从旧格式贴图转移为新格式
 *
 * @return 贴图
 */
fun Image.format(): Image {
    if (width == height) return this
    val multiple = (width / 64).toInt()
    val new = WritableImage(width.toInt(), height.toInt() * 2)
    val reader = pixelReader
    val writer = new.pixelWriter
    // 旧皮肤
    writer.setPixels(
        0, 0,
        64 * multiple, 32 * multiple,
        reader,
        0, 0,
    )
    // ll
    writer.setPixels(
        16 * multiple, 48 * multiple,
        16 * multiple, 16 * multiple,
        reader,
        0, 16 * multiple,
    )
    // la
    writer.setPixels(
        32 * multiple, 48 * multiple,
        16 * multiple, 16 * multiple,
        reader,
        40 * multiple, 16 * multiple,
    )
    return new
}

fun Image.enlarge(multiple: Int): Image {
    val new = WritableImage(width.toInt() * multiple, height.toInt() * multiple)
    val reader = pixelReader
    val writer = new.pixelWriter
    for (x in 0 until width.toInt()) for (y in 0 until height.toInt()) {
        for (tx in 0 until multiple) for (ty in 0 until multiple) {
            writer.setArgb(x * multiple + tx, y * multiple + ty, reader.getArgb(x, y))
        }
    }
    return new
}

fun runTask(block: () -> Unit) {
    var t: Throwable? = null
    var done = false
    Platform.runLater {
        runCatching {
            block()
        }.onFailure {
            t = it
        }
        done = true
    }
    while (!done) Thread.sleep(100)
    t?.let { throw it }
}

fun snapshot(
    bg: Color,
    pane: Pane
) = WritableImage(
    pane.width.toInt(),
    pane.height.toInt()
).also {
    pane.snapshot(SnapshotParameters().apply { fill = bg }, it)
}
