package top.e404.mcsk.test

import kotlin.test.Test
import kotlin.test.assertEquals
import top.e404.mcsk.server.Config
import top.e404.mcsk.server.ConfigManager
import top.e404.mcsk.server.RenderCacheConfig

class ConfigTest {
    @Test
    fun `默认配置包含渲染缓存限制`() {
        val config = Config()

        assertEquals("127.0.0.1", config.host)
        assertEquals(2345, config.port)
        assertEquals(RenderCacheConfig(), config.renderCache)
    }

    @Test
    fun `旧 address 字段会兼容为 host`() {
        val text = """
            address: 0.0.0.0
            proxy:
              address: localhost
              port: 7890
        """.trimIndent()

        val normalized = ConfigManager.normalizeConfigText(text)

        assertEquals(
            """
            host: 0.0.0.0
            proxy:
              host: localhost
              port: 7890
            """.trimIndent(),
            normalized
        )
    }
}
