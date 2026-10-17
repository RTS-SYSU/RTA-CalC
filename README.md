# Single-Core-RTA-Simulator

单核固定优先级响应时间分析与调度可视化工具。使用 Vue 2 前端导入任务 JSON，由 Spring Boot 后端计算静态 RTA 结果并生成任务甘特图。

本仓库包含前后端、调度与绘图组件，以及测试输入；构建和测试不依赖仓库外的项目或数据文件。

## 目录

```text
Single-Core-RTA-Simulator/
├── client/                 # Vue 前端，默认端口 8081
├── server/                 # Spring Boot 后端，默认端口 8080
│   └── src/test/resources/fixtures/  # 数据链测试输入
├── scripts/                # 跨平台构建及测试入口
└── package.json
```

## 环境与安装

需要 Node.js、npm 和 JDK 8 或更高版本。后端自带 Maven Wrapper，无需另外安装 Maven。首次安装需联网下载 npm 和 Maven 依赖。

在仓库根目录执行：

```powershell
npm run install:client
```

## 本地启动

打开两个终端，均在仓库根目录运行：

```powershell
# 终端一：启动后端
npm run dev:server
```

```powershell
# 终端二：启动前端
npm run dev:client
```

访问 http://localhost:8081，点击“导入 json”，可选择仓库中的 `server/src/main/resources/test.json`。页面展示任务参数、WCRT/迭代末值、pass/miss 状态及每次作业释放对应的甘特图。

默认后端接口为 `http://localhost:8080/api/rta`。GET 分析默认输入，POST 分析请求体 JSON。前端接口地址可通过 `client/.env.local` 中的 `VUE_APP_RTA_API` 配置。

所有时间参数使用毫秒，priority 数值越小优先级越高。未收敛任务显示的有限末值不是收敛 WCRT；静态分析结果与窗口内调度记录分别表示分析结论和实际观察过程。

## 测试与构建

以下命令支持 Windows、macOS 和 Linux，在仓库根目录执行：

```powershell
npm test          # 后端测试，然后验证前后端数据链
npm run build     # 后端测试并打包，然后构建前端
npm run verify    # 后端 clean/package、前端 lint、数据链测试和生产构建
```

后端测试自动生成 `server/target/rta-response-test.json` 和重复名称场景响应，前端数据链测试直接消费这些结果。所有测试输入均随仓库提供。

构建产物为 `client/dist/` 和 `server/target/rta-serve-side-0.0.1-SNAPSHOT.jar`。可在仓库根目录运行后端 JAR：

```powershell
java -jar server/target/rta-serve-side-0.0.1-SNAPSHOT.jar
```

前端构建产物需由静态文件服务器提供；部署时在构建前配置后端接口地址。`node_modules/`、`dist/`、`target/` 和本地环境配置已被 Git 忽略。

## 详细说明

- [前端说明](client/README.md)：页面、缩放与结果展示。
- [后端说明](server/README.md)：输入字段、API、分析与调度语义。
