import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Versions.kotlin
    kotlin("plugin.serialization") version Versions.kotlin
    `maven-publish`
    `java-library`
}

group = "top.e404"
version = "1.0.0"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    // slf4j
    implementation("org.slf4j:slf4j-api:1.7.36")
    // jfx
    api(javafx("controls", "win"))
    api(javafx("graphics", "win"))
    api(javafx("base", "win"))
    api(javafx("controls", "linux"))
    api(javafx("graphics", "linux"))
    api(javafx("base", "linux"))
    // test
    testImplementation(kotlin("test", Versions.kotlin))
}

tasks.test {
    useJUnitPlatform()
    workingDir = projectDir.resolve("run")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

java {
    withJavadocJar()
    withSourcesJar()
}

afterEvaluate {
    publishing.publications.create<MavenPublication>("java") {
        from(components["kotlin"])
        artifact(tasks.getByName("sourcesJar"))
        artifact(tasks.getByName("javadocJar"))
        artifactId = project.name
        groupId = project.group.toString()
        version = project.version.toString()
    }
}

kotlin {
    jvmToolchain(11)
}

tasks.jar {
    doLast {
        val jar = projectDir.resolve("jar").also { it.mkdirs() }
        println("==== copy ====")
        for (file in project.buildDir.resolve("libs").listFiles() ?: return@doLast) {
            if ("source" in file.name || "javadoc" in file.name) continue
            println("正在复制`${file.path}`")
            file.copyTo(jar.resolve(file.name), true)
        }
    }
}
