package top.e404.skin.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import top.e404.skin.server.main
import kotlin.test.Test

class TestServer {
    @Disabled("会启动并阻塞 HTTP 服务，仅用于本地手动验证")
    @Test
    fun test() {
        runBlocking(Dispatchers.IO) { main() }
    }
}
