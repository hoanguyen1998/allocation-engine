package com.foreign.relation.editable.allocation_v2;

import com.foreign.relation.editable.Allocation;
import com.foreign.relation.editable.allocation_optimization.AllocationOptimization;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class AllocationV2 {
    // =========================
    // Obligation
    // =========================
    static class Obligation {
        BigDecimal amount;
        LocalDate date;

        Obligation(BigDecimal amount, LocalDate date) {
            this.amount = amount;
            this.date = date;
        }
    }

    // =========================
    // Allocation Rule
    // =========================
    static class AllocationRule {

        BigDecimal thresholdFrom;
        BigDecimal thresholdTo;

        LocalDate appliedFrom;
        LocalDate appliedTo;

        Map<String, BigDecimal> ratios = new HashMap<>();

        boolean matches(LocalDate date, BigDecimal cumulative) {

            if (appliedFrom != null && date.isBefore(appliedFrom)) return false;
            if (appliedTo != null && !date.isBefore(appliedTo)) return false;

            if (thresholdFrom != null && cumulative.compareTo(thresholdFrom) < 0) return false;
            if (thresholdTo != null && cumulative.compareTo(thresholdTo) >= 0) return false;

            return true;
        }
    }

    // =========================
    // Allocation Result
    // =========================
    static class AllocationResult {

        Map<String, BigDecimal> assetAmounts = new HashMap<>();
        BigDecimal unsecured = BigDecimal.ZERO;

        void add(String asset, BigDecimal value) {
            assetAmounts.merge(asset, value, BigDecimal::add);
        }

        void addUnsecured(BigDecimal value) {
            unsecured = unsecured.add(value);
        }
    }
    private final List<AllocationRule> rules;

    AllocationV2(List<AllocationRule> rules) {
        this.rules = new ArrayList<>(rules);
        validateRules();
        validateRules(rules);
    }

    private void validateRules(List<AllocationV2.AllocationRule> rules) {

        for (int i = 0; i < rules.size(); i++) {

            for (int j = i + 1; j < rules.size(); j++) {

                AllocationV2.AllocationRule r1 = rules.get(i);
                AllocationV2.AllocationRule r2 = rules.get(j);

                if (dateOverlap(r1, r2) && thresholdOverlap(r1, r2)) {

                    throw new IllegalStateException(
                            "Overlapping rules detected: " + r1 + " and " + r2
                    );
                }
            }
        }
    }

    private boolean thresholdOverlap(AllocationV2.AllocationRule r1, AllocationV2.AllocationRule r2) {

        BigDecimal from1 = r1.thresholdFrom == null ? BigDecimal.ZERO : r1.thresholdFrom;
        BigDecimal to1   = r1.thresholdTo   == null ? new BigDecimal("999999999") : r1.thresholdTo;

        BigDecimal from2 = r2.thresholdFrom == null ? BigDecimal.ZERO : r2.thresholdFrom;
        BigDecimal to2   = r2.thresholdTo   == null ? new BigDecimal("999999999") : r2.thresholdTo;

        return from1.compareTo(to2) < 0 && from2.compareTo(to1) < 0;
    }

    private boolean dateOverlap(AllocationV2.AllocationRule r1, AllocationV2.AllocationRule r2) {

        LocalDate from1 = r1.appliedFrom == null ? LocalDate.MIN : r1.appliedFrom;
        LocalDate to1   = r1.appliedTo   == null ? LocalDate.MAX : r1.appliedTo;

        LocalDate from2 = r2.appliedFrom == null ? LocalDate.MIN : r2.appliedFrom;
        LocalDate to2   = r2.appliedTo   == null ? LocalDate.MAX : r2.appliedTo;

        return !from1.isAfter(to2) && !from2.isAfter(to1);
    }

    List<AllocationResult> allocate(List<Obligation> obligations) {

        List<AllocationResult> results = new ArrayList<>();

        BigDecimal cumulative = BigDecimal.ZERO;

        for (Obligation ob : obligations) {

            BigDecimal start = cumulative;
            BigDecimal end = cumulative.add(ob.amount);

            AllocationResult result = new AllocationResult();

            while (start.compareTo(end) < 0) {

                AllocationRule rule = findRule(ob.date, start);

                BigDecimal nextBreak = end;

                if (rule != null && rule.thresholdTo != null) {
                    nextBreak = nextBreak.min(rule.thresholdTo);
                }

                if (rule == null) {

                    BigDecimal nextThreshold = findNextThreshold(start);

                    if (nextThreshold != null) {
                        nextBreak = nextBreak.min(nextThreshold);
                    }
                }

                BigDecimal portion = nextBreak.subtract(start);

                if (rule == null) {

                    result.addUnsecured(portion);

                } else {

                    BigDecimal ratioSum = BigDecimal.ZERO;

                    for (Map.Entry<String, BigDecimal> e : rule.ratios.entrySet()) {

                        BigDecimal value = portion.multiply(e.getValue());

                        result.add(e.getKey(), value);

                        ratioSum = ratioSum.add(e.getValue());
                    }

                    if (ratioSum.compareTo(BigDecimal.ONE) < 0) {

                        BigDecimal unsecured = portion.multiply(
                                BigDecimal.ONE.subtract(ratioSum)
                        );

                        result.addUnsecured(unsecured);
                    }
                }

                start = nextBreak;
            }

            cumulative = end;

            results.add(result);
        }

        return results;
    }

    private AllocationRule findRule(LocalDate date, BigDecimal cumulative) {
        for (AllocationRule r : rules) {
            if (r.matches(date, cumulative)) {
                return r;
            }
        }

        return null;
    }

    private BigDecimal findNextThreshold(BigDecimal cumulative) {

        BigDecimal next = null;

        for (AllocationRule r : rules) {

            if (r.thresholdFrom != null &&
                    r.thresholdFrom.compareTo(cumulative) > 0) {

                if (next == null ||
                        r.thresholdFrom.compareTo(next) < 0) {

                    next = r.thresholdFrom;
                }
            }
        }

        return next;
    }

    private void validateRules() {

        for (AllocationRule r : rules) {

            BigDecimal sum = BigDecimal.ZERO;

            for (BigDecimal v : r.ratios.values()) {
                sum = sum.add(v);
            }

            if (sum.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException(
                        "Ratio sum > 1 in rule"
                );
            }
        }
    }

//    public static void main(String[] args) {
//
//        List<AllocationRule> rules = new ArrayList<>();
//
//        // ===== Rules before 2024-01-03 =====
//
//        AllocationRule r1 = new AllocationRule();
//        r1.appliedFrom = LocalDate.of(2024,1,1);
//        r1.appliedTo = LocalDate.of(2024,1,3);
//        r1.thresholdFrom = new BigDecimal("1");
//        r1.thresholdTo = new BigDecimal("5");
//        r1.ratios.put("A", new BigDecimal("1.0"));
//
//        AllocationRule r2 = new AllocationRule();
//        r2.appliedFrom = LocalDate.of(2024,1,1);
//        r2.appliedTo = LocalDate.of(2024,1,3);
//        r2.thresholdFrom = new BigDecimal("5");
//        r2.thresholdTo = new BigDecimal("10");
//        r2.ratios.put("A", new BigDecimal("0.7"));
//        r2.ratios.put("B", new BigDecimal("0.3"));
//
//        // ===== Rules from 2024-01-03 =====
//
//        AllocationRule r3 = new AllocationRule();
//        r3.appliedFrom = LocalDate.of(2024,1,3);
//        r3.thresholdFrom = new BigDecimal("1");
//        r3.thresholdTo = new BigDecimal("5");
//        r3.ratios.put("B", new BigDecimal("1.0"));
//
//        AllocationRule r4 = new AllocationRule();
//        r4.appliedFrom = LocalDate.of(2024,1,3);
//        r4.thresholdFrom = new BigDecimal("5");
//        r4.thresholdTo = new BigDecimal("10");
//        r4.ratios.put("B", new BigDecimal("0.5"));
//        r4.ratios.put("C", new BigDecimal("0.5"));
//
//        rules.add(r1);
//        rules.add(r2);
//        rules.add(r3);
//        rules.add(r4);
//
//        AllocationV2 engine = new AllocationV2(rules);
//
//        // ===== Obligations =====
//
//        List<Obligation> obligations = Arrays.asList(
//
//                new Obligation(
//                        new BigDecimal("4"),
//                        LocalDate.of(2024,1,2)
//                ),
//
//                new Obligation(
//                        new BigDecimal("4"),
//                        LocalDate.of(2024,1,3)
//                )
//        );
//
//        List<AllocationResult> results = engine.allocate(obligations);
//
//        // ===== Print results =====
//
//        for (int i = 0; i < results.size(); i++) {
//
//            System.out.println("Obligation " + (i + 1));
//
//            AllocationResult r = results.get(i);
//
//            for (Map.Entry<String, BigDecimal> e : r.assetAmounts.entrySet()) {
//                System.out.println(e.getKey() + " = " + e.getValue());
//            }
//
//            System.out.println("Unsecured = " + r.unsecured);
//            System.out.println();
//        }
//    }

        public static void main(String[] args) {

        List<AllocationRule> rules = new ArrayList<>();
//
//        AllocationRule rule1 = new AllocationRule();
//        rule1.thresholdFrom = BigDecimal.ONE;
//        rule1.thresholdTo = new BigDecimal(5);
//        rule1.ratios.put("A", BigDecimal.ONE);
//
//        AllocationRule rule2 = new AllocationRule();
//        rule2.thresholdFrom = new BigDecimal(5);
//        rule2.thresholdTo = new BigDecimal(10);
//        rule2.ratios.put("A", new BigDecimal("0.7"));
//        rule2.ratios.put("B", new BigDecimal("0.3"));
//
//        rules.add(rule1);
//        rules.add(rule2);
//
//        AllocationV2 engine = new AllocationV2(rules);
//
//        List<Obligation> obligations = Arrays.asList(
//                new Obligation(new BigDecimal(2), LocalDate.of(2024,1,1)),
//                new Obligation(new BigDecimal(2), LocalDate.of(2024,1,2)),
//                new Obligation(new BigDecimal(2), LocalDate.of(2024,1,3)),
//                new Obligation(new BigDecimal(3), LocalDate.of(2024,1,4)),
//                new Obligation(new BigDecimal(5), LocalDate.of(2024,1,5))
//        );

        // -------- Rules before 2024-01-04

        AllocationRule r1 = new AllocationRule();
        r1.appliedFrom = LocalDate.of(2024,1,1);
        r1.appliedTo = LocalDate.of(2024,1,4);
        r1.thresholdFrom = new BigDecimal("1");
        r1.thresholdTo = new BigDecimal("5");
        r1.ratios.put("A", new BigDecimal("1.0"));

        AllocationRule r2 = new AllocationRule();
        r2.appliedFrom = LocalDate.of(2024,1,1);
        r2.appliedTo = LocalDate.of(2024,1,4);
        r2.thresholdFrom = new BigDecimal("5");
        r2.thresholdTo = new BigDecimal("10");
        r2.ratios.put("A", new BigDecimal("0.7"));
        r2.ratios.put("B", new BigDecimal("0.3"));

        // -------- Rules from 2024-01-04

        AllocationRule r3 = new AllocationRule();
        r3.appliedFrom = LocalDate.of(2024,1,4);
        r3.thresholdFrom = new BigDecimal("1");
        r3.thresholdTo = new BigDecimal("5");
        r3.ratios.put("B", new BigDecimal("1.0"));

        AllocationRule r4 = new AllocationRule();
        r4.appliedFrom = LocalDate.of(2024,1,4);
        r4.thresholdFrom = new BigDecimal("5");
        r4.thresholdTo = new BigDecimal("10");
        r4.ratios.put("B", new BigDecimal("0.5"));
        r4.ratios.put("C", new BigDecimal("0.5"));

        rules.add(r1);
        rules.add(r2);
        rules.add(r3);
        rules.add(r4);

        AllocationV2 engine = new AllocationV2(rules);

        List<Obligation> obligations = List.of(

                new Obligation(new BigDecimal("3"), LocalDate.of(2024,1,1)),
                new Obligation(new BigDecimal("4"), LocalDate.of(2024,1,2)),
                new Obligation(new BigDecimal("3"), LocalDate.of(2024,1,4)),
                new Obligation(new BigDecimal("4"), LocalDate.of(2024,1,5))
        );

        List<AllocationResult> results = engine.allocate(obligations);

        for (int i = 0; i < results.size(); i++) {

            System.out.println("Obligation " + (i+1));

            AllocationResult r = results.get(i);

            for (Map.Entry<String, BigDecimal> e : r.assetAmounts.entrySet()) {
                System.out.println(e.getKey() + " = " + e.getValue());
            }

            System.out.println("Unsecured = " + r.unsecured);

            System.out.println();
        }
    }
}
