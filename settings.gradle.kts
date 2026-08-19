rootProject.name = "mc-skin-render"
include(
    "core",
    "http-client",
    "http-server",
    "http-server-tavolo-win",
    "http-server-tavolo-linux",
    "http-server-opengl-win",
    "http-server-opengl-linux",
    "renderer-tavolo",
    "renderer-opengl",
    "render-benchmark",
)

// 仅在显式提供 Tavolo 源码路径时启用本地 Composite Build，不影响默认构建和 CI。
providers.gradleProperty("tavoloSource").orNull?.let { tavoloSource ->
    includeBuild(tavoloSource) {
        dependencySubstitution {
            substitute(module("top.e404.tavolo:tavolo-gif-codec")).using(project(":gif-codec"))
        }
    }
}
