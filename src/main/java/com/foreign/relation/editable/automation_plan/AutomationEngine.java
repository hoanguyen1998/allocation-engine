package com.foreign.relation.editable.automation_plan;

import java.math.BigDecimal;
import java.util.*;

public class AutomationEngine {
    private static final String LC_TYPE = "LC";
    public void allocateRefsToMargins(List<MarginAllocationTransaction> margins, List<RefDetailDTO> refs) {
        Map<Long, Map<String, RefAndCategoryList>> marginMap = getMarginMap(margins);

        for (RefDetailDTO ref: refs) {
            String creditDemand = ref.getCreditDemand();
            String creditType = ref.getCreditType();

            Date valueDate = ref.getValueDate();

            if (LC_TYPE.equals(creditType)) creditDemand += ref.getPurpose();

            Map<Long, MarginAllocationCategory> matchedCategoryMap = new HashMap<>();

            for (Map.Entry<Long, Map<String, RefAndCategoryList>> entry: marginMap.entrySet()) {
                Long marginId = entry.getKey();
                Map<String, RefAndCategoryList> refAndCategoryListMap = entry.getValue();

                RefAndCategoryList refAndCategoryList = refAndCategoryListMap.get(creditDemand);

                if (refAndCategoryList == null) continue;

                List<MarginAllocationCategory> categories = refAndCategoryList.getCategories();

                for (MarginAllocationCategory category: categories) {
                    Date appliedFrom = category.getAppliedFrom();
                    Date appliedTo = category.getAppliedTo();

                    if (appliedFrom != null && appliedFrom.after(valueDate)) continue;

                    if (appliedTo != null && appliedTo.before(valueDate)) continue;

                    matchedCategoryMap.put(marginId, category);
                }
            }

            if (matchedCategoryMap.size() == 1) {
                Long marginId = matchedCategoryMap.keySet().iterator().next();

                Map<String, RefAndCategoryList> categoryMap = marginMap.get(marginId);

                if (categoryMap != null) {
                    RefAndCategoryList list = categoryMap.get(creditDemand);

                    if (list != null) {
                        list.getRefs().add(ref);
                        // ko can continue,
                    }
                }
            }

            if (matchedCategoryMap.size() > 1) {
                BigDecimal highestMinRatio = BigDecimal.ZERO;
                BigDecimal lowestMaxRatio = null;

                Long bestMarginIdWithMinRatio = null;
                Long bestMarginIdWithMaxRatio = null;

                for (Map.Entry<Long, MarginAllocationCategory> marginAllocationCategoryEntry: matchedCategoryMap.entrySet()) {
                    Long matchedMarginId = marginAllocationCategoryEntry.getKey();
                    List<MarginAllocationCategoryRatio> marginRatios = marginAllocationCategoryEntry.getValue().getRatios();

                    if (marginRatios == null) continue;

                    for (MarginAllocationCategoryRatio marginAllocationCategoryRatio: marginRatios) {
                        List<String> assetTypes = marginAllocationCategoryRatio.getAssetTypes();

                        if (assetTypes.contains("TS chuan")) {
                            BigDecimal minRatio = marginAllocationCategoryRatio.getMinRatio();

                            if (minRatio != null && minRatio.compareTo(highestMinRatio) > 0) {
                                highestMinRatio = minRatio;
                                bestMarginIdWithMinRatio = matchedMarginId;
                            }
                        }

                        if (assetTypes.contains("TS khac chuan")) {
                            BigDecimal maxRatio = marginAllocationCategoryRatio.getMaxRatio();

                            if (maxRatio != null && maxRatio.compareTo(lowestMaxRatio) < 0) {
                                lowestMaxRatio = maxRatio;
                                bestMarginIdWithMaxRatio = matchedMarginId;
                            }
                        }
                    }
                }

                Long chosenMargin = bestMarginIdWithMinRatio != null
                        ? bestMarginIdWithMinRatio
                        : bestMarginIdWithMaxRatio;

                if (chosenMargin != null) {
                    Map<String, RefAndCategoryList> categoryMap = marginMap.get(chosenMargin);

                    if (categoryMap != null) {
                        RefAndCategoryList refAndCategoryList = categoryMap.get(creditDemand);

                        if (refAndCategoryList != null) {
                            refAndCategoryList.getRefs().add(ref);
                        }
                    }
                }
            }
        }
    }

    public Map<Long, Map<String, RefAndCategoryList>> getMarginMap(List<MarginAllocationTransaction> margins) {
        Map<Long, Map<String, RefAndCategoryList>> marginMap = new HashMap<>();

        for (MarginAllocationTransaction margin: margins) {
            Map<String, RefAndCategoryList> categoryMap =
                    marginMap.computeIfAbsent(margin.getId(), k -> new HashMap<>());

            List<MarginAllocationCategory> categories = margin.getMarginAllocationCategories();

            if (categories == null) continue;

            for (MarginAllocationCategory category: categories) {
                String creditDemand = category.getCreditDemand();

                if ("LC".equals(category.getCreditType())) creditDemand += category.getLcLifeCycle();

                categoryMap.computeIfAbsent(creditDemand, k -> {
                            RefAndCategoryList refAndCategoryList = new RefAndCategoryList();
                            refAndCategoryList.setCategories(new ArrayList<>());
                            refAndCategoryList.setRefs(new ArrayList<>());

                            return refAndCategoryList;
                        })
                        .getCategories()
                        .add(category);
            }
        }

        return marginMap;
    }
}
