# http-client 模块

`http-client` 提供基于 Ktor HttpClient 的类型化调用封装，不依赖服务端模块和渲染器模块。

## 基本用法

```kotlin
import top.e404.mcsk.client.McSkinRenderClient
import top.e404.mcsk.client.ModelOptions
import top.e404.mcsk.client.OverlayMode
import top.e404.mcsk.client.PlayerRef
import top.e404.mcsk.client.RenderOptions

suspend fun main() {
    McSkinRenderClient("http://localhost:2345").use { client ->
        val png = client.renderSkin(
            player = PlayerRef.name("404E"),
            render = RenderOptions(
                shadow = true,
                overlay = OverlayMode.THREE_D,
            ),
            model = ModelOptions(headScale = 1.5)
        )
    }
}
```

## 复用下游 HttpClient

传入外部 `HttpClient` 时，`McSkinRenderClient.close()` 不会关闭该实例，生命周期由调用方管理。

```kotlin
import io.ktor.client.HttpClient
import io.ktor.http.Url
import top.e404.mcsk.client.McSkinRenderClient
import top.e404.mcsk.client.PlayerRef

suspend fun renderWithSharedClient(httpClient: HttpClient) {
    val client = McSkinRenderClient(Url("http://localhost:2345"), httpClient)
    val gif = client.renderSkinRotate(PlayerRef.id("22df77dd37b0414b8f1e3c7d2585fc79"))
}
```

## 通用渲染入口

新增渲染模式时可以先用 `render(RenderRequest)` 调用，不需要马上增加便捷方法。

```kotlin
import top.e404.mcsk.client.AnimationOptions
import top.e404.mcsk.client.PlayerRef
import top.e404.mcsk.client.RenderPosition
import top.e404.mcsk.client.RenderRequest

val request = RenderRequest(
    player = PlayerRef.name("404E"),
    position = RenderPosition.SKIN_ROTATE,
    animation = AnimationOptions(frameCount = 24, durationMs = 40)
)
```

`RenderPosition` 也可以直接传入新路径，例如 `RenderPosition("new-mode")`，便于服务端先扩展新渲染位置。
