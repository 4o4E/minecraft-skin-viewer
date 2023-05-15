# minecraft-skin-viewer

用于生成我的世界玩家渲染图

## http服务

[http-server模块](http-server)提供了一个简单的http server用于根据需求生成皮肤渲染图

### **:warning:注意:warning:**

1. 此http服务使用mysql来储存玩家`uuid`和`name`, 请在部署后配置db.properties, 默认使用名为`skin`的数据库
2. `config.yml`可以设置http服务的地址和端口, 以及代理(用于从mojang的服务器获取玩家数据, 没有可以留空)
3. 对于linux, 此应用需要`xserver`, 你可以使用`gnome`/`kde`/`xvfb`, 另外我自己在`ubuntu`上使用的时候, 需要添加`-Dprism.forceGPU=true`

### 使用

1. 安装11或更高版本的[java](https://adoptium.net/)以及[mysql](https://downloads.mysql.com/archives/community/)
2. 从[release](https://github.com/4o4E/minecraft-skin-viewer/releases/latest)下载对应操作系统的jar文件
3. 在控制台中使用`java -jar http-server-${plateform}.jar`启动服务

### 示例配置文件

```yaml
# http服务绑定地址
address: "127.0.0.1"
# http服务绑定端口
port: 80
# 请求mojang服务器时使用的代理, 设置为null则不使用代理
proxy:
  # 代理地址
  address: localhost
  # 代理端口
  port: 7890
# 缓存超时时长(超时后获取时将重新从mojang服务器获取, 包括uuid对应的用户名和皮肤, 服务不会主动移除过期缓存, 仅在获取时检测超时)
timeout: 86400
```

### api接口

#### 获取渲染图

url: `/render/{type}/{content}/{position}`

| url参数    | 含义        | 示例                                       |
|----------|-----------|------------------------------------------|
| type     | 以何种方式指定玩家 | `name`/`id`                              |
| content  | 指定玩家的内容   | `玩家名`/`uuid`                             |
| position | 生成的模式     | `sneak`/`sk`/`dsk`/`head`/`dhead`/`homo` |

**获取渲染图时不同模式的可用参数(打勾的参数意味支持get参数设置)**

| 请求参数     | 含义              | 备注             | sneak              | sk                 | dsk                | head               | dhead              | homo               |
|----------|-----------------|----------------|--------------------|--------------------|--------------------|--------------------|--------------------|--------------------|
| bg       | 背景颜色            | 默认值`#1F1B1D`   | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: |
| light    | 环境光颜色           | 默认值空           | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: |
| head     | 头大小             | 默认值`1.0`浮点数    | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: |                    |                    | :heavy_check_mark: |
| x        | 旋转时的速度          | 默认值`20`        |                    |                    | :heavy_check_mark: |                    | :heavy_check_mark: |                    |
| y        | 俯仰角             | 默认值`20`        |                    |                    | :heavy_check_mark: |                    | :heavy_check_mark: |                    |
| slim     | 是否使用alex模型      | 默认值空(跟随mc皮肤设置) | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: |                    |                    | :heavy_check_mark: |
| duration | gif的帧持续时长, 单位ms | 默认值`40`        | :heavy_check_mark: |                    | :heavy_check_mark: |                    | :heavy_check_mark: |                    |

**duration的精度为10ms(gif仅支持此精度)**

**示例请求**

```http request
GET http://localhost:2345/render/name/404E/sk?head=1.5
```

#### 刷新皮肤缓存

url: `/refresh/{type}/{content}`

| url参数   | 含义        | 示例           |
|---------|-----------|--------------|
| type    | 以何种方式指定玩家 | `name`/`id`  |
| content | 指定玩家的内容   | `玩家名`/`uuid` |

**示例请求**

```http request
GET http://localhost:2345/refresh/name/404E
```
