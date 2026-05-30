package top.e404.mcsk.renderer.tavolo.test

import kotlin.test.Test
import kotlin.test.assertTrue
import top.e404.mcsk.core.SkinRenderUseCases
import top.e404.mcsk.renderer.tavolo.TavoloSkinPngRenderer
import java.io.File

class TavoloHeadRenderManualTest {
    @Test
    fun renderHeadPngs() {
        tavoloRenderOutputDir.mkdirs()
        TavoloSkinPngRenderer().use { renderer ->
            renderer.startup()
            for ((fileName, _) in manualSkinFiles) {
                val file = File(fileName)
                assertTrue(file.isFile, "Put $fileName in the run directory first.")
                val png = SkinRenderUseCases.renderHead(
                    renderer = renderer,
                    bytes = file.readBytes(),
                    backgroundColor = DEFAULT_BG,
                    lightIntensity = null
                )
                tavoloRenderOutputDir.resolve("head_$fileName").writeBytes(png)
            }
        }
    }
}
