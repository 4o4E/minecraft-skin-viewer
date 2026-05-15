plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
    application
}

repositories {
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    // slf4j
    implementation("org.slf4j:slf4j-api:2.0.17")
    // skiko
    api("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:${Versions.SKIKO}")
    implementation("top.e404.tavolo:tavolo-gif-codec:${Versions.TAVOLO}")
    testRuntimeOnly(skiko("linux-x64"))
    // test
    testImplementation(kotlin("test", Versions.KOTLIN))
}
