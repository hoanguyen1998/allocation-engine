package com.foreign.relation.editable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class Allocation {

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

    // =========================
    // Allocation Engine
    // =========================
    static class AllocationEngine {

        List<AllocationRule> rules;

        AllocationEngine(List<AllocationRule> rules) {
            this.rules = rules;
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

                    if (rule != null) {

                        if (rule.thresholdTo != null) {
                            nextBreak = nextBreak.min(rule.thresholdTo);
                        }

                    } else {

                        BigDecimal nextThreshold = findNextThreshold(start);

                        if (nextThreshold != null) {
                            nextBreak = end.min(nextThreshold);
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

                        BigDecimal unsecured = portion.multiply(BigDecimal.ONE.subtract(ratioSum));

                        result.addUnsecured(unsecured);
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

                if (r.thresholdFrom != null && r.thresholdFrom.compareTo(cumulative) > 0) {

                    if (next == null || r.thresholdFrom.compareTo(next) < 0) {
                        next = r.thresholdFrom;
                    }
                }
            }

            return next;
        }
    }

    // =========================
    // Main demo
    // =========================
//    public static void main(String[] args) {
//
//        List<AllocationRule> rules = new ArrayList<>();
//
////        AllocationRule rule1 = new AllocationRule();
////        rule1.thresholdFrom = BigDecimal.ONE;
////        rule1.thresholdTo = new BigDecimal(5);
////        rule1.ratios.put("A", BigDecimal.ONE);
////
////        AllocationRule rule2 = new AllocationRule();
////        rule2.thresholdFrom = new BigDecimal(5);
////        rule2.thresholdTo = new BigDecimal(10);
////        rule2.ratios.put("A", new BigDecimal("0.7"));
////        rule2.ratios.put("B", new BigDecimal("0.3"));
////
////        rules.add(rule1);
////        rules.add(rule2);
////
////        AllocationEngine engine = new AllocationEngine(rules);
////
////        List<Obligation> obligations = Arrays.asList(
////                new Obligation(new BigDecimal(2), LocalDate.of(2024,1,1)),
////                new Obligation(new BigDecimal(2), LocalDate.of(2024,1,2)),
////                new Obligation(new BigDecimal(2), LocalDate.of(2024,1,3)),
////                new Obligation(new BigDecimal(3), LocalDate.of(2024,1,4)),
////                new Obligation(new BigDecimal(5), LocalDate.of(2024,1,5))
////        );
//
//        // -------- Rules before 2024-01-04
//
//        AllocationRule r1 = new AllocationRule();
//        r1.appliedFrom = LocalDate.of(2024,1,1);
//        r1.appliedTo = LocalDate.of(2024,1,4);
//        r1.thresholdFrom = new BigDecimal("1");
//        r1.thresholdTo = new BigDecimal("5");
//        r1.ratios.put("A", new BigDecimal("1.0"));
//
//        AllocationRule r2 = new AllocationRule();
//        r2.appliedFrom = LocalDate.of(2024,1,1);
//        r2.appliedTo = LocalDate.of(2024,1,4);
//        r2.thresholdFrom = new BigDecimal("5");
//        r2.thresholdTo = new BigDecimal("10");
//        r2.ratios.put("A", new BigDecimal("0.7"));
//        r2.ratios.put("B", new BigDecimal("0.3"));
//
//        // -------- Rules from 2024-01-04
//
//        AllocationRule r3 = new AllocationRule();
//        r3.appliedFrom = LocalDate.of(2024,1,4);
//        r3.thresholdFrom = new BigDecimal("1");
//        r3.thresholdTo = new BigDecimal("5");
//        r3.ratios.put("B", new BigDecimal("1.0"));
//
//        AllocationRule r4 = new AllocationRule();
//        r4.appliedFrom = LocalDate.of(2024,1,4);
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
//        AllocationEngine engine = new AllocationEngine(rules);
//
//        List<Obligation> obligations = List.of(
//
//                new Obligation(new BigDecimal("3"), LocalDate.of(2024,1,1)),
//                new Obligation(new BigDecimal("4"), LocalDate.of(2024,1,2)),
//                new Obligation(new BigDecimal("3"), LocalDate.of(2024,1,4)),
//                new Obligation(new BigDecimal("4"), LocalDate.of(2024,1,5))
//        );
//
//        List<AllocationResult> results = engine.allocate(obligations);
//
//        for (int i = 0; i < results.size(); i++) {
//
//            System.out.println("Obligation " + (i+1));
//
//            AllocationResult r = results.get(i);
//
//            for (Map.Entry<String, BigDecimal> e : r.assetAmounts.entrySet()) {
//                System.out.println(e.getKey() + " = " + e.getValue());
//            }
//
//            System.out.println("Unsecured = " + r.unsecured);
//
//            System.out.println();
//        }
//    }

    public static void main(String[] args) {

        List<AllocationRule> rules = new ArrayList<>();

        // ===== Rules before 2024-01-03 =====

        AllocationRule r1 = new AllocationRule();
        r1.appliedFrom = LocalDate.of(2024,1,1);
        r1.appliedTo = LocalDate.of(2024,1,3);
        r1.thresholdFrom = new BigDecimal("1");
        r1.thresholdTo = new BigDecimal("5");
        r1.ratios.put("A", new BigDecimal("1.0"));

        AllocationRule r2 = new AllocationRule();
        r2.appliedFrom = LocalDate.of(2024,1,1);
        r2.appliedTo = LocalDate.of(2024,1,3);
        r2.thresholdFrom = new BigDecimal("5");
        r2.thresholdTo = new BigDecimal("10");
        r2.ratios.put("A", new BigDecimal("0.7"));
        r2.ratios.put("B", new BigDecimal("0.3"));

        // ===== Rules from 2024-01-03 =====

        AllocationRule r3 = new AllocationRule();
        r3.appliedFrom = LocalDate.of(2024,1,3);
        r3.thresholdFrom = new BigDecimal("1");
        r3.thresholdTo = new BigDecimal("5");
        r3.ratios.put("B", new BigDecimal("1.0"));

        AllocationRule r4 = new AllocationRule();
        r4.appliedFrom = LocalDate.of(2024,1,3);
        r4.thresholdFrom = new BigDecimal("5");
        r4.thresholdTo = new BigDecimal("10");
        r4.ratios.put("B", new BigDecimal("0.5"));
        r4.ratios.put("C", new BigDecimal("0.5"));

        rules.add(r1);
        rules.add(r2);
        rules.add(r3);
        rules.add(r4);

        AllocationEngine engine = new AllocationEngine(rules);

        // ===== Obligations =====

        List<Obligation> obligations = Arrays.asList(

                new Obligation(
                        new BigDecimal("4"),
                        LocalDate.of(2024,1,2)
                ),

                new Obligation(
                        new BigDecimal("4"),
                        LocalDate.of(2024,1,3)
                )
        );

        List<AllocationResult> results = engine.allocate(obligations);

        // ===== Print results =====

        for (int i = 0; i < results.size(); i++) {

            System.out.println("Obligation " + (i + 1));

            AllocationResult r = results.get(i);

            for (Map.Entry<String, BigDecimal> e : r.assetAmounts.entrySet()) {
                System.out.println(e.getKey() + " = " + e.getValue());
            }

            System.out.println("Unsecured = " + r.unsecured);
            System.out.println();
        }
    }
}