plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    api(ktor("client-core-jvm"))
    api(kotlinx("serialization-json", "1.9.0"))
    implementation(ktor("client-okhttp"))

    testImplementation(ktor("client-mock-jvm"))
}
