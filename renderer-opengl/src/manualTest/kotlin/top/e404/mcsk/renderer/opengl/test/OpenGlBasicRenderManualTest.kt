package top.e404.mcsk.renderer.opengl.test

import kotlin.test.Test
import kotlin.test.assertTrue
import top.e404.mcsk.core.SkinRenderUseCases
import top.e404.mcsk.renderer.opengl.OpenGlSkinPngRenderer
import java.io.File

class OpenGlBasicRenderManualTest {
    private val renderer = OpenGlSkinPngRenderer()

    @Test
    fun renderFullBodyPngs() {
        val outputDir = openGlRenderOutputDir.apply { mkdirs() }
        renderer.startup()
        try {
            for ((fileName, isSlim) in manualSkinFiles) {
                val file = File(fileName)
                assertTrue(file.isFile, "Put $fileName in the run directory first.")
                val bytes = SkinRenderUseCases.renderSkin(
                    renderer = renderer,
                    bytes = file.readBytes(),
                    slim = isSlim,
                    backgroundColor = DEFAULT_BG,
                    lightIntensity = null,
                    headScale = 1.0,
                    capeBytes = wikiManualCapePng()
                )
                outputDir.resolve("rendered_$fileName").writeBytes(bytes)
            }
        } finally {
            renderer.close()
        }
    }
}
