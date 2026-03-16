package com.foreign.relation.editable.automation_plan;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class MarginAllocationCategory {
    Long id;

    String creditType;

    public String getCreditType() {
        return creditType;
    }

    public void setCreditType(String creditType) {
        this.creditType = creditType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    BigDecimal minCreditThreshold;
    BigDecimal maxCreditThreshold;
    Date appliedFrom;
    Date appliedTo;
    String creditDemand;
    String lcLifeCycle;

    List<MarginAllocationCategoryRatio> ratios;

    public MarginAllocationCategory(String creditType, Long id, BigDecimal minCreditThreshold, BigDecimal maxCreditThreshold, Date appliedFrom, Date appliedTo, String creditDemand, String lcLifeCycle, List<MarginAllocationCategoryRatio> ratios) {
        this.id = id;
        this.minCreditThreshold = minCreditThreshold;
        this.maxCreditThreshold = maxCreditThreshold;
        this.appliedFrom = appliedFrom;
        this.appliedTo = appliedTo;
        this.creditDemand = creditDemand;
        this.lcLifeCycle = lcLifeCycle;
        this.ratios = ratios;
        this.creditType = creditType;
    }

    public MarginAllocationCategory() {}

    public BigDecimal getMinCreditThreshold() {
        return minCreditThreshold;
    }

    public void setMinCreditThreshold(BigDecimal minCreditThreshold) {
        this.minCreditThreshold = minCreditThreshold;
    }

    public BigDecimal getMaxCreditThreshold() {
        return maxCreditThreshold;
    }

    public void setMaxCreditThreshold(BigDecimal maxCreditThreshold) {
        this.maxCreditThreshold = maxCreditThreshold;
    }

    public Date getAppliedFrom() {
        return appliedFrom;
    }

    public void setAppliedFrom(Date appliedFrom) {
        this.appliedFrom = appliedFrom;
    }

    public Date getAppliedTo() {
        return appliedTo;
    }

    public void setAppliedTo(Date appliedTo) {
        this.appliedTo = appliedTo;
    }

    public String getCreditDemand() {
        return creditDemand;
    }

    public void setCreditDemand(String creditDemand) {
        this.creditDemand = creditDemand;
    }

    public String getLcLifeCycle() {
        return lcLifeCycle;
    }

    public void setLcLifeCycle(String lcLifeCycle) {
        this.lcLifeCycle = lcLifeCycle;
    }

    public List<MarginAllocationCategoryRatio> getRatios() {
        return ratios;
    }

    public void setRatios(List<MarginAllocationCategoryRatio> ratios) {
        this.ratios = ratios;
    }
}
