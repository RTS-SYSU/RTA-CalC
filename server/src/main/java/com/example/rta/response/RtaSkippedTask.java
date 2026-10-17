package com.example.rta.response;

import com.example.rta.service.CommonUse.PeriodicTask;

/**
 * {@code RtaSkippedTask} 是后端向前端传递信息的载体，用于传输未参与 RTA 的任务及其原因。
 */
public class RtaSkippedTask {

    /** 任务标识。 */
    private String name;

    /** 任务优先级。 */
    private Double prio;

    /** 最差执行时间。 */
    private Double budgetMs;

    /** 周期。 */
    private Double periodMs;

    /** 未参与分析的原因。 */
    private String reason;

    public RtaSkippedTask(PeriodicTask task, String reason) {
        this.name = task.getName();
        this.prio = task.getPriority();
        this.budgetMs = task.getWcet();
        this.periodMs = task.getPeriod();
        this.reason = reason;
    }

    /* 以下均是上面属性的 setter 和 getter 函数。 */

    public String getName() { return this.name; }

    public Double getPrio() { return this.prio; }

    public Double getBudgetMs() { return this.budgetMs; }

    public Double getPeriodMs() { return this.periodMs; }

    public String getReason() { return this.reason; }

    public void setName(String name) { this.name = name; }

    public void setPrio(Double prio) { this.prio = prio; }

    public void setBudgetMs(Double budgetMs) { this.budgetMs = budgetMs; }

    public void setPeriodMs(Double periodMs) { this.periodMs = periodMs; }

    public void setReason(String reason) { this.reason = reason; }
}
