package com.compassites.model.amadeus.reissue;


import com.compassites.model.PAXFareDetails;
import com.compassites.model.PassengerTypeCode;

import java.math.BigDecimal;

public class ReIssuePerPaxPricingInfo {

    private int paxCount;

    private PassengerTypeCode paxType;

    private BigDecimal totalAmount;

    private BigDecimal taxAmount;

    private BigDecimal baseFare;

    private BigDecimal grandTotalDifferenceAmountD;

    private BigDecimal taxDifferenceB;

    private BigDecimal penaltyAmountP;

    private BigDecimal additionalCollectionA;

    private BigDecimal mcoResidualValueC;

    private PAXFareDetails paxFareDetails;

    public int getPaxCount() {
        return paxCount;
    }

    public void setPaxCount(int paxCount) {
        this.paxCount = paxCount;
    }

    public PassengerTypeCode getPaxType() {
        return paxType;
    }

    public void setPaxType(PassengerTypeCode paxType) {
        this.paxType = paxType;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getBaseFare() {
        return baseFare;
    }

    public void setBaseFare(BigDecimal baseFare) {
        this.baseFare = baseFare;
    }

    public BigDecimal getGrandTotalDifferenceAmountD() {
        return grandTotalDifferenceAmountD;
    }

    public void setGrandTotalDifferenceAmountD(BigDecimal grandTotalDifferenceAmountD) {
        this.grandTotalDifferenceAmountD = grandTotalDifferenceAmountD;
    }

    public BigDecimal getTaxDifferenceB() {
        return taxDifferenceB;
    }

    public void setTaxDifferenceB(BigDecimal taxDifferenceB) {
        this.taxDifferenceB = taxDifferenceB;
    }

    public BigDecimal getPenaltyAmountP() {
        return penaltyAmountP;
    }

    public void setPenaltyAmountP(BigDecimal penaltyAmountP) {
        this.penaltyAmountP = penaltyAmountP;
    }

    public BigDecimal getAdditionalCollectionA() {
        return additionalCollectionA;
    }

    public void setAdditionalCollectionA(BigDecimal additionalCollectionA) {
        this.additionalCollectionA = additionalCollectionA;
    }

    public BigDecimal getMcoResidualValueC() {
        return mcoResidualValueC;
    }

    public void setMcoResidualValueC(BigDecimal mcoResidualValueC) {
        this.mcoResidualValueC = mcoResidualValueC;
    }

    public PAXFareDetails getPaxFareDetails() {
        return paxFareDetails;
    }

    public void setPaxFareDetails(PAXFareDetails paxFareDetails) {
        this.paxFareDetails = paxFareDetails;
    }

}
