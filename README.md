# mc-skin-render

用于生成我的世界玩家渲染图

## http服务

[http-server模块](http-server)提供了一个简单的http server用于根据需求生成皮肤渲染图

### **:warning:注意:warning:**

1. 此http服务使用mysql来储存玩家`uuid`和`name`, 请在部署后配置db.properties, 默认使用名为`skin`的数据库
2. `config.yml`可以设置http服务的地址和端口, 以及代理(用于从mojang的服务器获取玩家数据, 没有可以留空)
3. 渲染实现已迁移到 `Tavolo`，不再依赖 JavaFX 或 `xserver`

### 使用

1. 安装11或更高版本的[java](https://adoptium.net/)以及[mysql](https://downloads.mysql.com/archives/community/)
2. 从[release](https://github.com/4o4E/mc-skin-render/releases/latest)下载对应渲染器和操作系统的 jar 文件
3. 在控制台中使用 `java -jar http-server-tavolo-linux.jar` 或 `java -jar http-server-opengl-win.jar` 启动服务

### 测试

- `./gradlew test`：运行不依赖外部服务、本地图片或数据库的单元测试。
- `./gradlew manualTest`：运行需要人工准备环境的测试，例如本地皮肤渲染样例、图形环境或本地资产相关测试。
- `./gradlew manualTestClasses`：只编译人工测试，不实际连接外部环境。
- `./gradlew manualTest "-PmanualTest.maxParallelForks=4"`：以 4 个 Gradle test fork 并行运行人工测试；默认仍为 1，适合外部服务或图形环境不稳定时使用。

### 示例配置文件

```yaml
# http服务绑定地址
host: "127.0.0.1"
# http服务绑定端口
port: 80
# 请求mojang服务器时使用的代理, 设置为null则不使用代理
proxy:
  # 代理地址
  host: localhost
  # 代理端口
  port: 7890
# 缓存超时时长(超时后获取时将重新从mojang服务器获取, 包括uuid对应的用户名和皮肤, 服务不会主动移除过期缓存, 仅在获取时检测超时)
timeout: 86400
# 渲染结果文件缓存
renderCache:
  enabled: true
  dir: render-cache
  maxBytes: 2147483648
  maxEntries: 10000
  lowWatermarkRatio: 0.9
```

### api接口

接口文档已迁移到 [HTTP 接口文档](docs/http接口文档.md)。

### HTTP Client

[http-client模块](http-client)提供了基于 Ktor HttpClient 的类型化客户端，可直接调用渲染、头像、刷新和数据接口，并支持复用下游传入的 `HttpClient`。

## Maven 依赖

正式版本发布到 Maven Central，可直接使用 `mavenCentral()` 引入：

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("top.e404.mcsk:http-client:2.5.2")
}
```

可发布模块：

```kotlin
implementation("top.e404.mcsk:core:2.5.2")
implementation("top.e404.mcsk:http-client:2.5.2")
implementation("top.e404.mcsk:renderer-tavolo:2.5.2")
implementation("top.e404.mcsk:renderer-opengl:2.5.2")
```

`-SNAPSHOT` 版本发布到 Sonatype snapshot 仓库：

```kotlin
repositories {
    maven("https://central.sonatype.com/repository/maven-snapshots/")
}
```

内部联调优先使用自建 Nexus；版本号以 `-SNAPSHOT` 结尾时发布到 snapshots 仓库，正式版本发布到 releases 仓库。

## Docker 部署

支持使用 Docker 进行部署, 镜像不包含 mysql：

- Tavolo CPU 渲染：`ghcr.io/4o4e/mc-skin-render-tavolo-linux:2.5.2`，[compose 部署参考](http-server-tavolo-linux/docker-compose.yml)
- OpenGL + Xvfb 渲染：`ghcr.io/4o4e/mc-skin-render-opengl-linux:2.5.2`，[compose 部署参考](http-server-opengl-linux/docker-compose.yml)
