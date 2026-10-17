package com.example.serveside.response;

/**
 * {@code EventInformation} 用于记录任务在执行过程中某一时间段的执行状态（正常执行、到达阻塞、访问资源等）。
 */
public class EventInformation {
    /**
     * 任务静态标识符。
     */
    private Integer staticPid;

    /**
     * 任务动态标识符。
     */
    private Integer dynamicPid;

    /**
     * 任务所处的执行状态。
     */
    private String state;

    /**
     * 状态的开始时间。
     */
    private Integer startTime;

    /**
     * 状态的结束时间。
     */
    private Integer endTime;

    /**
     * 构造函数，记录在 MrsP 资源共享协议下任务从指定时间开始所处的状态。
     *
     * @param task 记录任务状态的任务。
     * @param _startTime 状态开始的时间点。
     * @param _state 任务所处的状态
     */
    public EventInformation(com.example.serveside.service.mrsp.ProcedureControlBlock task, Integer _startTime, String _state)
    {
        // 任务为空，此时 cpu 空闲
        if (task == null)
        {
            staticPid = -1;
            dynamicPid = -1;
            startTime = _startTime;
            endTime = -1;
            state = "spare";
            return ;
        }

        staticPid = task.basicPCB.staticTaskId;;
        dynamicPid = task.basicPCB.dynamicTaskId;
        startTime = _startTime;
        endTime = -1;

        if (_state.isEmpty())
        {
            // 设置状态
            if (task.basicPCB.spin)
                state = "direct-spinning";
            else if (task.basicPCB.isAccessGlobalResource || task.basicPCB.isAccessLocalResource)
                state = "access-resource";
            else
                state = "normal-execution";
        }else
            state = _state;
    }

    /**
     * 构造函数，记录在 MSRP 资源共享协议下任务从指定时间开始所处的状态。
     *
     * @param task 记录任务状态的任务。
     * @param _startTime 状态开始的时间点。
     * @param _state 任务所处的状态
     */
    public EventInformation(com.example.serveside.service.msrp.ProcedureControlBlock task, Integer _startTime, String _state)
    {
        // 任务为空，此时 cpu 空闲
        if (task == null)
        {
            staticPid = -1;
            dynamicPid = -1;
            startTime = _startTime;
            endTime = -1;
            state = "spare";
            return ;
        }

        staticPid = task.basicPCB.staticTaskId;;
        dynamicPid = task.basicPCB.dynamicTaskId;
        startTime = _startTime;
        endTime = -1;

        if (_state.isEmpty())
        {
            // 设置状态
            if (task.basicPCB.spin)
                state = "direct-spinning";
            else if (task.basicPCB.isAccessGlobalResource || task.basicPCB.isAccessLocalResource)
                state = "access-resource";
            else
                state = "normal-execution";
        }else
            state = _state;
    }

    /**
     * 构造函数，记录在 PWLP 资源共享协议下任务从指定时间开始所处的状态。
     *
     * @param task 记录任务状态的任务。
     * @param _startTime 状态开始的时间点。
     * @param _state 任务所处的状态
     */
    public EventInformation(com.example.serveside.service.pwlp.ProcedureControlBlock task, Integer _startTime, String _state)
    {
        // 任务为空，此时 cpu 空闲
        if (task == null)
        {
            staticPid = -1;
            dynamicPid = -1;
            startTime = _startTime;
            endTime = -1;
            state = "spare";
            return ;
        }

        staticPid = task.basicPCB.staticTaskId;;
        dynamicPid = task.basicPCB.dynamicTaskId;
        startTime = _startTime;
        endTime = -1;

        if (_state.isEmpty())
        {
            // 设置状态
            if (task.basicPCB.spin)
                state = "direct-spinning";
            else if (task.basicPCB.isAccessGlobalResource || task.basicPCB.isAccessLocalResource)
                state = "access-resource";
            else
                state = "normal-execution";
        }else
            state = _state;
    }

    /* 以下均是上面属性的 setter 和 getter 函数。 */
    public Integer getStaticPid() {
        return this.staticPid;
    }

    public Integer getDynamicPid() {
        return this.dynamicPid;
    }

    public String getState() {
        return this.state;
    }

    public Integer getStartTime() {
        return this.startTime;
    }

    public Integer getEndTime() {
        return this.endTime;
    }

    public void setStaticPid(Integer staticPid) {
        this.staticPid = staticPid;
    }

    public void setDynamicPid(Integer dynamicPid) {
        this.dynamicPid = dynamicPid;
    }

    public void setState(String state){
        this.state = state;
    }

    public void setStartTime(Integer startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(Integer endTime) {
        this.endTime = endTime;
    }
}
