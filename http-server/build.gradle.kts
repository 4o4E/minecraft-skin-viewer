plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
    // sonatype
    maven("https://s01.oss.sonatype.org/content/groups/public/")
}

dependencies {
    implementation(rootProject)
    // slf4j
    implementation("org.slf4j:slf4j-api:2.0.7")
    // log4j2
    implementation(log4j("core"))
    implementation(log4j("slf4j2-impl")) {
        exclude("org.slf4j")
    }
    // 异步
    implementation("com.lmax:disruptor:3.4.4")

    // gif
    implementation("top.e404:skiko-util-gif-codec:1.1.1")
    // skiko
    compileOnly("org.jetbrains.skiko:skiko-awt:0.7.90")

    // serialization
    implementation(kotlinx("serialization-core-jvm", "1.6.2"))
    implementation(kotlinx("serialization-json", "1.6.2"))
    // kaml
    implementation(kaml)

    // ktor
    implementation(ktor("server-core"))
    implementation(ktor("server-netty"))

    implementation(ktor("server-call-logging"))
    implementation(ktor("server-content-negotiation"))
    implementation(ktor("serialization-kotlinx-json"))

    implementation(ktor("client-core-jvm"))
    implementation(ktor("client-okhttp"))

    // coroutines
    implementation(kotlinx("coroutines-core-jvm", "1.6.4"))

    // mysql
    implementation("mysql:mysql-connector-java:8.0.30")
    // hikari
    implementation("com.zaxxer:HikariCP:5.0.1")
    // mybatis
    implementation("org.mybatis:mybatis:3.5.11")
    // ehcache
    implementation("org.mybatis.caches:mybatis-ehcache:1.2.2")

    // test
    testImplementation(kotlin("test", Versions.kotlin))
}
