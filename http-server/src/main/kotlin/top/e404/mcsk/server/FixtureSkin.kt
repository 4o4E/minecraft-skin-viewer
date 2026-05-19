package top.e404.mcsk.server

import top.e404.mcsk.server.sql.pojo.SkinData
import java.io.File
import java.security.MessageDigest

object FixtureSkin {
    private val filePath: String? = System.getProperty("skin.fixture.file")
        ?: System.getenv("SKIN_FIXTURE_FILE")
    private val name: String = System.getProperty("skin.fixture.name")
        ?: System.getenv("SKIN_FIXTURE_NAME")
        ?: "Fixture"
    private val uuid: String = System.getProperty("skin.fixture.uuid")
        ?: System.getenv("SKIN_FIXTURE_UUID")
        ?: "00000000000000000000000000000000"
    private val slim: Boolean = (System.getProperty("skin.fixture.slim")
        ?: System.getenv("SKIN_FIXTURE_SLIM"))
        ?.toBooleanStrictOrNull()
        ?: true

    val enabled: Boolean
        get() = filePath != null

    fun byName(name: String): SkinData? =
        if (enabled && name.equals(this.name, ignoreCase = true)) data() else null

    fun byId(uuid: String): SkinData? =
        if (enabled && uuid.equals(this.uuid, ignoreCase = true)) data() else null

    fun refreshByName(name: String): Boolean =
        byName(name) != null

    fun refreshById(uuid: String): Boolean =
        byId(uuid) != null

    private fun data(): SkinData {
        val source = File(requireNotNull(filePath) { "Fixture skin file is not configured" })
        require(source.isFile) { "Fixture skin file does not exist: ${source.absolutePath}" }
        val bytes = source.readBytes()
        val data = SkinData(
            uuid = uuid,
            name = name,
            slim = slim,
            update = System.currentTimeMillis(),
            hash = bytes.sha256()
        )
        data.skinFile.parentFile.mkdirs()
        if (!data.skinFile.isFile || !data.skinFile.readBytes().contentEquals(bytes)) {
            data.skinFile.writeBytes(bytes)
        }
        return data
    }

    private fun ByteArray.sha256(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(this)
            .joinToString("") { "%02x".format(it) }
}
