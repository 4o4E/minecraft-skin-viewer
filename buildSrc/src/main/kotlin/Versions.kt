object Versions {
    const val GROUP = "top.e404"
    const val VERSION = "2.2.0"
    const val KOTLIN = "2.2.21"
    const val LOG4J = "2.25.2"
    const val KTOR = "2.3.13"
    const val KAML = "0.80.1"
    const val SKIKO = "0.9.30"
    const val JAVAFX = "17.0.12"
    const val LWJGL = "3.3.6"
    const val TAVOLO = "2.0.0-SNAPSHOT"
}

fun kotlinx(id: String, version: String) = "org.jetbrains.kotlinx:kotlinx-$id:$version"
fun log4j(module: String, version: String = Versions.LOG4J) = "org.apache.logging.log4j:log4j-$module:$version"
fun ktor(module: String, version: String = Versions.KTOR) = "io.ktor:ktor-$module:$version"
fun skiko(module: String, version: String = Versions.SKIKO) = "org.jetbrains.skiko:skiko-awt-runtime-$module:$version"
const val kaml = "com.charleskorn.kaml:kaml:${Versions.KAML}"
