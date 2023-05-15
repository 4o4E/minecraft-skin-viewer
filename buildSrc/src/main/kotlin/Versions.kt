object Versions {
    const val group = "top.e404"
    const val version = "1.1.0"
    const val kotlin = "1.8.10"
    const val javafx = "11.0.2"
    const val log4j = "2.20.0"
    const val ktor = "2.2.3"
    const val kaml = "0.52.0"
    const val skiko = "0.7.58"
}

fun kotlinx(id: String, version: String) = "org.jetbrains.kotlinx:kotlinx-$id:$version"
fun javafx(module: String, os: String? = null, version: String = Versions.javafx) = buildString {
    append("org.openjfx:javafx-")
    append(module)
    append(":")
    append(version)
    if (os != null) append(":$os")
}

fun log4j(module: String, version: String = Versions.log4j) = "org.apache.logging.log4j:log4j-$module:$version"
fun ktor(module: String, version: String = Versions.ktor) = "io.ktor:ktor-$module:$version"
fun skiko(module: String, version: String = Versions.skiko) = "org.jetbrains.skiko:skiko-awt-runtime-$module:$version"
const val kaml = "com.charleskorn.kaml:kaml:${Versions.kaml}"
