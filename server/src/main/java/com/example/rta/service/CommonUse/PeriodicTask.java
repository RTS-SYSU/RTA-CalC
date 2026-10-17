package com.example.rta.service.CommonUse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PeriodicTask {

    private Integer id;

    private String function;
    private String name;
    public void setName(String name) { this.name = name; }

    private Integer partition;

    private double priority;

    private double period;

    private Double phase;

    private Double deadline;

    @JsonProperty("WCET")
    private double wcet;

    public Integer getId() {
        return this.id;
    }

    public String getFunction() {
        return this.function;
    }

    public Integer getPartition() {
        return this.partition;
    }

    public double getPriority() {
        return this.priority;
    }

    public double getPeriod() {
        return this.period;
    }

    public Double getPhase() {
        return this.phase;
    }

    public Double getDeadline() {
        return this.deadline;
    }

    public double getWcet() {
        return this.wcet;
    }

    public double deadlineOrDefault() {
        return this.deadline == null ? this.period : this.deadline;
    }

    public String getName() { return this.name; }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public void setPartition(Integer partition) {
        this.partition = partition;
    }

    public void setPriority(double priority) {
        this.priority = priority;
    }

    public void setPeriod(double period) {
        this.period = period;
    }

    public void setPhase(Double phase) {
        this.phase = phase;
    }

    public void setDeadline(Double deadline) {
        this.deadline = deadline;
    }

    public void setWcet(double wcet) {
        this.wcet = wcet;
    }
}
