package top.e404.skin.server.sql.pojo

import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import top.e404.skin.server.client
import java.io.File

@Serializable
data class SkinData(
    var uuid: String,
    var name: String,
    var slim: Boolean,
    var update: Long,
    var hash: String
) {
    companion object {
        val cacheDir = File("skin").also { it.mkdir() }
        private const val url = "https://textures.minecraft.net/texture/"
    }

    @Suppress("UNUSED")
    constructor() : this("", "", true, 0L, "")

    suspend fun updateSkinFile() {
        skinFile.writeBytes(client.get("$url$hash").readBytes())
    }

    val skinFile get() = cacheDir.resolve("$uuid.png")

    val skinBytes: ByteArray
        get() {
            if (!skinFile.exists()) runBlocking(Dispatchers.IO) { updateSkinFile() }
            return skinFile.readBytes()
        }
}
