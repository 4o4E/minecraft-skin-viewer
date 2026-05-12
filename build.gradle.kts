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
    apply(plugin = "org.jetbrains.kotlin.jvm")
    group = Versions.GROUP
    version = Versions.VERSION

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }

    repositories {
        mavenLocal()
        maven("https://nexus.e404.top:3443/repository/maven-snapshots/")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        mavenCentral()
    }

    dependencies {
        // slf4j
        implementation("org.slf4j:slf4j-api:2.0.17")
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
}

subprojects {
    apply(plugin = "org.gradle.maven-publish")
    apply(plugin = "org.gradle.java-library")
    apply(plugin = "org.gradle.application")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.github.johnrengelman.shadow")

    extensions.configure<org.gradle.api.plugins.JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    val manualTestSourceSet = sourceSets.create("manualTest") {
        java.srcDir("src/manualTest/kotlin")
        resources.srcDir("src/manualTest/resources")
        compileClasspath += sourceSets["main"].output + sourceSets["test"].output + configurations["testRuntimeClasspath"]
        runtimeClasspath += output + compileClasspath
    }

    configurations["manualTestImplementation"].extendsFrom(configurations["testImplementation"])
    configurations["manualTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

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

        register<Test>("manualTest") {
            description = "运行需要人工准备环境、外部服务或本地资产的测试"
            group = "verification"
            testClassesDirs = manualTestSourceSet.output.classesDirs
            classpath = manualTestSourceSet.runtimeClasspath
            useJUnitPlatform()
            workingDir = rootDir.resolve("run")
            shouldRunAfter(test)
        }
    }
}
