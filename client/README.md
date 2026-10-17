# RTA 单核响应时间分析前端

基于 Vue 2 / Vue CLI 5 的单核响应时间分析可视化页面。导入任务 JSON 后，调用 RTA 后端获取静态分析结果与调度记录，展示任务配置、pass/miss 状态和任务甘特图。

## 环境与目录

需要 Node.js 和 npm，以及本仓库的 `server` 后端服务。绘图组件位于 `src/components/mixed/`，无需其他仓库。

## 安装与启动

先启动本仓库的 RTA 后端服务，再在`client` 目录运行：

```powershell
npm ci
npm run serve
```

默认访问地址为 `http://localhost:8081`，后端接口为 `http://localhost:8080/api/rta`。

如需更换接口，在本目录创建 `.env.local`，然后重启开发服务；生产构建也会使用此配置：

```dotenv
VUE_APP_RTA_API=http://localhost:8080/api/rta
```

## 使用方式

1. 打开页面，点击“导入 json”选择任务文件，格式可参考`server/src/main/resources/test.json`。页面不会在打开时自动加载默认任务。
2. 前端通过 POST 提交 JSON，展示利用率、系统可调度状态及每个任务的分析结果。输入字段及约束见后端 README。
3. 在任务甘特图中查看每次释放的作业。时间轴滚轮向上放大、向下缩小，在 1 / 10 / 100 ms 刻度间切换，默认 10 ms；左侧信息区支持纵向滚动。

## 页面与结果说明

- 配置表包含 `id`、`name`、`priority`、`period`、`phase`、`deadline`、`WCET`、`WCRT (ms)` 和 `pass/miss`，每页显示 7 个任务。
- 静态分析结果按任务 `id` 关联，同名任务独立显示。pass 为绿色，miss 为红色；跳过分析的任务显示 `/`。
- 已证明的有限 WCRT 显示数值；无有限上界的任务显示“无界”，达到分析上限的任务显示“未确定”，跳过分析的任务显示 `/`。pass/miss 使用后端分析结论，尚未判定的情况显示“未确定”。
- 页面展示任务甘特图；CPU 调度记录由后端保留，用于计算干扰。每次释放对应一行，信息列为 `id`、`name`、`release (ms)`、`interference (ms)`、`complete (ms)`。
- 执行区间按所属任务的静态 pass/miss 着色。图例包含执行、释放、完成和干扰；任务名称仅用于显示。
- 甘特图范围为 0 到所有输入任务的最大 deadline，作业行全部展示、不分页。窗口末尾尚未完成的作业不会显示伪造的完成事件。
- `interference` 为观察窗口内其他任务占用 CPU 造成的等待时间；同一任务旧作业引起的排队单独记录为 `queued`，不冒充其他任务干扰，也不等同于 WCRT。
- 缩放只改变显示比例，不重新分析或修改真实时间；时间单位统一为毫秒。时间值保留接口数值精度，不固定截断为三位小数。

## 主要代码

| 路径 | 用途 |
| --- | --- |
| `src/components/GanttComponent.vue` | JSON 导入、接口请求与页面状态 |
| `src/components/taskDetails/` | 任务配置及分析结果表 |
| `src/components/gantt/` | 甘特图面板与绘图组件适配 |
| `src/utils/rtaResponse.js` | 响应归一化与任务 ID 关联 |
| `src/utils/timeAxis.js` | 时间轴与缩放计算 |

## 检查与构建

```powershell
npm run lint -- --no-fix
npm run build
```

构建产物位于 `dist/`。

项目另提供数据链验证命令：

```powershell
npm run test:data-chain
```

该脚本读取仓库内 `server/src/test/resources/fixtures/` 的输入和 `server/target/` 的后端测试响应。先在 `server` 目录运行 Maven 测试，再运行此命令；也可在仓库根目录执行 `npm test`，自动按顺序运行前后端测试。
