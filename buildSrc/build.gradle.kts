plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    maven("https://maven.aliyun.com/repository/public") // 阿里云国内代理仓库
    mavenCentral()
    gradlePluginPortal()
    google()
}