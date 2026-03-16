package com.foreign.relation.editable.automation_plan;

import java.math.BigDecimal;
import java.util.List;

public class MarginAllocationCategoryRatio {
    Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    BigDecimal maxRatio;
   BigDecimal minRatio;
   List<String> assetTypes;

    public MarginAllocationCategoryRatio(Long id, BigDecimal maxRatio, BigDecimal minRatio, List<String> assetTypes) {
        this.id = id;
        this.maxRatio = maxRatio;
        this.minRatio = minRatio;
        this.assetTypes = assetTypes;
    }

    public BigDecimal getMaxRatio() {
        return maxRatio;
    }

    public void setMaxRatio(BigDecimal maxRatio) {
        this.maxRatio = maxRatio;
    }

    public BigDecimal getMinRatio() {
        return minRatio;
    }

    public void setMinRatio(BigDecimal minRatio) {
        this.minRatio = minRatio;
    }

    public List<String> getAssetTypes() {
        return assetTypes;
    }

    public void setAssetTypes(List<String> assetTypes) {
        this.assetTypes = assetTypes;
    }
}
