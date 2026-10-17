package com.example.rta;

import com.example.rta.service.*;
import com.example.rta.service.CommonUse.PeriodicTask;
import com.example.rta.response.ToTalInformation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TimelineTests {
    private PeriodicTask task(int id,double period,double phase,double cost) {
        PeriodicTask t=new PeriodicTask();t.setId(id);t.setName("T"+id);t.setPartition(0);
        t.setPriority(id);t.setPeriod(period);t.setPhase(phase);t.setWcet(cost);t.setDeadline(period);return t;
    }
    @SuppressWarnings("unchecked")
    @Test void phasePreemptionAndCompletion() {
        Map<String,Object> data=new MixedTimelineAdapter().simulate(Arrays.asList(task(1,10,2,2),task(2,20,0,5)),10);
        List<Map<String,Object>> rows=(List<Map<String,Object>>)data.get("rows");
        Map<String,Object> low=rows.stream().filter(r->r.get("id").equals(2)).findFirst().get();
        assertEquals(2.0,(Double)low.get("interference"),1e-9);
        List<Map<String,Object>> states=(List<Map<String,Object>>)low.get("states");
        assertEquals(Arrays.asList("executing","interference","executing"),
                Arrays.asList(states.get(0).get("state"),states.get(1).get("state"),states.get(2).get("state")));
        assertEquals(2.0,(Double)states.get(1).get("start"),1e-9);
        assertEquals(4.0,(Double)states.get(1).get("end"),1e-9);
        List<Map<String,Object>> events=(List<Map<String,Object>>)low.get("events");
        assertEquals(7.0,(Double)events.get(1).get("time"),1e-9);
    }
    @SuppressWarnings("unchecked")
    @Test void realJsonAndSerialization() throws Exception {
        String json=new String(Files.readAllBytes(Paths.get("src/test/resources/fixtures/tasks.json")),java.nio.charset.StandardCharsets.UTF_8);
        RtaService service=new RtaService();
        ToTalInformation result=service.analyse(json);
        assertEquals(6,result.getSystemTasks().size());assertEquals("SwLatA",result.getSystemTasks().get(0).getName());
        String output=new ObjectMapper().writeValueAsString(result);
        assertTrue(output.contains("\"WCET\":10.6"));assertFalse(output.contains("resourceId"));
        List<Map<String,Object>> rows=(List<Map<String,Object>>)result.getTimeline().get("rows");
        Map<String,Object> second=rows.stream().filter(r->r.get("id").equals(2)).findFirst().get();
        assertEquals(10.6,(Double)second.get("interference"),1e-9);
        Map<String,Object> starved=rows.stream().filter(r->r.get("id").equals(6)).findFirst().get();
        assertEquals(false,starved.get("completed"));
        assertEquals(1000.0,(Double)starved.get("interference"),1e-8);
        List<Map<String,Object>> cpu=(List<Map<String,Object>>)result.getTimeline().get("cpu");
        double cursor=0;
        for(Map<String,Object> s:cpu){assertEquals(cursor,(Double)s.get("start"),1e-8);cursor=(Double)s.get("end");}
        assertEquals(1000.0,cursor,1e-8);
        assertEquals(cursor,(Double)result.getTimeline().get("horizon"),1e-8);
    }
    @Test void invalidInputDoesNotReturnSuccess() {
        RtaService service=new RtaService();
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.analyse("{bad"));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.analyse("{\"tasks\":[]}"));
        String legacy="{\"system\":{\"core_count\":1},\"tasks\":[{\"id\":1,\"name\":\"old\",\"function\":\"\",\"partition\":0,\"prio\":1,\"period\":10,\"phase\":0,\"deadline\":10,\"budget_ms\":2}]}";
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.analyse(legacy));
    }
}
