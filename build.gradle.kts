import java.net.URI
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

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
    Regex("""http-server-(tavolo|opengl)-(win|linux)""")
        .matchEntire(projectName)
        ?.destructured
        ?.let { (renderer, os) -> ServerPackageSpec(renderer, os) }

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

fun shouldPublishMavenPackage(projectName: String): Boolean =
    !projectName.startsWith("http-server") && projectName != "render-benchmark"

fun parsePositiveInt(value: String?, propertyName: String): Int? =
    value
        ?.takeIf { it.isNotBlank() }
        ?.toIntOrNull()
        ?.takeIf { it > 0 }
        ?: value
            ?.takeIf { it.isNotBlank() }
            ?.let { error("$propertyName must be a positive integer, but was '$it'") }

val runDir = rootDir.resolve("run")
val runSkinAssets = mapOf(
    "alex_skin.png" to "https://textures.minecraft.net/texture/4daa024bc2d35de2b26025051817d04491ad586e5a2ab85f9dad608b009ac7d",
    "steve_skin.png" to "https://textures.minecraft.net/texture/1a4af718455d4aab528e7a61f86fa25e6a369d1768dcb13f7df319a713eb810b",
)

val prepareRunDir = tasks.register("prepareRunDir") {
    outputs.dir(runDir)
    doLast {
        if (runDir.isFile) runDir.delete()
        runDir.mkdirs()
    }
}

val prepareRunAssets = tasks.register("prepareRunAssets") {
    dependsOn(prepareRunDir)
    outputs.files(runSkinAssets.keys.map { runDir.resolve(it) })
    doLast {
        runSkinAssets.forEach { (fileName, url) ->
            val target = runDir.resolve(fileName)
            if (target.isFile) return@forEach
            logger.lifecycle("Downloading missing run asset $fileName")
            target.outputStream().use { output ->
                URI(url).toURL().openStream().use { input ->
                    input.copyTo(output)
                }
            }
        }
    }
}

val manualTestMaxParallelForks: Int = parsePositiveInt(
    providers.gradleProperty("manualTest.maxParallelForks").orNull
        ?: providers.gradleProperty("manualTestMaxParallelForks").orNull,
    "manualTest.maxParallelForks"
) ?: 1

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
        if (shouldPublishMavenPackage(project.name)) {
            withSourcesJar()
        }
    }

    if (shouldPublishMavenPackage(project.name)) {
        (components["java"] as AdhocComponentWithVariants).withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) {
            skip()
        }

        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])
                    pom {
                        name.set(project.name)
                        description.set("Minecraft skin viewer ${project.name} module")
                        url.set("https://github.com/4o4E/minecraft-skin-viewer")
                        licenses {
                            license {
                                name.set("GNU General Public License v3.0")
                                url.set("https://www.gnu.org/licenses/gpl-3.0.txt")
                            }
                        }
                        scm {
                            url.set("https://github.com/4o4E/minecraft-skin-viewer")
                            connection.set("scm:git:https://github.com/4o4E/minecraft-skin-viewer.git")
                        }
                    }
                }
            }
            repositories {
                maven {
                    name = "GitHubPackages"
                    val githubRepository = providers.gradleProperty("githubPackagesRepository")
                        .orElse(System.getenv("GITHUB_REPOSITORY") ?: "4o4E/minecraft-skin-viewer")
                    url = uri("https://maven.pkg.github.com/${githubRepository.get()}")
                    credentials {
                        username = providers.gradleProperty("gpr.user")
                            .orElse(System.getenv("GITHUB_ACTOR") ?: "")
                            .get()
                        password = providers.gradleProperty("gpr.key")
                            .orElse(System.getenv("GITHUB_TOKEN") ?: "")
                            .get()
                    }
                }
            }
        }
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
            dependsOn(rootProject.tasks.named("prepareRunDir"))
            workingDir = runDir
        }

        register<Test>("manualTest") {
            description = "运行需要人工准备环境、外部服务或本地资产的测试"
            group = "verification"
            maxParallelForks = manualTestMaxParallelForks
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
            dependsOn(rootProject.tasks.named("prepareRunAssets"))
            workingDir = runDir
            shouldRunAfter(test)
        }
    }
}
