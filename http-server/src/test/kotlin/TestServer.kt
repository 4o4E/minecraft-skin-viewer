package top.e404.skin.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import top.e404.skin.server.main
import kotlin.test.Test

class TestServer {
    @Test
    fun test() {
        runBlocking(Dispatchers.IO) { main("2345") }
    }
}
