package com.foreign.relation.editable.automation_plan;

import java.math.BigDecimal;
import java.util.Date;

public class RefDetailDTO {
    String refCode;
    BigDecimal outstandingAmount;
    String creditDemand;
    String creditType;
    String purpose;

    Date valueDate;

    public Date getValueDate() {
        return valueDate;
    }

    public void setValueDate(Date valueDate) {
        this.valueDate = valueDate;
    }

    public RefDetailDTO(String refCode, BigDecimal outstandingAmount, String creditDemand, String creditType, String purpose) {
        this.refCode = refCode;
        this.outstandingAmount = outstandingAmount;
        this.creditDemand = creditDemand;
        this.creditType = creditType;
        this.purpose = purpose;
    }

    public RefDetailDTO() {}

    public BigDecimal getOutstandingAmount() {
        return outstandingAmount;
    }

    public void setOutstandingAmount(BigDecimal outstandingAmount) {
        this.outstandingAmount = outstandingAmount;
    }

    public String getCreditDemand() {
        return creditDemand;
    }

    public void setCreditDemand(String creditDemand) {
        this.creditDemand = creditDemand;
    }

    public String getCreditType() {
        return creditType;
    }

    public void setCreditType(String creditType) {
        this.creditType = creditType;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getRefCode() {
        return refCode;
    }

    public void setRefCode(String refCode) {
        this.refCode = refCode;
    }
}
