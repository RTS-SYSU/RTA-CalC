package com.example.rta.response;

import com.example.rta.service.CommonUse.PeriodicTask;
import com.example.rta.service.RtaEngine;

import java.util.List;

/**
 * {@code RtaTask} 是后端向前端传递信息的载体，用于传输单个周期任务的 RTA 结果。
 */
public class RtaTask {

    /** 优先级排名，1 表示优先级最高。 */
    private Integer rank;
    private Integer id;

    /** 任务标识。 */
    private String name;

    /** 任务优先级（数值越小优先级越高）。 */
    private Double prio;

    /** 最差执行时间 C。 */
    private Double budgetMs;

    /** 周期 T。 */
    private Double periodMs;

    /** 相对截止期 D。 */
    private Double deadlineMs;

    /** 利用率 C / T。 */
    private Double utilization;

    /** 收敛的 R，或未收敛时实际有限迭代末值；参见 converged/note。 */
    private Double responseTimeMs;

    /** 截止期余量 D - R。 */
    private Double slackMs;

    /** 是否满足截止期。 */
    private Boolean schedulable;

    /** 迭代是否收敛到不动点。 */
    private Boolean converged;

    /** 实际迭代次数。 */
    private Integer iterations;

    /** 更高优先级任务的个数。 */
    private Integer hpCount;

    /** 更高优先级任务的名称，逗号分隔。 */
    private String hp;

    /** 结论说明。 */
    private String note;

    /** 是否被饿死。 */
    private Boolean starved;

    public RtaTask(Integer rank, PeriodicTask task, RtaEngine.Result result, List<String> higherPriorityTaskNames) {
        this.rank = rank;
        this.id = task.getId();
        this.name = task.getName();
        this.prio = task.getPriority();
        this.budgetMs = task.getWcet();
        this.periodMs = task.getPeriod();
        this.deadlineMs = task.deadlineOrDefault();
        this.utilization = task.getWcet() / task.getPeriod();
        this.starved = result.starved;
        this.responseTimeMs = !Double.isFinite(result.responseTime) ? null : result.responseTime;
        this.slackMs = this.responseTimeMs == null ? null : task.deadlineOrDefault() - result.responseTime;
        this.schedulable = result.converged ? result.schedulable : result.deadlineExceeded ? false : null;
        this.converged = result.converged;
        this.iterations = result.iterations;
        this.hpCount = higherPriorityTaskNames.size();
        this.hp = String.join(", ", higherPriorityTaskNames);
        this.note = result.note;
    }

    /* 以下均是上面属性的 setter 和 getter 函数。 */

    public Integer getRank() { return this.rank; }
    public Integer getId() { return this.id; }

    public String getName() { return this.name; }

    public Double getPrio() { return this.prio; }

    public Double getBudgetMs() { return this.budgetMs; }

    public Double getPeriodMs() { return this.periodMs; }

    public Double getDeadlineMs() { return this.deadlineMs; }

    public Double getUtilization() { return this.utilization; }

    public Double getResponseTimeMs() { return this.responseTimeMs; }

    public Double getSlackMs() { return this.slackMs; }

    public Boolean getSchedulable() { return this.schedulable; }

    public Boolean getConverged() { return this.converged; }

    public Integer getIterations() { return this.iterations; }

    public Integer getHpCount() { return this.hpCount; }

    public String getHp() { return this.hp; }

    public String getNote() { return this.note; }

    public Boolean getStarved() { return this.starved; }

    public void setRank(Integer rank) { this.rank = rank; }

    public void setName(String name) { this.name = name; }

    public void setPrio(Double prio) { this.prio = prio; }

    public void setBudgetMs(Double budgetMs) { this.budgetMs = budgetMs; }

    public void setPeriodMs(Double periodMs) { this.periodMs = periodMs; }

    public void setDeadlineMs(Double deadlineMs) { this.deadlineMs = deadlineMs; }

    public void setUtilization(Double utilization) { this.utilization = utilization; }

    public void setResponseTimeMs(Double responseTimeMs) { this.responseTimeMs = responseTimeMs; }

    public void setSlackMs(Double slackMs) { this.slackMs = slackMs; }

    public void setSchedulable(Boolean schedulable) { this.schedulable = schedulable; }

    public void setConverged(Boolean converged) { this.converged = converged; }

    public void setIterations(Integer iterations) { this.iterations = iterations; }

    public void setHpCount(Integer hpCount) { this.hpCount = hpCount; }

    public void setHp(String hp) { this.hp = hp; }

    public void setNote(String note) { this.note = note; }

    public void setStarved(Boolean starved) { this.starved = starved; }
}
