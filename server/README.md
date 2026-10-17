# RTA 单核响应时间分析后端

基于 Java 8 / Spring Boot 2.7.6 的单核固定优先级响应时间分析服务。读取任务 JSON，计算静态响应时间分析结果，并调用 Mixed 原始调度器生成任务甘特图数据，供配套前端展示。

## 环境与目录

需要 JDK 8 及以上版本，使用本目录的 Maven Wrapper 构建。Mixed 调度器及其数据类型已内置在 `src/main/java/com/example/serveside/`，无需其他仓库。RTA 应用位于 `com.example.rta`，通过 `MixedTimelineAdapter` 调用调度器。源码来源见 [复用说明](../THIRD_PARTY_NOTICES.md)。

## 启动服务

在本目录运行：

```powershell
./mvnw.cmd spring-boot:run
```

默认接口为 `http://localhost:8080/api/rta`。可配合 本仓库的 `client` 前端使用。

默认任务文件按以下顺序查找：`rta.input` 指定的有效文件、当前工作目录的 `test.json`、类路径内置的 `test.json`。可在 `src/main/resources/application.properties` 中配置：

```properties
rta.input=./src/main/resources/test.json
```

甘特图观察窗口由所有输入任务的最大 deadline 决定；空任务列表使用 100 ms。当前实现不通过 `rta.gantt-horizon-ms` 配置窗口。

## API

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/api/rta` | 分析默认任务文件 |
| POST | `/api/rta` | 分析请求体中的任务 JSON，使用 `Content-Type: application/json` |

空请求体也会读取默认文件。输入读取、解析或字段校验失败时返回 HTTP 400。接口已启用跨域访问。

在本目录使用 PowerShell 调用：

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/rta
Invoke-RestMethod -Uri http://localhost:8080/api/rta -Method Post -ContentType 'application/json; charset=utf-8' -InFile ./src/main/resources/test.json
```

## 输入格式

```json
{
  "system": { "core_count": 1 },
  "tasks": [
    {
      "id": 1,
      "name": "TaskA",
      "function": "",
      "partition": 0,
      "priority": 1,
      "period": 20,
      "phase": 0,
      "deadline": 20,
      "WCET": 5
    }
  ]
}
```

- `system.core_count` 必须为 1，`tasks` 必须为数组；示例中的全部任务字段均为必填。
- `id` 为唯一整数，`partition` 必须为整数 0；`name`、`function` 为字符串，名称可重复。
- `priority` 为有限数值，数值越小优先级越高；参与分析的任务优先级必须唯一。
- `period`、`phase`、`deadline`、`WCET` 为有限非负数，单位统一为毫秒，支持小数。
- `period = 0` 或 `WCET = 0` 的任务保留在配置结果中，列入 `skippedTasks`，不参与静态分析及调度。
- 使用上述字段名，`WCET` 为大写；不支持旧版 `prio`、`budget_ms`、`period_ms` 等字段替代。

## 分析与调度语义

`RtaEngine` 使用精确十进制进行临界时刻单作业递推，初值为任务自身 WCET：

```text
R_i(0) = C_i
R_i(n+1) = C_i + Σ ceil(R_i(n) / T_j) × C_j，j ∈ hp(i)
```

`hp(i)` 只包含 priority 严格小于当前任务的任务。静态递推不使用 phase，phase 用于时间线中的实际释放。

收敛结果与 deadline 比较以判定可调度性；未收敛时保留最后实际有限迭代值及 `lastIterationValue`、`converged`、`nonConvergent` 等元数据，不用 Infinity 替代。连续过载或 deadline 大于 period 时，单作业不动点不能直接当作所有后续作业的响应时间上界。

时间线由 `MixedTimelineAdapter` 驱动原始 Mixed 调度器产生：

- 将输入优先级按 rank 转换，保持 Mixed 内部“大数优先”规则与 RTA 输入“小数优先”语义一致。
- 将毫秒按精确公共量子转换为整数 ticks，输出时还原毫秒，不舍入输入时间；超过 2,000,000 ticks 或 20,000 个作业的输入会被拒绝。
- 固定窗口内持续调度，即使出现 deadline miss 也继续观察；同一任务的作业按 FIFO 处理。
- 执行区间和完成事件取自原始记录，区间采用 `[start, end)`；窗口末尾未完成或被饿死的作业不会生成虚假的完成时间。

## 响应数据

| 字段 | 内容 |
| --- | --- |
| `analysis` | 任务数量、跳过数量、利用率、miss 数量及系统可调度结论 |
| `tasks` | 参与静态分析的任务及结果 |
| `skippedTasks` | 未参与分析的任务及原因 |
| `systemTasks` | 全部输入任务及按 ID 关联的分析结果 |
| `warnings` | 输入加载等过程中的提示 |
| `timeline` | 甘特图观察窗口、CPU 记录、事件及作业行 |

`systemTasks.rtaDetails` 包含干扰任务集合、逐轮输入/输出及每个高优先级任务的向上取整次数和干扰量。

`timeline` 包含 `horizon`、`cpu`、`events`、`rows`，并提供 `engine` 和 `tickMs` 以追溯调度来源。每次释放对应一个作业行，内部通过 `jobId = taskId:jobIndex` 保持身份稳定。

`interference` 是观察窗口内，作业已释放但由其他任务占用 CPU 的累计时长，区间中的 ID 标识实际占用 CPU 的任务。同一任务旧作业引起的等待单独记为 `queued`。二者均为调度记录的派生统计，不是输入字段，也不是静态 WCRT；未完成作业仅统计窗口内已观察到的等待。

## 测试与打包

在本目录运行：

```powershell
./mvnw.cmd test
./mvnw.cmd package
```

打包后可运行：

```powershell
java -jar target/rta-serve-side-0.0.1-SNAPSHOT.jar
```

测试输入全部位于 `src/test/resources/fixtures/`，覆盖静态递推、输入校验、时间线、Mixed 源码复用及数据链一致性。后端测试通过后会生成 `target/rta-response-test.json`，随后可在前端执行 `npm run test:data-chain`。

