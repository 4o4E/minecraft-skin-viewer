package top.e404.mcsk.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import top.e404.mcsk.server.main as startServer

/**
 * 手动启动 HTTP 服务。
 *
 * 该入口会阻塞当前进程，不放入 JUnit manualTest 批量任务。
 */
fun main() {
    runBlocking(Dispatchers.IO) { startServer() }
}
