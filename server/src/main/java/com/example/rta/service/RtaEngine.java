package com.example.rta.service;

import com.example.rta.service.CommonUse.PeriodicTask;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/** Fully preemptive fixed-priority RTA without blocking, including arbitrary deadlines.
 * For each job q in the level-i busy period, solve
 * w(q) = (q + 1) C_i + sum(ceil(w(q) / T_j) C_j), then R(q) = w(q) - q T_i.
 * The busy period ends when w(q) <= (q + 1) T_i. WCRT is the maximum R(q).
 * A finite iteration limit is an unknown result, never a schedulability proof.
 */
public class RtaEngine {
    private static final int MAX_ITERATION = 10000;

    public static class Result {
        // Set only after proving a finite WCRT for the complete busy period.
        public double responseTime = Double.NaN;
        public boolean starved;
        public boolean unbounded;
        public boolean schedulable;
        public boolean converged;
        public boolean deadlineExceeded;
        public boolean nonConvergent;
        public BigDecimal lastIterationValue;
        public int iterations;
        public String note = "";
        public List<Map<String,Object>> higherPriorityTasks = new ArrayList<>();
        // Detailed recurrence for q=0; later jobs have compact completion/response records.
        public List<Map<String,Object>> trace = new ArrayList<>();
        public List<Map<String,Object>> jobResponses = new ArrayList<>();
    }

    private static BigDecimal decimal(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("RTA 参数必须为有限数值");
        return BigDecimal.valueOf(value);
    }

    private static int compareUtilizationToOne(List<PeriodicTask> tasks) {
        BigInteger numerator = BigInteger.ZERO, denominator = BigInteger.ONE;
        for (PeriodicTask task : tasks) {
            BigDecimal c = decimal(task.getWcet()), t = decimal(task.getPeriod());
            int scale = Math.max(c.scale(), t.scale());
            BigInteger cost = c.movePointRight(scale).toBigIntegerExact();
            BigInteger period = t.movePointRight(scale).toBigIntegerExact();
            numerator = numerator.multiply(period).add(cost.multiply(denominator));
            denominator = denominator.multiply(period);
            BigInteger gcd = numerator.gcd(denominator);
            numerator = numerator.divide(gcd); denominator = denominator.divide(gcd);
        }
        return numerator.compareTo(denominator);
    }

    public static boolean utilizationAtMostOne(List<PeriodicTask> tasks) {
        return compareUtilizationToOne(tasks) <= 0;
    }

    public static Result worstCaseResponseTime(List<PeriodicTask> tasks, int index) {
        PeriodicTask task = tasks.get(index);
        BigDecimal cost = decimal(task.getWcet()), period = decimal(task.getPeriod());
        BigDecimal deadline = decimal(task.deadlineOrDefault());
        if (cost.signum() <= 0 || period.signum() <= 0 || deadline.signum() < 0)
            throw new IllegalArgumentException("RTA 要求 WCET/period > 0，deadline >= 0");
        List<PeriodicTask> hp = new ArrayList<>();
        for (PeriodicTask other : tasks) {
            if (other == task) continue;
            if (other.getPriority() == task.getPriority())
                throw new IllegalArgumentException("固定优先级 RTA 要求任务 priority 唯一");
            if (other.getPriority() < task.getPriority()) hp.add(other);
        }
        Result result = new Result();
        hp.sort(java.util.Comparator.comparingDouble(PeriodicTask::getPriority));
        for (PeriodicTask higher : hp) {
            Map<String,Object> h = new LinkedHashMap<>();
            h.put("id",higher.getId()); h.put("name",higher.getName()); h.put("priority",higher.getPriority());
            h.put("WCET",higher.getWcet()); h.put("period",higher.getPeriod()); result.higherPriorityTasks.add(h);
        }
        result.starved = compareUtilizationToOne(hp) >= 0;
        List<PeriodicTask> level = new ArrayList<>(hp); level.add(task);
        result.unbounded = compareUtilizationToOne(level) > 0;
        result.deadlineExceeded = result.unbounded;
        BigDecimal previousCompletion = BigDecimal.ZERO, worst = BigDecimal.ZERO;
        for (int job = 0; result.iterations < MAX_ITERATION; job++) {
            BigDecimal ownCost = cost.multiply(BigDecimal.valueOf(job + 1L));
            BigDecimal release = period.multiply(BigDecimal.valueOf(job));
            BigDecimal window = ownCost.max(previousCompletion.add(cost));
            boolean fixed = false;
            while (result.iterations < MAX_ITERATION) {
                result.iterations++;
                BigDecimal next = ownCost;
                List<Map<String,Object>> contributions = new ArrayList<>();
                for (PeriodicTask higher : hp) {
                    BigDecimal releases = window.divide(decimal(higher.getPeriod()), 0, RoundingMode.CEILING);
                    BigDecimal interference = releases.multiply(decimal(higher.getWcet()));
                    next = next.add(interference);
                    if (job == 0) {
                        Map<String,Object> h = new LinkedHashMap<>();
                        h.put("id",higher.getId()); h.put("name",higher.getName()); h.put("WCET",higher.getWcet());
                        h.put("period",higher.getPeriod()); h.put("count",releases); h.put("interference",interference);
                        contributions.add(h);
                    }
                }
                if (job == 0) {
                    Map<String,Object> step = new LinkedHashMap<>(); step.put("iteration",result.iterations);
                    step.put("response",window); step.put("next",next); step.put("contributions",contributions);
                    result.trace.add(step);
                }
                result.lastIterationValue = next.subtract(release);
                result.deadlineExceeded |= result.lastIterationValue.compareTo(deadline) > 0;
                if (result.starved && result.lastIterationValue.compareTo(deadline) > 0) return unbounded(result);
                fixed = next.compareTo(window) == 0;
                window = next;
                if (fixed) break;
            }
            if (!fixed) break;
            Map<String,Object> jobResult = new LinkedHashMap<>();
            jobResult.put("jobIndex",job); jobResult.put("release",release);
            jobResult.put("completion",window); jobResult.put("responseTime",window.subtract(release));
            result.jobResponses.add(jobResult);
            if (result.unbounded) return unbounded(result);
            worst = worst.max(window.subtract(release));
            if (window.compareTo(release.add(period)) <= 0) {
                if (!Double.isFinite(worst.doubleValue())) break;
                result.responseTime = worst.doubleValue();
                result.converged = true;
                result.schedulable = worst.compareTo(deadline) <= 0;
                result.note = "已分析忙期内 " + (job + 1) + " 个作业，WCRT = " + worst.toPlainString()
                        + (result.schedulable ? " ms，满足 deadline" : " ms，超过 deadline");
                return result;
            }
            previousCompletion = window;
        }
        if (result.unbounded) return unbounded(result);
        result.nonConvergent = true;
        result.note = "达到分析上限，尚未确定 WCRT；迭代末值仅供诊断"
                + (result.deadlineExceeded ? "，已证明超过 deadline" : "，可调度性未确定");
        return result;
    }

    private static Result unbounded(Result result) {
        result.nonConvergent = true;
        result.note = result.starved ? "高优先级利用率 >= 1，无有限 WCRT；迭代末值仅供诊断"
                : "本任务及高优先级任务总利用率 > 1，后续作业排队持续增长，无有限 WCRT；首作业结果仅供诊断";
        return result;
    }
}
