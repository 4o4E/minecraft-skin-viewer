plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    application
}

application {
    mainClass.set("top.e404.skin.benchmark.RenderImageBenchmarkKt")
}

val benchmark by sourceSets.creating {
    java.srcDir("src/benchmark/kotlin")
    resources.srcDir("src/benchmark/resources")
    compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
    runtimeClasspath += output + compileClasspath
}

configurations["benchmarkImplementation"].extendsFrom(configurations["testImplementation"])
configurations["benchmarkRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

dependencies {
    implementation(project(":core"))
    implementation(project(":renderer-tavolo"))
    implementation(project(":renderer-opengl"))
    implementation("top.e404.tavolo:tavolo-graphics:${Versions.TAVOLO}")
    implementation("org.openjfx:javafx-base:${Versions.JAVAFX}:win")
    implementation("org.openjfx:javafx-base:${Versions.JAVAFX}:linux")
    implementation("org.openjfx:javafx-base:${Versions.JAVAFX}:mac")
    implementation("org.openjfx:javafx-graphics:${Versions.JAVAFX}:win")
    implementation("org.openjfx:javafx-graphics:${Versions.JAVAFX}:linux")
    implementation("org.openjfx:javafx-graphics:${Versions.JAVAFX}:mac")
    implementation("org.lwjgl:lwjgl:${Versions.LWJGL}")
    implementation("org.lwjgl:lwjgl-glfw:${Versions.LWJGL}")
    implementation("org.lwjgl:lwjgl-opengl:${Versions.LWJGL}")
    runtimeOnly("org.lwjgl:lwjgl:${Versions.LWJGL}:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-glfw:${Versions.LWJGL}:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-opengl:${Versions.LWJGL}:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl:${Versions.LWJGL}:natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-glfw:${Versions.LWJGL}:natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-opengl:${Versions.LWJGL}:natives-linux")
    add("benchmarkImplementation", project(":core"))
    add("benchmarkImplementation", project(":renderer-tavolo"))
    add("benchmarkImplementation", project(":renderer-opengl"))
    add("benchmarkImplementation", kotlin("test", Versions.KOTLIN))
    add("benchmarkImplementation", "org.openjfx:javafx-base:${Versions.JAVAFX}:win")
    add("benchmarkImplementation", "org.openjfx:javafx-base:${Versions.JAVAFX}:linux")
    add("benchmarkImplementation", "org.openjfx:javafx-base:${Versions.JAVAFX}:mac")
    add("benchmarkImplementation", "org.openjfx:javafx-graphics:${Versions.JAVAFX}:win")
    add("benchmarkImplementation", "org.openjfx:javafx-graphics:${Versions.JAVAFX}:linux")
    add("benchmarkImplementation", "org.openjfx:javafx-graphics:${Versions.JAVAFX}:mac")
}

tasks.register<JavaExec>("openglCubeDemo") {
    description = "生成 OpenGL 方块、半透明外壳和阴�?demo"
    group = "verification"
    mainClass.set("top.e404.skin.benchmark.OpenGlCubeDemoKt")
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = rootDir.resolve("run")
    jvmArgs("-Dfile.encoding=UTF-8")
    systemProperty(
        "skin.openglDemo.reportDir",
        layout.buildDirectory.dir("reports/opengl-cube-demo").get().asFile.absolutePath
    )
}

tasks.register<Test>("renderImageBenchmark") {
    description = "运行 Minecraft 皮肤渲染生成 PNG 图片的耗时对比测试"
    group = "verification"
    testClassesDirs = benchmark.output.classesDirs
    classpath = benchmark.runtimeClasspath
    useJUnitPlatform()
    workingDir = rootDir.resolve("run")
    testLogging {
        events("passed", "failed", "skipped", "standardOut", "standardError")
        showStandardStreams = true
    }
    systemProperty(
        "skin.benchmark.reportDir",
        layout.buildDirectory.dir("reports/render-image-benchmark").get().asFile.absolutePath
    )
}

tasks.register<JavaExec>("javafxQualityDemo") {
    description = "Generate JavaFX transparency and shadow quality demo"
    group = "verification"
    mainClass.set("top.e404.skin.benchmark.JavaFxQualityDemoKt")
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = rootDir.resolve("run")
    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-Dprism.order=es2",
        "-Dprism.forceGPU=true"
    )
    systemProperty(
        "skin.qualityDemo.reportDir",
        layout.buildDirectory.dir("reports/javafx-quality-demo").get().asFile.absolutePath
    )
}
