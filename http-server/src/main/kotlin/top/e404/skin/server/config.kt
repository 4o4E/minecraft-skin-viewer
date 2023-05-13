package top.e404.skin.server

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import java.net.InetSocketAddress

object ConfigManager {
    private val file = File("config.yml")
    lateinit var config: Config

    fun saveDefault(): Config? {
        if (file.isDirectory) file.deleteRecursively()
        if (file.exists()) return null
        val default = Config()
        file.writeText(Yaml.default.encodeToString(default))
        return default
    }

    fun load() {
        val default = saveDefault()
        if (default != null) {
            config = default
            return
        }
        config = Yaml.default.decodeFromString(file.readText())
    }
}

@Serializable
data class Config(
    val address: String = "127.0.0.1",
    val port: Int = 2345,
    val proxy: Proxy? = null,
)

@Serializable
data class Proxy(
    val address: String = "localhost",
    val port: Int = 7890
) {
    val socketAddress get() = InetSocketAddress(address, port)
}
