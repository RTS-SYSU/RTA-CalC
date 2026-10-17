package com.example.rta.response;

import java.util.*;

public class ToTalInformation {
    private Map<String, Object> timeline;
    public Map<String, Object> getTimeline() { return timeline; }
    public void setTimeline(Map<String, Object> timeline) { this.timeline = timeline; }

    private RtaAnalysis analysis;
    private List<RtaTask> tasks;
    private List<RtaSkippedTask> skippedTasks;
    private List<RtaSystemTask> systemTasks;
    private List<String> warnings;
    public RtaAnalysis getAnalysis() { return analysis; }
    public void setAnalysis(RtaAnalysis value) { analysis = value; }
    public List<RtaTask> getTasks() { return tasks; }
    public void setTasks(List<RtaTask> value) { tasks = value; }
    public List<RtaSkippedTask> getSkippedTasks() { return skippedTasks; }
    public void setSkippedTasks(List<RtaSkippedTask> value) { skippedTasks = value; }
    public List<RtaSystemTask> getSystemTasks() { return systemTasks; }
    public void setSystemTasks(List<RtaSystemTask> value) { systemTasks = value; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> value) { warnings = value; }

}
