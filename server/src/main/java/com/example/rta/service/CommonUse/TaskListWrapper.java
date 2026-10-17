package com.example.rta.service.CommonUse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * {@code TaskListWrapper} 用于把 periodic_tasks.json 反序列化，对应文件里的 {@code "system"} 与 {@code "tasks"} 字段。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskListWrapper {

    /**
     * 系统级配置。
     */
    private SystemSettings system;

    /**
     * 周期任务集合。
     */
    private List<PeriodicTask> tasks;

    /**
     * {@code SystemSettings} 对应输入文件中的 {@code "system"} 字段。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SystemSettings {

        /**
         * 处理器核心个数。
         */
        @JsonProperty("core_count")
        private Integer coreCount;

        public Integer getCoreCount() {
            return this.coreCount;
        }

        public void setCoreCount(Integer coreCount) {
            this.coreCount = coreCount;
        }
    }

    public SystemSettings getSystem() {
        return this.system;
    }

    public List<PeriodicTask> getTasks() {
        return this.tasks;
    }

    public void setSystem(SystemSettings system) {
        this.system = system;
    }

    public void setTasks(List<PeriodicTask> tasks) {
        this.tasks = tasks;
    }
}
