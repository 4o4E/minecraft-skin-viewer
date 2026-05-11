package top.e404.skin.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import top.e404.skin.server.Mojang

class TestApi {
    @Test
    fun testGetId() {
        runBlocking(Dispatchers.IO) {
            println(Mojang.getIdByName("404E"))
        }
    }
}
