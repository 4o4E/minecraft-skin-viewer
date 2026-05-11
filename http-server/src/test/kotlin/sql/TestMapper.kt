package top.e404.skin.test.sql

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import top.e404.skin.server.sql.Database
import top.e404.skin.server.sql.SkinDao
import top.e404.skin.server.sql.pojo.SkinData

@Disabled("需要本地 MySQL 环境，默认只保留编译检查")
class TestSkinDao {
    private fun testDao(block: suspend () -> Unit) {
        runBlocking(Dispatchers.IO) {
            Database.initTables()
            block()
        }
    }

    @Test
    fun testAdd() {
        testDao {
            SkinDao.add(SkinData("0113a2aba768477baa4a566f0f093a64", "PvP", true, 1683900897724, "b6f30602ff221c02262162c11b3f254bd6e8e83008fab67e920a68a447148f5d"))
        }
    }

    @Test
    fun testGetByName() {
        testDao {
            println(SkinDao.getByName("404E"))
        }
    }
}
