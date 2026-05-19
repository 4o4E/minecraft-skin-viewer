package top.e404.mcsk.test

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.h2.jdbcx.JdbcDataSource
import top.e404.mcsk.server.sql.Database
import top.e404.mcsk.server.sql.SkinDao
import top.e404.mcsk.server.sql.pojo.SkinData

class SkinDaoTest {
    private lateinit var dataSource: JdbcDataSource

    @BeforeTest
    fun setUp() {
        dataSource = JdbcDataSource().apply {
            setURL("jdbc:h2:mem:skin_dao_test;MODE=MySQL;DB_CLOSE_DELAY=-1")
        }
        Database.testDataSource = dataSource
        dataSource.connection.use { connection ->
            connection.createStatement().use {
                it.executeUpdate(
                    """
                    CREATE TABLE skin
                    (
                        `uuid` VARCHAR(36) NOT NULL PRIMARY KEY,
                        `name` VARCHAR(16) NOT NULL,
                        `slim` BOOLEAN NOT NULL,
                        `update` BIGINT NOT NULL,
                        `hash` VARCHAR(64) NOT NULL,
                        INDEX `idx_skin_name` (`name`)
                    )
                    """.trimIndent()
                )
            }
        }
    }

    @AfterTest
    fun tearDown() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { it.executeUpdate("DROP ALL OBJECTS") }
        }
        Database.testDataSource = null
    }

    @Test
    fun `adds and reads skin by name and id`() = runBlocking {
        val skin = SkinData(
            uuid = "0113a2aba768477baa4a566f0f093a64",
            name = "PvP",
            slim = true,
            update = 1683900897724,
            hash = "b6f30602ff221c02262162c11b3f254bd6e8e83008fab67e920a68a447148f5d"
        )

        SkinDao.add(skin)

        assertEquals(skin, SkinDao.getByName("PvP"))
        assertEquals(skin, SkinDao.getById("0113a2aba768477baa4a566f0f093a64"))
    }

    @Test
    fun `replaces skin with the same id`() = runBlocking {
        val first = SkinData(
            uuid = "0113a2aba768477baa4a566f0f093a64",
            name = "PvP",
            slim = true,
            update = 1,
            hash = "old"
        )
        val second = first.copy(name = "PvP2", slim = false, update = 2, hash = "new")

        SkinDao.add(first)
        SkinDao.add(second)

        assertEquals(second, SkinDao.getById(first.uuid))
        assertEquals(second, SkinDao.getByName("PvP2"))
    }
}
