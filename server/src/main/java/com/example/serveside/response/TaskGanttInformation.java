package com.example.serveside.response;

/**
 * {@code TaskGanttInformation} 是后端向前端传递信息的载体，用于传输前端绘制任务甘特图所需的信息。
 * <p>
 *     {@code TaskGanttInformation} 包含任务的静态标识符、动态标识符、分配的处理器核心以及任务全周期执行情况和触发事件。
 * </p>
 */
public class TaskGanttInformation
{
    /**
     *  任务静态标识符。
     */
    private Integer staticPid;

    /**
     *  任务动态标识符。
     */
    private Integer dynamicPid;

    /**
     *  任务分配的处理器核心。
     */
    private Integer runningCPUCore;

    /**
     * 前端绘制任务甘特图所需信息，即任务全周期执行情况和触发事件。
     */
    private com.example.serveside.response.GanttInformation taskGanttInformation;


    /**
     * {@code TaskGanttInformation} 构造函数。
     */
    public TaskGanttInformation(Integer staticPid, Integer dynamicPid, Integer runningCPUCore, com.example.serveside.response.GanttInformation taskGanttInformation)
    {
        this.staticPid = staticPid;
        this.dynamicPid = dynamicPid;
        this.runningCPUCore = runningCPUCore;
        this.taskGanttInformation = taskGanttInformation;
    }

    /* 以下均是上面属性的 setter 和 getter 函数。 */

    public void setStaticPid(Integer staticPid)
    {
        this.staticPid = staticPid;
    }

    public Integer getStaticPid()
    {
        return this.staticPid;
    }

    public void setDynamicPid(Integer dynamicPid)
    {
        this.dynamicPid = dynamicPid;
    }

    public Integer getDynamicPid()
    {
        return this.dynamicPid;
    }

    public void setRunningCPUCore(Integer runningCPUCore)
    {
        this.runningCPUCore = runningCPUCore;
    }

    public Integer getRunningCPUCore()
    {
        return this.runningCPUCore;
    }

    public void setTaskGanttInformation(com.example.serveside.response.GanttInformation taskGanttInformation)
    {
        this.taskGanttInformation = taskGanttInformation;
    }

    public com.example.serveside.response.GanttInformation getTaskGanttInformation()
    {
        return this.taskGanttInformation;
    }
}
