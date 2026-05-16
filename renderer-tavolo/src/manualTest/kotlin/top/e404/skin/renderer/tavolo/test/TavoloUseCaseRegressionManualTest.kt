package top.e404.skin.renderer.tavolo.test

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue
import top.e404.skin.core.SkinRenderUseCases
import top.e404.skin.renderer.tavolo.TavoloSkinPngRenderer
import java.io.File

class TavoloUseCaseRegressionManualTest {
    @Test
    fun renderAmbientRotateGifs() = runBlocking {
        val outputDir = tavoloRenderOutputDir.apply { mkdirs() }
        TavoloSkinPngRenderer().use { renderer ->
            renderer.startup()
            for ((fileName, isSlim) in manualSkinFiles) {
                val file = File(fileName)
                assertTrue(file.isFile, "Put $fileName in the run directory first.")

                // 直接走用例层，覆盖线上旋转 GIF 的默认参数。
                val gif = SkinRenderUseCases.renderSkinRotate(
                    renderer = renderer,
                    bytes = file.readBytes(),
                    slim = isSlim,
                    backgroundColor = DEFAULT_BG,
                    frameCount = GIF_FRAME_COUNT,
                    pitchAmplitude = 20,
                    lightIntensity = null,
                    headScale = 1.0,
                    duration = GIF_FRAME_DURATION_MS,
                    showPlatform = true
                )
                outputDir.resolve("use_case_ambient_rotate_${file.nameWithoutExtension}.gif").writeBytes(gif)
            }
        }
    }

    @Test
    fun renderSneakPoseGifs() = runBlocking {
        val outputDir = tavoloRenderOutputDir.apply { mkdirs() }
        TavoloSkinPngRenderer().use { renderer ->
            renderer.startup()
            for ((fileName, isSlim) in manualSkinFiles) {
                val file = File(fileName)
                assertTrue(file.isFile, "Put $fileName in the run directory first.")

                // 直接走用例层，覆盖线上下蹲 GIF 的默认参数。
                val gif = SkinRenderUseCases.renderSneak(
                    renderer = renderer,
                    bytes = file.readBytes(),
                    slim = isSlim,
                    backgroundColor = DEFAULT_BG,
                    lightIntensity = null,
                    headScale = 1.0,
                    showPlatform = false
                )
                outputDir.resolve("use_case_sneak_pose_${file.nameWithoutExtension}.gif").writeBytes(gif)
            }
        }
    }
}
