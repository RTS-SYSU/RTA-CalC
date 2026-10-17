package com.example.serveside.service.CommonUse;

import java.util.ArrayList;
import java.util.Stack;

/**
 * {@code BasicPCB} 类是模拟器中用于存储任务管理和控制信息的基本数据结构。
 * <p>
 *     该类描述了在不同资源共享协议下任务执行时，模拟器所需的通用控制和管理信息。<br>
 *     根据协议的特点和需求，通过在 {@code BasicPCB} 的基础上添加新属性，构建了相应的进程控制块（Procedure Control Block, PCB）。<br>
 *     每个任务都关联一个 PCB，在任务发布时，模拟器创建相应的 PCB，并负责在整个任务运行过程中进行管理。
 * </p>
 */
public class BasicPCB {
    /**
     * 表示任务是否处于自旋等待资源状态。
     * <p>
     *      初始值为 false。<br>
     *      当为 true 时，表示任务正在自旋等待资源。
     * </p>
     * */
    public boolean spin;

    /**
     * 表示任务是否持有全局资源。
     * <p>
     *     初始值为 false。<br>
     *     当为 true 时，表示任务当前持有全局资源。
     * </p>
     * */
    public boolean isAccessGlobalResource;

    /**
     * 表示任务是否持有局部资源。
     * <p>
     *     初始值为 false。<br>
     *     当为 true 时，表示任务当前持有局部资源。
     * </p>
     * */
    public boolean isAccessLocalResource;

    /**
     * 表示任务剩余使用资源时间，即在 {@code remainResourceComputationTime} 时间后释放资源。
     * <p>
     *     仅在 {@code isAccessGlobalResource} 或 {@code isAccessLocalResource} 为 true 时，即任务正在访问资源时，此属性有效。
     * </p>
     */
    public int remainResourceComputationTime;

    /**
     * 用于索引 {@code accessResourceIndex} 和 {@code resourceAccessTime} 列表。
     * <p>
     *     初始值为 0。<br>
     *     在任务释放完资源后，{@code requestResourceTh}自增。
     * </p>
     * */
    public int requestResourceTh;

    /**
     * 存储任务访问资源的时间点。
     * */
    public ArrayList<Integer> resourceAccessTime;

    /**
     * 存储任务访问的资源标识。
     * */
    public ArrayList<Integer> accessResourceIndex;

    /**
     * 任务访问资源时所处的系统关键级。
     * <p>
     *     当 {@code systemCriticalityWhenAccessResource} 为 1 时，表示任务持有资源时系统处于高关键级。<br>
     *     当 {@code systemCriticalityWhenAccessResource} 为 0 时，表示任务持有资源时系统处于低关键级。<br>
     * </p>
     * */
    public int systemCriticalityWhenAccessResource;

    /**
     * 任务自发布以来经过的时间。
     * <p>
     *     每经过一个时钟周期，{@code elapsedTime} 自增加一。
     * </p>
     */
    public int elapsedTime;

    /**
     * 任务总的执行时长，包括纯计算时长和访问资源时长。
     */
    public int totalNeededTime;

    /**
     * 任务到目前为止的执行时长。
     * <p>
     *     当任务在处理器核心上进行纯计算或使用资源时，{@code executedTime} 自增。
     * </p>
     */
    public int executedTime;

    /**
     * 任务到目前为止的纯计算时长和自旋等待资源时长之和。
     * <p>
     *     当任务在处理器核心上进行纯计算或自旋等待资源时，{@code computeAndSpinTime} 自增。
     * </p>
     */
    public int computeAndSpinTime;

    /**
     * 任务的截止时间。
     */
    public int deadline;

    /**
     * 任务的最短发布周期间隔。
     */
    public int period;

    /**
     * 任务的利用率。
     */
    public double utilization;

    /**
     * {@code priorities} 动态管理任务在执行过程中的优先级。
     * <p>
     *     在模拟系统运行过程中，任务以栈顶的优先级执行。<br>
     *     当任务持有资源时，资源对应的资源上限优先级入栈。<br>
     *     当任务释放资源时，资源对应的资源上限优先级出栈。
     * </p>
     */
    public Stack<Integer> priorities;

    /**
     * 任务的基础优先级。
     * <p>
     *     {@code basePriority} 值越大，任务的基础优先级越高。
     * </p>
     */
    public int basePriority;

    /**
     * 任务的静态标识符。
     * <p>
     *     每种任务对应一个唯一的静态标识符。
     * </p>
     */
    public int staticTaskId;

    /**
     * 任务的动态标识符。
     * <p>
     *     在系统模拟运行过程中，每个已发布的任务对应一个唯一的动态标识符。
     * </p>
     */
    public int dynamicTaskId;

    /**
     * 任务分配的处理器。
     */
    public int baseRunningCpuCore;

    /**
     * 任务的关键级。
     * <p>
     *     当 {@code criticality} 为 0 时，任务为低关键级任务。<br>
     *     当 {@code criticality} 为 1 时，任务为高关键级任务。
     * </p>
     */
    public int criticality;

    /**
     * 任务在低关键级下的最差计算时间（不包括访问资源的时长）。
     */
    public int WCCT_low;

    /**
     * 任务在高关键级下的最差计算时间（不包括访问资源的时长）。
     */
    public int WCCT_high;

    /**
     * {@code BasicPCB} 的构造函数，初始化一个任务的管理和控制信息。
     *
     * @param _priority 任务的优先级。
     * @param _period 任务的最短发布周期间隔。
     * @param _utilization 任务的利用率。
     * @param _task_id 任务的静态标识符。
     */
    public BasicPCB(int _priority, int _period, double _utilization, int _task_id)
    {
        priorities = new Stack<>();
        priorities.add(_priority);
        basePriority = _priority;

        period = _period;
        deadline = _period;
        utilization = _utilization;
        staticTaskId = _task_id;
        totalNeededTime = (int)(period * utilization);
        resourceAccessTime = new ArrayList<>();
        accessResourceIndex = new ArrayList<>();

        /* Initialize the procedure control block. */
        dynamicTaskId = -1;
        spin = false;
        elapsedTime = 0;
        executedTime = 0;
        computeAndSpinTime = 0;
        isAccessGlobalResource = false;
        isAccessLocalResource = false;
        remainResourceComputationTime = 0;
        requestResourceTh = 0;
        criticality = 0;
        WCCT_low = 0;
        WCCT_high = 0;
        systemCriticalityWhenAccessResource = 0;

        baseRunningCpuCore = -1;
    }

    /**
     * {@code BasicPCB} 的拷贝构造函数：从现有的 {@code BasicPCB} 实例中复制用于管理和控制任务的信息，创建一个新的 {@code BasicPCB}。
     *
     * @param copy 要复制的 {@code BasicPCB} 实例。
     */
    public BasicPCB(BasicPCB copy)
    {
        priorities = new Stack<>();
        priorities.addAll(copy.priorities);
        basePriority = copy.basePriority;

        period = copy.period;
        deadline = copy.period;
        utilization = copy.utilization;
        staticTaskId = copy.staticTaskId;
        totalNeededTime = copy.totalNeededTime;
        resourceAccessTime = copy.resourceAccessTime;
        accessResourceIndex = copy.accessResourceIndex;
        baseRunningCpuCore = copy.baseRunningCpuCore;
        criticality = copy.criticality;
        WCCT_low = copy.WCCT_low;
        WCCT_high = copy.WCCT_high;

        /* Initialize the procedure control block. */
        dynamicTaskId = -1;
        spin = false;
        elapsedTime = 0;
        executedTime = 0;
        computeAndSpinTime = 0;
        isAccessGlobalResource = false;
        isAccessLocalResource = false;
        remainResourceComputationTime = 0;
        requestResourceTh = 0;
        systemCriticalityWhenAccessResource = 0;
    }
}
