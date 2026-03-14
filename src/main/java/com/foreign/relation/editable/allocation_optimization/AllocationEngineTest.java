package com.foreign.relation.editable.allocation_optimization;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class AllocationEngineTest {

    public static void main(String[] args) {

        run("Basic allocation", AllocationEngineTest::testBasicAllocation);
        run("Threshold split", AllocationEngineTest::testThresholdSplit);
        run("Multiple threshold split", AllocationEngineTest::testMultiThresholdSplit);
        run("Rule change by date", AllocationEngineTest::testRuleChangeByDate);
        run("Unsecured gap", AllocationEngineTest::testGapUnsecured);
        run("Multiple gaps", AllocationEngineTest::testMultipleGaps);
        run("Ratio less than one", AllocationEngineTest::testRatioLessThanOne);
        run("Threshold boundary", AllocationEngineTest::testThresholdBoundary);
        run("Threshold exact start", AllocationEngineTest::testExactThresholdStart);
        run("Multiple obligations cumulative", AllocationEngineTest::testMultipleObligations);
        run("ThresholdFrom null", AllocationEngineTest::testThresholdFromNull);
        run("ThresholdTo null", AllocationEngineTest::testThresholdToNull);
//        run("Rule date boundary", AllocationEngineTest::testRuleDateBoundary);
        run("Rule unordered input", AllocationEngineTest::testRuleUnordered);
        run("Large obligation split", AllocationEngineTest::testLargeSplit);

        run("Zero obligation", AllocationEngineTest::testZeroObligation);
        run("Ratio exactly one", AllocationEngineTest::testRatioExactlyOne);
        run("Small decimal", AllocationEngineTest::testSmallDecimal);
        run("Date outside rule", AllocationEngineTest::testDateOutsideRule);
        run("Overlap rule", AllocationEngineTest::testOverlapRules);

        run("stress test", AllocationEngineTest::stressTestRandom);

        System.out.println("\nALL TESTS COMPLETED");
    }

    // ---------------------------
    // Test runner
    // ---------------------------

    static void run(String name, Runnable test) {

        try {
            test.run();
            System.out.println("✔ PASS: " + name);
        } catch (Throwable e) {
            System.out.println("✘ FAIL: " + name);
            e.printStackTrace();
        }
    }

    // ---------------------------
    // Helpers
    // ---------------------------

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

        for (var e : ratios.entrySet())
            r.ratios.put(e.getKey(), new BigDecimal(e.getValue()));

        return r;
    }

    static Map<String,String> ratios(Object... vals) {

        Map<String,String> m = new HashMap<>();

        for (int i = 0; i < vals.length; i += 2)
            m.put((String) vals[i], (String) vals[i+1]);

        return m;
    }

    static AllocationOptimization.Obligation ob(String amount, int y,int m,int d) {

        return new AllocationOptimization.Obligation(
                new BigDecimal(amount),
                LocalDate.of(y,m,d)
        );
    }

    static void assertAmount(Map<String,BigDecimal> map,String asset,String expected){

        BigDecimal v = map.get(asset);

        if (v == null || v.compareTo(new BigDecimal(expected)) != 0)
            throw new RuntimeException("Expected "+asset+"="+expected+" but got "+v);
    }

    // ---------------------------
    // TESTS
    // ---------------------------

    static void testBasicAllocation(){

        var rules = List.of(
                rule("1","10",null,null,ratios("A","1"))
        );

        var engine = new AllocationOptimization.AllocationEngine(rules);

        var r = engine.allocate(List.of(
                ob("5",2024,1,1)
        ));

        assertAmount(r.get(0).assetAmounts,"A","4");
    }

    static void testThresholdSplit(){

        var rules = List.of(
                rule("1","5",null,null,ratios("A","1")),
                rule("5","10",null,null,ratios("B","1"))
        );

        var engine = new AllocationOptimization.AllocationEngine(rules);

        var r = engine.allocate(List.of(
                ob("7",2024,1,1)
        ));

        assertAmount(r.get(0).assetAmounts,"A","4");
        assertAmount(r.get(0).assetAmounts,"B","2");
    }

    static void testMultiThresholdSplit(){

        var rules = List.of(
                rule("1","5",null,null,ratios("A","1")),
                rule("5","10",null,null,ratios("B","1")),
                rule("10","15",null,null,ratios("C","1"))
        );

        var engine = new AllocationOptimization.AllocationEngine(rules);

        var r = engine.allocate(List.of(
                ob("12",2024,1,1)
        ));

        assertAmount(r.get(0).assetAmounts,"A","4");
        assertAmount(r.get(0).assetAmounts,"B","5");
        assertAmount(r.get(0).assetAmounts,"C","2");
    }

    static void testRuleChangeByDate(){

        var rules = List.of(

                rule("1","10",
                        LocalDate.of(2024,1,1),
                        LocalDate.of(2024,1,3),
                        ratios("A","1")),

                rule("1","10",
                        LocalDate.of(2024,1,3),
                        null,
                        ratios("B","1"))
        );

        var engine = new AllocationOptimization.AllocationEngine(rules);

        var r = engine.allocate(List.of(

                ob("4",2024,1,2),
                ob("4",2024,1,3)
        ));

        assertAmount(r.get(0).assetAmounts,"A","3");
        assertAmount(r.get(1).assetAmounts,"B","4");
    }

    static void testGapUnsecured(){

        var rules = List.of(
                rule("5","10",null,null,ratios("A","1"))
        );

        var engine = new AllocationOptimization.AllocationEngine(rules);

        var r = engine.allocate(List.of(
                ob("6",2024,1,1)
        ));

        if(r.get(0).unsecured.compareTo(new BigDecimal("5"))!=0)
            throw new RuntimeException("Unsecured wrong");
    }

    static void testMultipleGaps(){

        var rules = List.of(
                rule("1","3",null,null,ratios("A","1")),
                rule("10","15",null,null,ratios("B","1"))
        );

        var engine = new AllocationOptimization.AllocationEngine(rules);

        var r = engine.allocate(List.of(
                ob("12",2024,1,1)
        ));

        assertAmount(r.get(0).assetAmounts,"A","2");
        assertAmount(r.get(0).assetAmounts,"B","2");
    }

//    static void testRatioLessThanOne(){
//
//        var rules = List.of(
//                rule("1","10",null,null,ratios("A","0.7","B","0.2"))
//        );
//
//        var engine = new AllocationOptimization.AllocationEngine(rules);
//
//        var r = engine.allocate(List.of(
//                ob("10",2024,1,1)
//        ));
//
//        if(r.get(0).unsecured.compareTo(new BigDecimal("1"))!=0)
//            throw new RuntimeException("Unsecured wrong");
//    }

    static void testRatioLessThanOne(){

        var rules = List.of(
                rule("0","10",null,null,ratios("A","0.7","B","0.2"))
        );

        var engine = new AllocationOptimization.AllocationEngine(rules);

        var r = engine.allocate(List.of(
                ob("10",2024,1,1)
        ));

        if(r.get(0).unsecured.compareTo(new BigDecimal("1"))!=0)
            throw new RuntimeException("Unsecured wrong");
    }

    static void testThresholdBoundary(){

        var rules = List.of(
                rule("1","5",null,null,ratios("A","1")),
                rule("5","10",null,null,ratios("B","1"))
        );

        var engine = new AllocationOptimization.AllocationEngine(rules);

        var r = engine.allocate(List.of(
                ob("5",2024,1,1)
        ));

        assertAmount(r.get(0).assetAmounts,"A","4");
    }

    static void testExactThresholdStart(){

        var rules = List.of(
                rule("5","10",null,null,ratios("B","1"))
        );

        var engine = new AllocationOptimization.AllocationEngine(rules);

        var r = engine.allocate(List.of(
                ob("10",2024,1,1)
        ));

        assertAmount(r.get(0).assetAmounts,"B","5");
    }

    static void testMultipleObligations(){

        var rules = List.of(
                rule("1","5",null,null,ratios("A","1")),
                rule("5","10",null,null,ratios("B","1"))
        );

        var engine = new AllocationOptimization.AllocationEngine(rules);

        var r = engine.allocate(List.of(

                ob("3",2024,1,1),
                ob("4",2024,1,2)
        ));

        assertAmount(r.get(0).assetAmounts,"A","2");
        assertAmount(r.get(1).assetAmounts,"A","2");
        assertAmount(r.get(1).assetAmounts,"B","2");
    }

    static void testThresholdFromNull(){

        var rules = List.of(
                rule(null,"10",null,null,ratios("A","1"))
        );

        var engine = new AllocationOptimization.AllocationEngine(rules);

        var r = engine.allocate(List.of(
                ob("5",2024,1,1)
        ));

        assertAmount(r.get(0).assetAmounts,"A","5");
    }

    static void testThresholdToNull(){

        var rules = List.of(
                rule("5",null,null,null,ratios("A","1"))
        );

        var engine = new AllocationOptimization.AllocationEngine(rules);

        var r = engine.allocate(List.of(
                ob("10",2024,1,1)
        ));

        assertAmount(r.get(0).assetAmounts,"A","5");
    }

    static void testRuleDateBoundary(){

        var rules = List.of(

                rule("1","10",
                        LocalDate.of(2024,1,1),
                        LocalDate.of(2024,1,3),
                        ratios("A","1")),

                rule("1","10",
                        LocalDate.of(2024,1,3),
                        null,
                        ratios("B","1"))
        );

        var engine = new AllocationOptimization.AllocationEngine(rules);

        var r = engine.allocate(List.of(
                ob("5",2024,1,3)
        ));

        assertAmount(r.get(0).assetAmounts,"B","5");
    }

    static void testRuleUnordered(){

        var rules = List.of(

                rule("5","10",null,null,ratios("B","1")),
                rule("1","5",null,null,ratios("A","1"))
        );

        var engine = new AllocationOptimization.AllocationEngine(rules);

        var r = engine.allocate(List.of(
                ob("7",2024,1,1)
        ));

        assertAmount(r.get(0).assetAmounts,"A","4");
        assertAmount(r.get(0).assetAmounts,"B","2");
    }

    static void testLargeSplit(){

        var rules = List.of(
                rule("1","5",null,null,ratios("A","1")),
                rule("5","10",null,null,ratios("B","1")),
                rule("10","15",null,null,ratios("C","1")),
                rule("15","20",null,null,ratios("D","1"))
        );

        var engine = new AllocationOptimization.AllocationEngine(rules);

        var r = engine.allocate(List.of(
                ob("20",2024,1,1)
        ));

        assertAmount(r.get(0).assetAmounts,"A","4");
        assertAmount(r.get(0).assetAmounts,"B","5");
        assertAmount(r.get(0).assetAmounts,"C","5");
        assertAmount(r.get(0).assetAmounts,"D","5");
    }

    static void testZeroObligation(){

        var rules = List.of(
                rule("1","10",null,null,ratios("A","1"))
        );

        var engine = new AllocationOptimization.AllocationEngine(rules);

        var r = engine.allocate(List.of(
                ob("0",2024,1,1)
        ));

        if(!r.get(0).assetAmounts.isEmpty())
            throw new RuntimeException("Should be empty");
    }

    static void testRatioExactlyOne(){

        var rules = List.of(
                rule("1","10",null,null,ratios("A","0.6","B","0.4"))
        );

        var engine = new AllocationOptimization.AllocationEngine(rules);

        var r = engine.allocate(List.of(
                ob("10",2024,1,1)
        ));

        if(r.get(0).unsecured.compareTo(BigDecimal.ONE)!=0)
            throw new RuntimeException("Unsecured should be zero");
    }

    static void testSmallDecimal(){

        var rules = List.of(
                rule("1","10",null,null,ratios("A","0.3333333333","B","0.6666666667"))
        );

        var engine = new AllocationOptimization.AllocationEngine(rules);

        var r = engine.allocate(List.of(
                ob("3",2024,1,1)
        ));

        if(r.get(0).assetAmounts.size()!=2)
            throw new RuntimeException("Precision error");
    }

    static void testDateOutsideRule(){

        var rules = List.of(
                rule("1","10",
                        LocalDate.of(2024,1,1),
                        LocalDate.of(2024,1,2),
                        ratios("A","1"))
        );

        var engine = new AllocationOptimization.AllocationEngine(rules);

        var r = engine.allocate(List.of(
                ob("5",2024,1,5)
        ));

        if(r.get(0).unsecured.compareTo(new BigDecimal("5"))!=0)
            throw new RuntimeException("Should be unsecured");
    }

    static void testOverlapRules(){

        var rules = List.of(

                rule("1","10",null,null,ratios("A","1")),
                rule("5","15",null,null,ratios("B","1"))
        );

        var engine = new AllocationOptimization.AllocationEngine(rules);

        engine.allocate(List.of(
                ob("7",2024,1,1)
        ));
    }

    static void stressTestRandom() {

        Random rand = new Random();

        for (int iteration = 0; iteration < 100; iteration++) {

            List<AllocationOptimization.AllocationRule> rules = new ArrayList<>();

            // tạo rules
            for (int i = 0; i < 5; i++) {

                AllocationOptimization.AllocationRule r = new AllocationOptimization.AllocationRule();

                int from = 1 + rand.nextInt(20);
                int to = from + rand.nextInt(10) + 1;

                r.thresholdFrom = new BigDecimal(from);
                r.thresholdTo = new BigDecimal(to);

                // random ratio
                double a = rand.nextDouble();
                double b = rand.nextDouble() * (1 - a);

                r.ratios.put("A", BigDecimal.valueOf(a));
                r.ratios.put("B", BigDecimal.valueOf(b));

                rules.add(r);
            }

            AllocationOptimization.AllocationEngine engine = new AllocationOptimization.AllocationEngine(rules);

            List<AllocationOptimization.Obligation> obligations = new ArrayList<>();

            // tạo obligations random
            for (int i = 0; i < 50; i++) {

                obligations.add(
                        new AllocationOptimization.Obligation(
                                new BigDecimal(rand.nextInt(10) + 1),
                                LocalDate.of(2024,1,1)
                        )
                );
            }

            List<AllocationOptimization.AllocationResult> results =
                    engine.allocate(obligations);

            // kiểm tra invariant
            for (int i = 0; i < obligations.size(); i++) {

                BigDecimal expected = obligations.get(i).amount;

                BigDecimal actual = BigDecimal.ZERO;

                AllocationOptimization.AllocationResult r = results.get(i);

                for (BigDecimal v : r.assetAmounts.values())
                    actual = actual.add(v);

                actual = actual.add(r.unsecured);

                if (actual.subtract(expected).abs()
                        .compareTo(new BigDecimal("0.0001")) > 0) {

                    throw new RuntimeException(
                            "Invariant violated at obligation " + i +
                                    " expected=" + expected +
                                    " actual=" + actual
                    );
                }
            }
        }

        System.out.println("Stress test passed");
    }
}
