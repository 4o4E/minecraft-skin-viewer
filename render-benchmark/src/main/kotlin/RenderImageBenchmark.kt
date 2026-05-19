package top.e404.mcsk.benchmark

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.IRect
import top.e404.mcsk.core.SkinLightingMode
import top.e404.mcsk.core.SkinOverlayMode
import top.e404.mcsk.core.SkinPngRenderer
import top.e404.mcsk.core.SkinRenderRequest
import top.e404.mcsk.core.SkinRenderSettings
import top.e404.mcsk.core.SkinRenderVec3
import top.e404.mcsk.renderer.opengl.OpenGlSkinPngRenderer
import top.e404.mcsk.renderer.tavolo.TavoloSkinPngRenderer
import java.io.File
import java.util.Locale
import kotlin.system.measureNanoTime

private const val WIDTH = 600
private const val HEIGHT = 900
private const val WARMUP_IMAGES = 2
private const val MULTI_IMAGES = 8
private const val SAMPLE_YAW = 45f

internal data class BenchmarkRenderSettings(
    val width: Int = WIDTH,
    val height: Int = HEIGHT,
    val target: SkinRenderVec3 = SkinRenderVec3(0f, 10f, 0f),
    val pitch: Float = 20f,
    val distance: Float = 58f,
    val backgroundColor: Int = Color.makeRGB(28, 32, 38),
    val lightDirection: SkinRenderVec3 = SkinRenderVec3(0.35f, 0.9f, 0.45f).normalized(),
    val platformTopY: Float = -8.2f,
    val platformThickness: Float = 2f,
)

internal enum class OverlayMode(val label: String) {
    NONE("none"),
    FLAT("flat"),
    THREE_D("3d")
}

internal enum class LightingMode(val label: String) {
    AMBIENT("ambient"),
    DIRECTIONAL("directional")
}

internal data class BenchmarkScenario(
    val overlayMode: OverlayMode,
    val lightingMode: LightingMode,
    val shadows: Boolean,
) {
    val id: String = "overlay=${overlayMode.label};light=${lightingMode.label};shadow=$shadows"
}

data class RenderBenchmarkResult(
    val scenario: String,
    val overlayMode: String,
    val lightingMode: String,
    val shadows: Boolean,
    val scheme: String,
    val status: String,
    val startupMs: Double?,
    val oneImageMs: Double?,
    val multiImageMs: Double?,
    val averageImageMs: Double?,
    val pngBytes: Int?,
    val sampleImage: String?,
    val note: String = "",
)

object RenderImageBenchmark {
    private val settings = BenchmarkRenderSettings()

    private val scenarios = OverlayMode.entries.flatMap { overlayMode ->
        LightingMode.entries.flatMap { lightingMode ->
            listOf(false, true).map { shadows ->
                BenchmarkScenario(overlayMode, lightingMode, shadows)
            }
        }
    }

    fun run(): List<RenderBenchmarkResult> {
        val skinWithOverlay = createBenchmarkSkinPng(includeOuterLayer = true)
        val skinWithoutOverlay = createBenchmarkSkinPng(includeOuterLayer = false)
        val renderers = listOf(
            TavoloSkinPngRenderer(),
            OpenGlSkinPngRenderer()
        )
        return try {
            scenarios.flatMap { scenario ->
                renderers.map { renderer -> runRenderer(renderer, scenario, skinWithOverlay, skinWithoutOverlay) }
            }
        } finally {
            renderers.forEach { it.close() }
        }
    }

    fun report(results: List<RenderBenchmarkResult>): String = buildString {
        appendLine("# Minecraft 皮肤渲染生成 PNG 耗时对比")
        appendLine()
        appendLine("尺寸: ${settings.width}x${settings.height}, 预热图片数: $WARMUP_IMAGES, 多图数量: $MULTI_IMAGES")
        appendLine()
        appendLine("共享视角: target=${settings.target}, yaw=$SAMPLE_YAW, pitch=${settings.pitch}, distance=${settings.distance}")
        appendLine("共享光照: lightDirection=${settings.lightDirection}")
        appendLine()
        appendLine("- `overlay=none`：不渲染外层皮肤。")
        appendLine("- `overlay=flat`：渲染放大 cuboid 外层皮肤。")
        appendLine("- `overlay=3d`：渲染逐像素体素化 3D 外层皮肤。")
        appendLine("- `shadow=true`：Tavolo 使用 CPU shadow map；OpenGL 使用 GPU depth texture shadow map。")
        appendLine()
        appendLine("## 可比性说明")
        appendLine()
        appendLine("- 两个方案使用同一张 64x64 测试皮肤、同一输出尺寸、同一 target/yaw/pitch/distance 和同一平台尺寸。")
        appendLine("- 两个实现都复用 core 的 `createMinecraftPlayerMeshes`，因此基础模型、flat 外层和 3D 外层皮肤的几何来源一致。")
        appendLine("- 耗时差异主要来自渲染管线：Tavolo 当前走 CPU 软件光栅；OpenGL 走 LWJGL + GLFW 隐藏 OpenGL context、FBO 渲染并读回 PNG。")
        appendLine("- OpenGL 当前仍是 benchmark 模块内的实现形态，后续服务端落地时应把 GLFW context 替换为 EGL/OSMesa，并把 immediate mode 改为 VBO/shader batch。")
        appendLine()
        scenarios.forEach { scenario ->
            appendLine("## overlay=${scenario.overlayMode.label}, light=${scenario.lightingMode.label}, shadow=${scenario.shadows}")
            appendLine()
            appendLine("| tavolo-skia-renderSceneToImage | opengl-lwjgl-fbo |")
            appendLine("| --- | --- |")
            appendLine("| ${results.imageMarkdown(scenario, "tavolo-skia-renderSceneToImage")} | ${results.imageMarkdown(scenario, "opengl-lwjgl-fbo")} |")
            appendLine()
            appendLine("| 方案 | 状态 | 启动耗时(ms) | 生成一张 PNG(ms) | 生成多张 PNG(ms) | 平均每张(ms) | PNG 字节数 | 备注 |")
            appendLine("| --- | --- | ---: | ---: | ---: | ---: | ---: | --- |")
            results.filter { it.scenario == scenario.id }.forEach { result ->
                appendLine(
                    "| ${result.scheme} | ${result.status} | ${result.startupMs.formatNullableMs()} | " +
                        "${result.oneImageMs.formatNullableMs()} | ${result.multiImageMs.formatNullableMs()} | " +
                        "${result.averageImageMs.formatNullableMs()} | ${result.pngBytes ?: ""} | ${result.note} |"
                )
            }
            appendLine()
        }
    }

    fun writeReport(results: List<RenderBenchmarkResult>) {
        val reportDir = reportDir()
        reportDir.mkdirs()
        reportDir.resolve("summary.csv").writeText(
            buildString {
                appendLine("scenario,overlay_mode,lighting_mode,shadows,scheme,status,startup_ms,one_image_ms,multi_image_ms,average_image_ms,png_bytes,sample_image,note")
                results.forEach { result ->
                    appendLine(
                        listOf(
                            result.scenario,
                            result.overlayMode,
                            result.lightingMode,
                            result.shadows.toString(),
                            result.scheme,
                            result.status,
                            result.startupMs.formatNullableMs(),
                            result.oneImageMs.formatNullableMs(),
                            result.multiImageMs.formatNullableMs(),
                            result.averageImageMs.formatNullableMs(),
                            result.pngBytes?.toString().orEmpty(),
                            result.sampleImage.orEmpty(),
                            result.note
                        ).joinToString(",") { it.csvEscape() }
                    )
                }
            }
        )
        reportDir.resolve("summary.md").writeText(report(results))
    }

    fun shutdown() = Unit

    private fun runRenderer(
        renderer: SkinPngRenderer,
        scenario: BenchmarkScenario,
        skinWithOverlay: ByteArray,
        skinWithoutOverlay: ByteArray,
    ): RenderBenchmarkResult {
        val startupNs = measureNanoTime {
            renderer.startup()
        }

        repeat(WARMUP_IMAGES) { index ->
            renderer.renderPng(request(scenario, 35f + index * 12f, skinWithOverlay, skinWithoutOverlay))
        }

        var oneImageBytes = ByteArray(0)
        val oneImageNs = measureNanoTime {
            oneImageBytes = renderer.renderPng(request(scenario, SAMPLE_YAW, skinWithOverlay, skinWithoutOverlay))
        }
        val sampleImage = writeSampleImage(renderer, scenario, oneImageBytes)

        var lastBytes = ByteArray(0)
        val multiImageNs = measureNanoTime {
            repeat(MULTI_IMAGES) { index ->
                lastBytes = renderer.renderPng(request(scenario, 25f + index * 10f, skinWithOverlay, skinWithoutOverlay))
            }
        }

        return RenderBenchmarkResult(
            scenario = scenario.id,
            overlayMode = scenario.overlayMode.label,
            lightingMode = scenario.lightingMode.label,
            shadows = scenario.shadows,
            scheme = renderer.name,
            status = "ok",
            startupMs = startupNs.toMs(),
            oneImageMs = oneImageNs.toMs(),
            multiImageMs = multiImageNs.toMs(),
            averageImageMs = multiImageNs.toMs() / MULTI_IMAGES,
            pngBytes = maxOf(oneImageBytes.size, lastBytes.size),
            sampleImage = sampleImage,
            note = resultNote(renderer, scenario)
        )
    }

    private fun request(
        scenario: BenchmarkScenario,
        yaw: Float,
        skinWithOverlay: ByteArray,
        skinWithoutOverlay: ByteArray,
    ): SkinRenderRequest =
        SkinRenderRequest(
            skinPng = if (scenario.overlayMode == OverlayMode.NONE) skinWithoutOverlay else skinWithOverlay,
            isSlim = true,
            yaw = yaw,
            settings = settings.toSkinRenderSettings(scenario.lightingMode),
            overlayMode = scenario.overlayMode.toSkinOverlayMode(),
            lightingMode = scenario.lightingMode.toSkinLightingMode(),
            shadows = scenario.shadows
        )

    private fun writeSampleImage(renderer: SkinPngRenderer, scenario: BenchmarkScenario, bytes: ByteArray): String {
        val reportDir = reportDir()
        val imagesDir = reportDir.resolve("images")
        imagesDir.mkdirs()
        val relativePath = "images/${scenario.fileId()}-${renderer.name}.png"
        reportDir.resolve(relativePath).writeBytes(bytes)
        return relativePath
    }

    private fun reportDir(): File =
        File(System.getProperty("skin.benchmark.reportDir", "build/reports/render-image-benchmark"))
}

fun main() {
    val mode = System.getProperty("skin.benchmark.mode") ?: System.getenv("SKIN_BENCHMARK_MODE")
    if (mode == "javafx-quality-demo") {
        JavaFxQualityDemo.run()
        return
    }
    try {
        val results = RenderImageBenchmark.run()
        RenderImageBenchmark.writeReport(results)
        println(RenderImageBenchmark.report(results))
    } finally {
        RenderImageBenchmark.shutdown()
    }
}

private fun createBenchmarkSkinPng(includeOuterLayer: Boolean): ByteArray {
    val bitmap = Bitmap().apply {
        allocN32Pixels(64, 64)
    }
    val canvas = Canvas(bitmap)
    canvas.clear(Color.makeRGB(88, 132, 190))

    fun rect(x: Int, y: Int, w: Int, h: Int, color: Int) {
        bitmap.erase(color, IRect.makeXYWH(x, y, w, h))
    }

    rect(8, 8, 8, 8, Color.makeRGB(238, 183, 132))
    rect(20, 20, 8, 12, Color.makeRGB(45, 90, 170))
    rect(44, 20, 4, 12, Color.makeRGB(238, 183, 132))
    rect(4, 20, 4, 12, Color.makeRGB(70, 92, 130))
    rect(20, 52, 4, 12, Color.makeRGB(48, 62, 92))
    rect(4, 20, 4, 12, Color.makeRGB(48, 62, 92))

    rect(32, 0, 32, 16, Color.TRANSPARENT)
    rect(16, 32, 48, 32, Color.TRANSPARENT)
    if (includeOuterLayer) {
        rect(40, 8, 8, 8, Color.makeARGB(190, 50, 58, 74))
        rect(20, 36, 8, 12, Color.makeARGB(160, 35, 75, 145))
    }

    return Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG)!!.bytes
}

private fun Double?.formatNullableMs(): String = this?.let { String.format(Locale.US, "%.3f", it) }.orEmpty()

private fun Long.toMs(): Double = this / 1_000_000.0

private fun BenchmarkScenario.fileId(): String =
    "overlay-${overlayMode.label}-light-${lightingMode.label}-shadow-$shadows"

private fun BenchmarkRenderSettings.toSkinRenderSettings(lightingMode: LightingMode): SkinRenderSettings =
    SkinRenderSettings(
        width = width,
        height = height,
        target = target,
        pitch = pitch,
        distance = distance,
        backgroundColor = backgroundColor,
        lightDirection = lightDirection,
        platformTopY = platformTopY,
        platformThickness = platformThickness,
        lightIntensity = when (lightingMode) {
            LightingMode.AMBIENT -> 1.0f
            LightingMode.DIRECTIONAL -> 0.65f
        },
        antiAliasingLevel = 2
    )

private fun OverlayMode.toSkinOverlayMode(): SkinOverlayMode = when (this) {
    OverlayMode.NONE -> SkinOverlayMode.NONE
    OverlayMode.FLAT -> SkinOverlayMode.FLAT
    OverlayMode.THREE_D -> SkinOverlayMode.THREE_D
}

private fun LightingMode.toSkinLightingMode(): SkinLightingMode = when (this) {
    LightingMode.AMBIENT -> SkinLightingMode.AMBIENT
    LightingMode.DIRECTIONAL -> SkinLightingMode.DIRECTIONAL
}

private fun resultNote(renderer: SkinPngRenderer, scenario: BenchmarkScenario): String =
    when (renderer.name) {
        "opengl-lwjgl-fbo" -> when {
            scenario.overlayMode == OverlayMode.THREE_D && scenario.shadows -> "GPU shadow map + 3D 外层 detail shadow"
            scenario.shadows -> "GPU shadow map"
            scenario.overlayMode == OverlayMode.THREE_D -> "逐像素 3D 外层"
            else -> ""
        }
        else -> ""
    }

private fun List<RenderBenchmarkResult>.imageMarkdown(scenario: BenchmarkScenario, scheme: String): String =
    firstOrNull { it.scenario == scenario.id && it.scheme == scheme }?.sampleImage
        ?.let { "![$scheme]($it)" }
        ?: ""

private fun String.csvEscape(): String {
    val escaped = replace("\"", "\"\"")
    return if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"$escaped\"" else escaped
}
