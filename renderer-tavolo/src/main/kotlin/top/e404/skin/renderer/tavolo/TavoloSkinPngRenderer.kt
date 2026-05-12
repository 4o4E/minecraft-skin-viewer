package top.e404.skin.renderer.tavolo

import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import top.e404.skin.core.SkinLightingMode
import top.e404.skin.core.SkinOverlayMode
import top.e404.skin.core.SkinPngRenderer
import top.e404.skin.core.SkinRenderRequest
import top.e404.skin.core.createSkinPlatform
import top.e404.tavolo.draw.render3d.OrbitCamera
import top.e404.tavolo.draw.render3d.RenderConfig
import top.e404.tavolo.draw.render3d.Vec3

class TavoloSkinPngRenderer : SkinPngRenderer {
    override val name: String = "tavolo-skia-renderSceneToImage"

    override fun startup() = Unit

    override fun renderPng(request: SkinRenderRequest): ByteArray {
        val settings = request.settings
        val image = renderMinecraftViewTavolo(
            skin = Image.makeFromEncoded(request.skinPng),
            isSlim = request.isSlim,
            renderConfig = RenderConfig(
                width = settings.width,
                height = settings.height,
                camera = OrbitCamera(
                    target = Vec3(settings.target.x, settings.target.y, settings.target.z),
                    yaw = request.yaw,
                    pitch = settings.pitch,
                    distance = settings.distance
                ),
                backgroundColor = settings.backgroundColor,
                useBackFaceCulling = false,
                antiAliasingLevel = 2,
                lightDirection = Vec3(
                    settings.lightDirection.x,
                    settings.lightDirection.y,
                    settings.lightDirection.z
                ),
                lightIntensity = when (request.lightingMode) {
                    SkinLightingMode.AMBIENT -> 1.0f
                    SkinLightingMode.DIRECTIONAL -> 0.65f
                },
                enableShadows = request.shadows
            ),
            backgroundMeshes = if (request.shadows) {
                listOf(createSkinPlatform(settings.platformTopY, settings.platformThickness))
            } else {
                emptyList()
            },
            use3DOverlay = request.overlayMode == SkinOverlayMode.THREE_D
        )
        return image.encodeToData(EncodedImageFormat.PNG)!!.bytes
    }
}
