package com.example.rta;

import com.example.rta.service.*;
import com.example.rta.service.CommonUse.PeriodicTask;
import com.example.rta.response.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RtaEngineTests {
    private PeriodicTask task(int priority,double c,double t,double d) {
        PeriodicTask x=new PeriodicTask();x.setPriority(priority);x.setWcet(c);x.setPeriod(t);x.setDeadline(d);return x;
    }
    private RtaEngine.Result pair(double hc,double ht,double c,double d) {
        return RtaEngine.worstCaseResponseTime(Arrays.asList(task(1,hc,ht,ht),task(2,c,20,d)),1);
    }
    @Test void costAlreadyMissesButResponseStillIncludesHigherPriorityWork() {
        RtaEngine.Result r=pair(1,10,3,2);
        assertEquals(4,r.responseTime);assertTrue(r.converged);assertFalse(r.schedulable);
    }
    @Test void decimalDeadlineEqualityIsScheduled() {
        RtaEngine.Result r=pair(.1,1,.2,.3);
        assertEquals(.3,r.responseTime);assertTrue(r.schedulable);
    }
    @Test void releaseJustAbovePeriodBoundaryMustNotBeRoundedDown() {
        RtaEngine.Result r=pair(.2,1,.8000000001,1.1);
        assertEquals(1.2000000001,r.responseTime);assertFalse(r.schedulable);
    }
    @Test void exactPeriodBoundaryDoesNotAddExtraRelease() {
        RtaEngine.Result r=pair(.2,1,.8,1);
        assertEquals(1,r.responseTime);assertTrue(r.schedulable);
    }
    @Test void utilizationBelowOneMustNotBeDeclaredUnbounded() {
        RtaEngine.Result r=pair(.9999999995,1,.0000000001,1);
        assertFalse(r.starved);assertEquals(.9999999996,r.responseTime);assertTrue(r.schedulable);
    }
    @Test void utilizationExactlyOneHasNoFiniteFixedPoint() {
        RtaEngine.Result r=pair(1,1,.1,10);
        assertTrue(r.starved);assertFalse(r.schedulable);
    }
    @Test void hpUsesStrictPriorityRatherThanArrayPositionOrSelf() {
        List<PeriodicTask> tasks=Arrays.asList(task(3,100,100,100),task(2,2,10,10),task(1,1,10,10));
        RtaEngine.Result r=RtaEngine.worstCaseResponseTime(tasks,1);
        assertEquals(3,r.responseTime);assertTrue(r.schedulable);
    }
    @Test void ambiguousEqualPrioritiesAreRejected() {
        assertThrows(IllegalArgumentException.class,()->RtaEngine.worstCaseResponseTime(
                Arrays.asList(task(1,1,10,10),task(1,2,10,10)),1));
    }
    @Test void nonConvergenceRetainsFiniteLastIterateAndExplicitFlag() {
        RtaEngine.Result r=pair(.99995,1,1,1);
        assertFalse(r.converged);assertTrue(Double.isFinite(r.responseTime));assertTrue(r.deadlineExceeded);
        assertTrue(r.nonConvergent);assertEquals(r.lastIterationValue.doubleValue(),r.responseTime);
    }
    // Independent oracle using integer tenths, not the production decimal/ceil helper.
    private long referenceStep(long response,long cost,JsonNode tasks,int priority) {
        long next=cost;
        for(JsonNode h:tasks)if(h.get("priority").asInt()<priority) {
            long period=Math.round(h.get("period").asDouble()*10);
            next+=((response+period-1)/period)*Math.round(h.get("WCET").asDouble()*10);
        }
        return next;
    }
    @Test void everyRealJsonTaskMatchesIndependentArithmeticAndApiStatus() throws Exception {
        String text=new String(Files.readAllBytes(Paths.get("src/test/resources/fixtures/tasks.json")),StandardCharsets.UTF_8);
        JsonNode root=new ObjectMapper().readTree(text);
        RtaService service=new RtaService();
        ToTalInformation result=service.analyse(text);
        double[] expected={10.6,21.2,22.7,63.9};
        for(int i=0;i<6;i++) {
            RtaSystemTask output=result.getSystemTasks().get(i);
            JsonNode input=root.get("tasks").get(i);
            assertEquals(input.get("name").asText(),output.getName());
            assertEquals(input.get("WCET").asDouble(),output.getWcet());
            assertEquals(input.get("period").asDouble(),output.getPeriod());
            assertEquals(input.get("deadline").asDouble(),output.getDeadline());
            if(i<4) {
                long c=Math.round(input.get("WCET").asDouble()*10),r=c,next;
                do {next=referenceStep(r,c,root.get("tasks"),input.get("priority").asInt());if(next==r)break;r=next;}while(true);
                assertEquals(expected[i],r/10.0);
                assertEquals(r/10.0,output.getWcrt());
                assertEquals(r<=Math.round(input.get("deadline").asDouble()*10),output.getSchedulable());
            } else {
                assertTrue(output.getStarved());assertEquals(i==4?227.7:1217.3,output.getWcrt());assertEquals(false,output.getSchedulable());
                assertTrue(output.getNonConvergent());assertFalse(output.getConverged());
                long c=Math.round(input.get("WCET").asDouble()*10),r=c,d=Math.round(input.get("deadline").asDouble()*10);
                for(int k=0;k<20 && r<=d;k++)r=referenceStep(r,c,root.get("tasks"),input.get("priority").asInt());
                assertTrue(r>d);
            }
            System.out.println("AUDIT "+output.getName()+" WCRT="+output.getWcrt()+" unbounded="+output.getStarved()+" scheduled="+output.getSchedulable());
        }
        assertEquals(3,result.getAnalysis().getMissCount());
    }
    @Test void lvglCompleteIterationTrace() {
        List<PeriodicTask> tasks=Arrays.asList(task(3,10.6,33.6,33),task(4,10.6,33.6,33),task(8,1.5,200,200),task(15,20,33,30));
        RtaEngine.Result r=RtaEngine.worstCaseResponseTime(tasks,3);
        assertEquals(63.9,r.responseTime);assertEquals(3,r.iterations);assertFalse(r.schedulable);
    }
}
