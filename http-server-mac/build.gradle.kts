plugins {
    kotlin("jvm")
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
}

dependencies {
    // impl
    implementation(project(":http-server")) {
        exclude("org.openjfx")
    }
    // skiko
    implementation(skiko("macos-x64"))
    // javafx
    implementation(javafx("controls", "mac"))
    implementation(javafx("graphics", "mac"))
    implementation(javafx("base", "mac"))
}

tasks {
    runShadow {
        workingDir = project.projectDir.resolve("run")
        doFirst {
            if (workingDir.isFile) workingDir.delete()
            workingDir.mkdirs()
        }
    }

    shadowJar {
        archiveFileName.set("${project.name}.jar")
        val jar = project.rootDir.resolve("jar")
        doLast {
            jar.mkdir()
            project.buildDir
                .resolve("libs/${project.name}.jar")
                .copyTo(jar.resolve("${project.name}.jar"), true)
        }
    }
}
