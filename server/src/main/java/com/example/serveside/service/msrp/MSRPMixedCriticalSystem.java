package com.example.serveside.service.msrp;

import com.example.serveside.response.EventTimePoint;
import com.example.serveside.response.TaskInformation;
import com.example.serveside.service.CommonUse.BasicPCB;
import com.example.serveside.service.CommonUse.BasicResource;

import java.util.*;

/**
 * {@code MSRPixedCriticalSystem}用以模拟任务在Multiprocessor Stack Resource Protocol(MSRP)资源共享协议下的执行并记录任务全周期执行情况。
 *
 * <p>
 * {@code MSRPMixedCriticalSystem} 主要由以下三个函数构成：
 * <ul>
 *   <li>{@link #MSRPInitialize(ArrayList, ArrayList, int, TreeMap, Boolean, Integer) MSRPInitialize}</li>
 *   <li>{@link #SystemExecute() SystemExecute}</li>
 *   <li>{@link #PackageTotalInformation() PackageTotalInformation}</li>
 * </ul>
 * 其余函数均用来辅助完成这三个函数的功能。
 * 函数{@code MSRPInitialize}初始化系统环境配置，例如任务信息、资源信息、任务发布时间等。
 * 函数{@code SystemExecute}模拟任务在PWLP资源共享协议下的执行。
 * 函数{@code PackageTotalInformation}打包整理任务在MSRP资源共享协议下的全周期执行情况并将相关信息传递给调用函数。
 * @author 陈炜琰
 * @author 李伊鹏
 * @author 许自泓
 * @author 介琛
 * */
public class MSRPMixedCriticalSystem {
    /**
     * 表示系统当前所处的关键级。
     * <p>
     * 该属性是一个整数，初始值为 0，用于表示系统当前所处的关键级。
     * 当 {@code criticality_indicator} 为 0 时，系统处于低关键级。
     * 当 {@code criticality_indicator} 为 1 时，系统处于高关键级。
     */
    public static int criticality_indicator = 0;

    /**
     * 表示系统的处理器个数
     * <p>
     * 该属性用于指定系统的处理器数量。在当前配置中，系统有{@code TOTAL_CPU_CORE_NUM}个处理器核心。
     * */
    public static int TOTAL_CPU_CORE_NUM = 2;

    /**
     * 表示系统的全局时钟。
     * <p>
     * 该属性是一个整数 初始值为0，用于存储系统当前的全局时钟值。
     * */
    public static int systemClock;

    /**
     * 表示系统已发布的任务数量。
     * <p>
     * 该属性是一个整数 初始值为0，用于跟踪系统已经发布的任务数量。当新任务被成功发布时，此计数将递增。
     * */
    public static int releaseTaskNum;

    /**
     * 存储系统中所有任务的信息。
     * <p>
     * 该属性是一个列表，用于存储系统中所有任务的信息。每个任务信息被封装为 {@code ProcedureControlBlock} 对象。
     * 每种任务对应列表中的一项。
     */
    public static ArrayList<com.example.serveside.service.msrp.ProcedureControlBlock>  totalTasks;

    /**
     * 存储系统中所有资源的信息。
     * <p>
     * 该属性是一个列表，用于存储系统中所有资源的信息。每个资源信息被封装为 {@code Resource} 对象。
     */
    public static ArrayList<com.example.serveside.service.msrp.Resource> totalResources;

    /**
     * 记录每种任务已经完成执行的次数。
     * <p>
     * 该属性是一个数组，用于记录系统中每种类型任务已经完成执行的次数。索引值与任务的静态标识符 {@code ProcedureControlBlock.basicPCB.staticTaskId} 相对应，对应的值表示该类型任务已经完成的次数。
     */
    public static int[] taskFinishTimes;

    /**
     * 记录每个处理器核心上正在执行的任务。
     * <p>
     * 该属性是一个列表，用于记录每个处理器核心当前正在执行的任务。
     * 每个任务被封装为 {@code ProcedureControlBlock} 对象。
     */
    public static ArrayList<com.example.serveside.service.msrp.ProcedureControlBlock> runningTaskPerCore;

    /**
     * 记录每个处理器核心正在等待执行的任务。
     * <p>
     * 该属性是一个列表的列表，用于记录每个处理器核心当前正在等待执行的任务。
     * 每个处理器核心对应一个子列表，子列表中的每个元素是一个被封装为 {@code ProcedureControlBlock} 对象的任务。
     * */
    public static ArrayList<ArrayList<com.example.serveside.service.msrp.ProcedureControlBlock>> waitingTasksPerCore;

    /**
     * 记录任务在执行过程中触发的事件以及事件发生的时间点。
     * <p>
     * 该属性是一个列表的列表，用于记录系统中每个任务在执行过程中触发的事件及其发生的时间点。
     * 每个任务对应一个子列表，子列表中的每个元素是一个被封装为 {@code EventTimePoint} 对象的事件记录。
     * 可能的事件包括任务发布、请求资源、释放资源等。
     */
    public static ArrayList<ArrayList<com.example.serveside.response.EventTimePoint>> TaskEventTimePointsRecords;

    /**
     * 记录任务在执行过程中的状态。
     * <p>
     * 该属性是一个列表的列表，用于记录系统中每个任务在执行过程中的状态信息。
     * 每个任务对应一个子列表，子列表中的每个元素是一个被封装为 {@code EventInformation} 对象的状态记录。
     * 可能的状态包括到达阻塞、正常执行、使用资源等。
     * </p>
     */
    public static ArrayList<ArrayList<com.example.serveside.response.EventInformation>> TaskEventInformationRecords;

    /**
     * 记录每个处理器核心当前正在执行的任务的状态。
     * <p>
     * 该属性是一个列表的列表，用于记录系统中每个处理器核心当前正在执行的任务的状态信息。
     * 每个处理器核心对应一个子列表，子列表中的每个元素是一个被封装为 {@code EventInformation} 对象的状态记录。
     */
    public static ArrayList<ArrayList<com.example.serveside.response.EventInformation>> cpuEventInformationRecords;

    /**
     * 记录每个处理器核心当前正在执行的任务触发的事件以及事件发生的时间点。
     * <p>
     * 该属性是一个列表的列表，用于记录系统中每个处理器核心当前正在执行的任务触发的事件及其发生的时间点。
     * 每个处理器核心对应一个子列表，子列表中的每个元素是一个被封装为 {@code EventTimePoint} 对象的事件记录。
     */
    public static ArrayList<ArrayList<com.example.serveside.response.EventTimePoint>> cpuEventTimePointRecords;

    /**
     * 表示前端甘特图的长度，即系统模拟运行的时长。
     * <p>
     * 该属性是一个整数，表示前端甘特图的长度，即系统模拟运行的时长。
     */
    public static Integer timeAxisLength;

    /**
     * 记录每个已发布的任务的信息。
     * <p>
     * 该属性是一个列表，用于记录系统中每个已发布的任务的信息。
     * 同一种类型的任务可能多次发布。
     * 每个已发布任务信息被封装为 {@code TaskInformation} 对象。
     * </p>
     */
    public static List<TaskInformation> releaseTaskInformations;

    /**
     * 系统模拟运行过程中是否开启关键级切换。
     * <p>
     * 该属性是一个布尔值，用于表示系统模拟运行过程中是否开启关键级切换。
     * 当 {@code isStartUpSwitch} 为 true 时，表示系统开启关键级切换。
     * 当 {@code isStartUpSwitch} 为 false 时，表示系统关闭关键级切换。
     */
    public static Boolean isStartUpSwitch;

    /**
     * 系统在开启关键级切换时是否自动完成关键级切换。
     * <p>
     * 该属性是一个布尔值，用于表示系统在开启关键级切换时是否自动完成关键级切换。
     * 该属性在 {@code isStartUpSwitch} 为 true 时有效。
     * 当 {@code isAutomaticallySwitch} 为 true 时，表示系统自动完成关键级切换。
     * 当 {@code isAutomaticallySwitch} 为 false 时，表示系统在 {@code criticalitySwitchTime} 时间点完成关键级切换。
     */
    public static Boolean isAutomaticallySwitch;

    /**
     * 系统完成关键级切换的时间点。
     * <p>
     * 该属性是一个整数，用于表示系统完成关键级切换的时间点。
     * 当 {@code criticalitySwitchTime} 为 -1 时，表示系统没有发生关键级切换。
     * 当 {@code criticalitySwitchTime} 大于 0 时，表示系统在 {@code criticalitySwitchTime} 发生关键级切换。
     */
    public static Integer criticalitySwitchTime;

    /**
     * 表示系统中的任务在运行过程中能否在截止日期之前完成执行，即是否可调度。
     * 初始值为 true。
     * 当 {@code isSchedulable} 为 true 时，表示系统可调度，即所有任务都能够在截止时间之前完成执行。
     * 当 {@code isSchedulable} 为 false 时，表示系统不可调度，即存在任务的执行超过了对应的截止时间。
     */
    public static Boolean isSchedulable;

    /**
     * 存储每个时间点发布的任务的映射关系。
     * <p>
     * 使用 {@code TreeMap} 来确保任务按照时间点的升序存储。
     * 键表示发布时间点，值是包含在该时间点发布的所有任务组成的 {@code ArrayList<Integer>}。
     */
    public static TreeMap<Integer, ArrayList<Integer>> taskReleaseTimes;

    /**
     * 用于遍历 {@code taskReleaseTimes} 哈希表的迭代器。
     */
    public static Iterator<Map.Entry<Integer, ArrayList<Integer>>> iteratorTaskReleaseTimes;

    /**
     * 下一个释放任务的时间点及相应的任务列表。
     */
    public static Map.Entry<Integer, ArrayList<Integer>> itemTaskReleaseTimes = null;

    /**
     * 函数{@code MSRPInitialize()}初始化系统环境（例如任务信息、资源信息、任务发布时间等）、系统模拟运行过程使用到的属性（例如系统时钟、任务已经完成的次数、用于记录状态的列表等）。
     * @param _totalTasks          包含所有任务信息的 {@code ArrayList<BasicPCB>}。
     * @param _totalResources      包含所有资源信息的 {@code ArrayList<BasicResource>}。
     * @param _totalCpuCoreNum     总的处理器核心数量。
     * @param _taskReleaseTimes    任务发布时间的映射关系，使用 {@code TreeMap} 以确保按照时间点的升序存储。
     * @param _isStartUpSwitch     系统启动关键级切换开关。
     * @param _criticalitySwitchTime 系统关键级切换时间点。
     * */
    public static void MSRPInitialize(ArrayList<BasicPCB> _totalTasks, ArrayList<BasicResource> _totalResources, int _totalCpuCoreNum,
                                      TreeMap<Integer, ArrayList<Integer>> _taskReleaseTimes, Boolean _isStartUpSwitch, Integer _criticalitySwitchTime)
    {
        // 1. 初始化系统环境配置信息
        totalTasks = new ArrayList<>(_totalTasks.size());
        for (BasicPCB task : _totalTasks)
            totalTasks.add(new ProcedureControlBlock(task));

        totalResources = new ArrayList<>(_totalResources.size());
        for (BasicResource resource : _totalResources)
            totalResources.add(new Resource(resource));

        TOTAL_CPU_CORE_NUM = _totalCpuCoreNum;

        taskReleaseTimes = _taskReleaseTimes;

        iteratorTaskReleaseTimes = _taskReleaseTimes.entrySet().iterator();
        if (iteratorTaskReleaseTimes.hasNext())
            itemTaskReleaseTimes = iteratorTaskReleaseTimes.next();

        isStartUpSwitch = _isStartUpSwitch;
        isAutomaticallySwitch = (_criticalitySwitchTime == -1);
        criticalitySwitchTime = _criticalitySwitchTime;

        // 2. 初始化系统记录信息
        isSchedulable = true;

        // 时钟周期
        systemClock = 0;

        // 目前已经发布的任务个数
        releaseTaskNum = 0;

        // 初始化系统的关键级
        criticality_indicator = 0;

        // 每一种任务执行完的次数
        taskFinishTimes = new int[totalTasks.size()];

        // CPU 上正在执行哪些任务
        runningTaskPerCore = new ArrayList<>(TOTAL_CPU_CORE_NUM);
        for (int i = 0; i < TOTAL_CPU_CORE_NUM; ++i)
            runningTaskPerCore.add(null);

        // 每个 CPU 上正在等待的任务
        waitingTasksPerCore = new ArrayList<>();
        for (int i = 0; i < TOTAL_CPU_CORE_NUM; ++i)
            waitingTasksPerCore.add(new ArrayList<>());

        // 记录每一个任务在运行过程中发生的一些事件点
        TaskEventTimePointsRecords = new ArrayList<>();

        // 记录每一个任务的运行过程
        TaskEventInformationRecords = new ArrayList<>();

        // 记录每一个发布的任务的信息
        releaseTaskInformations = new ArrayList<>();

        // 记录 cpu 在运行过程中的状态
        cpuEventInformationRecords = new ArrayList<>();
        for (int i = 0; i < TOTAL_CPU_CORE_NUM; ++i)
            cpuEventInformationRecords.add(new ArrayList<>());

        // 记录 cpu 在运行过程发生的事件的时间点
        cpuEventTimePointRecords = new ArrayList<>();
        for (int i = 0; i < TOTAL_CPU_CORE_NUM; ++i)
            cpuEventTimePointRecords.add(new ArrayList<>());
    }


    /**
     * 该方法 {@code CalculateTimeAxisLength} 根据模拟运行过程中收集到的信息计算出前端甘特图的长度。
     * */
    public static void CalculateTimeAxisLength()
    {
        timeAxisLength = 0;

        if (!TaskEventInformationRecords.isEmpty()) {
            for (ArrayList<com.example.serveside.response.EventInformation> TaskEventInformations : TaskEventInformationRecords)
            {
                if (!TaskEventInformations.isEmpty())
                    timeAxisLength = Math.max(timeAxisLength, TaskEventInformations.get(TaskEventInformations.size() - 1).getEndTime());
            }
        }
    }

    /**
     * 该方法 {@code PackageTotalInformation} 通过整合 {@code cpuEventInformationRecords}、{@code cpuEventTimePointRecords}、
     * {@code TaskEventInformationRecords}、{@code TaskEventTimePointsRecords} 记录的信息，将其打包成 {@code TotalInformation} 对象，以便传递给前端。
     */
    public static com.example.serveside.response.ToTalInformation PackageTotalInformation()
    {
        // 计算时间轴长度
        CalculateTimeAxisLength();

        // cpu gantt chart information cpu 甘特图信息 :
        List<com.example.serveside.response.GanttInformation> cpuGanttInformations = new ArrayList<>();
        for (int i = 0; i < TOTAL_CPU_CORE_NUM; ++i)
        {
            cpuGanttInformations.add(new com.example.serveside.response.GanttInformation(cpuEventInformationRecords.get(i), cpuEventTimePointRecords.get(i), timeAxisLength));
        }

        // task gantt chart information: 任务甘特图信息：
        List<com.example.serveside.response.TaskGanttInformation> taskGanttInformations = new ArrayList<>();
        for (com.example.serveside.response.TaskInformation releaseTaskInformation : releaseTaskInformations)
        {
            com.example.serveside.response.GanttInformation ganttInformation = new com.example.serveside.response.GanttInformation(TaskEventInformationRecords.get(releaseTaskInformation.getDynamicPid()), TaskEventTimePointsRecords.get(releaseTaskInformation.getDynamicPid()), timeAxisLength);

            int baseRunningCPUCore = -1;
            for (com.example.serveside.service.msrp.ProcedureControlBlock staticTask : totalTasks)
                if (releaseTaskInformation.getStaticPid() == staticTask.basicPCB.staticTaskId)
                {
                    baseRunningCPUCore = staticTask.basicPCB.baseRunningCpuCore;
                    break;
                }

            taskGanttInformations.add(new com.example.serveside.response.TaskGanttInformation(releaseTaskInformation.getStaticPid(), releaseTaskInformation.getDynamicPid(), baseRunningCPUCore, ganttInformation));
        }

        return new com.example.serveside.response.ToTalInformation(cpuGanttInformations, taskGanttInformations, criticalitySwitchTime);
    }

    /**
     * 模拟器根据函数 {@code MSRPInitialize()} 初始化后的系统环境、任务信息、资源信息等来模拟任务的执行，并记录任务的执行情况。
     * 模拟器在函数 {@code SystemSuspend()} 返回为 true 时结束模拟，即所有类型的任务都执行完一次或者某个任务执行超过截止日期。
     * */
    public static void SystemExecute()
    {
        while (!systemSuspend(taskFinishTimes))
            ExecuteOneClock();

        // 终止所有当前所有运行在 cpu 上的任务的状态
        for (int i = 0; i < TOTAL_CPU_CORE_NUM; ++i)
        {
            // 1. 终止 cpu 上的状态
            ChangeCpuTaskState(i, null, "");
            // 2. 终止 running 的状态
            if (runningTaskPerCore.get(i) != null)
            {
                ArrayList<com.example.serveside.response.EventInformation> taskEventInformation = TaskEventInformationRecords.get(runningTaskPerCore.get(i).basicPCB.dynamicTaskId);
                taskEventInformation.get(taskEventInformation.size() - 1).setEndTime(systemClock);
            }
        }

        // 终止所有没有运行在 cpu 上的任务的状态
        for (ArrayList<com.example.serveside.service.msrp.ProcedureControlBlock> waitingTasks : waitingTasksPerCore)
        {
            // 1. 终止 waiting Task 的状态
            for (com.example.serveside.service.msrp.ProcedureControlBlock waitingTask : waitingTasks) {
                ArrayList<com.example.serveside.response.EventInformation> taskEventInformation = TaskEventInformationRecords.get(waitingTask.basicPCB.dynamicTaskId);
                if (!taskEventInformation.isEmpty())
                    taskEventInformation.get(taskEventInformation.size() - 1).setEndTime(systemClock);
            }
        }
    }

    /**
     * 函数{@code SystemSuspend()}用以表示模拟器是否继续模拟任务的执行。
     * @return {@code true} 当模拟器在模拟任务的执行过程中出现所有的任务都完成一次或者某个任务执行超过截止时间时，模拟器终止模拟。<strong>注意：</strong>返回值为 {@code true} 表示模拟器终止模拟。<br>
     *         {@code false} 当模拟器在模拟任务的执行过程中没有出现上述两种情况时，模拟器继续模拟。<strong>注意：</strong>返回值为 {@code false} 表示模拟器继续模拟。
     * */
    public static boolean systemSuspend(int[] taskFinishTimes)
    {
        if (!isSchedulable)
            return true;

        for (int finishTimes : taskFinishTimes)
            if (finishTimes == 0)
                return false;

        return true;
    }

    /**
     * 函数{@code ExecuteOneClock()}用以执行模拟器在一个时钟周期内需要完成的操作。
     * <p>
     * 该方法 {@code ExecuteOneClock()} 包含以下步骤：
     * <ol>
     *   <li>执行 {@link #ExecuteTasks()} 处理占用处理器核心的任务请求资源。</li>
     *   <li>执行 {@link #IncreaseElapsedTime()} 增加所有已发布但未执行完成的任务的各种类型时间。</li>
     *   <li>执行 {@link #ReleaseTasks()} 发布新的任务。</li>
     *   <li>执行 {@link #ChooseTaskToRun()} 对每个处理器进行重新调度，即把任务调度到对应处理器核心上执行。</li>
     * </ol>
     */
    public static void ExecuteOneClock()
    {
        ExecuteTasks();

        IncreaseElapsedTime();

        ReleaseTasks();

        ChooseTaskToRun();
    }

    /**
     * 函数{@code ExecuteTasks()}用以处理在当前系统时钟下占据处理器核心的任务请求资源的情况。
     * <p>
     * 如果任务请求的资源正在被其他任务持有，那么任务加入到请求资源的FIFO队列末尾中，任务的执行状态变成直接自旋（direct-spinning）并且不可被抢占。
     * 除此之外，其余在该处理器上等待执行的任务其状态由阻塞（blocked）变成间接自旋（indirect-spinning）。
     * 注意，在MSRP协议下，自旋等待全局资源的任务不可被抢占，会给其他任务带来 arrival-blocking，在 {@code ChooseTaskToRun}时需要进行特殊处理。
     * </p>
     * <p>
     * 如果任务请求的资源空闲，那么任务成功持有资源。如果访问的资源是局部资源，那么任务在持有资源期间优先级就会短暂提升到该资源对应的资源上限优先级（resource ceiling priority），
     * 在释放资源后恢复到原先的优先级。
     * </p>
     * */
    public static void ExecuteTasks()
    {
        for (ProcedureControlBlock runningTask : runningTaskPerCore)
        {
            // 1. 处理器核心空闲，不做任何处理
            if (runningTask == null)
                continue;

            // 2. 任务不需要请求资源
            if(runningTask.basicPCB.accessResourceIndex.isEmpty())
                continue;

            // 3. 任务正在持有资源
            if(runningTask.basicPCB.isAccessGlobalResource || runningTask.basicPCB.isAccessLocalResource)
                continue;

            // 4. 任务已经请求完所有资源 || 任务正在自旋等待资源
            if (runningTask.basicPCB.requestResourceTh >= runningTask.basicPCB.accessResourceIndex.size() || runningTask.basicPCB.spin)
                continue;

            // 任务到申请资源的时间点
            if (runningTask.basicPCB.executedTime == runningTask.basicPCB.resourceAccessTime.get(runningTask.basicPCB.requestResourceTh)) {
                com.example.serveside.service.msrp.Resource accquireResource = totalResources.get(runningTask.basicPCB.accessResourceIndex.get(runningTask.basicPCB.requestResourceTh));

                // accquireResource 被占据(说明是全局资源)
                if (accquireResource.basicResource.isOccupied) {
                    // 设置 runningTask 的状态：spin, 优先级短暂提升
                    runningTask.basicPCB.spin = true;
                    runningTask.basicPCB.priorities.push(accquireResource.basicResource.ceiling.get(runningTask.basicPCB.baseRunningCpuCore));

                    // 任务加入到资源的等待队列当中
                    accquireResource.waitingQueue.add(runningTask);

                    // 任务新增状态：direct-spinning
                    ChangeTaskState(runningTask, "direct-spinning");
                    // 任务新增：事件时间点
                    TaskEventTimePointsRecords.get(runningTask.basicPCB.dynamicTaskId).add(new com.example.serveside.response.EventTimePoint(runningTask.basicPCB.staticTaskId, runningTask.basicPCB.dynamicTaskId, "lock-attempt", systemClock, accquireResource.basicResource.id));

                    // cpu 任务状态发生变化：execution --> direct-spinning
                    ChangeCpuTaskState(runningTask.basicPCB.baseRunningCpuCore, runningTask, "direct-spinning");
                    // cpu 新增事件发生时间点：lock-attempt
                    cpuEventTimePointRecords.get(runningTask.basicPCB.baseRunningCpuCore).add(new com.example.serveside.response.EventTimePoint(runningTask.basicPCB.staticTaskId, runningTask.basicPCB.dynamicTaskId, "lock-attempt", systemClock, accquireResource.basicResource.id));

                    // 处理 indirect spinning 的情况：高优先级任务抢占低优先级的任务，然后高优先级的任务申请访问资源时进行自旋
                    ArrayList<com.example.serveside.service.msrp.ProcedureControlBlock> waitingTasks = waitingTasksPerCore.get(runningTask.basicPCB.baseRunningCpuCore);
                    for (com.example.serveside.service.msrp.ProcedureControlBlock waitingTask : waitingTasks)
                    {
                        if (waitingTask.basicPCB.basePriority < runningTask.basicPCB.basePriority)
                        {
                            ChangeTaskState(waitingTask, "indirect-spinning");
                        }
                    }
                }
                // accquireResource 没有被占据，空闲的
                else {
                    // accquireResource 访问的是全局资源还是局部资源（访问资源的类型）
                    runningTask.basicPCB.isAccessGlobalResource = accquireResource.basicResource.isGlobal;
                    runningTask.basicPCB.isAccessLocalResource = !runningTask.basicPCB.isAccessGlobalResource;

                    // 设置资源访问时间、访问资源时的系统关键级、优先级
                    runningTask.basicPCB.remainResourceComputationTime = criticality_indicator == 0 ? accquireResource.basicResource.c_low : accquireResource.basicResource.c_high;
                    runningTask.basicPCB.systemCriticalityWhenAccessResource = criticality_indicator;
                    runningTask.basicPCB.priorities.push(accquireResource.basicResource.ceiling.get(runningTask.basicPCB.baseRunningCpuCore));

                    // 设置资源现在被占据
                    accquireResource.basicResource.isOccupied = true;

                    // 任务新增状态：access-resource
                    ChangeTaskState(runningTask, "access-resource");
                    // 任务新增：事件时间点
                    TaskEventTimePointsRecords.get(runningTask.basicPCB.dynamicTaskId).add(new com.example.serveside.response.EventTimePoint(runningTask.basicPCB.staticTaskId, runningTask.basicPCB.dynamicTaskId, "locked", systemClock, accquireResource.basicResource.id));

                    // cpu 上运行的任务状态发生变化：normal-execution --> access-resource
                    ChangeCpuTaskState(runningTask.basicPCB.baseRunningCpuCore, runningTask, "access-resource");
                    // cpu 新增事件发生时间点：locked
                    cpuEventTimePointRecords.get(runningTask.basicPCB.baseRunningCpuCore).add(new com.example.serveside.response.EventTimePoint(runningTask.basicPCB.staticTaskId, runningTask.basicPCB.dynamicTaskId, "locked", systemClock, accquireResource.basicResource.id));

                }
            }

        }
    }

    /**
     * 该方法 {@code IncreaseElapsedTime} 用于增加系统中所有已发布但尚未完成的任务的各种时间，处理使用完资源的任务、执行完成的任务，以及启动关键级别切换。
     * 方法执行顺序如下：
     *
     * <ol>
     *     <li>
     *         增加未完成执行的任务的经过时间：
     *         <ul>
     *             <li>对于在处理器核心上执行的任务：
     *                 <ul>
     *                     <li>增加任务自发布以来的经过时间。</li>
     *                     <li>对于非自旋等待资源的任务，增加计算或资源使用时间。</li>
     *                 </ul>
     *             </li>
     *             <li>对于等待执行的任务，增加任务自发布以来的经过时间。</li>
     *         </ul>
     *     </li>
     *     <li>
     *         处理释放资源的任务：
     *         <ul>
     *             <li>调用 {@code ReleaseResource} 函数释放资源并将释放的资源分配给FIFO请求队列头部的任务。</li>
     *         </ul>
     *     </li>
     *     <li>
     *         处理完成执行的任务：
     *         <ul>
     *             <li>标记处理器核心为空闲。</li>
     *             <li>增加任务完成次数。</li>
     *         </ul>
     *     </li>
     *     <li>
     *         执行关键级切换：
     *         <ul>
     *             <li>如果某个任务的执行时长超过 WCCT_low，触发关键级切换，将系统关键级提升为高关键级。</li>
     *         </ul>
     *     </li>
     *     <li>
     *         在高关键级模式下终止低关键级任务的执行：
     *         <ul>
     *             <li>取消低优先级任务的执行，增加其完成次数。</li>
     *              <li>如果低优先级任务当前持有资源，在延迟取消执行直到其释放资源。</li>
     *         </ul>
     *     </li>
     * </ol>
     */
    public static void IncreaseElapsedTime()
    {
        ++systemClock;

        // 1. 处理运行在 cpu 上的任务
        for (com.example.serveside.service.msrp.ProcedureControlBlock runningTask : runningTaskPerCore) {
            if (runningTask == null)
                continue;

            ++runningTask.basicPCB.elapsedTime;

            // 任务正在访问资源（资源使用还剩多少时间）
            if (runningTask.basicPCB.isAccessGlobalResource || runningTask.basicPCB.isAccessLocalResource)
                --runningTask.basicPCB.remainResourceComputationTime;

            // 增加 computeAndSpinTime（判断有没有超 WCCT_Low）
            // computeAndSpinTIme: pure computation + spin
            if (!runningTask.basicPCB.isAccessGlobalResource && !runningTask.basicPCB.isAccessLocalResource)
                ++runningTask.basicPCB.computeAndSpinTime;

            // 增加 executedTime（用于判断什么时候访问资源）
            if (!runningTask.basicPCB.spin)
                ++runningTask.basicPCB.executedTime;

            // 任务的 elapsedTime 超过 DeadLine 的, 这样子的话就没有必要进行调度了
            if (runningTask.basicPCB.elapsedTime > runningTask.basicPCB.deadline)
                isSchedulable = false;
        }

        // 2. 处理在等待队列（waitingTasksPerCore 上的任务）
        for (ArrayList<com.example.serveside.service.msrp.ProcedureControlBlock> waitingTasks : waitingTasksPerCore)
        {
            for (com.example.serveside.service.msrp.ProcedureControlBlock waitingTask : waitingTasks)
            {
                ++waitingTask.basicPCB.elapsedTime;

                // 任务的 elapsedTime 超过 DeadLine 的, 这样子的话就没有必要进行调度了
                if (waitingTask.basicPCB.elapsedTime > waitingTask.basicPCB.deadline)
                    isSchedulable = false;
            }
        }

        // 3. 处理使用完资源的任务
        for (com.example.serveside.service.msrp.ProcedureControlBlock runningTask : runningTaskPerCore)
            // 资源使用完，释放资源
            if (runningTask != null && (runningTask.basicPCB.isAccessLocalResource || runningTask.basicPCB.isAccessGlobalResource) && runningTask.basicPCB.remainResourceComputationTime == 0)
                ReleaseResource(runningTask);

        // 4. 处理在 baseRunningCpuCore 上完成计算的的任务
        for (com.example.serveside.service.msrp.ProcedureControlBlock runningTask : runningTaskPerCore)
            // 需要注意：此时 task 不能够访问资源（在高关键级下面，executedTime 可能会超过 totalNeededTime）
            if (runningTask != null && !runningTask.basicPCB.isAccessLocalResource && !runningTask.basicPCB.isAccessGlobalResource && runningTask.basicPCB.executedTime == runningTask.basicPCB.totalNeededTime)
            {
                runningTaskPerCore.set(runningTask.basicPCB.baseRunningCpuCore, null);
                ++taskFinishTimes[runningTask.basicPCB.staticTaskId];

                // 任务新增：事件时间点（完成任务）
                ChangeTaskState(runningTask, "completion");
                TaskEventTimePointsRecords.get(runningTask.basicPCB.dynamicTaskId).add(new com.example.serveside.response.EventTimePoint(runningTask.basicPCB.staticTaskId, runningTask.basicPCB.dynamicTaskId, "completion", systemClock, -1));

                // cpu 上的任务完成(状态发生变化)：normal-execution --> completion
                cpuEventTimePointRecords.get(runningTask.basicPCB.baseRunningCpuCore).add(new com.example.serveside.response.EventTimePoint(runningTask.basicPCB.staticTaskId, runningTask.basicPCB.dynamicTaskId, "completion", systemClock, -1));
                ChangeCpuTaskState(runningTask.basicPCB.baseRunningCpuCore, null, "");
            }

        // 5. 最后在完成关键级的切换
        if (isStartUpSwitch) {
            // 6. 判断 runningTaskPerCore 上的 task 和 waitingTask 上的 helpTask 的 computeAndSpin 有没有超过 WCCT_low
            if (criticality_indicator != 1 && isAutomaticallySwitch)
            {
                // 在 cpu 上运行的任务/running task
                for (com.example.serveside.service.msrp.ProcedureControlBlock runningTask : runningTaskPerCore)
                {
                    if (runningTask != null && runningTask.basicPCB.computeAndSpinTime > runningTask.basicPCB.WCCT_low)
                    {
                        criticalitySwitchTime = systemClock;
                        System.out.printf("Static Task Id : %d (Dynamic Task Id : %d) cause Criticality Switch at System Clock : %d\n", runningTask.basicPCB.staticTaskId, runningTask.basicPCB.dynamicTaskId, systemClock);
                        criticality_indicator = 1;
                        break;
                    }
                }
            }
        }
            // 手动设置某一个时间点发生关键级切换
        if (isStartUpSwitch && !isAutomaticallySwitch && systemClock == criticalitySwitchTime) {
            criticality_indicator = 1;
        }

            // 终止低关键级任务的执行
        if (criticality_indicator == 1)
        {
            // 1. 处理在 cpu 上运行的低关键级任务
            for (int i = 0; i < runningTaskPerCore.size(); ++i)
            {
                com.example.serveside.service.msrp.ProcedureControlBlock runningTask = runningTaskPerCore.get(i);

                if (runningTask == null)
                    continue;

                // 高关键级下不需要被终止的任务
                // 1. 高关键级任务
                if (runningTask.basicPCB.criticality >= criticality_indicator)
                    continue;

                // 2. 任务正在访问资源
                if (runningTask.basicPCB.isAccessGlobalResource || runningTask.basicPCB.isAccessLocalResource)
                    continue;

                // 不满足前面两个条件的进程可以被终止(killed)
                // 1. 低关键级的任务 && 2. (任务 normal-execution || 任务 spin)

                // cpu 核上运行的任务变为空
                runningTaskPerCore.set(i, null);

                // cpu 甘特图上终止 runningTask 的运行, 并显示 shut-down 符号
                ChangeCpuTaskState(i, null, "");
                cpuEventTimePointRecords.get(i).add(new EventTimePoint(runningTask.basicPCB.staticTaskId, runningTask.basicPCB.dynamicTaskId, "killed", systemClock, -1));

                // task 甘特图上终止运行，并显示 shut-down 符号
                ChangeTaskState(runningTask, "killed");
                TaskEventTimePointsRecords.get(runningTask.basicPCB.dynamicTaskId).add(new EventTimePoint(runningTask.basicPCB.staticTaskId, runningTask.basicPCB.dynamicTaskId, "killed", systemClock, -1));

                // task 标记为已经执行完一次
                ++taskFinishTimes[runningTask.basicPCB.staticTaskId];

                // 如果任务在资源的等待队列当中，将其移除
                if (runningTask.basicPCB.spin)
                {
                    com.example.serveside.service.msrp.Resource accquireResource = totalResources.get(runningTask.basicPCB.accessResourceIndex.get(runningTask.basicPCB.requestResourceTh));
                    accquireResource.waitingQueue.remove(runningTask);

                    // 终止该处理器上其他任务的indirect-spinning
                    for (com.example.serveside.service.msrp.ProcedureControlBlock _waitingTask : waitingTasksPerCore.get(i))
                    {
                        ArrayList<com.example.serveside.response.EventInformation> _taskInformations = TaskEventInformationRecords.get(_waitingTask.basicPCB.dynamicTaskId);
                        if (!_taskInformations.isEmpty() && _taskInformations.get(_taskInformations.size() - 1).getState().equals("indirect-spinning"))
                        {
                            // 由 indirect-spinning-delay 变成 blocked
                            ChangeTaskState(_waitingTask, "blocked");
                        }
                    }

                    // 处理该处理器上其他任务的 arrival-blocking
                    for (com.example.serveside.service.msrp.ProcedureControlBlock _waitingTask : waitingTasksPerCore.get(i))
                    {
                        ArrayList<com.example.serveside.response.EventInformation> _taskInformations = TaskEventInformationRecords.get(_waitingTask.basicPCB.dynamicTaskId);
                        if (!_taskInformations.isEmpty() && _taskInformations.get(_taskInformations.size() - 1).getState().equals("arrival-blocking"))
                        {
                            // 由 indirect-spinning-delay 变成 blocked
                            ChangeTaskState(_waitingTask, "blocked");
                        }
                    }
                }
            }

            // 2. 处理在 cpu 等待队列上的任务
            for (ArrayList<com.example.serveside.service.msrp.ProcedureControlBlock> waitingTasks : waitingTasksPerCore)
            {
                Iterator<com.example.serveside.service.msrp.ProcedureControlBlock> waitingTasksIterator = waitingTasks.iterator();
                while (waitingTasksIterator.hasNext())
                {
                    com.example.serveside.service.msrp.ProcedureControlBlock waitingTask = waitingTasksIterator.next();

                    // 终止任务需要满足的条件
                    // 1. 低关键级任务
                    // 2. 任务没有访问资源
                    if (waitingTask.basicPCB.criticality < criticality_indicator && !waitingTask.basicPCB.isAccessLocalResource && !waitingTask.basicPCB.isAccessGlobalResource)
                    {
                        // 将 waitingTask 从等待列表中移除
                        waitingTasksIterator.remove();

                        // 任务甘特图上显示终止/shut-down
                        ChangeTaskState(waitingTask, "killed");
                        TaskEventTimePointsRecords.get(waitingTask.basicPCB.dynamicTaskId).add(new EventTimePoint(waitingTask.basicPCB.staticTaskId, waitingTask.basicPCB.dynamicTaskId, "killed", systemClock, -1));

                        // task 标记为已经执行完一次
                        ++taskFinishTimes[waitingTask.basicPCB.staticTaskId];
                    }
                }
            }
        }
    }

    /**
     * 该方法 {@code ReleaseResource} 用于释放 {@code runningTask} 所持有的资源，并将释放的资源分配给 FIFO 请求队列头部的任务。
     * <p>
     *     方法执行顺序如下：
     *      <ol>
     *          <li>
     *              结束同一处理器上等待任务到达的阻塞（arrival-blocking）状态。
     *              任务持有局部资源期间，优先级会短暂提升到资源上限优先级（resource ceiling priority）；
     *              任务持有全局资源期间不可被抢占。
     *              上述两种情况均可能导致高优先级任务遭受到达阻塞。
     *          </li>
     *          <li>
     *              {@code runningTask} 释放资源。
     *              修改 {@code runningTask} 实例中的属性，表明该任务不再持有资源。
     *          </li>
     *          <li>
     *              将刚释放的资源分配给 FIFO 请求队列头部的任务。
     *          </li>
     *      </ol>
     * </p>
     *
     * @param runningTask 释放资源的任务
     */
    public static void ReleaseResource(ProcedureControlBlock runningTask) {
        // 要释放的资源
        com.example.serveside.service.msrp.Resource releaseResource = totalResources.get(runningTask.basicPCB.accessResourceIndex.get(runningTask.basicPCB.requestResourceTh));

        // runningTask 在释放资源之前先结束该 cpu 上某些任务的 arrival-blocking
        ArrayList<com.example.serveside.service.msrp.ProcedureControlBlock> waitingTasks = waitingTasksPerCore.get(runningTask.basicPCB.baseRunningCpuCore);
        for (com.example.serveside.service.msrp.ProcedureControlBlock waitingTask : waitingTasks)
        {
            // 判断 Arrival Blocking 发生的情况
            // 1. runningTask 访问 local-resource 造成优先级反转
            // 2. runningTask 访问 global-resource && runningTask 的优先级更低 （访问全局资源时不可被抢占）
            if (waitingTask.basicPCB.executedTime == 0 && runningTask.basicPCB.basePriority < waitingTask.basicPCB.basePriority)
            {
                    ChangeTaskState(waitingTask, "blocked");
            }
        }

        // 1. runningTask 释放资源
        // runningTask 释放资源
        releaseResource.basicResource.isOccupied = false;
        // 1. 弹出 runningTask 的 boosted priority 并修改相关配置
        runningTask.basicPCB.priorities.pop();
        runningTask.basicPCB.isAccessLocalResource = runningTask.basicPCB.isAccessGlobalResource = false;
        ++runningTask.basicPCB.requestResourceTh;
        if (runningTask.basicPCB.systemCriticalityWhenAccessResource == 1)
            runningTask.basicPCB.executedTime -= (releaseResource.basicResource.c_high - releaseResource.basicResource.c_low);

        // runningTask 记录事件发生的时间点
        TaskEventTimePointsRecords.get(runningTask.basicPCB.dynamicTaskId).add(new com.example.serveside.response.EventTimePoint(runningTask.basicPCB.staticTaskId, runningTask.basicPCB.dynamicTaskId, "unlocked", systemClock, releaseResource.basicResource.id));
        // cpu 上也需要记录释放资源的时间点
        cpuEventTimePointRecords.get(runningTask.basicPCB.baseRunningCpuCore).add(new com.example.serveside.response.EventTimePoint(runningTask.basicPCB.staticTaskId, runningTask.basicPCB.dynamicTaskId, "unlocked", systemClock, releaseResource.basicResource.id));
        // 切换一下 runningTask 的状态
        // 在原本的核心上执行，简单切换一下状态就行
        ChangeTaskState(runningTask, "");
        ChangeCpuTaskState(runningTask.basicPCB.baseRunningCpuCore, runningTask, "");

        // 2. 把资源让给其他正在等待该资源的任务
        if (!releaseResource.waitingQueue.isEmpty()) {
            com.example.serveside.service.msrp.ProcedureControlBlock waitingTask = releaseResource.waitingQueue.get(0);
            releaseResource.waitingQueue.remove(0);
            // waitingTask 占据资源
            releaseResource.basicResource.isOccupied = true;
            waitingTask.basicPCB.remainResourceComputationTime = (criticality_indicator == 0) ? releaseResource.basicResource.c_low : releaseResource.basicResource.c_high;
            waitingTask.basicPCB.systemCriticalityWhenAccessResource = criticality_indicator;
            // waitingTask 访问资源的类型
            waitingTask.basicPCB.isAccessGlobalResource = releaseResource.basicResource.isGlobal;
            waitingTask.basicPCB.isAccessLocalResource = !waitingTask.basicPCB.isAccessGlobalResource;
            waitingTask.basicPCB.spin = false;

            // 任务状态进行更改
            ChangeTaskState(waitingTask, "access-resource");
            TaskEventTimePointsRecords.get(waitingTask.basicPCB.dynamicTaskId).add(new com.example.serveside.response.EventTimePoint(waitingTask.basicPCB.staticTaskId, waitingTask.basicPCB.dynamicTaskId, "locked", systemClock, releaseResource.basicResource.id));

            // 在 MSRP 协议下，有任务正在等待全局资源，那么这个任务一定在 CPU 上执行 --> 需要更改 CPU 上面的状态
            // cpu state:
            ChangeCpuTaskState(waitingTask.basicPCB.baseRunningCpuCore, waitingTask, "access-resource");
            cpuEventTimePointRecords.get(waitingTask.basicPCB.baseRunningCpuCore).add(new com.example.serveside.response.EventTimePoint(waitingTask.basicPCB.staticTaskId, waitingTask.basicPCB.dynamicTaskId, "locked", systemClock, releaseResource.basicResource.id));

            // waitingTask 获取到资源之后，waitingTasks 中的阻塞任务状态就需要从 indirect-spinning --> blocked
            // arrival-blocking 还将继续保持直到任务释放资源
            for (com.example.serveside.service.msrp.ProcedureControlBlock _waitingTask : waitingTasksPerCore.get(waitingTask.basicPCB.baseRunningCpuCore))
            {
                ArrayList<com.example.serveside.response.EventInformation> _taskInformations = TaskEventInformationRecords.get(_waitingTask.basicPCB.dynamicTaskId);
                if (!_taskInformations.isEmpty() && _taskInformations.get(_taskInformations.size() - 1).getState().equals("indirect-spinning"))
                {
                    // 由 indirect-spinning-delay 变成 blocked
                    ChangeTaskState(_waitingTask, "blocked");
                }
            }
        }

    }

    /**
     * 函数{@code ReleaseTasks}用以发布新任务。
     * */
    public static void ReleaseTasks()
    {
        // 1. 某一时间点有任务需要释放
        // 2. 当前的时间点 = 任务释放的时间点
        if (itemTaskReleaseTimes != null && itemTaskReleaseTimes.getKey() == systemClock)
        {
            for (Integer releaseTaskId : itemTaskReleaseTimes.getValue())
            {
                // 系统模式处于高关键级下，低关键的任务直接不发布了
                if (totalTasks.get(releaseTaskId).basicPCB.criticality < criticality_indicator){
                    ++taskFinishTimes[totalTasks.get(releaseTaskId).basicPCB.staticTaskId];
                    continue;
                }

                // 初始化任务的基本信息
                com.example.serveside.service.msrp.ProcedureControlBlock releaseTask = new com.example.serveside.service.msrp.ProcedureControlBlock(totalTasks.get(releaseTaskId));
                releaseTask.basicPCB.dynamicTaskId = releaseTaskNum++;
                waitingTasksPerCore.get(totalTasks.get(releaseTaskId).basicPCB.baseRunningCpuCore).add(releaseTask);

                // 记录一下发布的任务的基本信息
                releaseTaskInformations.add(new com.example.serveside.response.TaskInformation(releaseTask, systemClock));

                System.out.printf("Release Task:\n\tStatic Task id:%d\n\tDynamic Task id:%d\n\tRelease Time:%d\n\n", releaseTask.basicPCB.staticTaskId, releaseTask.basicPCB.dynamicTaskId, systemClock);

                // 记录一下任务发布的时间点
                TaskEventTimePointsRecords.add(new ArrayList<>());
                TaskEventTimePointsRecords.get(releaseTask.basicPCB.dynamicTaskId).add(new com.example.serveside.response.EventTimePoint(releaseTask.basicPCB.staticTaskId, releaseTask.basicPCB.dynamicTaskId, "release", systemClock, -1));

                // 创建一个新的 taskEventInformation 来保存任务的运行过程
                TaskEventInformationRecords.add(new ArrayList<>());
            }

            if (iteratorTaskReleaseTimes.hasNext())
                itemTaskReleaseTimes = iteratorTaskReleaseTimes.next();
            else
                itemTaskReleaseTimes = null;
        }
    }

    /**
     * 该方法 {@code ChooseTaskToRun} 将任务分配到相应的处理器核心上执行。
     * <p>
     *     任务分配到处理器上执行的逻辑如下：
     *     <ol>
     *         <li>当处理器核心处于空闲状态时，它将开始执行与之对应的等待执行任务列表中优先级最高的任务。</li>
     *         <li>当处理器核心正在执行任务并且任务正在访问或者自旋等待全局资源时，执行的任务保持不变，因为该任务不可被抢占。</li>
     *         <li>
     *             当处理器核心正在执行任务（任务单纯执行或者任务访问局部资源）并且等待执行的任务列表中存在优先级更高的任务时，更高优先级的任务抢占正在执行的任务，占据处理器核心，被抢占的任务重新放入等待执行的列表中。
     *         </li>
     *     </ol>
     * </p>
     * <p>
     *     除了将任务分配到处理器上执行，我们还需要考虑一个新发布的任务可能遭受到达阻塞（arrival-blocking）。
     * </p>
     * */
    public static void ChooseTaskToRun()
    {
        // 1. 高优先级的任务抢占低优先级的任务
        for (int i = 0; i < TOTAL_CPU_CORE_NUM; ++i) {
            com.example.serveside.service.msrp.ProcedureControlBlock runningTask = runningTaskPerCore.get(i);

            ArrayList<com.example.serveside.service.msrp.ProcedureControlBlock> waitingTasks = waitingTasksPerCore.get(i);
            if (waitingTasks.isEmpty())
                continue;

            // 先对 waitingTasks 按 priorities.peek() 进行优先级从大到小进行排序
            waitingTasks.sort((task1, task2) -> -Integer.compare(task1.basicPCB.priorities.peek(), task2.basicPCB.priorities.peek()));

            com.example.serveside.service.msrp.ProcedureControlBlock waitingTask = waitingTasks.get(0);

            // 1. CPU 空闲
            if (runningTask == null)
            {
                // waiting Task 放在对应的 CPU 核上运行
                runningTaskPerCore.set(i, waitingTask);

                // 任务状态发生变化: 恢复 waitingTask 以前的状态 / waitingTask 开始执行
                ChangeTaskState(waitingTask, "");
                // 需要将 waitingTask 从 waitingTasks 中移出来
                waitingTasks.remove(0);

                // cpu 甘特图发生变化: 运行的任务发生变化(state 根据任务自身的状态来决定)
                ChangeCpuTaskState(i, waitingTask, "");
                // cpu 甘特图加入 switch-task 标号
                cpuEventTimePointRecords.get(i).add(new EventTimePoint(waitingTask.basicPCB.staticTaskId, waitingTask.basicPCB.dynamicTaskId, "switch-task", systemClock, -1));

                continue;
            }

            // runningTask 不能够在访问 global-resource / spin （MSRP : 占据处理器核心的任务处在这两种状态下不可以被抢占）
            if (runningTask.basicPCB.spin || runningTask.basicPCB.isAccessGlobalResource) {
                continue;
            }

            // 2. 其余情况只需要满足：waitingTask的优先级 > runningTask 的优先级 即可抢占
                // i. normal-execution
                // ii. access local resource
            if (waitingTask.basicPCB.priorities.peek() > runningTask.basicPCB.priorities.peek())
            {
                // waitingTask 状态发生变化, waitingTask 放在 CPU 上运行
                ChangeTaskState(waitingTask, "");
                runningTaskPerCore.set(i, waitingTask);

                // runningTask 放入原生的 waitingTasks 中
                waitingTasksPerCore.get(runningTask.basicPCB.baseRunningCpuCore).add(runningTask);

                // 需要将 waitingTask 从 waitingTasks 中移出来
                waitingTasks.remove(0);

                // runningTask 的状态也需要发生变化：blocked
                ChangeTaskState(runningTask, "blocked");

                // cpu 甘特图发生变化 : 运行的任务发生变化(state 根据任务自身的状态来决定)
                ChangeCpuTaskState(i, waitingTask, "");

                // cpu 甘特图中标出 switch task
                cpuEventTimePointRecords.get(i).add(new EventTimePoint(waitingTask.basicPCB.staticTaskId, waitingTask.basicPCB.dynamicTaskId, "switch-task", systemClock, -1));

                // 在 runningTask 访问局部资源时，其可能造成其他任务 arrival-blocking, 这里也需要终止一下 arrival-blocking 状态
                if (runningTask.basicPCB.isAccessLocalResource) {
                    // 处理该处理器上其他任务的 arrival-blocking
                    for (com.example.serveside.service.msrp.ProcedureControlBlock _waitingTask : waitingTasksPerCore.get(i))
                    {
                        ArrayList<com.example.serveside.response.EventInformation> _taskInformations = TaskEventInformationRecords.get(_waitingTask.basicPCB.dynamicTaskId);
                        if (!_taskInformations.isEmpty() && _taskInformations.get(_taskInformations.size() - 1).getState().equals("arrival-blocking"))
                        {
                            // 由 indirect-spinning-delay 变成 blocked
                            ChangeTaskState(_waitingTask, "blocked");
                        }
                    }
                }
            }
        }

        // 2.  处理 Arrival Blocking 的情况
        for (int i = 0; i < TOTAL_CPU_CORE_NUM; ++i)
        {
            com.example.serveside.service.msrp.ProcedureControlBlock runningTask = runningTaskPerCore.get(i);
            if (waitingTasksPerCore.get(i).isEmpty())
                continue;

            // Arrival Blocking 发生的条件
            // 1. waitingTask 刚刚到达，即还没有开始执行
            // 2. waitingTask 的基础优先级比 runningTask 的基础优先级更高
            // 3. runningTask 的运行时优先级(priorities.peek()) 比 waitingTask 的运行时优先级(priorities.peek()) 更高
            ArrayList<com.example.serveside.service.msrp.ProcedureControlBlock> waitingTasks = waitingTasksPerCore.get(i);
            for (com.example.serveside.service.msrp.ProcedureControlBlock waitingTask : waitingTasks)
            {
                // 判断 Arrival Blocking 发生的情况
                if (waitingTask.basicPCB.executedTime == 0)
                {
                    // 1. runningTask 访问 local-resource 造成优先级反转
                    if (runningTask.basicPCB.basePriority < waitingTask.basicPCB.basePriority
                            && runningTask.basicPCB.isAccessLocalResource) {
                        ChangeTaskState(waitingTask, "arrival-blocking");
                    }
                    // 2. (runningTask 访问 global-resource || runningTask 目前正在 spin) && runningTask 的优先级更低
                    else if (runningTask.basicPCB.basePriority < waitingTask.basicPCB.basePriority
                            && (runningTask.basicPCB.isAccessGlobalResource || runningTask.basicPCB.spin)) {
                        ChangeTaskState(waitingTask, "arrival-blocking");
                    }
                }
            }
        }

    }

    /**
     * 该方法 {@code ChangeTaskState} 结束任务 {@code task} 当前所处的状态，并开启新的状态 {@code _state}。
     * @param task 进行任务状态切换的任务。
     * @param _state 任务{@code task} 的新状态。
     * */
    public static void ChangeTaskState(com.example.serveside.service.msrp.ProcedureControlBlock task, String _state)
    {
        ArrayList<com.example.serveside.response.EventInformation> taskInformations = TaskEventInformationRecords.get(task.basicPCB.dynamicTaskId);

        // 先终止上一个状态(如果有的话)
        if (!taskInformations.isEmpty())
        {
            if (taskInformations.get(taskInformations.size() - 1).getStartTime() != systemClock)
                taskInformations.get(taskInformations.size() - 1).setEndTime(systemClock);
            else
                taskInformations.remove(taskInformations.size() - 1);
        }

        // 新状态和旧状态状态一致（_arrival-blocking），那么就保持不变
        if (!taskInformations.isEmpty() && taskInformations.get(taskInformations.size() - 1).getState().equals(_state))
            return ;

        // 如果既不是 completion，也不是 shutdown，那么就开启一个新的状态
        if (!_state.equals("completion") && !_state.equals("killed"))
            taskInformations.add(new com.example.serveside.response.EventInformation(task, systemClock, _state));
    }

    /**
     * 该方法 {@code ChangeTaskState} 结束处理器 {@code runningCpuCore} 上执行的任务 {@code task} 当前所处的状态，并开启新的状态 {@code _state}。
     * @param runningCpuCore 进行任务状态切换的处理器核心。
     * @param task 进行任务状态切换的任务。
     * @param _state 任务{@code task} 的新状态。
     * */
    public static void ChangeCpuTaskState(int runningCpuCore, com.example.serveside.service.msrp.ProcedureControlBlock task, String _state)
    {
        ArrayList<com.example.serveside.response.EventInformation> cpuEventInformationRecord = cpuEventInformationRecords.get(runningCpuCore);
        // 终止上一个状态(如果不为空的话)
        if (!cpuEventInformationRecord.isEmpty())
        {
            if (cpuEventInformationRecord.get(cpuEventInformationRecord.size() - 1).getStartTime() != systemClock)
                cpuEventInformationRecord.get(cpuEventInformationRecord.size() - 1).setEndTime(systemClock);
            else
                cpuEventInformationRecord.remove(cpuEventInformationRecord.size() - 1);
        }

        // 开启一个新的状态
        cpuEventInformationRecord.add(new com.example.serveside.response.EventInformation(task, systemClock, _state));
    }

}
