package com.foreign.relation.editable.allocation_optimization;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class AllocationTesting {

    public static void main(String[] args) {

        testBasicAllocation();
        testThresholdSplit();
        testRuleChangeByDate();
        testUnsecuredGap();
        testRatioLessThanOne();
        testNoThresholdFrom();
        testNoThresholdTo();
        testMultipleObligations();
        testThresholdBoundary();
        testRuleOverlap();
        testComplexAllocationEngine();

        System.out.println("All tests executed");
    }

    static AllocationOptimization.AllocationEngine engine(List<AllocationOptimization.AllocationRule> rules) {
        return new AllocationOptimization.AllocationEngine(rules);
    }

    static AllocationOptimization.Obligation ob(String amount, int y, int m, int d) {
        return new AllocationOptimization.Obligation(
                new BigDecimal(amount),
                LocalDate.of(y, m, d)
        );
    }

    static AllocationOptimization.AllocationRule rule(
            String from,
            String to,
            LocalDate appliedFrom,
            LocalDate appliedTo,
            Map<String,String> ratios
    ) {

        AllocationOptimization.AllocationRule r = new AllocationOptimization.AllocationRule();

        if (from != null) r.thresholdFrom = new BigDecimal(from);
        if (to != null) r.thresholdTo = new BigDecimal(to);

        r.appliedFrom = appliedFrom;
        r.appliedTo = appliedTo;

        for (Map.Entry<String,String> e : ratios.entrySet()) {
            r.ratios.put(e.getKey(), new BigDecimal(e.getValue()));
        }

        return r;
    }

    static Map<String,String> ratios(Object... vals) {

        Map<String,String> m = new HashMap<>();

        for (int i=0;i<vals.length;i+=2)
            m.put((String)vals[i], (String)vals[i+1]);

        return m;
    }

    // ---------------------------------------------------
    // 1 Basic allocation
    // ---------------------------------------------------
    static void testBasicAllocation() {

        List<AllocationOptimization.AllocationRule> rules = List.of(
                rule("1","10",null,null,ratios("A","1"))
        );

        AllocationOptimization.AllocationEngine engine = engine(rules);

        List<AllocationOptimization.Obligation> obs = List.of(
                ob("5",2024,1,1)
        );

        var r = engine.allocate(obs);

        System.out.println("testBasicAllocation: " + r.get(0).assetAmounts);
    }

    // ---------------------------------------------------
    // 2 Threshold split
    // ---------------------------------------------------
    static void testThresholdSplit() {

        List<AllocationOptimization.AllocationRule> rules = List.of(

                rule("1","5",null,null,ratios("A","1")),
                rule("5","10",null,null,ratios("B","1"))
        );

        var engine = engine(rules);

        var obs = List.of(
                ob("7",2024,1,1)
        );

        var r = engine.allocate(obs);

        System.out.println("testThresholdSplit: " + r.get(0).assetAmounts);
    }

    // ---------------------------------------------------
    // 3 Rule change by date
    // ---------------------------------------------------
    static void testRuleChangeByDate() {

        List<AllocationOptimization.AllocationRule> rules = List.of(

                rule("1","10",
                        LocalDate.of(2024,1,1),
                        LocalDate.of(2024,1,3),
                        ratios("A","1")),

                rule("1","10",
                        LocalDate.of(2024,1,3),
                        null,
                        ratios("B","1"))
        );

        var engine = engine(rules);

        var obs = List.of(
                ob("4",2024,1,2),
                ob("4",2024,1,3)
        );

        var r = engine.allocate(obs);

        System.out.println("testRuleChangeByDate: " );
        printResults(r);
    }

    static void printResults(List<AllocationOptimization.AllocationResult> results) {

        for (int i = 0; i < results.size(); i++) {

            var r = results.get(i);

            System.out.println("Obligation " + (i+1));

            for (var e : r.assetAmounts.entrySet()) {
                System.out.println(e.getKey() + " = " + e.getValue());
            }

            System.out.println("Unsecured = " + r.unsecured);
            System.out.println();
        }
    }

    // ---------------------------------------------------
    // 4 Gap → unsecured
    // ---------------------------------------------------
    static void testUnsecuredGap() {

        List<AllocationOptimization.AllocationRule> rules = List.of(

                rule("5","10",null,null,ratios("A","1"))
        );

        var engine = engine(rules);

        var obs = List.of(
                ob("6",2024,1,1)
        );

        var r = engine.allocate(obs);

        System.out.println("testUnsecuredGap: unsecured=" + r.get(0).unsecured);
    }

    // ---------------------------------------------------
    // 5 ratio < 1
    // ---------------------------------------------------
    static void testRatioLessThanOne() {

        List<AllocationOptimization.AllocationRule> rules = List.of(

                rule("1","10",null,null,
                        ratios("A","0.7","B","0.2"))
        );

        var engine = engine(rules);

        var obs = List.of(
                ob("10",2024,1,1)
        );

        var r = engine.allocate(obs);

        System.out.println("testRatioLessThanOne: unsecured=" + r.get(0).unsecured);
    }

    // ---------------------------------------------------
    // 6 thresholdFrom = null
    // ---------------------------------------------------
    static void testNoThresholdFrom() {

        List<AllocationOptimization.AllocationRule> rules = List.of(

                rule(null,"10",null,null,ratios("A","1"))
        );

        var engine = engine(rules);

        var obs = List.of(
                ob("5",2024,1,1)
        );

        var r = engine.allocate(obs);

        System.out.println("testNoThresholdFrom: " + r.get(0).assetAmounts);
    }

    // ---------------------------------------------------
    // 7 thresholdTo = null
    // ---------------------------------------------------
    static void testNoThresholdTo() {

        List<AllocationOptimization.AllocationRule> rules = List.of(

                rule("5",null,null,null,ratios("A","1"))
        );

        var engine = engine(rules);

        var obs = List.of(
                ob("10",2024,1,1)
        );

        var r = engine.allocate(obs);

        System.out.println("testNoThresholdTo: " + r.get(0).assetAmounts);
    }

    // ---------------------------------------------------
    // 8 cumulative obligations
    // ---------------------------------------------------
    static void testMultipleObligations() {

        List<AllocationOptimization.AllocationRule> rules = List.of(

                rule("1","5",null,null,ratios("A","1")),
                rule("5","10",null,null,ratios("B","1"))
        );

        var engine = engine(rules);

        var obs = List.of(
                ob("3",2024,1,1),
                ob("4",2024,1,2)
        );

        var r = engine.allocate(obs);

        System.out.println("testMultipleObligations: ");
        printResults(r);
    }

    // ---------------------------------------------------
    // 9 threshold boundary
    // ---------------------------------------------------
    static void testThresholdBoundary() {

        List<AllocationOptimization.AllocationRule> rules = List.of(

                rule("1","5",null,null,ratios("A","1")),
                rule("5","10",null,null,ratios("B","1"))
        );

        var engine = engine(rules);

        var obs = List.of(
                ob("5",2024,1,1)
        );

        var r = engine.allocate(obs);

        System.out.println("testThresholdBoundary: " + r.get(0).assetAmounts);
    }

    // ---------------------------------------------------
    // 10 overlap rules
    // ---------------------------------------------------
    static void testRuleOverlap() {

        List<AllocationOptimization.AllocationRule> rules = List.of(

                rule("1","10",null,null,ratios("A","1")),
                rule("5","15",null,null,ratios("B","1"))
        );

        var engine = engine(rules);

        var obs = List.of(
                ob("7",2024,1,1)
        );

        var r = engine.allocate(obs);

        System.out.println("testRuleOverlap: " + r.get(0).assetAmounts);
    }

    static void testComplexAllocationEngine() {

        List<AllocationOptimization.AllocationRule> rules = new ArrayList<>();

        rules.add(rule(
                "1","5",
                LocalDate.of(2024,1,1),
                LocalDate.of(2024,1,3),
                ratios("A","1")
        ));

        rules.add(rule(
                "5","10",
                LocalDate.of(2024,1,1),
                LocalDate.of(2024,1,3),
                ratios("A","0.7","B","0.3")
        ));

        rules.add(rule(
                "1","5",
                LocalDate.of(2024,1,3),
                null,
                ratios("B","1")
        ));

        rules.add(rule(
                "5","10",
                LocalDate.of(2024,1,3),
                null,
                ratios("B","0.5","C","0.5")
        ));

        var engine = engine(rules);

        List<AllocationOptimization.Obligation> obs = List.of(

                ob("4",2024,1,2),
                ob("4",2024,1,3)
        );

        var results = engine.allocate(obs);

        System.out.println("testComplexAllocationEngine");

        for (int i = 0; i < results.size(); i++) {

            var r = results.get(i);

            System.out.println("Obligation " + (i+1));
            System.out.println("Assets: " + r.assetAmounts);
            System.out.println("Unsecured: " + r.unsecured);
        }
    }
}
