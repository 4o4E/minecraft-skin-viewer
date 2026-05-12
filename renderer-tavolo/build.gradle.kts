plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":core"))
    api("top.e404.tavolo:tavolo-graphics:${Versions.TAVOLO}")
    api("top.e404.tavolo:tavolo-gif-codec:${Versions.TAVOLO}")
    api("top.e404.tavolo:tavolo-common:${Versions.TAVOLO}")
}
