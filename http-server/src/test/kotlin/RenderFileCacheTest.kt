package top.e404.mcsk.test

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import top.e404.mcsk.server.service.RenderFileCache

class RenderFileCacheTest {
    @Test
    fun `参数摘要不受 Map 插入顺序影响`() {
        val first = RenderFileCache.paramsMd5(
            linkedMapOf(
                "position" to "sk",
                "bg" to "ff1f1b1d",
                "light" to null,
                "aa" to 2
            )
        )
        val second = RenderFileCache.paramsMd5(
            linkedMapOf(
                "aa" to 2,
                "light" to null,
                "bg" to "ff1f1b1d",
                "position" to "sk"
            )
        )

        assertEquals(first, second)
        assertEquals(32, first.length)
    }

    @Test
    fun `参数变更会改变摘要`() {
        val png = RenderFileCache.paramsMd5(mapOf("position" to "sk", "ext" to "png"))
        val gif = RenderFileCache.paramsMd5(mapOf("position" to "sk", "ext" to "gif"))

        assertNotEquals(png, gif)
    }

    @Test
    fun `参数类型不同会生成不同摘要`() {
        val nullValue = RenderFileCache.paramsMd5(mapOf("light" to null))
        val nullText = RenderFileCache.paramsMd5(mapOf("light" to "null"))
        val numericValue = RenderFileCache.paramsMd5(mapOf("aa" to 1))
        val numericText = RenderFileCache.paramsMd5(mapOf("aa" to "1"))

        assertNotEquals(nullValue, nullText)
        assertNotEquals(numericValue, numericText)
    }
}
