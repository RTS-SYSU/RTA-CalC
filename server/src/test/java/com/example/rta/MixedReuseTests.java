package com.example.rta;

import com.example.rta.service.MixedTimelineAdapter;
import com.example.rta.service.RtaEngine;
import com.example.rta.service.CommonUse.PeriodicTask;
import com.example.serveside.service.msrp.MSRPMixedCriticalSystem;
import com.example.serveside.response.EventInformation;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class MixedReuseTests {
    private PeriodicTask task(int id,double priority,double phase,double cost,double period) {
        PeriodicTask t=new PeriodicTask();t.setId(id);t.setName("T"+id);t.setPartition(0);t.setPriority(priority);
        t.setPhase(phase);t.setWcet(cost);t.setPeriod(period);t.setDeadline(period);return t;
    }
    @SuppressWarnings("unchecked")
    private List<Map<String,Object>> list(Map<String,Object> map,String key) { return (List<Map<String,Object>>)map.get(key); }
    @Test void mixedOriginalSourcesAndUnchangedLargerPriorityComparator() throws Exception {
        String pom=new String(Files.readAllBytes(Paths.get("pom.xml")),StandardCharsets.UTF_8);
        assertFalse(pom.contains("../Mixed-Criticality-System-Simulator-Serve-Side"));
        assertTrue(Files.exists(Paths.get("src/main/java/com/example/serveside/service/msrp/MSRPMixedCriticalSystem.java")));
        String source=new String(Files.readAllBytes(Paths.get("src/main/java/com/example/serveside/service/msrp/MSRPMixedCriticalSystem.java")),StandardCharsets.UTF_8);
        assertTrue(source.contains("-Integer.compare(task1.basicPCB.priorities.peek(), task2.basicPCB.priorities.peek())"));
        assertTrue(source.contains("waitingTask.basicPCB.priorities.peek() > runningTask.basicPCB.priorities.peek()"));
    }
    @Test void adapterMapsFractionalPrioritiesAndExecutionIsOriginalMixedRecord() {
        PeriodicTask low=task(91,20.5,0,3,20),high=task(77,-3.5,.5,1,10);
        Map<String,Object> result=new MixedTimelineAdapter().simulate(Arrays.asList(low,high),5);
        assertEquals("mixed/MSRPMixedCriticalSystem",result.get("engine"));
        assertTrue(MSRPMixedCriticalSystem.totalTasks.get(0).basicPCB.basePriority>
                MSRPMixedCriticalSystem.totalTasks.get(1).basicPCB.basePriority);
        List<Map<String,Object>> cpu=list(result,"cpu");
        assertEquals(91,cpu.get(0).get("id"));assertEquals(77,cpu.get(1).get("id"));
        assertEquals(.5,cpu.get(1).get("start"));assertEquals(1.5,cpu.get(1).get("end"));
        int n=0;
        for(EventInformation raw:MSRPMixedCriticalSystem.cpuEventInformationRecords.get(0)) {
            if(raw.getEndTime()<=raw.getStartTime())continue;
            Map<String,Object> adapted=cpu.get(n++);
            assertEquals(raw.getStartTime()*.5,(Double)adapted.get("start"));
            assertEquals(raw.getEndTime()*.5,(Double)adapted.get("end"));
        }
        assertEquals(cpu.size(),n);
        for(Map<String,Object> row:list(result,"rows")) {
            int dynamic=MSRPMixedCriticalSystem.releaseTaskInformations.stream()
                    .filter(j->j.getStaticPid()==(row.get("id").equals(77)?0:1)).findFirst().get().getDynamicPid();
            List<EventInformation> raw=MSRPMixedCriticalSystem.TaskEventInformationRecords.get(dynamic);
            long count=raw.stream().filter(s->"normal-execution".equals(s.getState()) && s.getEndTime()>s.getStartTime()).count();
            assertEquals(count,list(row,"states").stream().filter(s->"executing".equals(s.get("state"))).count());
        }
        RtaEngine.Result r=RtaEngine.worstCaseResponseTime(Arrays.asList(low,high),0);
        assertEquals(4,r.responseTime);assertEquals(77,r.higherPriorityTasks.get(0).get("id"));
    }
    @Test void initialIdleAndRepeatedEmptyReleaseRunDoNotLeakStaticMixedState() {
        MixedTimelineAdapter adapter=new MixedTimelineAdapter();
        Map<String,Object> a=adapter.simulate(Arrays.asList(task(1,1,.1,.5,2)),1);
        assertEquals("idle",list(a,"cpu").get(0).get("state"));
        assertEquals(0.0,list(a,"cpu").get(0).get("start"));
        assertEquals(.1,list(a,"cpu").get(0).get("end"));
        Map<String,Object> b=adapter.simulate(Arrays.asList(task(1,1,2,.5,2)),1);
        assertTrue(list(b,"rows").isEmpty());assertEquals(1,list(b,"cpu").size());
        assertEquals(1.0,list(b,"cpu").get(0).get("end"));
        assertEquals(1,list(a,"rows").size());
    }
    @Test void missDoesNotTruncateWindowAndTraceIncludesEveryHpContribution() {
        PeriodicTask high=task(1,1,0,2,3),low=task(2,2,0,2,10);low.setDeadline(1.0);
        Map<String,Object> result=new MixedTimelineAdapter().simulate(Arrays.asList(low,high),10);
        assertFalse(MSRPMixedCriticalSystem.isSchedulable);
        assertEquals(10.0,list(result,"cpu").get(list(result,"cpu").size()-1).get("end"));
        RtaEngine.Result r=RtaEngine.worstCaseResponseTime(Arrays.asList(low,high),0);
        assertEquals(6,r.responseTime);assertFalse(r.schedulable);
        assertEquals(3,r.trace.size());assertEquals(1,r.higherPriorityTasks.size());
    }
}
