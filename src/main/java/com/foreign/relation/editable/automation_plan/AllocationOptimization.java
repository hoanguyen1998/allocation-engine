package com.foreign.relation.editable.automation_plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class AllocationOptimization {
    // =========================
    // Allocation Result
    // =========================
    static class AllocationResult {

        Map<String, BigDecimal> assetAmounts = new HashMap<>();
        BigDecimal unsecured = BigDecimal.ZERO;

        void add(String asset, BigDecimal value) {

            assetAmounts.merge(
                    asset,
                    value,
                    BigDecimal::add
            );
        }

        void addUnsecured(BigDecimal value) {
            unsecured = unsecured.add(value);
        }
    }

    // =========================
    // Allocation Engine
    // =========================
    static class AllocationEngine {

        private final TreeMap<BigDecimal, List<MarginAllocationCategory>> ruleIndex
                = new TreeMap<>();

        AllocationEngine(List<MarginAllocationCategory> rules) {
            validateRules(rules);

            for (MarginAllocationCategory r : rules) {

                BigDecimal key =
                        r.getMinCreditThreshold() == null
                                ? BigDecimal.ZERO
                                : r.getMinCreditThreshold();

                ruleIndex
                        .computeIfAbsent(key, k -> new ArrayList<>())
                        .add(r);
            }
        }

        List<AllocationResult> allocate(List<RefDetailDTO> refDetailDTOS) {

            List<AllocationResult> results = new ArrayList<>();

            BigDecimal cumulative = BigDecimal.ZERO;

            for (RefDetailDTO ref : refDetailDTOS) {

                BigDecimal start = cumulative;
                BigDecimal end = cumulative.add(ref.getOutstandingAmount());

                AllocationResult result = new AllocationResult();

                while (start.compareTo(end) < 0) {

                    MarginAllocationCategory rule =
                            findRule(ref.valueDate, start);

                    BigDecimal nextBreak = end;

                    if (rule != null && rule.getMaxCreditThreshold() != null) {

                        nextBreak = nextBreak.min(rule.getMaxCreditThreshold());

                    } else {

                        BigDecimal nextThreshold =
                                findNextThreshold(start);

                        if (nextThreshold != null)
                            nextBreak = nextBreak.min(nextThreshold);
                    }

                    BigDecimal portion =
                            nextBreak.subtract(start);

                    if (rule == null) {

                        result.addUnsecured(portion);

                    } else {

                        BigDecimal ratioSum = BigDecimal.ZERO;

                        for (MarginAllocationCategoryRatio e: rule.getRatios()) {
                            // dùng tạm minRatio
                            BigDecimal value =
                                    portion.multiply(e.getMinRatio());

                            result.add(String.join("/", e.getAssetTypes()), value);

                            ratioSum = ratioSum.add(e.getMinRatio());
                        }

                        if (ratioSum.compareTo(BigDecimal.ONE) < 0) {

                            BigDecimal unsecured =
                                    portion.multiply(
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

        private void validateRules(List<MarginAllocationCategory> rules) {

            for (int i = 0; i < rules.size(); i++) {

                for (int j = i + 1; j < rules.size(); j++) {

                    MarginAllocationCategory r1 = rules.get(i);
                    MarginAllocationCategory r2 = rules.get(j);

                    if (dateOverlap(r1, r2) && thresholdOverlap(r1, r2)) {

                        throw new IllegalStateException(
                                "Overlapping rules detected: " + r1 + " and " + r2
                        );
                    }
                }
            }
        }

        boolean matches(
                Date appliedFrom,
                Date appliedTo,
                Date date,
                BigDecimal minCreditThreshold,
                BigDecimal maxCreditThreshold,
                BigDecimal cumulative) {

            if (appliedFrom != null && date.before(appliedFrom))
                return false;

            if (appliedTo != null && !date.before(appliedTo))
                return false;

            if (minCreditThreshold != null &&
                    cumulative.compareTo(minCreditThreshold) < 0)
                return false;

            if (maxCreditThreshold != null &&
                    cumulative.compareTo(maxCreditThreshold) >= 0)
                return false;

            return true;
        }

        private boolean thresholdOverlap(MarginAllocationCategory r1, MarginAllocationCategory r2) {

            BigDecimal from1 = r1.getMinCreditThreshold() == null ? BigDecimal.ZERO : r1.getMinCreditThreshold();
            BigDecimal to1   = r1.getMaxCreditThreshold() == null ? new BigDecimal("999999999") : r1.getMaxCreditThreshold();

            BigDecimal from2 = r2.getMinCreditThreshold() == null ? BigDecimal.ZERO : r2.getMinCreditThreshold();
            BigDecimal to2   = r2.getMaxCreditThreshold() == null ? new BigDecimal("999999999") : r2.getMaxCreditThreshold();

            return from1.compareTo(to2) < 0 && from2.compareTo(to1) < 0;
        }

        private boolean dateOverlap(MarginAllocationCategory r1, MarginAllocationCategory r2) {

            LocalDate from1 = r1.appliedFrom == null
                    ? LocalDate.MIN
                    : r1.appliedFrom.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            LocalDate to1 = r1.appliedTo == null
                    ? LocalDate.MAX
                    : r1.appliedTo.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            LocalDate from2 = r2.appliedFrom == null
                    ? LocalDate.MIN
                    : r2.appliedFrom.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            LocalDate to2 = r2.appliedTo == null
                    ? LocalDate.MAX
                    : r2.appliedTo.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            return !from1.isAfter(to2) && !from2.isAfter(to1);
        }

        private MarginAllocationCategory findRule(
                Date date,
                BigDecimal cumulative
        ) {

            Map.Entry<BigDecimal, List<MarginAllocationCategory>> entry =
                    ruleIndex.floorEntry(cumulative);

            if (entry == null)
                return null;

            for (MarginAllocationCategory r : entry.getValue()) {

                if (matches(r.appliedFrom, r.appliedTo, date, r.minCreditThreshold, r.maxCreditThreshold, cumulative))
                    return r;
            }

            return null;
        }


        private BigDecimal findNextThreshold(BigDecimal cumulative) {

            Map.Entry<BigDecimal, List<MarginAllocationCategory>> next =
                    ruleIndex.higherEntry(cumulative);

            if (next == null)
                return null;

            return next.getKey();
        }
    }

    // =========================
    // Main Demo
    // =========================
}
