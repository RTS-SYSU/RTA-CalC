package com.example.serveside.service.mrsp;

import java.util.ArrayList;
import com.example.serveside.service.CommonUse.BasicResource;

/**
 * 在MrsP资源共享协议下资源的信息表示。
 * <p>
 *     在{@code BasicResource}的基础上加入FIFO请求队列。
 * </p>
 */
public class Resource {

    /**
     * 资源的FIFO请求队列
     */
    public ArrayList<com.example.serveside.service.mrsp.ProcedureControlBlock> waitingQueue;

    /**
     * 任务的基本信息。
     */
    public BasicResource basicResource;


    public Resource(BasicResource basicResource) {
        this.basicResource = new BasicResource(basicResource);
        waitingQueue = new ArrayList<>();
    }
}
