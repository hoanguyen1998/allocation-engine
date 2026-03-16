package com.foreign.relation.editable.automation_plan;

import java.util.List;

public class RefAndCategoryList {
    List<RefDetailDTO> refs;

    List<MarginAllocationCategory> categories;

    public RefAndCategoryList() {}

    public RefAndCategoryList(List<RefDetailDTO> refs, List<MarginAllocationCategory> categories) {
        this.refs = refs;
        this.categories = categories;
    }

    public List<RefDetailDTO> getRefs() {
        return refs;
    }

    public void setRefs(List<RefDetailDTO> refs) {
        this.refs = refs;
    }

    public List<MarginAllocationCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<MarginAllocationCategory> categories) {
        this.categories = categories;
    }
}
