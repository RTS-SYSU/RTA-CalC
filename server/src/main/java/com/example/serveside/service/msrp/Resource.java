package com.example.serveside.service.msrp;

import java.util.ArrayList;
import com.example.serveside.service.CommonUse.BasicResource;

/**
 * 在MSRP资源共享协议下资源的信息表示。
 * <p>
 *     在{@code BasicResource}的基础上加入FIFO请求队列。
 * </p>
 */
public class Resource {

    /**
     * 资源的FIFO请求队列
     */
    public ArrayList<ProcedureControlBlock> waitingQueue;

    /**
     *  资源信息
     */
    public BasicResource basicResource;

    public Resource(BasicResource basicResource) {

        this.basicResource = new BasicResource(basicResource);
        waitingQueue = new ArrayList<>();
    }
}
