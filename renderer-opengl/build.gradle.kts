plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":core"))
    implementation("org.lwjgl:lwjgl:${Versions.LWJGL}")
    implementation("org.lwjgl:lwjgl-glfw:${Versions.LWJGL}")
    implementation("org.lwjgl:lwjgl-opengl:${Versions.LWJGL}")
    testRuntimeOnly(skiko("linux-x64"))
    testRuntimeOnly("org.lwjgl:lwjgl:${Versions.LWJGL}:natives-linux")
    testRuntimeOnly("org.lwjgl:lwjgl-glfw:${Versions.LWJGL}:natives-linux")
    testRuntimeOnly("org.lwjgl:lwjgl-opengl:${Versions.LWJGL}:natives-linux")
    "manualTestRuntimeOnly"("org.lwjgl:lwjgl:${Versions.LWJGL}:natives-windows")
    "manualTestRuntimeOnly"("org.lwjgl:lwjgl-glfw:${Versions.LWJGL}:natives-windows")
    "manualTestRuntimeOnly"("org.lwjgl:lwjgl-opengl:${Versions.LWJGL}:natives-windows")
    "manualTestRuntimeOnly"("org.lwjgl:lwjgl:${Versions.LWJGL}:natives-linux")
    "manualTestRuntimeOnly"("org.lwjgl:lwjgl-glfw:${Versions.LWJGL}:natives-linux")
    "manualTestRuntimeOnly"("org.lwjgl:lwjgl-opengl:${Versions.LWJGL}:natives-linux")
}
