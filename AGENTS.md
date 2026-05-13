# AGENTS.md

## 提交规则

- 提交信息必须使用 Conventional Commits 格式：`type(scope): 中文说明` 或 `type: 中文说明`。
- `type` 使用小写英文，优先使用：`feat`、`fix`、`refactor`、`test`、`ci`、`docs`、`build`、`chore`。
- `scope` 可选，使用小写英文或项目模块名，例如：`core`、`renderer`、`opengl`、`server`。
- 标题说明必须使用中文，保持简短、准确，避免英文标题，例如不要写 `Fix OpenGL shadow texture binding`。
- 标题使用祈使/陈述均可，但要描述本次提交实际改变，不写泛泛的 `update`、`fix bug`。
- 不要把无关变更混在一个提交里；代码变更、测试补充、CI 配置应尽量拆开提交。

示例：

```text
feat(render): 添加地台渲染开关
fix(opengl): 修正阴影贴图绑定
refactor(server): 拆分 Tavolo 和 OpenGL 打包模块
test(renderer): 添加阴影和外层皮肤人工测试
ci: 添加 GitHub Actions 构建和发布流程
```
