import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Versions.kotlin
    kotlin("plugin.serialization") version Versions.kotlin
    id("me.him188.maven-central-publish") version "1.0.0-dev-3"
}

allprojects {
    apply(plugin = "org.gradle.maven-publish")
    apply(plugin = "org.gradle.java-library")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = Versions.group
    version = Versions.version

    kotlin {
        jvmToolchain(11)
    }

    tasks {
        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "11"
        }

        test {
            enabled = false
            useJUnitPlatform()
            workingDir = projectDir.resolve("run")
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // slf4j
    implementation("org.slf4j:slf4j-api:1.7.36")
    // jfx
    api(javafx("controls", "win"))
    api(javafx("graphics", "win"))
    api(javafx("base", "win"))
    // test
    testImplementation(kotlin("test", Versions.kotlin))
}

mavenCentralPublish {
    useCentralS01()
    singleDevGithubProject("4o4E", "minecraft-skin-viewer")
    licenseGplV3()
    workingDir = buildDir.resolve("publishing-tmp")
}
