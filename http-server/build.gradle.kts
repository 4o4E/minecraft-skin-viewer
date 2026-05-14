plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":core"))
    // slf4j
    implementation("org.slf4j:slf4j-api:2.0.17")
    // log4j2
    implementation(log4j("core"))
    implementation(log4j("slf4j2-impl")) {
        exclude("org.slf4j")
    }
    // 异步
    implementation("com.lmax:disruptor:3.4.4")

    // serialization
    implementation(kotlinx("serialization-core-jvm", "1.9.0"))
    implementation(kotlinx("serialization-json", "1.9.0"))
    // kaml
    implementation(kaml)

    // ktor
    implementation(ktor("server-core"))
    implementation(ktor("server-netty"))

    implementation(ktor("server-call-logging"))
    implementation(ktor("server-content-negotiation-jvm"))
    implementation(ktor("serialization-kotlinx-json-jvm"))

    implementation(ktor("client-core-jvm"))
    implementation(ktor("client-okhttp"))

    // coroutines
    implementation(kotlinx("coroutines-core-jvm", "1.10.2"))

    // mysql
    implementation("mysql:mysql-connector-java:8.0.33")
    // hikari
    implementation("com.zaxxer:HikariCP:5.0.1")

    // test
    testImplementation(kotlin("test", Versions.KOTLIN))
    testImplementation("com.h2database:h2:2.2.224")
}
