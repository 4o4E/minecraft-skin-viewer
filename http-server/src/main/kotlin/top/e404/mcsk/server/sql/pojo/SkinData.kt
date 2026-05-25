package top.e404.mcsk.server.sql.pojo

import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import top.e404.mcsk.server.Mojang
import top.e404.mcsk.server.client
import java.io.File

@Serializable
data class SkinData(
    var uuid: String,
    var name: String,
    var slim: Boolean,
    var update: Long,
    var hash: String,
    var capeHash: String? = null,
) {
    companion object {
        val cacheDir = File("skin").also { it.mkdir() }
    }

    @Suppress("UNUSED")
    constructor() : this("", "", true, 0L, "", null)

    suspend fun updateSkinFile() {
        skinFile.writeBytes(client.get("${Mojang.TEXTURE_URL}$hash").readBytes())
    }

    suspend fun updateCapeFile() {
        val hash = capeHash ?: return
        capeFile.writeBytes(client.get("${Mojang.TEXTURE_URL}$hash").readBytes())
    }

    val skinFile get() = cacheDir.resolve("$uuid.png")

    val capeFile get() = cacheDir.resolve("$uuid-cape.png")

    val skinBytes: ByteArray
        get() {
            if (!skinFile.exists()) runBlocking(Dispatchers.IO) { updateSkinFile() }
            return skinFile.readBytes()
        }

    val capeBytes: ByteArray?
        get() {
            capeHash ?: return null
            if (!capeFile.exists()) runBlocking(Dispatchers.IO) { updateCapeFile() }
            return capeFile.readBytes()
        }
}
