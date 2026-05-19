package top.e404.mcsk.server.sql

import java.sql.Connection
import java.sql.ResultSet

data class RenderCacheRecord(
    val uuid: String,
    val paramsMd5: String,
    val ext: String,
    val skinHash: String,
    val size: Long,
    val createdAt: Long,
    val lastAccess: Long,
)

object RenderCacheDao {
    fun initTable(connection: Connection) {
        connection.createStatement().use {
            it.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS render_cache
                (
                    `uuid`       CHAR(36)        NOT NULL COMMENT '玩家 uuid',
                    `params_md5` CHAR(32)        NOT NULL COMMENT '渲染参数 MD5',
                    `ext`        VARCHAR(8)      NOT NULL COMMENT '缓存文件扩展名',
                    `skin_hash`  CHAR(64)        NOT NULL COMMENT '生成缓存时的皮肤 hash',
                    `size`       BIGINT UNSIGNED NOT NULL COMMENT '缓存文件大小',
                    `created_at` BIGINT UNSIGNED NOT NULL COMMENT '创建时间',
                    `last_access` BIGINT UNSIGNED NOT NULL COMMENT '最后访问时间',
                    PRIMARY KEY (`uuid`, `params_md5`, `ext`),
                    INDEX `idx_render_cache_lru` (`last_access`)
                ) ENGINE InnoDB
                  DEFAULT CHARSET UTF8MB4
                """.trimIndent()
            )
        }
    }

    suspend fun find(uuid: String, paramsMd5: String, ext: String): RenderCacheRecord? =
        Database.withConnection { connection ->
            connection.prepareStatement(
                """
                SELECT uuid, params_md5, ext, skin_hash, size, created_at, last_access
                FROM render_cache
                WHERE uuid = ? AND params_md5 = ? AND ext = ?
                """.trimIndent()
            ).use {
                it.setString(1, uuid)
                it.setString(2, paramsMd5)
                it.setString(3, ext)
                it.executeQuery().use { rs -> rs.toRenderCacheRecordOrNull() }
            }
        }

    suspend fun touch(uuid: String, paramsMd5: String, ext: String, lastAccess: Long) =
        Database.withConnection { connection ->
            connection.prepareStatement(
                """
                UPDATE render_cache
                SET last_access = ?
                WHERE uuid = ? AND params_md5 = ? AND ext = ?
                """.trimIndent()
            ).use {
                it.setLong(1, lastAccess)
                it.setString(2, uuid)
                it.setString(3, paramsMd5)
                it.setString(4, ext)
                it.executeUpdate()
            }
        }

    suspend fun upsert(record: RenderCacheRecord) = Database.withConnection { connection ->
        connection.prepareStatement(
            """
            INSERT INTO render_cache (`uuid`, `params_md5`, `ext`, `skin_hash`, `size`, `created_at`, `last_access`)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                `skin_hash` = VALUES(`skin_hash`),
                `size` = VALUES(`size`),
                `last_access` = VALUES(`last_access`)
            """.trimIndent()
        ).use {
            it.setString(1, record.uuid)
            it.setString(2, record.paramsMd5)
            it.setString(3, record.ext)
            it.setString(4, record.skinHash)
            it.setLong(5, record.size)
            it.setLong(6, record.createdAt)
            it.setLong(7, record.lastAccess)
            it.executeUpdate()
        }
    }

    suspend fun delete(uuid: String, paramsMd5: String, ext: String) = Database.withConnection { connection ->
        connection.prepareStatement(
            "DELETE FROM render_cache WHERE uuid = ? AND params_md5 = ? AND ext = ?"
        ).use {
            it.setString(1, uuid)
            it.setString(2, paramsMd5)
            it.setString(3, ext)
            it.executeUpdate()
        }
    }

    suspend fun deleteByUuid(uuid: String): List<RenderCacheRecord> = Database.withConnection { connection ->
        val records = connection.prepareStatement(
            """
            SELECT uuid, params_md5, ext, skin_hash, size, created_at, last_access
            FROM render_cache
            WHERE uuid = ?
            """.trimIndent()
        ).use {
            it.setString(1, uuid)
            it.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) add(rs.toRenderCacheRecord())
                }
            }
        }
        connection.prepareStatement("DELETE FROM render_cache WHERE uuid = ?").use {
            it.setString(1, uuid)
            it.executeUpdate()
        }
        records
    }

    suspend fun listByLastAccess(): List<RenderCacheRecord> = Database.withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT uuid, params_md5, ext, skin_hash, size, created_at, last_access
            FROM render_cache
            ORDER BY last_access ASC
            """.trimIndent()
        ).use {
            it.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) add(rs.toRenderCacheRecord())
                }
            }
        }
    }

    private fun ResultSet.toRenderCacheRecordOrNull(): RenderCacheRecord? {
        if (!next()) return null
        return toRenderCacheRecord()
    }

    private fun ResultSet.toRenderCacheRecord() = RenderCacheRecord(
        uuid = getString("uuid"),
        paramsMd5 = getString("params_md5"),
        ext = getString("ext"),
        skinHash = getString("skin_hash"),
        size = getLong("size"),
        createdAt = getLong("created_at"),
        lastAccess = getLong("last_access")
    )
}
