package top.e404.skin.test.sql

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import top.e404.skin.server.sql.mapper.SkinMapper
import top.e404.skin.server.sql.useSkinMapper

class TestMapper {
    private fun testMapper(block: (SkinMapper) -> Unit) {
        runBlocking(Dispatchers.IO) {
            useSkinMapper(block)
        }
    }

    @Test
    fun testAdd() {
        testMapper {
            it.addRow("0113a2aba768477baa4a566f0f093a64","PvP",true,1683900897724,"b6f30602ff221c02262162c11b3f254bd6e8e83008fab67e920a68a447148f5d")
            it.addRow("026efb201fb84a75a3f51d61860d643b","flavoryberry",true,1683896843186,"6b2ac71755b51c8b12b9e4e238d6474a5b91f95c2f5f8a348882594c027cdeff")
            it.addRow("3692df6959e34245b3da53f1a1f84df1","A_noxia",true,1683549069182,"8952484a401b4f282837a7920f393edb993de413c3af90a5eeb3d27746e2b533")
            it.addRow("5b4124b5202145ccac23378a24c1253e","UnfairLobster",true,1683900438398,"baa18cd0a98ecb419bb5b6d5e7c827181fa7eec9993759fdc4b4d3ce7b7e76fd")
            it.addRow("c53db9316d2d489996d183975a99eb6d","ABC",true,1683900907652,"107edeac9a1cb36d82c8c839fc17542b932e02f855dcaddac877de063ff5befb")
        }
    }

    @Test
    fun testGetByName() {
        testMapper {
            println(it.getByName("404E"))
        }
    }

    @Test
    fun testTimeout() {
        testMapper {
            println(it.getTimeout(System.currentTimeMillis()))
        }
    }

    @Test
    fun testDelete() {
        val list = mutableListOf("0113a2aba768477baa4a566f0f093a64", "026efb201fb84a75a3f51d61860d643b", "3692df6959e34245b3da53f1a1f84df1", "5b4124b5202145ccac23378a24c1253e", "c53db9316d2d489996d183975a99eb6d")
        testMapper {
            println(it.delete(list))
        }
    }
}
