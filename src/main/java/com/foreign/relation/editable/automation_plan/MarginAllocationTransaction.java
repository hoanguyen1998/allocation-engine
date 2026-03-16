package com.foreign.relation.editable.automation_plan;

import java.util.List;

public class MarginAllocationTransaction {
    Long id;

    List<MarginAllocationCategory> marginAllocationCategories;

    public MarginAllocationTransaction(Long id, List<MarginAllocationCategory> marginAllocationCategories) {
        this.id = id;
        this.marginAllocationCategories = marginAllocationCategories;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<MarginAllocationCategory> getMarginAllocationCategories() {
        return marginAllocationCategories;
    }

    public void setMarginAllocationCategories(List<MarginAllocationCategory> marginAllocationCategories) {
        this.marginAllocationCategories = marginAllocationCategories;
    }
}
