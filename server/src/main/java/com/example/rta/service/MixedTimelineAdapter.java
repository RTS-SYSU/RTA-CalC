package com.example.rta.service;

import com.example.rta.service.CommonUse.PeriodicTask;
import com.example.serveside.service.CommonUse.BasicPCB;
import com.example.serveside.service.msrp.MSRPMixedCriticalSystem;
import com.example.serveside.service.msrp.ProcedureControlBlock;
import com.example.serveside.response.EventInformation;
import com.example.serveside.response.EventTimePoint;
import com.example.serveside.response.TaskInformation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/** Input/output adapter ONLY. All job lifecycle and execution comes from mixed's original MSRP engine.
 * No resources, no criticality switch. The completion driver keeps periodic releases active until
 * the selected initial jobs finish, ignoring SystemExecute's stop-on-miss policy.
 * Integer ticks are an exact common decimal quantum, never rounded milliseconds.
 */
public class MixedTimelineAdapter {
    private static final int MAX_TICKS = 2000000;
    private static BigDecimal decimal(double v) { return BigDecimal.valueOf(v); }
    private static BigDecimal phase(PeriodicTask t) { return decimal(t.getPhase() == null ? 0 : t.getPhase()); }
    private static BigDecimal quantum(List<PeriodicTask> tasks, double horizon) {
        List<BigDecimal> values = new ArrayList<>(); values.add(decimal(horizon));
        for (PeriodicTask t : tasks) { values.add(phase(t)); values.add(decimal(t.getPeriod()));
            values.add(decimal(t.getWcet())); values.add(decimal(t.deadlineOrDefault())); }
        int scale = 0; for (BigDecimal v : values) scale = Math.max(scale, v.stripTrailingZeros().scale());
        BigInteger gcd = BigInteger.ZERO;
        for (BigDecimal v : values) gcd = gcd.gcd(v.movePointRight(scale).toBigIntegerExact());
        return new BigDecimal(gcd, scale);
    }
    private static int ticks(BigDecimal v, BigDecimal q) { return v.divide(q).intValueExact(); }
    private static double ms(int ticks, BigDecimal q) { return q.multiply(BigDecimal.valueOf(ticks)).doubleValue(); }
    private static Map<String,Object> event(EventTimePoint e, List<PeriodicTask> tasks, Map<Integer,String> ids, BigDecimal q) {
        Map<String,Object> m = new LinkedHashMap<>(); m.put("event",e.getEvent()); m.put("time",ms(e.getEventTime(),q));
        m.put("id",tasks.get(e.getStaticPid()).getId()); m.put("jobId",ids.get(e.getDynamicPid())); return m;
    }
    private static Map<String,Object> state(String type, int start, int end, EventInformation owner,
                                           List<PeriodicTask> tasks, Map<Integer,String> ids, BigDecimal q) {
        Map<String,Object> m = new LinkedHashMap<>(); m.put("state",type); m.put("start",ms(start,q)); m.put("end",ms(end,q));
        m.put("id",owner.getStaticPid()<0?null:tasks.get(owner.getStaticPid()).getId());
        m.put("taskId",owner.getStaticPid()<0?null:tasks.get(owner.getStaticPid()).getId());
        m.put("jobId",ids.get(owner.getDynamicPid())); return m;
    }
    public Map<String,Object> simulate(List<PeriodicTask> input, double horizon) {
        return simulate(input,horizon,Collections.emptySet());
    }
    public Map<String,Object> simulate(List<PeriodicTask> input, double horizon, Set<Integer> completionTaskIds) {
        if (!Double.isFinite(horizon) || horizon <= 0) horizon = 100;
        List<PeriodicTask> tasks = new ArrayList<>(input);
        tasks.sort(Comparator.comparingDouble(PeriodicTask::getPriority).thenComparing(PeriodicTask::getId));
        BigDecimal q = quantum(tasks,horizon);
        int limit;
        try { limit = ticks(decimal(horizon),q); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("mixed 整数时钟适配超出范围，不能舍入输入",e); }
        int releaseWindow=limit;
        int targetCount=0;
        for(PeriodicTask target:tasks) if(completionTaskIds.contains(target.getId())) {
            int jobs=Math.max(0,decimal(horizon).subtract(phase(target)).divide(decimal(target.getPeriod()),
                    0,java.math.RoundingMode.CEILING).intValueExact());
            targetCount+=jobs;
            double utilization=0,burst=0;
            for(PeriodicTask hp:tasks) if(hp.getPriority()<target.getPriority()) {
                utilization+=hp.getWcet()/hp.getPeriod();burst+=hp.getWcet();
            }
            if(jobs>0 && utilization>=1) throw new IllegalArgumentException("目标任务 "+target.getId()+" 的高优先级负载 >= 1，不能保证完成");
            if(jobs>0) {
                double bound=horizon+(jobs*target.getWcet()+burst)/(1-utilization);
                limit=Math.max(limit,decimal(bound).divide(q,0,java.math.RoundingMode.CEILING).intValueExact());
            }
        }
        if (limit > MAX_TICKS) throw new IllegalArgumentException("精确模拟超过 2000000 ticks；不会返回伪造完成结果");
        ArrayList<BasicPCB> pcbs = new ArrayList<>();
        TreeMap<Integer,ArrayList<Integer>> releases = new TreeMap<>(); int count = 0;
        for (int i=0;i<tasks.size();i++) {
            PeriodicTask t=tasks.get(i);
            if (t.getWcet()<=0 || t.getPeriod()<=0) throw new IllegalArgumentException("模拟要求 period/WCET > 0");
            // Rank mapping also handles fractional/negative external priorities without integer overflow.
            // Smaller external priority -> larger mixed priority. No mixed comparator is changed.
            BasicPCB pcb=new BasicPCB(tasks.size()-i,ticks(decimal(t.getPeriod()),q),0,i);
            pcb.totalNeededTime=ticks(decimal(t.getWcet()),q); pcb.WCCT_low=pcb.WCCT_high=pcb.totalNeededTime;
            pcb.deadline=ticks(decimal(t.deadlineOrDefault()),q); pcb.baseRunningCpuCore=0; pcbs.add(pcb);
            for(int k=0;;k++) {
                BigDecimal r=phase(t).add(decimal(t.getPeriod()).multiply(BigDecimal.valueOf(k)));
                if(r.compareTo(q.multiply(BigDecimal.valueOf(limit)))>=0)break;
                if(++count>20000)throw new IllegalArgumentException("时间窗口内作业数量超过 20000");
                releases.computeIfAbsent(ticks(r,q),key->new ArrayList<>()).add(i);
            }
        }
        // Original mixed engine uses static state. Serialize full runs, not individual tick calls.
        synchronized (MSRPMixedCriticalSystem.class) {
            MSRPMixedCriticalSystem.itemTaskReleaseTimes=null;
            MSRPMixedCriticalSystem.MSRPInitialize(pcbs,new ArrayList<>(),1,releases,false,-1);
            MSRPMixedCriticalSystem.ChangeCpuTaskState(0,null,"");
            MSRPMixedCriticalSystem.ReleaseTasks(); prepareReady(pcbs); MSRPMixedCriticalSystem.ChooseTaskToRun();
            while(MSRPMixedCriticalSystem.systemClock<limit) {
                MSRPMixedCriticalSystem.ExecuteTasks();
                MSRPMixedCriticalSystem.IncreaseElapsedTime();
                MSRPMixedCriticalSystem.ReleaseTasks(); prepareReady(pcbs);
                MSRPMixedCriticalSystem.ChooseTaskToRun();
                if(targetCount>0 && MSRPMixedCriticalSystem.systemClock>=releaseWindow
                        && completedTargets(tasks,completionTaskIds,releaseWindow)==targetCount)break;
            }
            if(targetCount>0 && completedTargets(tasks,completionTaskIds,releaseWindow)!=targetCount)
                throw new IllegalStateException("目标 job 未全部完成，不能将截断结果当作完整模拟");
            // Close open records using mixed's own record writers; no fabricated completion event.
            for(ProcedureControlBlock t:MSRPMixedCriticalSystem.waitingTasksPerCore.get(0))
                MSRPMixedCriticalSystem.ChangeTaskState(t,"completion");
            ProcedureControlBlock running=MSRPMixedCriticalSystem.runningTaskPerCore.get(0);
            if(running!=null)MSRPMixedCriticalSystem.ChangeTaskState(running,"completion");
            MSRPMixedCriticalSystem.ChangeCpuTaskState(0,null,"");
            return decorate(tasks,q,MSRPMixedCriticalSystem.systemClock,horizon);
        }
    }
    private static int completedTargets(List<PeriodicTask> tasks,Set<Integer> targetIds,int releaseWindow) {
        int count=0;
        for(TaskInformation job:MSRPMixedCriticalSystem.releaseTaskInformations)
            if(job.getReleaseTime()<releaseWindow && targetIds.contains(tasks.get(job.getStaticPid()).getId())
                    && MSRPMixedCriticalSystem.TaskEventTimePointsRecords.get(job.getDynamicPid()).stream()
                    .anyMatch(e->"completion".equals(e.getEvent())))count++;
        return count;
    }
    private static void prepareReady(ArrayList<BasicPCB> pcbs) {
        // mixed's stable priority sort needs a FIFO tie-order seed for multiple jobs of one task.
        // This does not choose a CPU owner or perform preemption; ChooseTaskToRun does both.
        MSRPMixedCriticalSystem.waitingTasksPerCore.get(0).sort(Comparator.comparingInt(t->t.basicPCB.dynamicTaskId));
        // BasicPCB's original copy constructor defaults deadline to period: restore input metadata.
        for(ProcedureControlBlock t:MSRPMixedCriticalSystem.waitingTasksPerCore.get(0))
            t.basicPCB.deadline=pcbs.get(t.basicPCB.staticTaskId).deadline;
    }
    private static Map<String,Object> decorate(List<PeriodicTask> tasks,BigDecimal q,int limit,double horizon) {
        // Packaging and both primary segment collections are generated by mixed, not this adapter.
        com.example.serveside.response.ToTalInformation raw=MSRPMixedCriticalSystem.PackageTotalInformation();
        Map<Integer,String> ids=new HashMap<>(); Map<Integer,Integer> indices=new HashMap<>();
        for(TaskInformation job:MSRPMixedCriticalSystem.releaseTaskInformations) {
            int i=job.getStaticPid(),k=indices.getOrDefault(i,0); indices.put(i,k+1);
            ids.put(job.getDynamicPid(),tasks.get(i).getId()+":"+k);
        }
        List<Map<String,Object>> cpu=new ArrayList<>(),events=new ArrayList<>(),rows=new ArrayList<>();
        List<EventInformation> segments=raw.getCpuGanttInformations().get(0).getEventInformations();
        for(EventInformation s:segments) if(s.getEndTime()>s.getStartTime())
            cpu.add(state(s.getStaticPid()<0?"idle":"executing",s.getStartTime(),s.getEndTime(),s,tasks,ids,q));
        for(EventTimePoint e:raw.getCpuGanttInformations().get(0).getEventTimePoints())events.add(event(e,tasks,ids,q));
        indices.clear();
        for(TaskInformation job:MSRPMixedCriticalSystem.releaseTaskInformations) {
            int i=job.getStaticPid(),dynamic=job.getDynamicPid(),k=indices.getOrDefault(i,0); indices.put(i,k+1);
            PeriodicTask t=tasks.get(i); int release=job.getReleaseTime(),end=limit; Integer completion=null;
            List<Map<String,Object>> jobEvents=new ArrayList<>(),states=new ArrayList<>();
            for(EventTimePoint e:MSRPMixedCriticalSystem.TaskEventTimePointsRecords.get(dynamic)) {
                jobEvents.add(event(e,tasks,ids,q)); if("completion".equals(e.getEvent()))completion=e.getEventTime();
            }
            events.add(jobEvents.get(0));
            if(ms(release,q)>=horizon)continue;
            if(completion!=null)end=completion;
            long interference=0,queued=0,execution=0;
            // Execution segments come directly from mixed's task records.
            for(EventInformation s:MSRPMixedCriticalSystem.TaskEventInformationRecords.get(dynamic))
                if("normal-execution".equals(s.getState()) && s.getEndTime()>s.getStartTime()) {
                    states.add(state("executing",s.getStartTime(),s.getEndTime(),s,tasks,ids,q));
                    execution+=s.getEndTime()-s.getStartTime();
                }
            // Annotation only: intersect ready lifetime with OTHER recorded CPU execution.
            for(EventInformation s:segments) {
                int a=Math.max(release,s.getStartTime()),b=Math.min(end,s.getEndTime());
                if(b<=a || s.getStaticPid()<0 || s.getDynamicPid()==dynamic)continue;
                boolean own=s.getStaticPid()==i;
                states.add(state(own?"queued":"interference",a,b,s,tasks,ids,q));
                if(own)queued+=b-a;else interference+=b-a;
            }
            states.sort(Comparator.comparingDouble(s->(Double)s.get("start")));
            if(completion!=null && execution!=ticks(decimal(t.getWcet()),q))
                throw new IllegalStateException("job "+ids.get(dynamic)+" 执行总量与 WCET 不一致");
            Map<String,Object> row=new LinkedHashMap<>(); row.put("id",t.getId()); row.put("name",t.getName()); row.put("partition",t.getPartition());
            row.put("jobId",ids.get(dynamic));row.put("jobIndex",k);row.put("phase",t.getPhase());row.put("period",t.getPeriod());
            row.put("release",ms(release,q));row.put("interference",ms(Math.toIntExact(interference),q));row.put("queued",ms(Math.toIntExact(queued),q));
            row.put("execution",ms(Math.toIntExact(execution),q));row.put("completion",completion==null?null:ms(completion,q));
            row.put("observationEnd",ms(end,q));row.put("completed",completion!=null);row.put("states",states);row.put("events",jobEvents); rows.add(row);
            row.put("responseTime",completion==null?null:ms(completion-release,q));
        }
        events.sort(Comparator.comparingDouble(e->(Double)e.get("time")));
        Map<String,Object> result=new LinkedHashMap<>();result.put("schemaVersion",3);result.put("horizon",ms(limit,q));
        result.put("releaseWindow",horizon);
        result.put("engine","mixed/MSRPMixedCriticalSystem");result.put("tickMs",q.doubleValue());
        result.put("cpu",cpu);result.put("events",events);result.put("rows",rows);return result;
    }
}
