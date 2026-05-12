plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":core"))
    implementation("org.lwjgl:lwjgl:${Versions.LWJGL}")
    implementation("org.lwjgl:lwjgl-glfw:${Versions.LWJGL}")
    implementation("org.lwjgl:lwjgl-opengl:${Versions.LWJGL}")
    runtimeOnly("org.lwjgl:lwjgl:${Versions.LWJGL}:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-glfw:${Versions.LWJGL}:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-opengl:${Versions.LWJGL}:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl:${Versions.LWJGL}:natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-glfw:${Versions.LWJGL}:natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-opengl:${Versions.LWJGL}:natives-linux")
}
