package top.e404.skin.renderer.tavolo.test

import kotlin.test.Test
import kotlin.test.assertTrue
import org.jetbrains.skia.Color
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface
import top.e404.skin.core.BodyPart
import top.e404.skin.core.PlayerModel
import top.e404.skin.core.SkinTransform
import top.e404.skin.core.SkinVec3
import top.e404.skin.renderer.tavolo.renderMinecraftViewTavolo
import top.e404.tavolo.draw.render3d.OrbitCamera
import top.e404.tavolo.draw.render3d.RenderConfig
import top.e404.tavolo.draw.render3d.Vec3
import java.io.File

class BodyPartMappingManualTest {
    private val outputDir = File("manual-test-output/tavolo/body-part-mapping")
    private val tileSize = 720

    @Test
    fun renderExplodedAlexBodyParts() {
        val skinFile = File("alex_skin.png")
        assertTrue(skinFile.isFile, "Put alex_skin.png in the run directory first.")

        outputDir.mkdirs()
        val skin = Image.makeFromEncoded(skinFile.readBytes())
        val pose = explodedPose(isSlim = true, gap = 4f)
        val renderedViews = pitches.flatMap { pitch ->
            yaws.map { yaw ->
                renderMinecraftViewTavolo(
                    skin = skin,
                    isSlim = true,
                    renderConfig = RenderConfig(
                        width = tileSize,
                        height = tileSize,
                        camera = OrbitCamera(Vec3(0f, 10f, 0f), yaw = yaw, pitch = pitch, distance = 58f),
                        backgroundColor = Color.makeRGB(32, 34, 38),
                        lightDirection = Vec3(0.4f, 0.8f, 0.6f).normalized(),
                        lightIntensity = 0.95f,
                        antiAliasingLevel = 2
                    ),
                    pose = pose,
                    use3DOverlay = false
                )
            }
        }

        outputDir.resolve("body_parts_alex_exploded.png")
            .writeBytes(stitch(renderedViews, columns = yaws.size).encodeToData(EncodedImageFormat.PNG)!!.bytes)
    }

    private fun explodedPose(
        isSlim: Boolean,
        gap: Float,
    ): Map<BodyPart, List<SkinTransform>> {
        val model = PlayerModel(isSlim)
        val bodyDims = BodyPart.BODY.getDims(isSlim)
        val headDims = BodyPart.HEAD.getDims(isSlim)
        val rightArmDims = BodyPart.RIGHT_ARM.getDims(isSlim)
        val leftArmDims = BodyPart.LEFT_ARM.getDims(isSlim)
        val rightLegDims = BodyPart.RIGHT_LEG.getDims(isSlim)
        val leftLegDims = BodyPart.LEFT_LEG.getDims(isSlim)

        val desired = mapOf(
            BodyPart.BODY to SkinVec3(0f, 10f, 0f),
            BodyPart.HEAD to SkinVec3(0f, 10f + bodyDims.y / 2 + gap + headDims.y / 2, 0f),
            BodyPart.RIGHT_ARM to SkinVec3(-(bodyDims.x / 2 + gap + rightArmDims.x / 2), 10f, 0f),
            BodyPart.LEFT_ARM to SkinVec3(bodyDims.x / 2 + gap + leftArmDims.x / 2, 10f, 0f),
            BodyPart.RIGHT_LEG to SkinVec3(-(gap / 2 + rightLegDims.x / 2), 10f - bodyDims.y / 2 - gap - rightLegDims.y / 2, 0f),
            BodyPart.LEFT_LEG to SkinVec3(gap / 2 + leftLegDims.x / 2, 10f - bodyDims.y / 2 - gap - leftLegDims.y / 2, 0f),
        )

        return BodyPart.entries.associateWith { part ->
            val current = model.parts.getValue(part).pos
            val target = desired.getValue(part)
            listOf(
                SkinTransform.Translate(
                    x = target.x - current.x,
                    y = target.y - current.y,
                    z = target.z - current.z
                )
            )
        }
    }

    private fun stitch(images: List<Image>, columns: Int): Image {
        val rows = (images.size + columns - 1) / columns
        val surface = Surface.makeRasterN32Premul(columns * tileSize, rows * tileSize)
        val canvas = surface.canvas
        canvas.clear(Color.makeRGB(24, 26, 30))
        images.forEachIndexed { index, image ->
            val x = (index % columns) * tileSize
            val y = (index / columns) * tileSize
            canvas.drawImage(image, x.toFloat(), y.toFloat())
        }
        return surface.makeImageSnapshot()
    }

    private val yaws = listOf(45f, 135f, 225f, 315f)
    private val pitches = listOf(25f, -25f)
}
