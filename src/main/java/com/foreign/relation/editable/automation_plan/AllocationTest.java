package com.foreign.relation.editable.automation_plan;

import java.math.BigDecimal;
import java.util.*;

public class AllocationTest {
    static class Obligation {
        String id;
        int amount;
        public Obligation(String id, int amount) {
            this.id = id;
            this.amount = amount;
        }
    }
    static class Tier {
        int from;
        int to;
        double ratioA;
        double ratioB;
        public Tier(int from, int to, double ratioA, double ratioB) {
            this.from = from;
            this.to = to;
            this.ratioA = ratioA;
            this.ratioB = ratioB;
        }
    }
    static class AllocationResult {
        double assetA = 0;
        double assetB = 0;
        @Override
        public String toString() {
            return "A=" + assetA + ", B=" + assetB;
        }
    }
    public static void main(String[] args) {
        List<Obligation> obligations = Arrays.asList(
                new Obligation("ref1", 20),
                new Obligation("ref2", 20),
                new Obligation("ref3", 20),
                new Obligation("ref4", 30),
                new Obligation("ref5", 40)
        );
        List<Tier> tiers = Arrays.asList(
                new Tier(1, 40, 1.0, 0.0),
                new Tier(41, 60, 0.7, 0.3),
                new Tier(61, 100, 0, 0.5)
        );
        Map<String, AllocationResult> result = allocate(obligations, tiers);
        result.forEach((k, v) -> {
            System.out.println(k + " -> " + v);
        });
    }
    public static Map<String, AllocationResult> allocate(
            List<Obligation> obligations,
            List<Tier> tiers
    ) {
        TreeMap<Integer, Tier> tierMap = new TreeMap<>();
        for (Tier t : tiers) {
            tierMap.put(t.from, t);
        }
        Map<String, AllocationResult> result = new LinkedHashMap<>();
        int currentPosition = 1;
        for (Obligation ob : obligations) {
            int remaining = ob.amount;
            AllocationResult allocation = new AllocationResult();
            while (remaining > 0) {
                Tier tier = findTier(currentPosition, tierMap);

                if (tier == null) break;
                int tierEnd = tier.to;
                int availableInTier = tierEnd - currentPosition + 1;
                int used = Math.min(remaining, availableInTier);
                allocation.assetA += used * tier.ratioA;
                allocation.assetB += used * tier.ratioB;
                remaining -= used;
                currentPosition += used;
            }
            result.put(ob.id, allocation);
        }
        return result;
    }
    private static Tier findTier1(int position, List<Tier> tiers) {
        for (Tier t : tiers) {
            if (position >= t.from && position <= t.to) {
                return t;
            }
        }
        throw new RuntimeException("No tier found for position: " + position);
    }

    private static Tier findTier(int position, TreeMap<Integer, Tier> tierMap) {
        Map.Entry<Integer, Tier> entry = tierMap.floorEntry(position);
        if (entry == null) {
            return null;
        }
        Tier t = entry.getValue();
        if (position <= t.to) {
            return t;
        }

        return null;
//        throw new RuntimeException("No tier found for position: " + position);
    }
}
