package com.example.serveside.response;

import java.util.ArrayList;

/**
 * {@code TaskGanttInformation} 是后端向前端传递信息的载体，用于传输系统任务的详细信息。
 */

public class TaskInformation
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
     *  任务优先级。
     */
    private Integer priority;

    /**
     *  任务关键级。
     */
    private Integer criticality;

    /**
     *  任务发布时间。
     */
    private Integer releaseTime;

    /**
     *  任务分配的处理器核心。
     */
    private Integer allocation;

    /**
     *  任务在低关键级下的最差执行时间。
     */
    private Integer WCCTLow;

    /**
     *  任务在高关键级下的最差执行时间。
     */
    private Integer WCCTHigh;

    /**
     *  任务的利用率。
     */
    private Double utilization;

    /**
     *  任务最短发布周期。
     */
    private Integer period;

    /**
     * 任务总的执行时长，包含使用处理器进行计算以及使用资源的时长。
     */
    private Integer totalTime;

    /**
     * 任务在执行过程中访问资源的时间点。
     */
    private ArrayList<Integer> resourceAccessTime;

    /**
     * 任务在执行过程中访问的资源。
     */
    private ArrayList<Integer> resourceAccessIndex;


    public TaskInformation(com.example.serveside.service.msrp.ProcedureControlBlock procedureControlBlock, Integer systemClock)
    {
        this.staticPid = procedureControlBlock.basicPCB.staticTaskId;
        this.dynamicPid = procedureControlBlock.basicPCB.dynamicTaskId;
        this.priority = procedureControlBlock.basicPCB.priorities.peek();
        this.criticality = procedureControlBlock.basicPCB.criticality;
        this.resourceAccessIndex = procedureControlBlock.basicPCB.accessResourceIndex;
        this.releaseTime = systemClock;
        this.WCCTLow = procedureControlBlock.basicPCB.WCCT_low;
        this.WCCTHigh = procedureControlBlock.basicPCB.WCCT_high;
        this.utilization = procedureControlBlock.basicPCB.utilization;
        this.period = procedureControlBlock.basicPCB.period;
        this.totalTime = procedureControlBlock.basicPCB.totalNeededTime;
        this.allocation = procedureControlBlock.basicPCB.baseRunningCpuCore;

        this.resourceAccessTime = new ArrayList<>();
        for (Integer accessTime : procedureControlBlock.basicPCB.resourceAccessTime)
        {
            this.resourceAccessTime.add(accessTime + systemClock);
        }
    }

    public TaskInformation(com.example.serveside.service.mrsp.ProcedureControlBlock procedureControlBlock, Integer systemClock)
    {
        this.staticPid = procedureControlBlock.basicPCB.staticTaskId;
        this.dynamicPid = procedureControlBlock.basicPCB.dynamicTaskId;
        this.priority = procedureControlBlock.basicPCB.priorities.peek();
        this.criticality = procedureControlBlock.basicPCB.criticality;
        this.resourceAccessIndex = procedureControlBlock.basicPCB.accessResourceIndex;
        this.releaseTime = systemClock;
        this.WCCTLow = procedureControlBlock.basicPCB.WCCT_low;
        this.WCCTHigh = procedureControlBlock.basicPCB.WCCT_high;
        this.utilization = procedureControlBlock.basicPCB.utilization;
        this.period = procedureControlBlock.basicPCB.period;
        this.totalTime = procedureControlBlock.basicPCB.totalNeededTime;
        this.allocation = procedureControlBlock.basicPCB.baseRunningCpuCore;

        this.resourceAccessTime = new ArrayList<>();
        for (Integer accessTime : procedureControlBlock.basicPCB.resourceAccessTime)
        {
            this.resourceAccessTime.add(accessTime + systemClock);
        }
    }

    public TaskInformation(com.example.serveside.service.pwlp.ProcedureControlBlock procedureControlBlock, Integer systemClock)
    {
        this.staticPid = procedureControlBlock.basicPCB.staticTaskId;
        this.dynamicPid = procedureControlBlock.basicPCB.dynamicTaskId;
        this.priority = procedureControlBlock.basicPCB.priorities.peek();
        this.criticality = procedureControlBlock.basicPCB.criticality;
        this.resourceAccessIndex = procedureControlBlock.basicPCB.accessResourceIndex;
        this.releaseTime = systemClock;
        this.WCCTLow = procedureControlBlock.basicPCB.WCCT_low;
        this.WCCTHigh = procedureControlBlock.basicPCB.WCCT_high;
        this.utilization = procedureControlBlock.basicPCB.utilization;
        this.period = procedureControlBlock.basicPCB.period;
        this.totalTime = procedureControlBlock.basicPCB.totalNeededTime;
        this.allocation = procedureControlBlock.basicPCB.baseRunningCpuCore;

        this.resourceAccessTime = new ArrayList<>();
        for (Integer accessTime : procedureControlBlock.basicPCB.resourceAccessTime)
        {
            this.resourceAccessTime.add(accessTime + systemClock);
        }
    }


    public TaskInformation(com.example.serveside.service.CommonUse.BasicPCB basicPCB)
    {
        this.staticPid = basicPCB.staticTaskId;
        this.dynamicPid = basicPCB.dynamicTaskId;
        this.priority = basicPCB.priorities.peek();
        this.criticality = basicPCB.criticality;
        this.resourceAccessIndex = basicPCB.accessResourceIndex;
        this.releaseTime = 1;
        this.WCCTLow = basicPCB.WCCT_low;
        this.WCCTHigh = basicPCB.WCCT_high;
        this.utilization = basicPCB.utilization;
        this.period = basicPCB.period;
        this.totalTime = basicPCB.totalNeededTime;
        this.allocation = basicPCB.baseRunningCpuCore;

        this.resourceAccessTime = new ArrayList<>();
        this.resourceAccessTime.addAll(basicPCB.resourceAccessTime);
    }

    /* 以下均是上面属性的 setter 和 getter 函数。 */

    public Integer getStaticPid() {
        return this.staticPid;
    }

    public Integer getDynamicPid() {
        return this.dynamicPid;
    }

    public Integer getPriority() {
        return this.priority;
    }

    public Integer getCriticality() {
        return this.criticality;
    }

    public Integer getReleaseTime() {
        return this.releaseTime;
    }

    public Integer getWCCTLow() { return this.WCCTLow; }

    public Integer getWCCTHigh() { return this.WCCTHigh; }

    public Double getUtilization() { return this.utilization; }

    public Integer getPeriod() { return this.period; }

    public Integer getTotalTime() { return this.totalTime; }

    public Integer getAllocation() { return this.allocation; }

    public ArrayList<Integer> getResourceAccessTime() { return this.resourceAccessTime; }

    public ArrayList<Integer> getResourceAccessIndex() { return this.resourceAccessIndex; }

    public void setStaticPid(Integer staticPid) {
        this.staticPid = staticPid;
    }

    public void setDynamicPid(Integer dynamicPid) {
        this.dynamicPid = dynamicPid;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public void setCriticality(Integer criticality) {
        this.criticality = criticality;
    }

    public void setReleaseTime(Integer releaseTime) { this.releaseTime = releaseTime; }

    public void setResourceAccessTime(ArrayList<Integer> resourceAccessTime) { this.resourceAccessTime = resourceAccessTime; }

    public void setResourceAccessIndex(ArrayList<Integer> resourceAccessIndex) { this.resourceAccessIndex = resourceAccessIndex; }

    public void setWCCTLow(Integer _WCCTLow) { this.WCCTLow = _WCCTLow; }

    public void setWCCTHigh(Integer _WCCTHigh) { this.WCCTHigh = _WCCTHigh; }

    public void setUtilization(Double _utilization) { this.utilization = _utilization; }

    public void setPeriod(Integer _period) { this.period = _period; }

    public void setTotalTime(Integer _totalTime) { this.totalTime = _totalTime; }

    public void setAllocation(Integer _allocation) { this.allocation = _allocation; }
}
