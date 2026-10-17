package com.example.rta.service;

import com.example.rta.response.RtaAnalysis;
import com.example.rta.response.RtaSkippedTask;
import com.example.rta.response.RtaSystemTask;
import com.example.rta.response.RtaTask;
import com.example.rta.response.ToTalInformation;
import com.example.rta.service.CommonUse.PeriodicTask;
import com.example.rta.service.CommonUse.TaskListWrapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RtaService {

    private static final double EPSILON = 1e-9;

    @Value("${rta.input:}")
    private String inputPath;


    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public ToTalInformation analyse() {
        return analyse(null);
    }

    public ToTalInformation analyse(String jsonOverride) {
        List<String> warnings = new ArrayList<>();
        List<PeriodicTask> allTasks = new ArrayList<>();
        int coreCount = 1;

        try {
            String text = (jsonOverride != null && !jsonOverride.trim().isEmpty())
                    ? jsonOverride
                    : readInputText(warnings);
            TaskListWrapper wrapper = parseWrapper(text, warnings);
            validate(wrapper);
            allTasks = wrapper.getTasks();
        } catch (Exception exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "读取或解析输入文件失败：" + exception.getMessage(), exception);
        }

        // 区分可以分析的周期任务与需要跳过的任务
        List<PeriodicTask> activeTasks = new ArrayList<>();
        List<RtaSkippedTask> skippedTasks = new ArrayList<>();
        for (PeriodicTask task : allTasks) {
            String reason = skipReason(task);
            if (reason == null) {
                activeTasks.add(task);
            } else {
                skippedTasks.add(new RtaSkippedTask(task, reason));
            }
        }

        // priority 数值越小优先级越高
        activeTasks.sort(Comparator.comparingDouble(PeriodicTask::getPriority)
                .thenComparing(PeriodicTask::getId));

        // 依次求出每个任务的最坏响应时间
        List<RtaTask> rtaTasks = new ArrayList<>();
        List<RtaEngine.Result> results = new ArrayList<>();
        List<String> higherPriorityNames = new ArrayList<>();
        double totalUtilization = 0.0;
        int missCount = 0;
        int starvedCount = 0;

        for (int i = 0; i < activeTasks.size(); ++i) {
            PeriodicTask task = activeTasks.get(i);
            RtaEngine.Result result = RtaEngine.worstCaseResponseTime(activeTasks, i);
            results.add(result);
            rtaTasks.add(new RtaTask(i + 1, task, result, new ArrayList<>(higherPriorityNames)));

            totalUtilization += task.getWcet() / task.getPeriod();
            if (!result.schedulable) {
                ++missCount;
            }
            if (result.starved) {
                ++starvedCount;
            }
            higherPriorityNames.add(task.getName());
        }

        RtaAnalysis analysis = new RtaAnalysis(coreCount, allTasks.size(), activeTasks.size(), skippedTasks.size(),
                missCount, totalUtilization, totalUtilization <= 1.0 + EPSILON, missCount == 0);

        // 系统里每个任务的原始配置（顺带带上算出来的 WCRT）
        Map<Integer, RtaEngine.Result> resultById = new HashMap<>();
        for (int i = 0; i < activeTasks.size(); ++i) {
            resultById.put(activeTasks.get(i).getId(), results.get(i));
        }
        List<RtaSystemTask> systemTasks = new ArrayList<>();
        for (PeriodicTask task : allTasks) {
            systemTasks.add(new RtaSystemTask(task, resultById.get(task.getId())));
        }

        ToTalInformation totalInformation = new ToTalInformation();
        // Simulate exactly the same JSON-derived range that both charts display.
        double displayEnd=allTasks.stream().mapToDouble(PeriodicTask::deadlineOrDefault).max().orElse(100.0);
        totalInformation.setTimeline(new MixedTimelineAdapter().simulate(activeTasks, displayEnd));
        totalInformation.setAnalysis(analysis);
        totalInformation.setTasks(rtaTasks);
        totalInformation.setSkippedTasks(skippedTasks);
        totalInformation.setSystemTasks(systemTasks);
        totalInformation.setWarnings(warnings);

        printSummary(analysis, starvedCount);

        return totalInformation;
    }

    private String readInputText(List<String> warnings) throws IOException {
        if (this.inputPath != null && !this.inputPath.trim().isEmpty()) {
            Path configuredFile = Paths.get(this.inputPath.trim()).toAbsolutePath();
            if (Files.isRegularFile(configuredFile)) {
                return new String(Files.readAllBytes(configuredFile), StandardCharsets.UTF_8);
            }
            warnings.add("配置的输入文件不存在（" + configuredFile + "），继续查找默认 test.json。");
        }

        Path workingDirectoryFile = Paths.get("test.json").toAbsolutePath();
        if (Files.isRegularFile(workingDirectoryFile)) {
            return new String(Files.readAllBytes(workingDirectoryFile), StandardCharsets.UTF_8);
        }

        ClassPathResource resource = new ClassPathResource("test.json");
        try (InputStream input = resource.getInputStream()) {
            return new String(FileCopyUtils.copyToByteArray(input), StandardCharsets.UTF_8);
        }
    }

    private TaskListWrapper parseWrapper(String text, List<String> warnings) throws IOException {
        String json = text;
        if (json.startsWith("\uFEFF")) {
            json = json.substring(1);
        }

        com.fasterxml.jackson.databind.JsonNode root = OBJECT_MAPPER.readTree(json);
        if (root == null || !root.isObject() || !root.path("tasks").isArray())
            throw new IllegalArgumentException("输入须为包含 tasks 数组的 JSON 对象。");
        for (com.fasterxml.jackson.databind.JsonNode task : root.path("tasks")) {
            for (String field : new String[]{"id", "name", "function", "partition", "priority", "period", "phase", "deadline", "WCET"})
                if (!task.hasNonNull(field)) throw new IllegalArgumentException("任务缺少新版字段 " + field);
            for (String field : new String[]{"id", "partition", "priority", "period", "phase", "deadline", "WCET"})
                if (!task.get(field).isNumber()) throw new IllegalArgumentException(field + " 必须为数值。");
            if (!task.get("id").isIntegralNumber() || !task.get("partition").isIntegralNumber()
                    || !task.get("name").isTextual() || !task.get("function").isTextual())
                throw new IllegalArgumentException("id/partition 必须为整数，name/function 必须为字符串。");
        }
        return OBJECT_MAPPER.treeToValue(root, TaskListWrapper.class);
    }

    private void validate(TaskListWrapper wrapper) {
        if (wrapper == null || wrapper.getTasks() == null || wrapper.getSystem() == null
                || !Integer.valueOf(1).equals(wrapper.getSystem().getCoreCount()))
            throw new IllegalArgumentException("输入须包含 system.core_count = 1 和 tasks 数组。");
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        java.util.Set<Double> priorities = new java.util.HashSet<>();
        for (PeriodicTask task : wrapper.getTasks()) {
            if (task == null || task.getId() == null || !ids.add(task.getId()))
                throw new IllegalArgumentException("任务 id 必须存在且唯一。");
            if (!Integer.valueOf(0).equals(task.getPartition()))
                throw new IllegalArgumentException("单核任务 partition 必须为 0。");
            if (task.getPeriod() > 0 && task.getWcet() > 0 && !priorities.add(task.getPriority()))
                throw new IllegalArgumentException("参与 RTA 的任务 priority 必须唯一。");
            if (!Double.isFinite(task.getPeriod()) || task.getPeriod() < 0
                    || !Double.isFinite(task.getWcet()) || task.getWcet() < 0
                    || !Double.isFinite(task.getPriority())
                    || task.getPhase() == null || !Double.isFinite(task.getPhase()) || task.getPhase() < 0
                    || task.getDeadline() == null || !Double.isFinite(task.getDeadline()) || task.getDeadline() < 0)
                throw new IllegalArgumentException("任务 " + task.getId() + " 的时间参数无效。");
        }
    }

    private String skipReason(PeriodicTask task) {
        if (task.getId() == null) {
            return "缺少 id 字段，无法识别该任务";
        }
        if (task.getPeriod() <= 0) {
            return "period = 0（非周期任务 / 空闲任务）";
        }
        if (task.getWcet() <= 0) {
            return "WCET = 0（无执行需求，不产生干扰）";
        }
        return null;
    }

    private void printSummary(RtaAnalysis analysis, int starvedCount) {
        System.out.println("[RTA] 核心数 = " + analysis.getCoreCount()
                + "，分析 " + analysis.getAnalyzedCount() + "/" + analysis.getTaskCount()
                + " 个任务（跳过 " + analysis.getSkippedCount() + " 个），总利用率 = "
                + String.format("%.4f", analysis.getTotalUtilization())
                + (analysis.getUtilizationOk() ? "" : "（> 1，过载）")
                + "，超过截止期 " + analysis.getMissCount() + " 个（其中被饿死 " + starvedCount + " 个），系统"
                + (analysis.getSchedulable() ? "可调度" : "不可调度"));
    }
}
