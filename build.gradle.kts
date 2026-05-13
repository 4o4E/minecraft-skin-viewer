plugins {
    kotlin("jvm") version Versions.KOTLIN
    kotlin("plugin.serialization") version Versions.KOTLIN
    id("com.github.johnrengelman.shadow") version "7.1.2"
    application
}

data class ServerPackageSpec(
    val renderer: String,
    val os: String,
)

fun serverPackageSpec(projectName: String): ServerPackageSpec? =
    when (projectName) {
        "http-server-win" -> ServerPackageSpec("tavolo", "win")
        "http-server-linux" -> ServerPackageSpec("tavolo", "linux")
        "http-server-mac" -> ServerPackageSpec("tavolo", "mac")
        else -> Regex("""http-server-(tavolo|opengl)-(win|linux)""")
            .matchEntire(projectName)
            ?.destructured
            ?.let { (renderer, os) -> ServerPackageSpec(renderer, os) }
    }

fun skikoOsClassifier(os: String): String =
    when (os) {
        "mac" -> "macos-x64"
        "win" -> "windows-x64"
        else -> "linux-x64"
    }

fun lwjglNativeClassifier(os: String): String =
    when (os) {
        "win" -> "natives-windows"
        "linux" -> "natives-linux"
        else -> error("LWJGL native classifier is not configured for $os")
    }

fun rendererClassName(renderer: String): String =
    when (renderer) {
        "tavolo" -> "top.e404.skin.renderer.tavolo.TavoloSkinPngRenderer"
        "opengl" -> "top.e404.skin.renderer.opengl.OpenGlSkinPngRenderer"
        else -> error("Unknown renderer $renderer")
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

        val serverPackage = serverPackageSpec(name) ?: return@dependencies
        // impl
        implementation(project(":http-server"))
        implementation(project(":renderer-${serverPackage.renderer}"))
        // skiko
        implementation(skiko(skikoOsClassifier(serverPackage.os)))
        if (serverPackage.renderer == "opengl") {
            val nativeClassifier = lwjglNativeClassifier(serverPackage.os)
            runtimeOnly("org.lwjgl:lwjgl:${Versions.LWJGL}:$nativeClassifier")
            runtimeOnly("org.lwjgl:lwjgl-glfw:${Versions.LWJGL}:$nativeClassifier")
            runtimeOnly("org.lwjgl:lwjgl-opengl:${Versions.LWJGL}:$nativeClassifier")
        }
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
        val serverPackage = serverPackageSpec(project.name)
        mainClass.set("top.e404.skin.server.App")
        applicationDefaultJvmArgs = listOf(
            "-Dio.netty.tryReflectionSetAccessible=true",
            "--add-opens",
            "java.base/jdk.internal.misc=ALL-UNNAMED",
        ) + if (serverPackage != null) {
            listOf("-Dskin.renderer.class=${rendererClassName(serverPackage.renderer)}")
        } else {
            emptyList()
        }
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
            testClassesDirs = files(
                manualTestSourceSet.output.classesDirs,
                layout.buildDirectory.dir("classes/kotlin/manualTest")
            )
            classpath = manualTestSourceSet.runtimeClasspath
            useJUnitPlatform()
            testLogging {
                events("passed", "failed", "skipped", "standardOut", "standardError")
                showStandardStreams = true
            }
            workingDir = rootDir.resolve("run")
            shouldRunAfter(test)
        }
    }
}
