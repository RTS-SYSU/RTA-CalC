# 复用源码说明

本项目内置原 Mixed-Criticality-System-Simulator 项目中运行 RTA 所需的绘图组件和 Java 调度数据类型，保留原包名、作者注释及实现。无需检出同级 Mixed 仓库。

- 前端来源：Mixed-Criticality-System-Simulator-Client-Side/src/components/taskGantt/taskGanttChartLines.vue，现位于 client/src/components/mixed/。
- 后端来源：Mixed-Criticality-System-Simulator-Serve-Side/src/main/java/com/example/serveside/，现位于 server/src/main/java/com/example/serveside/。

纳入的后端文件：

- `com/example/serveside/response/EventInformation.java`
- `com/example/serveside/response/EventTimePoint.java`
- `com/example/serveside/response/GanttInformation.java`
- `com/example/serveside/response/TaskGanttInformation.java`
- `com/example/serveside/response/TaskInformation.java`
- `com/example/serveside/response/ToTalInformation.java`
- `com/example/serveside/service/CommonUse/BasicPCB.java`
- `com/example/serveside/service/CommonUse/BasicResource.java`
- `com/example/serveside/service/mrsp/ProcedureControlBlock.java`
- `com/example/serveside/service/mrsp/Resource.java`
- `com/example/serveside/service/msrp/MSRPMixedCriticalSystem.java`
- `com/example/serveside/service/msrp/ProcedureControlBlock.java`
- `com/example/serveside/service/msrp/Resource.java`
- `com/example/serveside/service/pwlp/ProcedureControlBlock.java`
- `com/example/serveside/service/pwlp/Resource.java`
