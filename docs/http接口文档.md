# HTTP 接口文档

## 获取渲染图

url: `/render/{type}/{content}/{position}`

| url 参数 | 含义 | 可选值 |
|---|---|---|
| `type` | 玩家查询方式 | `name` / `id` |
| `content` | 玩家名或 uuid | 例如 `404E` |
| `position` | 生成模式 | `sneak` / `sk` / `dsk` / `head` / `dhead` / `homo` |

### 模式默认值

| 模式 | 输出 | `width` | `height` | `target` | `yaw` | `pitch` | `distance` | `aa` | `lighting` | `shadow` | `platform` | `modelYaw` |
|---|---|---:|---:|---|---:|---:|---:|---:|---|---|---|---:|
| `sneak` | gif | 600 | 900 | `0,10,0` | 315 | 10 | 65 | 1 | `directional` | `true` | `true` | 270 |
| `sk` | png | 600 | 900 | `0,10,0` | 45 | 15 | 65 | 2 | `directional` | `true` | `true` | 0 |
| `dsk` | gif | 600 | 900 | `0,10,0` | 45 | 20 | 65 | 1 | `directional` | `true` | `true` | 0 |
| `head` | png | 400 | 400 | `0,20,0` | 45 | 15 | 30 | 2 | `directional` | `true` | `true` | 0 |
| `dhead` | gif | 400 | 400 | `0,20,0` | 45 | 20 | 30 | 1 | `directional` | `true` | `true` | 0 |
| `homo` | png | 1024 | 768 | `0,8,0` | 30 | 0 | 80 | 2 | `directional` | `true` | `true` | 0 |

### 通用渲染参数

这些参数对所有 `/render` 模式都可用，接口层会按上方模式默认值补齐。

| 请求参数 | 含义 | 默认值或说明 |
|---|---|---|
| `bg` | 背景颜色 | 默认 `#1F1B1D`，支持 `#rgb`、`#argb`、`#rrggbb`、`#aarrggbb` |
| `width` | 输出宽度 | 按模式决定 |
| `height` | 输出高度 | 按模式决定 |
| `target` | 相机目标点 | 三个数字，用逗号分隔，例如 `0,10,0` |
| `targetX` / `targetY` / `targetZ` | 相机目标点分量 | 可替代 `target` |
| `yaw` | 相机水平角度 | 按模式决定 |
| `pitch` | 相机俯仰角度 | 按模式决定；`y` 是兼容旧接口的别名 |
| `distance` | 相机距离 | 按模式决定 |
| `light` | 光照强度 | 默认 `0.8`，范围 `0.0` 到 `1.0` |
| `lightDirection` | 方向光方向 | 三个数字，用逗号分隔；不传时按相机自动生成左上方参考光 |
| `lightDir` | 方向光方向别名 | 等同 `lightDirection` |
| `lightX` / `lightY` / `lightZ` | 方向光方向分量 | 可替代 `lightDirection` |
| `lighting` | 光照模式 | `ambient` 或 `directional`；`lightingMode` 是别名 |
| `shadow` | 是否启用光照投影 | 默认 `true`；`shadows` 是别名 |
| `platform` | 是否显示地台 | 默认 `true`；`showPlatform` 是别名 |
| `platformTopY` | 地台顶面 Y 坐标 | 默认 `-8.2`；`platformY` 是别名 |
| `platformThickness` | 地台厚度 | 默认 `2` |
| `aa` | 抗锯齿等级 | 按模式决定；`antiAliasingLevel` 是别名 |
| `overlay` | 外层皮肤模式 | `none`、`flat`、`3d`；`overlayMode` 是别名 |
| `cape` | 是否渲染账号披风 | 默认 `true`；`showCape` 是别名，账号无披风时自动忽略 |
| `modelYaw` | 模型自身水平旋转角度 | 静态图直接使用，旋转 gif 作为每帧旋转的起始偏移 |
| `pose` | 额外姿态变换 | URL 编码后的 JSON，追加到当前模式内置姿态之后 |

`shadow=true` 表示启用“方向光照 + 影子”。接口默认启用 `shadow=true`、`platform=true` 和 `overlay=3d`，保证 3D 外层、地台和地面投影默认可见。`shadow=true&lighting=ambient` 会返回 `400 Bad Request`，因为环境光模式没有方向光投影；如需环境光渲染，需要同时传 `shadow=false&lighting=ambient`。

### 模式参数

| 请求参数 | 含义 | 适用模式 | 默认值 |
|---|---|---|---|
| `slim` | 是否使用 Alex 细手臂模型 | `sneak` / `sk` / `dsk` / `homo` | 不传时跟随皮肤数据 |
| `t` | `slim` 旧别名 | `sneak` / `sk` / `dsk` / `homo` | 不传时跟随皮肤数据 |
| `head` | 头部缩放倍率 | `sneak` / `sk` / `dsk` / `homo` | `1.0` |
| `frameCount` | gif 帧数 | `dsk` / `dhead` | `20` |
| `x` | `frameCount` 旧别名 | `dsk` / `dhead` | `20` |
| `duration` | gif 每帧持续时间，单位 ms | `sneak` / `dsk` / `dhead` | `40` |

GIF 的时间精度为 10ms。

### pose 参数

`pose` 是 JSON 对象，key 为身体部位，value 为变换数组。身体部位支持：

`head`、`body`、`rightArm`、`leftArm`、`rightLeg`、`leftLeg`、`cape`

变换支持：

```json
{
  "body": [
    { "type": "rotate", "x": 30, "y": 0, "z": 0 },
    { "type": "translate", "x": 0, "y": -1, "z": 2 }
  ],
  "head": [
    { "type": "scale", "x": 1.2, "y": 1.2, "z": 1.2 }
  ]
}
```

请求时需要进行 URL 编码。

### 示例请求

```http request
GET http://localhost:2345/render/name/404E/sk?head=1.5
```

```http request
GET http://localhost:2345/render/name/404E/dsk?frameCount=24&duration=40&pitch=15&lightDirection=0.5,0.9,0.35
```

## 通过皮肤生成头像

url: `/face/{type}/{content}`

| url 参数 | 含义 | 可选值 |
|---|---|---|
| `type` | 玩家查询方式 | `name` / `id` |
| `content` | 玩家名或 uuid | 例如 `404E` |

| 请求参数 | 含义 | 默认值 |
|---|---|---|
| `bg` | 背景颜色 | `#0000` |
| `scale` | 头像缩放倍率 | `5` |
| `margin` | 头像边距 | `40` |

```http request
GET http://localhost:2345/face/name/404E?bg=%23ffff&scale=5&margin=40
```

## 刷新皮肤缓存

url: `/refresh/{type}/{content}`

| url 参数 | 含义 | 可选值 |
|---|---|---|
| `type` | 玩家查询方式 | `name` / `id` |
| `content` | 玩家名或 uuid | 例如 `404E` |

```http request
GET http://localhost:2345/refresh/name/404E
```

## 获取原始数据

url: `/data/{type}/{content}`

| url 参数 | 含义 | 可选值 |
|---|---|---|
| `type` | 玩家查询方式 | `name` / `id` |
| `content` | 玩家名或 uuid | 例如 `404E` |

```http request
GET http://localhost:2345/data/name/404E
```

示例响应：

```json
{
  "uuid": "22df77dd37b0414b8f1e3c7d2585fc79",
  "name": "404E",
  "slim": true,
  "update": 1683961680455,
  "hash": "4daa024bc2d35de2b26025051817d04491ad586e5a2ab85f9dad608b009ac7d",
  "capeHash": null
}
```
