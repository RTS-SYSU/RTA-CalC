package com.example.serveside.response;

import java.util.*;

/**
 * {@code TaskGanttInformation} 是后端向前端传递信息的载体，用于传输指定资源共享协议下的模拟运行情况。
 * <p>
 *     {@code TaskGanttInformation} 包含所有处理器的甘特图信息、所有任务执行的甘特图信息以及系统发生关键级切换的时间点。
 * </p>
 */
public class ToTalInformation {
    /**
     *  处理器甘特图的信息。
     */
    private List<GanttInformation> cpuGanttInformations;

    /**
     *  任务甘特图信息。
     */
    private List<TaskGanttInformation> taskGanttInformations;

    /**
     * 系统发生关键级切换的时间点。
     */
    private Integer criticalitySwitchTime;

    /**
     * {@code TotalInformation} 的构造函数。
     */
    public ToTalInformation(List<GanttInformation> cpuGanttInformations, List<com.example.serveside.response.TaskGanttInformation> taskGanttInformations, Integer criticalitySwitchTime)
    {
        this.cpuGanttInformations = cpuGanttInformations;
        this.taskGanttInformations = taskGanttInformations;
        this.criticalitySwitchTime = criticalitySwitchTime;
    }

    /* 以下均是上面属性的 setter 和 getter 函数。 */
    public void setGanttInformations(List<GanttInformation> cpuGanttInformations) { this.cpuGanttInformations = cpuGanttInformations; }

    public List<GanttInformation> getCpuGanttInformations() { return this.cpuGanttInformations; }

    public void setTaskGanttInformations(List<com.example.serveside.response.TaskGanttInformation> taskGanttInformations) { this.taskGanttInformations = taskGanttInformations; }

    public List<com.example.serveside.response.TaskGanttInformation> getTaskGanttInformations() { return this.taskGanttInformations; }

    public void setCriticalitySwitchTime(Integer criticalitySwitchTime) { this.criticalitySwitchTime = criticalitySwitchTime; }

    public Integer getCriticalitySwitchTime() { return this.criticalitySwitchTime; }
}
