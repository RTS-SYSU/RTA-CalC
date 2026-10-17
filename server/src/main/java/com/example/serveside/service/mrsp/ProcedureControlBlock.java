package com.example.serveside.service.mrsp;

import com.example.serveside.service.CommonUse.BasicPCB;

/**
 * {@code ProcedureControlBlock} 类是模拟器中用于存储任务管理和控制信息的基本数据结构。
 * <p>
 *     模拟工具模拟任务在MSRP资源共享协议下运行时在所需的管理和控制任务执行的信息。<br>
 *     根据MrsP资源共享协议的特点和需求，在{@code BasicPCB}的基础上加入{immigrateRunningCpuCore}和{isHelp}属性，构成进程控制块（Procedure Control Block, PCB）。<br>
 *     每个任务都关联一个 PCB，在任务发布时，模拟器创建相应的 PCB，并负责在整个任务运行过程中进行管理。
 * </p>
 */
public class ProcedureControlBlock {
    public BasicPCB basicPCB;

    /**
     *  任务迁移之后运行的处理器核心。
     */
    public int immigrateRunningCpuCore;

    /**
     * 表示 task 是否在帮助其他任务完成资源访问
     */
    public boolean isHelp;

    /* Constructor function */
    public ProcedureControlBlock(BasicPCB basicPCB)
    {
        this.basicPCB = new BasicPCB(basicPCB);

        immigrateRunningCpuCore = -1;
        isHelp = false;
    }

    public ProcedureControlBlock(ProcedureControlBlock copy)
    {
        this.basicPCB = new BasicPCB(copy.basicPCB);

        immigrateRunningCpuCore = -1;
        isHelp = false;
    }
}
