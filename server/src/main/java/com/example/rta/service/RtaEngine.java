package com.example.rta.service;

import com.example.rta.service.CommonUse.PeriodicTask;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Single-core fully preemptive fixed-priority RTA, B = 0.
 * Solves the requested single-job critical-instant recurrence:
 * R = C + sum(ceil(R / T_hp) * C_hp).
 * After a deadline miss, the fixed point is diagnostic; in an overloaded system
 * it must not be interpreted as a bound for every successive queued job.
 * All arithmetic uses the input decimal milliseconds without epsilon offsets.
 */
public class RtaEngine {
    private static final int MAX_ITERATION = 10000;

    public static class Result {
        public double responseTime = Double.NaN;
        public boolean starved;
        public boolean schedulable;
        public boolean converged;
        public boolean deadlineExceeded;
        public boolean nonConvergent;
        public BigDecimal lastIterationValue;
        public int iterations;
        public String note = "";
        public List<Map<String,Object>> higherPriorityTasks = new ArrayList<>();
        public List<Map<String,Object>> trace = new ArrayList<>();
    }

    private static BigDecimal decimal(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("RTA 参数必须为有限数值");
        return BigDecimal.valueOf(value);
    }

    // Compare sum(C/T) against 1 as an exact rational, not a rounded double.
    private static boolean higherPrioritySaturates(List<PeriodicTask> hp) {
        BigInteger numerator = BigInteger.ZERO, denominator = BigInteger.ONE;
        for (PeriodicTask task : hp) {
            BigDecimal c = decimal(task.getWcet()), t = decimal(task.getPeriod());
            int scale = Math.max(c.scale(), t.scale());
            BigInteger cost = c.movePointRight(scale).toBigIntegerExact();
            BigInteger period = t.movePointRight(scale).toBigIntegerExact();
            numerator = numerator.multiply(period).add(cost.multiply(denominator));
            denominator = denominator.multiply(period);
            BigInteger gcd = numerator.gcd(denominator);
            numerator = numerator.divide(gcd); denominator = denominator.divide(gcd);
        }
        return numerator.compareTo(denominator) >= 0;
    }

    public static Result worstCaseResponseTime(List<PeriodicTask> tasks, int index) {
        PeriodicTask task = tasks.get(index);
        BigDecimal cost = decimal(task.getWcet());
        BigDecimal deadline = decimal(task.deadlineOrDefault());
        if (cost.signum() <= 0 || task.getPeriod() <= 0 || deadline.signum() < 0)
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
        boolean unbounded = higherPrioritySaturates(hp);
        result.nonConvergent = unbounded;

        BigDecimal response = cost;
        for (int iteration = 1; iteration <= MAX_ITERATION; iteration++) {
            result.iterations = iteration;
            BigDecimal next = cost;
            List<Map<String,Object>> contributions = new ArrayList<>();
            for (PeriodicTask higher : hp) {
                BigDecimal releases = response.divide(decimal(higher.getPeriod()), 0, RoundingMode.CEILING);
                BigDecimal interference = releases.multiply(decimal(higher.getWcet()));
                next = next.add(interference);
                Map<String,Object> h = new LinkedHashMap<>();
                h.put("id",higher.getId()); h.put("name",higher.getName()); h.put("WCET",higher.getWcet());
                h.put("period",higher.getPeriod()); h.put("count",releases); h.put("interference",interference);
                contributions.add(h);
            }
            Map<String,Object> step = new LinkedHashMap<>(); step.put("iteration",iteration);
            step.put("response",response); step.put("next",next); step.put("contributions",contributions);
            result.trace.add(step);
            result.lastIterationValue = next;
            if (Double.isFinite(next.doubleValue())) result.responseTime = next.doubleValue();
            result.deadlineExceeded = unbounded || next.compareTo(deadline) > 0;
            if (unbounded && next.compareTo(deadline) > 0) {
                // F(R) >= C + U_hp * R > R: no finite fixed point. Keep actual rounds up to miss.
                result.starved = true;
                result.note = "miss / non-convergent：高优先级利用率 >= 1，已证明无有限不动点；显示最后实际迭代值 "
                        + next.toPlainString() + " ms，不是收敛 WCRT。保留迭代至首次超过 deadline 的全部明细";
                return result;
            }
            if (next.compareTo(response) == 0) {
                result.converged = true;
                result.responseTime = next.doubleValue();
                if (!Double.isFinite(result.responseTime)) {
                    result.responseTime = Double.NaN;
                    result.note = "已收敛，但响应时间超出接口数值范围";
                    return result;
                }
                result.schedulable = !result.deadlineExceeded;
                result.note = result.schedulable ? "指定递推收敛，R <= deadline"
                        : "指定递推收敛，但 R > deadline；数值仅描述临界时刻首个作业";
                return result;
            }
            response = next;
        }
        if (unbounded) {
            result.starved = true;
            result.note = "高优先级利用率 >= 1，已证明无有限不动点；详细迭代达到上限";
            return result;
        }
        result.nonConvergent = true;
        result.note = "达到迭代上限，未证明收敛；显示最后实际迭代值，不是已求得 WCRT"
                + (result.deadlineExceeded ? "，已证明超过 deadline" : "");
        return result;
    }
}

