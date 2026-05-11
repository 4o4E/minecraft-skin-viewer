package top.e404.skin.server.sql

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.sql.Connection
import java.util.Properties
import javax.sql.DataSource

object Database {
    private val dataSource: DataSource by lazy {
        initProperties()
        HikariDataSource(HikariConfig(Properties().apply {
            File("db.properties").inputStream().use { load(it) }
        }))
    }

    private fun initProperties() {
        val db = File("db.properties")
        if (db.exists()) return
        javaClass.classLoader
            .getResourceAsStream("default.properties")!!
            .use { input ->
                db.outputStream().buffered().use { output ->
                    input.copyTo(output)
                }
            }
    }

    suspend fun initTables() {
        withConnection { connection ->
            SkinDao.initTable(connection)
            RenderCacheDao.initTable(connection)
        }
    }

    suspend fun <T> withConnection(block: (Connection) -> T): T = withContext(Dispatchers.IO) {
        dataSource.connection.use(block)
    }
}
