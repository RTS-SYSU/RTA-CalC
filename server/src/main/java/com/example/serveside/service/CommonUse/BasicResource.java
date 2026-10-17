package com.example.serveside.service.CommonUse;


import java.util.ArrayList;

/**
 * {@code BasicResource} 类是模拟器中用于存储管理和控制资源的信息的基本数据结构。
 * <p>
 *     该类描述了在不同资源共享协议下，模拟器所需的通用管理资源信息。<br>
 *     根据协议的特点和需求，通过在 {@code BasicResource} 的基础上添加新属性，构建相应的资源控制块。<br>
 *     每个资源都关联一个资源控制块，在生成资源时，模拟器创建相应的{@code BasicResource}，并负责在整个系统模拟运行过程中进行管理。
 * </p>
 */
public class BasicResource {
    /**
     * 资源目前是否被任务使用。
     * <p>
     *     当 {@code isOccupied} 为 false 时，资源没有被任务使用。
     *     当 {@code isOccupied} 为 true 时，资源正在被任务使用。
     * </p>
     */
    public boolean isOccupied;

    /**
     * 资源标识符。
     * <p>
     *     每种资源对应一个唯一的标识符。
     * </p>
     */
    public int id;

    /**
     * 任务在低关键级模式下使用资源的时长。
     */
    public int c_low;

    /**
     * 任务在高关键级模式下使用资源的时长。
     */
    public int c_high;

    /**
     * 资源对应的一组资源上限优先级。
     */
    public ArrayList<Integer> ceiling;

    /**
     * 表示资源是否是全局资源。
     * <p>
     *     当 {@code isGlobal} 为 true 时，资源是全局资源。
     *     当 {@code isGlobal} 为 false 时，资源是局部资源。
     * </p>
     */
    public boolean isGlobal = false;

    /**
     * {@code BasicResource} 的构造函数，初始化一个存储管理和控制资源的信息的基本数据结构。
     *
     * @param _id 资源标识符。
     * @param _c_low 任务在低关键级模式下使用资源的时长。
     * @param _c_high 任务在高关键级模式下使用资源的时长。
     */
    public BasicResource(int _id, int _c_low, int _c_high) {
        this.id = _id;
        this.c_low = _c_low;
        this.c_high = _c_high;
        ceiling = new ArrayList<>();
        isOccupied = false;
    }

    /**
     * {@code BasicResource} 的拷贝构造函数：从现有的 {@code BasicResource} 实例中复制用于管理和控制资源的信息，创建一个新的 {@code BasicResource}。
     *
     * @param basicResource 要复制的 {@code BasicResource} 实例。
     */
    public BasicResource(BasicResource basicResource)
    {
        this.isOccupied = basicResource.isOccupied;
        this.id = basicResource.id;
        this.c_low = basicResource.c_low;
        this.c_high = basicResource.c_high;
        this.isGlobal = basicResource.isGlobal;

        // 深度克隆 ArrayList<Integer>
        this.ceiling = new ArrayList<>();
        this.ceiling.addAll(basicResource.ceiling);

    }
}
