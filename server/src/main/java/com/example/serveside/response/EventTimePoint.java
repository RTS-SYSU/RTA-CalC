package com.example.serveside.response;

/**
 * {@code EventInformation} 用于记录任务在执行过程中发生的事件（任务释放、请求资源、释放资源等）。
 */
public class EventTimePoint {
    /**
     * 任务静态标识符。
     */
    private Integer staticPid;

    /**
     * 任务动态标识符。
     */
    private Integer dynamicPid;

    /**
     * 发生的事件。
     */
    private String event;

    /**
     * 事件发生的时间。
     */
    private Integer eventTime;

    /**
     * 访问或释放的资源的标识符。
     */
    private Integer resourceId;

    /**
     * 构造函数，记录任务在执行过程中发生的事件。
     *
     * @param staticPid 任务静态标识符。
     * @param dynamicPid 任务动态标识符。
     * @param event 发生的事件。
     * @param eventTime 事件发生的时间。
     * @param resourceId 访问或释放的资源的标识符。
     */
    /* 构造函数 */
    public EventTimePoint(Integer staticPid, Integer dynamicPid, String event, Integer eventTime, Integer resourceId)
    {
        this.staticPid = staticPid;
        this.dynamicPid = dynamicPid;
        this.event = event;
        this.eventTime = eventTime;
        this.resourceId = resourceId;
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

    public void setEvent(String event)
    {
        this.event = event;
    }

    public String getEvent()
    {
        return this.event;
    }

    public void setEventTime(Integer eventTime)
    {
        this.eventTime = eventTime;
    }

    public Integer getEventTime()
    {
        return this.eventTime;
    }

    public Integer getResourceId() { return this.resourceId; }

    public void setResourceId(Integer resourceId) { this.resourceId = resourceId; }
}
