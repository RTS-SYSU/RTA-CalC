package com.example.rta;

import com.example.rta.service.RtaEngine;
import com.example.rta.service.RtaService;
import com.example.rta.service.CommonUse.PeriodicTask;
import com.example.rta.response.RtaSystemTask;
import com.example.rta.response.RtaTask;
import com.example.rta.response.ToTalInformation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ArbitraryDeadlineTests {
    private PeriodicTask task(int id, double cost, double period, double deadline) {
        PeriodicTask t = new PeriodicTask();
        t.setId(id); t.setName("T" + id); t.setFunction(""); t.setPartition(0);
        t.setPriority(id); t.setPhase(0.0); t.setWcet(cost); t.setPeriod(period); t.setDeadline(deadline);
        return t;
    }

    private ToTalInformation analyse(List<PeriodicTask> tasks) throws Exception {
        Map<String,Object> input = new LinkedHashMap<>();
        input.put("system", Collections.singletonMap("core_count", 1)); input.put("tasks", tasks);
        return new RtaService().analyse(new ObjectMapper().writeValueAsString(input));
    }

    @Test void laterJobsDetermineWorstCaseAndApiRejectsFalsePass() throws Exception {
        List<PeriodicTask> tasks = Arrays.asList(task(1,26,70,70), task(2,62,100,115));
        RtaEngine.Result r = RtaEngine.worstCaseResponseTime(tasks,1);
        assertEquals(118.0,r.responseTime); assertTrue(r.converged); assertFalse(r.schedulable);
        assertEquals(7,r.jobResponses.size());
        double[] expected = {114,102,116,104,118,106,94};
        for (int i=0;i<expected.length;i++)
            assertEquals(expected[i],((Number)r.jobResponses.get(i).get("responseTime")).doubleValue());
        ToTalInformation api = analyse(tasks);
        assertEquals(118.0,api.getSystemTasks().get(1).getWcrt());
        assertEquals(false,api.getSystemTasks().get(1).getSchedulable());
        assertEquals(false,api.getAnalysis().getSchedulable());
        Files.write(Paths.get("target/rta-arbitrary-deadline-test.json"),new ObjectMapper().writeValueAsBytes(api));
        tasks.get(1).setDeadline(118.0);
        assertTrue(RtaEngine.worstCaseResponseTime(tasks,1).schedulable);
        RtaEngine.Result decimal = RtaEngine.worstCaseResponseTime(
                Arrays.asList(task(1,2.6,7,7),task(2,6.2,10,11.8)),1);
        assertEquals(11.8,decimal.responseTime); assertTrue(decimal.schedulable);
    }

    @Test void selfOverloadHasNoFiniteWcrtEvenWhenFirstJobMeetsDeadline() throws Exception {
        List<PeriodicTask> tasks = Arrays.asList(task(1,2,1,3));
        RtaEngine.Result r = RtaEngine.worstCaseResponseTime(tasks,0);
        assertTrue(r.unbounded); assertFalse(r.converged); assertFalse(r.starved);
        assertFalse(r.schedulable); assertTrue(Double.isNaN(r.responseTime));
        assertEquals(2.0,r.lastIterationValue.doubleValue());
        ToTalInformation api = analyse(tasks);
        assertNull(api.getSystemTasks().get(0).getWcrt());
        assertEquals(false,api.getAnalysis().getSchedulable());
        assertEquals(false,api.getAnalysis().getUtilizationOk());
        assertEquals(1,api.getAnalysis().getMissCount());
    }

    @Test void exactUnitUtilizationCanHaveFiniteWcrt() {
        List<PeriodicTask> tasks = Arrays.asList(task(1,1,2,2),task(2,1,2,2));
        RtaEngine.Result r = RtaEngine.worstCaseResponseTime(tasks,1);
        assertFalse(r.unbounded); assertTrue(r.converged); assertTrue(r.schedulable);
        assertEquals(2.0,r.responseTime);
        assertTrue(RtaEngine.utilizationAtMostOne(tasks));
        assertFalse(RtaEngine.utilizationAtMostOne(Arrays.asList(task(1,1.0000000001,1,2))));
    }

    @Test void iterationLimitIsUnknownAndDoesNotExposeDiagnosticAsWcrt() {
        List<PeriodicTask> tasks = Arrays.asList(task(1,12000,24001,24001),task(2,1,2,24001));
        RtaEngine.Result r = RtaEngine.worstCaseResponseTime(tasks,1);
        assertFalse(r.converged); assertFalse(r.unbounded); assertFalse(r.deadlineExceeded);
        assertTrue(r.nonConvergent); assertTrue(Double.isNaN(r.responseTime));
        RtaSystemTask dto = new RtaSystemTask(tasks.get(1),r);
        assertNull(dto.getWcrt()); assertNull(dto.getSchedulable());
        assertNotNull(dto.getLastIterationValue());
        assertNull(new RtaTask(2,tasks.get(1),r,Arrays.asList("T1")).getSchedulable());
    }

    @Test void skippedTasksDoNotBecomeNonConvergent() throws Exception {
        ToTalInformation api = analyse(Arrays.asList(task(1,0,10,10),task(2,1,0,10)));
        for (RtaSystemTask t : api.getSystemTasks()) {
            assertNull(t.getConverged()); assertFalse(t.getNonConvergent());
            assertFalse(t.getUnbounded()); assertNull(t.getWcrt()); assertNull(t.getSchedulable());
        }
        Files.write(Paths.get("target/rta-skipped-test.json"),new ObjectMapper().writeValueAsBytes(api));
    }

    @SuppressWarnings("unchecked")
    @Test void smallTimeValuesReachFrontendWithoutRounding() throws Exception {
        PeriodicTask high=task(1,.0001,.001,.001), low=task(2,.0003,.001,.001);
        high.setPhase(.0001);low.setPhase(.0001);
        ToTalInformation api=analyse(Arrays.asList(high,low));
        List<Map<String,Object>> rows=(List<Map<String,Object>>)api.getTimeline().get("rows");
        Map<String,Object> row=rows.stream().filter(r->r.get("id").equals(2)).findFirst().get();
        assertEquals(.0001,row.get("release"));assertEquals(.0001,row.get("interference"));
        assertEquals(.0005,row.get("completion"));assertEquals(.0003,row.get("execution"));
        Files.write(Paths.get("target/rta-small-times-test.json"),new ObjectMapper().writeValueAsBytes(api));
    }

    // Independent integer-tick scheduler: compare the entire first synchronous busy period.
    @Test void finiteWcrtMatchesIndependentSchedulerAcrossTaskSets() {
        Random random = new Random(20261017);
        int verified = 0;
        for (int trial=0;trial<250;trial++) {
            int count=1+random.nextInt(3);
            List<PeriodicTask> tasks=new ArrayList<>();
            for(int i=0;i<count;i++) tasks.add(task(i+1,1+random.nextInt(3),3+random.nextInt(10),20));
            if(!RtaEngine.utilizationAtMostOne(tasks))continue;
            List<int[]> jobs=new ArrayList<>(); int worst=0; boolean ended=false;
            for(int tick=0;tick<10000;tick++) {
                for(int i=0;i<count;i++)if(tick%(int)tasks.get(i).getPeriod()==0)
                    jobs.add(new int[]{i,tick,(int)tasks.get(i).getWcet()});
                int[] selected=null;
                for(int[] job:jobs)if(job[2]>0&&(selected==null||job[0]<selected[0]))selected=job;
                assertNotNull(selected);
                selected[2]--;
                if(selected[2]==0&&selected[0]==count-1)worst=Math.max(worst,tick+1-selected[1]);
                if(jobs.stream().allMatch(j->j[2]==0)){ended=true;break;}
            }
            assertTrue(ended);
            RtaEngine.Result result=RtaEngine.worstCaseResponseTime(tasks,count-1);
            assertTrue(result.converged); assertEquals(worst,result.responseTime,0.0);
            verified++;
        }
        assertTrue(verified>100);
    }
}
