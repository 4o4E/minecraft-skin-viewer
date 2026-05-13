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
        return renderPngInternal(request, Image.makeFromEncoded(request.skinPng))
    }

    override fun renderPngBatch(requests: List<SkinRenderRequest>): List<ByteArray> {
        if (requests.isEmpty()) return emptyList()
        if (!requests.canReusePreparedMeshes()) return requests.map(::renderPng)

        val first = requests.first()
        val skin = Image.makeFromEncoded(first.skinPng)
        val playerMeshes = prepareMinecraftPlayerMeshesTavolo(
            skin = skin,
            isSlim = first.isSlim,
            pose = first.pose,
            use3DOverlay = first.overlayMode == SkinOverlayMode.THREE_D
        )
        return requests.map { request ->
            val settings = request.settings
            val backgroundMeshes = if (request.showPlatform) {
                listOf(createSkinPlatform(settings.platformTopY, settings.platformThickness))
            } else {
                emptyList()
            }
            val image = renderMinecraftViewTavolo(
                playerMeshes = playerMeshes,
                renderConfig = request.renderConfig(),
                backgroundMeshes = backgroundMeshes,
                modelYaw = request.modelYaw
            )
            image.encodeToData(EncodedImageFormat.PNG)!!.bytes
        }
    }

    private fun renderPngInternal(request: SkinRenderRequest, skin: Image): ByteArray {
        val settings = request.settings
        val image = renderMinecraftViewTavolo(
            skin = skin,
            isSlim = request.isSlim,
            renderConfig = request.renderConfig(),
            backgroundMeshes = if (request.showPlatform) {
                listOf(createSkinPlatform(settings.platformTopY, settings.platformThickness))
            } else {
                emptyList()
            },
            pose = request.pose,
            use3DOverlay = request.overlayMode == SkinOverlayMode.THREE_D,
            modelYaw = request.modelYaw
        )
        return image.encodeToData(EncodedImageFormat.PNG)!!.bytes
    }

    private fun List<SkinRenderRequest>.canReusePreparedMeshes(): Boolean {
        val first = first()
        return all {
            it.skinPng.contentEquals(first.skinPng) &&
                it.isSlim == first.isSlim &&
                it.overlayMode == first.overlayMode &&
                it.pose == first.pose
        }
    }

    private fun SkinRenderRequest.renderConfig(): RenderConfig {
        val settings = settings
        return RenderConfig(
            width = settings.width,
            height = settings.height,
            camera = OrbitCamera(
                target = Vec3(settings.target.x, settings.target.y, settings.target.z),
                yaw = yaw,
                pitch = settings.pitch,
                distance = settings.distance
            ),
            backgroundColor = settings.backgroundColor,
            useBackFaceCulling = false,
            antiAliasingLevel = settings.antiAliasingLevel,
            lightDirection = Vec3(
                settings.lightDirection.x,
                settings.lightDirection.y,
                settings.lightDirection.z
            ),
            lightIntensity = settings.lightIntensity,
            enableShadows = shadows
        )
    }
}
