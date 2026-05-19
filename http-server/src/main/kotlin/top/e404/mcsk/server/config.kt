package top.e404.mcsk.server

import com.charleskorn.kaml.Yaml
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.ProxyType
import io.ktor.client.engine.http
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File

object ConfigManager {
    private val file = File("config.yml")
    private var loadedConfig: Config? = null
    val config: Config
        get() = loadedConfig ?: load()

    fun saveDefault(): Config? {
        if (file.isDirectory) file.deleteRecursively()
        if (file.exists()) return null
        val default = Config()
        file.writeText(Yaml.default.encodeToString(default))
        return default
    }

    fun load(): Config {
        val default = saveDefault()
        if (default != null) {
            loadedConfig = default
            return default
        }
        val loaded = Yaml.default.decodeFromString<Config>(normalizeConfigText(file.readText()))
        loadedConfig = loaded
        return loaded
    }

    internal fun normalizeConfigText(text: String): String =
        Regex("(?m)^(\\s*)address:").replace(text, "\$1host:")
}

@Serializable
data class Config(
    val host: String = "127.0.0.1",
    val port: Int = 2345,
    val proxy: Proxy? = null,
    val timeout: Long = 86400,
    val renderCache: RenderCacheConfig = RenderCacheConfig()
)

@Serializable
data class RenderCacheConfig(
    val enabled: Boolean = true,
    val dir: String = "render-cache",
    val maxBytes: Long = 2147483648L,
    val maxEntries: Int = 10000,
    val lowWatermarkRatio: Double = 0.9,
)

@Serializable
data class Proxy(
    val type: ProxyType = ProxyType.HTTP,
    val host: String = "localhost",
    val port: Int = 7890
) {
    val proxy by lazy {
        when (type) {
            ProxyType.SOCKS -> ProxyBuilder.socks(host, port)
            else -> ProxyBuilder.http("http://$host:$port/")
        }
    }
}
