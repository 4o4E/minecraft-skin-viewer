plugins {
    kotlin("jvm") version Versions.KOTLIN
    kotlin("plugin.serialization") version Versions.KOTLIN
    id("com.github.johnrengelman.shadow") version "7.1.2"
    application
}

kotlin {
    jvmToolchain(11)
}

allprojects {
    apply(plugin = "org.gradle.maven-publish")
    apply(plugin = "org.gradle.java-library")
    apply(plugin = "org.gradle.application")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.github.johnrengelman.shadow")

    group = Versions.GROUP
    version = Versions.VERSION

    repositories {
        maven("https://nexus.e404.top:3443/repository/maven-snapshots/")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        mavenCentral()
    }

    dependencies {
        // slf4j
        implementation("org.slf4j:slf4j-api:2.0.17")
        // skiko-util
        implementation("top.e404.skiko-util:skiko-util-draw:${Versions.SKIKO_UTILS}")
        implementation("top.e404.skiko-util:skiko-util-gif-codec:${Versions.SKIKO_UTILS}")
        // coroutines
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        // test
        testImplementation(kotlin("test", Versions.KOTLIN))

        if (!name.startsWith("http-server-")) return@dependencies
        val os = name.removePrefix("http-server-")
        // impl
        implementation(project(":http-server"))
        // skiko
        implementation(skiko(
            when (os) {
                "mac" -> "macos-x64"
                "win" -> "windows-x64"
                else -> "linux-x64"
            }
        ))
    }

    application {
        mainClass.set("top.e404.skin.server.App")
        applicationDefaultJvmArgs = listOf(
            "-Dio.netty.tryReflectionSetAccessible=true",
            "--add-opens",
            "java.base/jdk.internal.misc=ALL-UNNAMED"
        )
    }

    tasks {
        runShadow {
            workingDir = rootDir.resolve("run")
            doFirst {
                if (workingDir.isFile) workingDir.delete()
                workingDir.mkdirs()
            }
        }

        shadowJar {
            archiveFileName.set("${project.name}.jar")
        }

        build {
            if (project.name.startsWith("http-server-")) {
                dependsOn(shadowJar)
            }
        }

        test {
            useJUnitPlatform()
            workingDir = rootDir.resolve("run")
        }
    }
}
