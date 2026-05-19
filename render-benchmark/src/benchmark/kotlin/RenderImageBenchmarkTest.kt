package top.e404.mcsk.benchmark

import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

class RenderImageBenchmarkTest {
    @Test
    fun `对比生成一张皮肤渲染 PNG 的耗时`() {
        val results = try {
            RenderImageBenchmark.run()
        } finally {
            RenderImageBenchmark.shutdown()
        }

        RenderImageBenchmark.writeReport(results)
        println(RenderImageBenchmark.report(results))

        results.filter { it.status == "ok" }.forEach { result ->
            assertTrue((result.pngBytes ?: 0) > 0, "${result.scheme} 应生成非空 PNG")
        }
        val reportDir = File(System.getProperty("skin.benchmark.reportDir", "build/reports/render-image-benchmark"))
        results.filter { it.status == "ok" }.forEach { result ->
            val sampleImage = result.sampleImage ?: error("${result.scheme} 缺少样图路径")
            assertTrue(
                reportDir.resolve(sampleImage).hasForegroundPixels(),
                "${result.scheme} 的样图不应是纯背景: $sampleImage"
            )
        }
        assertTrue(results.all { it.status == "ok" }, "所有变量组合都应输出可比较结果")
    }

    private fun File.hasForegroundPixels(): Boolean {
        val image = ImageIO.read(this)
        val background = image.getRGB(0, 0)
        var changedPixels = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if (image.getRGB(x, y) != background) {
                    changedPixels++
                    if (changedPixels > 100) return true
                }
            }
        }
        return false
    }
}
