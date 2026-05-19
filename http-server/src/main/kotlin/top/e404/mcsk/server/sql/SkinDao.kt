package top.e404.mcsk.server.sql

import top.e404.mcsk.server.sql.pojo.SkinData
import java.sql.Connection
import java.sql.ResultSet

object SkinDao {
    fun initTable(connection: Connection) {
        connection.createStatement().use {
            it.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS skin
                (
                    `uuid`   CHAR(36)        NOT NULL PRIMARY KEY COMMENT '玩家 uuid',
                    `name`   VARCHAR(16)     NOT NULL COMMENT '玩家名',
                    `slim`   BOOLEAN         NOT NULL COMMENT '是否为 slim 模型',
                    `update` BIGINT UNSIGNED NOT NULL COMMENT '最后更新时间',
                    `hash`   CHAR(64)        NOT NULL COMMENT '皮肤材质 hash',
                    INDEX `idx_skin_name` (`name`)
                ) ENGINE InnoDB
                  DEFAULT CHARSET UTF8MB4
                """.trimIndent()
            )
        }
    }

    suspend fun add(data: SkinData) = Database.withConnection { connection ->
        connection.prepareStatement(
            """
            REPLACE INTO skin (`uuid`, `name`, `slim`, `update`, `hash`)
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent()
        ).use {
            it.setString(1, data.uuid)
            it.setString(2, data.name)
            it.setBoolean(3, data.slim)
            it.setLong(4, data.update)
            it.setString(5, data.hash)
            it.executeUpdate()
        }
    }

    suspend fun getByName(name: String): SkinData? = Database.withConnection { connection ->
        connection.prepareStatement(
            "SELECT uuid, name, slim, `update`, hash FROM skin WHERE name = ?"
        ).use {
            it.setString(1, name)
            it.executeQuery().use { rs -> rs.toSkinDataOrNull() }
        }
    }

    suspend fun getById(id: String): SkinData? = Database.withConnection { connection ->
        connection.prepareStatement(
            "SELECT uuid, name, slim, `update`, hash FROM skin WHERE uuid = ?"
        ).use {
            it.setString(1, id)
            it.executeQuery().use { rs -> rs.toSkinDataOrNull() }
        }
    }

    private fun ResultSet.toSkinDataOrNull(): SkinData? {
        if (!next()) return null
        return SkinData(
            uuid = getString("uuid"),
            name = getString("name"),
            slim = getBoolean("slim"),
            update = getLong("update"),
            hash = getString("hash")
        )
    }
}
