plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    application
}

application {
    mainClass.set("top.e404.skin.server.App")
    applicationDefaultJvmArgs = listOf(
        "-Dio.netty.tryReflectionSetAccessible=true",
        "--add-opens",
        "java.base/jdk.internal.misc=ALL-UNNAMED"
    )
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(project(":"))
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
    implementation("top.e404:skiko-util-gif-codec:1.0.0")
    implementation("top.e404:skiko-util-util:1.0.0")
    // skiko
    api(skiko("windows-x64"))
    api(skiko("linux-x64"))

    // serialization
    implementation(kotlinx("serialization-core-jvm", "1.4.0"))
    implementation(kotlinx("serialization-json", "1.4.0"))
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
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-client-okhttp-jvm:2.2.3")

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

repositories {
    mavenCentral()
}


tasks {
    test {
        jvmArgs = listOf(
            "-Dio.netty.tryReflectionSetAccessible=true",
            "--add-opens",
            "java.base/jdk.internal.misc=ALL-UNNAMED"
        )
        useJUnitPlatform()
        workingDir = project.projectDir.resolve("run")
        doFirst {
            if (workingDir.isFile) workingDir.delete()
            workingDir.mkdirs()
        }
    }
}
