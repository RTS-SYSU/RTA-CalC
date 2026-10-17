package com.example.rta.response;

import com.example.rta.service.CommonUse.PeriodicTask;
import com.example.rta.service.RtaEngine;

/**
 * {@code RtaSystemTask} 是后端向前端传递信息的载体，用于把输入文件里每个任务的原始配置原样传给前端展示。
 */
public class RtaSystemTask {

    /** 任务编号。 */
    private Integer id;
    private String name;

    /** 任务对应的功能名称，可以为空。 */
    private String function;

    /** 任务分配到的分区（处理器核心编号）。 */
    private Integer partition;

    /** 优先级，数值越小优先级越高。 */
    private Double priority;

    /** 最短发布周期间隔。 */
    private Double period;

    /** 相位。 */
    private Double phase;

    /** 相对截止期。 */
    private Double deadline;

    /** 最差执行时间。 */
    private Double wcet;

    /** 已证明的有限 WCRT；无界、未确定或跳过分析时为 null。 */
    private Double wcrt;

    /** 是否被饿死（响应时间为无穷大）。 */
    private Boolean starved;
    private Boolean schedulable;
    private Boolean converged;
    private Boolean nonConvergent;
    private Boolean unbounded;
    private java.math.BigDecimal lastIterationValue;
    private String rtaNote;
    private java.util.Map<String,Object> rtaDetails;

    public RtaSystemTask(PeriodicTask task, RtaEngine.Result result) {
        this.id = task.getId();
        this.name = task.getName();
        this.function = task.getFunction();
        this.partition = task.getPartition();
        this.priority = task.getPriority();
        this.period = task.getPeriod();
        this.phase = task.getPhase();
        this.deadline = task.getDeadline();
        this.wcet = task.getWcet();
        this.wcrt = (result == null || !Double.isFinite(result.responseTime)) ? null : result.responseTime;
        this.converged = result == null ? null : result.converged;
        this.nonConvergent = result != null && result.nonConvergent;
        this.unbounded = result != null && result.unbounded;
        this.lastIterationValue = result == null ? null : result.lastIterationValue;
        this.starved = result != null && result.starved;
        this.schedulable = result == null ? null
                : result.converged ? Boolean.valueOf(result.schedulable)
                : result.deadlineExceeded ? Boolean.FALSE : null;
        this.rtaNote = result == null ? "未参与 RTA 分析" : result.note;
        if (result != null) {
            this.rtaDetails = new java.util.LinkedHashMap<>();
            rtaDetails.put("name",name); rtaDetails.put("priority",priority); rtaDetails.put("WCET",wcet);
            rtaDetails.put("period",period); rtaDetails.put("deadline",deadline); rtaDetails.put("wcrt",wcrt);
            rtaDetails.put("starved",starved); rtaDetails.put("schedulable",schedulable); rtaDetails.put("note",rtaNote);
            rtaDetails.put("initial",wcet); rtaDetails.put("higherPriorityTasks",result.higherPriorityTasks);
            rtaDetails.put("trace",result.trace); rtaDetails.put("converged",result.converged);
            rtaDetails.put("nonConvergent",nonConvergent); rtaDetails.put("lastIterationValue",lastIterationValue);
            rtaDetails.put("unbounded",unbounded); rtaDetails.put("jobResponses",result.jobResponses);
        }
    }

    public Double getWcrt() {
        return this.wcrt;
    }

    public Boolean getStarved() {
        return this.starved;
    }
    public Boolean getSchedulable() { return this.schedulable; }
    public String getRtaNote() { return this.rtaNote; }
    public java.util.Map<String,Object> getRtaDetails() { return this.rtaDetails; }
    public Boolean getConverged() { return converged; }
    public Boolean getNonConvergent() { return nonConvergent; }
    public Boolean getUnbounded() { return unbounded; }
    public java.math.BigDecimal getLastIterationValue() { return lastIterationValue; }

    public void setWcrt(Double wcrt) {
        this.wcrt = wcrt;
    }

    public void setStarved(Boolean starved) {
        this.starved = starved;
    }

    /* 以下均是上面属性的 setter 和 getter 函数。 */

    public Integer getId() { return this.id; }

    public String getFunction() { return this.function; }

    public Integer getPartition() { return this.partition; }

    public Double getPriority() { return this.priority; }

    public Double getPeriod() { return this.period; }

    public Double getPhase() { return this.phase; }

    public Double getDeadline() { return this.deadline; }

    @com.fasterxml.jackson.annotation.JsonProperty("WCET")
    public Double getWcet() { return this.wcet; }
    public String getName() { return this.name; }

    public void setId(Integer id) { this.id = id; }

    public void setFunction(String function) { this.function = function; }

    public void setPartition(Integer partition) { this.partition = partition; }

    public void setPriority(Double priority) { this.priority = priority; }

    public void setPeriod(Double period) { this.period = period; }

    public void setPhase(Double phase) { this.phase = phase; }

    public void setDeadline(Double deadline) { this.deadline = deadline; }

    public void setWcet(Double wcet) { this.wcet = wcet; }
}
