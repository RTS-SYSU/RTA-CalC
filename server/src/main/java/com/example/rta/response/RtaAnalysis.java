package com.example.rta.response;

/**
 * {@code RtaAnalysis} 是后端向前端传递信息的载体，用于传输本次 RTA 的总体结论。
 */
public class RtaAnalysis {

    /** 处理器核心数。 */
    private Integer coreCount = 1;

    /** 调度策略。 */
    private String policy = "Fixed-Priority Preemptive";

    /** 分析方法。 */
    private String method = "Fixed-priority busy-period response time analysis";

    /** 优先级约定。 */
    private String priorityRule = "priority 数值越小优先级越高";

    /** 输入文件中的任务总数。 */
    private Integer taskCount;

    /** 参与分析的任务数。 */
    private Integer analyzedCount;

    /** 未参与分析的任务数。 */
    private Integer skippedCount;

    /** 超过截止期的任务数。 */
    private Integer missCount;
    private Integer unknownCount = 0;

    /** 总利用率。 */
    private Double totalUtilization;

    /** 总利用率是否不超过 1。 */
    private Boolean utilizationOk;

    /** 系统是否可调度。 */
    private Boolean schedulable;

    public RtaAnalysis(Integer coreCount, Integer taskCount, Integer analyzedCount, Integer skippedCount,
                       Integer missCount, Double totalUtilization, Boolean utilizationOk, Boolean schedulable) {
        this.coreCount = coreCount;
        this.taskCount = taskCount;
        this.analyzedCount = analyzedCount;
        this.skippedCount = skippedCount;
        this.missCount = missCount;
        this.totalUtilization = totalUtilization;
        this.utilizationOk = utilizationOk;
        this.schedulable = schedulable;
    }

    /* 以下均是上面属性的 setter 和 getter 函数。 */

    public Integer getCoreCount() { return this.coreCount; }

    public String getPolicy() { return this.policy; }

    public String getMethod() { return this.method; }

    public String getPriorityRule() { return this.priorityRule; }

    public Integer getTaskCount() { return this.taskCount; }

    public Integer getAnalyzedCount() { return this.analyzedCount; }

    public Integer getSkippedCount() { return this.skippedCount; }

    public Integer getMissCount() { return this.missCount; }
    public Integer getUnknownCount() { return this.unknownCount; }
    public void setUnknownCount(Integer unknownCount) { this.unknownCount = unknownCount; }

    public Double getTotalUtilization() { return this.totalUtilization; }

    public Boolean getUtilizationOk() { return this.utilizationOk; }

    public Boolean getSchedulable() { return this.schedulable; }

    public void setCoreCount(Integer coreCount) { this.coreCount = coreCount; }

    public void setPolicy(String policy) { this.policy = policy; }

    public void setMethod(String method) { this.method = method; }

    public void setPriorityRule(String priorityRule) { this.priorityRule = priorityRule; }

    public void setTaskCount(Integer taskCount) { this.taskCount = taskCount; }

    public void setAnalyzedCount(Integer analyzedCount) { this.analyzedCount = analyzedCount; }

    public void setSkippedCount(Integer skippedCount) { this.skippedCount = skippedCount; }

    public void setMissCount(Integer missCount) { this.missCount = missCount; }

    public void setTotalUtilization(Double totalUtilization) { this.totalUtilization = totalUtilization; }

    public void setUtilizationOk(Boolean utilizationOk) { this.utilizationOk = utilizationOk; }

    public void setSchedulable(Boolean schedulable) { this.schedulable = schedulable; }
}
