package com.example.rta;
import com.example.rta.service.*;
import com.example.rta.service.CommonUse.PeriodicTask;
import com.example.rta.response.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DataChainTests {
    @Test void sameNamesUseIndependentTaskIdsThroughoutChain() throws Exception {
        String json=new String(Files.readAllBytes(Paths.get("src/test/resources/fixtures/duplicate-task-names.json")),StandardCharsets.UTF_8);
        RtaService service=new RtaService();
        ToTalInformation result=service.analyse(json);
        assertEquals(4,result.getSystemTasks().get(0).getId());assertEquals(5,result.getSystemTasks().get(1).getId());
        assertEquals("LVGL",result.getSystemTasks().get(0).getName());assertEquals("LVGL",result.getSystemTasks().get(1).getName());
        assertEquals(20.0,result.getSystemTasks().get(0).getWcrt());assertEquals(false,result.getSystemTasks().get(0).getSchedulable());
        assertEquals(50.0,result.getSystemTasks().get(1).getWcrt());assertEquals(true,result.getSystemTasks().get(1).getSchedulable());
        Map<String,Object> a=job(rows(result.getTimeline()),"4:0"),b=job(rows(result.getTimeline()),"5:0");
        assertEquals(0.0,a.get("release"));assertEquals(0.0,a.get("interference"));assertEquals(20.0,a.get("completion"));
        assertEquals(0.0,b.get("release"));assertEquals(20.0,b.get("interference"));assertEquals(50.0,b.get("completion"));
        for(Map<String,Object> row:Arrays.asList(a,b))for(Map<String,Object> s:states(row))
            if("executing".equals(s.get("state")))assertEquals(row.get("id"),s.get("taskId"));
        Files.write(Paths.get("target/rta-duplicate-names-test.json"),new ObjectMapper().writeValueAsBytes(result));
    }
    @SuppressWarnings("unchecked")
    private List<Map<String,Object>> rows(Map<String,Object> timeline) { return (List<Map<String,Object>>)timeline.get("rows"); }
    @SuppressWarnings("unchecked")
    private List<Map<String,Object>> states(Map<String,Object> row) { return (List<Map<String,Object>>)row.get("states"); }
    private Map<String,Object> job(List<Map<String,Object>> rows,String id) {
        return rows.stream().filter(r->id.equals(r.get("jobId"))).findFirst().get();
    }
    @Test void fullJsonChainAndEveryInterval() throws Exception {
        String json=new String(Files.readAllBytes(Paths.get("src/test/resources/fixtures/tasks.json")),StandardCharsets.UTF_8);
        RtaService service=new RtaService();
        ToTalInformation result=service.analyse(json);
        List<Map<String,Object>> all=rows(result.getTimeline());
        assertEquals(102,all.size());Set<String> ids=new HashSet<>();
        for(Map<String,Object> row:all) {
            assertTrue(ids.add((String)row.get("jobId")));
            int index=(Integer)row.get("jobIndex");
            BigDecimal expected=BigDecimal.valueOf((Double)row.get("phase"))
                    .add(BigDecimal.valueOf((Double)row.get("period")).multiply(BigDecimal.valueOf(index)));
            assertEquals(expected.doubleValue(),(Double)row.get("release"));
            double cursor=(Double)row.get("release"), interference=0, execution=0, queued=0;
            for(Map<String,Object> s:states(row)){
                double start=(Double)s.get("start"),end=(Double)s.get("end");
                assertEquals(cursor,start,1e-10);assertTrue(end>start);cursor=end;
                assertTrue(end<=(Double)row.get("observationEnd"));
                double duration=end-start;
                if("interference".equals(s.get("state"))){
                    assertNotEquals(row.get("id"),s.get("id"));interference+=duration;
                }else if("executing".equals(s.get("state"))){assertEquals(row.get("jobId"),s.get("jobId"));execution+=duration;}
                else {assertEquals("queued",s.get("state"));assertEquals(row.get("id"),s.get("id"));assertNotEquals(row.get("jobId"),s.get("jobId"));queued+=duration;}
            }
            assertEquals((Double)row.get("observationEnd"),cursor,1e-10);
            assertEquals((Double)row.get("interference"),interference,1e-10);
            assertEquals((Double)row.get("execution"),execution,1e-10);
            assertEquals((Double)row.get("queued"),queued,1e-10);
            assertEquals(cursor-(Double)row.get("release"),execution+interference+queued,1e-10);
        }
        Map<String,Object> lvgl=job(all,"4:0");
        assertEquals(0.0,lvgl.get("release"));assertEquals(43.9,lvgl.get("interference"));assertEquals(63.9,lvgl.get("completion"));
        lvgl=job(all,"4:1");
        assertEquals(33.0,lvgl.get("release"));assertEquals(63.6,lvgl.get("interference"));assertEquals(9.7,lvgl.get("queued"));
        assertEquals(20.0,lvgl.get("execution"));assertEquals(126.3,lvgl.get("completion"));
        for(int k=0;k<4;k++) {
            Map<String,Object> target=job(all,"4:"+k);
            assertEquals(true,target.get("completed"));
            assertEquals(20.0,(Double)target.get("execution"),1e-9);
            assertEquals((Double)target.get("completion")-(Double)target.get("release"),(Double)target.get("responseTime"),1e-9);
        }
        assertEquals(1000.0,result.getTimeline().get("horizon"));
        assertEquals(990.0,job(all,"4:30").get("release"));
        assertEquals(974.4,job(all,"1:29").get("release"));
        assertEquals(1000.0,job(all,"6:0").get("interference"));
        Map<String,Object> second=job(all,"2:1");
        assertEquals(33.6,second.get("release"));assertEquals(10.6,second.get("interference"));assertEquals(54.8,second.get("completion"));
        for(RtaSystemTask task:result.getSystemTasks())assertNotNull(task.getSchedulable());
        // Generated integration fixture consumed by the actual frontend adapter and Vue render test.
        Files.write(Paths.get("target/rta-response-test.json"),new ObjectMapper().writeValueAsBytes(result));
    }
    @Test void phaseReleaseFormulaAndSeparateOwnTaskQueue() {
        PeriodicTask task=new PeriodicTask();task.setId(1);task.setPriority(1);task.setPhase(.1);task.setPeriod(.3);task.setWcet(.5);
        List<Map<String,Object>> all=rows(new MixedTimelineAdapter().simulate(Arrays.asList(task),2));
        assertEquals(.4,job(all,"1:1").get("release"));
        assertEquals(.7,job(all,"1:2").get("release"));
        assertEquals(0.0,job(all,"1:1").get("interference"));
        assertEquals(.2,job(all,"1:1").get("queued"));
        assertEquals(1.1,job(all,"1:1").get("completion"));
    }
}
