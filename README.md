# minecraft-skin-viewer

用于生成我的世界玩家渲染图

## http服务

[http-server模块](http-server)提供了一个简单的http server用于根据需求生成皮肤渲染图

### **:warning:注意:warning:**

1. 此http服务使用mysql来储存玩家`uuid`和`name`, 请在部署后配置db.properties
2. config.yml中可以设置http服务的地址和端口, 以及代理(用于从mojang的服务器获取玩家数据, 没有可以留空)

### 示例配置文件

```yaml
address: "127.0.0.1"
port: 80
proxy:
  address: localhost
  port: 7890
```

### 使用方式

```shell
java -jar http-server-all.jar
```

### api接口

url /render/{type}/{content}/{position}

| url参数    | 含义        | 示例                                       |
|----------|-----------|------------------------------------------|
| type     | 以何种方式指定玩家 | `name`/`id`                              |
| content  | 指定玩家的内容   | `玩家名`/`uuid`                             |
| position | 生成的模式     | `sneak`/`sk`/`dsk`/`head`/`dhead`/`homo` |

### 不同模式的可用参数

**打勾的参数意味支持get参数设置**

| 请求参数  | 含义     | 备注           | sneak              | sk                 | dsk                | head               | dhead              | homo               |
|-------|--------|--------------|--------------------|--------------------|--------------------|--------------------|--------------------|--------------------|
| bg    | 背景颜色   | 默认值`#1F1B1D` | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: |
| light | 环境光颜色  | 默认值空         | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: |
| head  | 头大小    | 默认值`1.0`浮点数  | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: |                    |                    | :heavy_check_mark: |
| x     | 旋转时的速度 | 默认值`20`      |                    |                    | :heavy_check_mark: |                    | :heavy_check_mark: |                    |
| y     | 俯仰角    | 默认值`20`      |                    |                    | :heavy_check_mark: |                    | :heavy_check_mark: |                    |

### 示例请求

```http request
GET http://localhost:2345/render/name/404E/sk?head=1.5
```

### 
