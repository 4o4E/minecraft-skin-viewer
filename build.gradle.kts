import java.net.URI
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.api.publish.PublishingExtension
import org.gradle.plugins.signing.SigningExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
    kotlin("jvm") version Versions.KOTLIN
    kotlin("plugin.serialization") version Versions.KOTLIN
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("com.vanniktech.maven.publish") version Versions.VANNIKTECH_MAVEN_PUBLISH apply false
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
val isCi = providers.environmentVariable("GITHUB_ACTIONS")
    .map { it.equals("true", ignoreCase = true) }
    .orElse(false)
val isCiTag = providers.environmentVariable("GITHUB_REF_TYPE")
    .map { it == "tag" }
    .orElse(providers.environmentVariable("GITHUB_REF").map { it.startsWith("refs/tags/") })
    .orElse(false)
val localSnapshotVersion = Versions.VERSION.removeSuffix("-SNAPSHOT") + "-SNAPSHOT"
val publishVersion = providers.provider {
    if (isCi.get() && isCiTag.get()) {
        providers.environmentVariable("GITHUB_REF_NAME").orNull
            ?.takeIf { it.isNotBlank() }
            ?: localSnapshotVersion
    } else {
        localSnapshotVersion
    }
}
val projectUrl = "https://github.com/4o4E/minecraft-skin-viewer"
val nexusSnapshotsUrl = "https://nexus.e404.top:3443/repository/maven-snapshots/"
val nexusReleasesUrl = "https://nexus.e404.top:3443/repository/maven-releases/"
val shouldConfigureMavenCentral = isCi.get() && !publishVersion.get().endsWith("-SNAPSHOT")

fun nexusCredential(propertyName: String, ciSecretEnvName: String): Provider<String> =
    if (isCi.get()) providers.environmentVariable(ciSecretEnvName) else providers.gradleProperty(propertyName)

kotlin {
    jvmToolchain(17)
}

tasks.register("printVersion") {
    description = "输出当前项目版本，供 CI 发布流程判断 release 或 snapshot。"
    group = "help"
    doLast {
        println(version)
    }
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    group = Versions.GROUP
    version = publishVersion.get()

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
    apply(plugin = "org.gradle.java-library")
    apply(plugin = "org.gradle.application")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.github.johnrengelman.shadow")

    extensions.configure<org.gradle.api.plugins.JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    if (shouldPublishMavenPackage(project.name)) {
        apply(plugin = "com.vanniktech.maven.publish")

        (components["java"] as AdhocComponentWithVariants).withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) {
            skip()
        }

        extensions.configure<MavenPublishBaseExtension> {
            coordinates(rootProject.group.toString(), project.name, rootProject.version.toString())

            if (shouldConfigureMavenCentral) {
                publishToMavenCentral()
                signAllPublications()
                val signingKey = providers.gradleProperty("signingInMemoryKey").orNull
                val signingPassword = providers.gradleProperty("signingInMemoryKeyPassword").orNull
                if (!signingKey.isNullOrBlank()) {
                    extensions.configure<SigningExtension> {
                        useInMemoryPgpKeys(signingKey, signingPassword)
                    }
                }
            }

            pom {
                name.set(project.name)
                description.set("Minecraft skin viewer ${project.name} module")
                url.set(projectUrl)
                licenses {
                    license {
                        name.set("GNU General Public License v3.0")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("4o4E")
                        name.set("4o4E")
                        email.set("869951226@qq.com")
                        organization.set("4o4E")
                        organizationUrl.set("https://github.com/4o4E")
                    }
                }
                scm {
                    url.set(projectUrl)
                    connection.set("scm:git:$projectUrl.git")
                    developerConnection.set("scm:git:$projectUrl.git")
                }
            }
        }

        extensions.configure<PublishingExtension> {
            repositories {
                maven {
                    name = "Nexus"
                    url = uri(if (version.toString().endsWith("-SNAPSHOT")) nexusSnapshotsUrl else nexusReleasesUrl)
                    credentials(PasswordCredentials::class) {
                        username = nexusCredential("nexus.username", "NEXUS_USERNAME").orNull
                        password = nexusCredential("nexus.password", "NEXUS_PASSWORD").orNull
                    }
                }
            }
        }

        if (!shouldConfigureMavenCentral) {
            tasks.register("publishToMavenCentral") {
                description = "本地或 SNAPSHOT 版本禁止发布 Maven Central"
                group = "publishing"
                doFirst {
                    error("Maven Central 只允许在 CI 中发布非 SNAPSHOT 版本；本地请使用 publishAllPublicationsToNexusRepository 或 publishToMavenLocal。")
                }
            }
            tasks.register("publishAndReleaseToMavenCentral") {
                description = "本地或 SNAPSHOT 版本禁止发布 Maven Central"
                group = "publishing"
                doFirst {
                    error("Maven Central 只允许在 CI 中发布非 SNAPSHOT 版本；本地请使用 publishAllPublicationsToNexusRepository 或 publishToMavenLocal。")
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
