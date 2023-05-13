package top.e404.skin.server.sql

import org.apache.ibatis.session.SqlSessionFactoryBuilder
import top.e404.skin.server.sql.mapper.SkinMapper
import java.io.File

private val sqlSessionFactory = HikariDataSourceFactory::class
    .java.classLoader
    .getResourceAsStream("mybatis-config.xml")!!
    .use {
        initPriority()
        SqlSessionFactoryBuilder().build(it)!!
    }

private fun initPriority() {
    val db = File("db.properties")
    if (db.exists()) return
    HikariDataSourceFactory::class
        .java.classLoader
        .getResourceAsStream("default.properties")!!
        .use {
            db.outputStream().buffered().use { os ->
                it.copyTo(os)
            }
        }
}

val session get() = sqlSessionFactory.openSession(true)!!

suspend fun <T> useSkinMapper(block: suspend (SkinMapper) -> T) = session.use {
    block.invoke(it.getMapper(SkinMapper::class.java))
}
