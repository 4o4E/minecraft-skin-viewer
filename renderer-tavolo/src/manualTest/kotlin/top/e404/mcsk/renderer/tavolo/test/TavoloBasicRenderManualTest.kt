package top.e404.mcsk.renderer.tavolo.test

import kotlin.test.assertTrue
import kotlin.test.Test
import top.e404.mcsk.core.SkinRenderUseCases
import top.e404.mcsk.renderer.tavolo.TavoloSkinPngRenderer
import java.io.File

class TavoloBasicRenderManualTest {
    @Test
    fun renderFullBodyPngs() {
        tavoloRenderOutputDir.mkdirs()
        TavoloSkinPngRenderer().use { renderer ->
            renderer.startup()
            for ((fileName, isSlim) in manualSkinFiles) {
                val file = File(fileName)
                assertTrue(file.isFile, "Put $fileName in the run directory first.")
                val png = SkinRenderUseCases.renderSkin(
                    renderer = renderer,
                    bytes = file.readBytes(),
                    slim = isSlim,
                    backgroundColor = DEFAULT_BG,
                    lightIntensity = null,
                    headScale = 1.0,
                    capeBytes = wikiManualCapePng()
                )
                tavoloRenderOutputDir.resolve("rendered_$fileName").writeBytes(png)
            }
        }
    }
}
