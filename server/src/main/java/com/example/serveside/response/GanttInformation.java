package com.example.serveside.response;

import java.util.List;

/**
 * {@code GanttInformation} 是后端向前端传递信息的载体，用于后端向前端传输绘制处理器甘特图所需的信息，包含处理器运行任务在每个时间段的执行状态、事件发生的时间点以及甘特图长度。
 */
public class GanttInformation {
    /**
     * 处理器运行任务在每个时间段的执行状态。
     */
    private List<com.example.serveside.response.EventInformation> eventInformations;

    /**
     * 甘特图长度。
     */
    private Integer timeAxisLength;

    /**
     * 处理器运行任务触发事件的时间点。
     */
    private List<EventTimePoint> eventTimePoints;

    /**
     * 构造函数：初始化一个 {@code GanttInformation} 实例对象。
     */
    public GanttInformation(List<EventInformation> eventInformations, List<EventTimePoint> eventTimePoints, Integer timeAxisLength) {
        this.eventInformations = eventInformations;
        this.eventTimePoints = eventTimePoints;
        this.timeAxisLength = timeAxisLength;
    }

    /* 以下均是上面属性的 setter 和 getter 函数。 */
    public List<EventInformation> getEventInformations() {
        return this.eventInformations;
    }

    public void setEventInformations(List<EventInformation> eventInformations) {
        this.eventInformations = eventInformations;
    }

    public Integer getTimeAxisLength()
    {
        return this.timeAxisLength;
    }

    public void setTimeAxisLength(Integer timeAxisLength)
    {
        this.timeAxisLength = timeAxisLength;
    }

    public List<EventTimePoint> getEventTimePoints() { return this.eventTimePoints; }

    public void setEventTimePoints(List<EventTimePoint> eventTimePoints) { this.eventTimePoints = eventTimePoints; }
}
