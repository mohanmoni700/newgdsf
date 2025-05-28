package dto.refund;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public class PerPaxRefundPricingInformation {

    private String fullName;

    private BigInteger paxTattoo;

    private String refundedSegmentString;

    private List<BigInteger> refundedSegmentTattoos;

    private String ticketNumber;

    private BigDecimal refundedAmount;

    private String refundedAmountCurrency;

    private List<DetailedMonetaryInformation> detailedMonetaryInformationList;

    private List<DetailedTaxInformation> detailedTaxInformationList;

    private List<DetailedPenaltyInformation> detailedPenaltyInformationList;

    private List<DetailedCommissionInformation> detailedCommissionInformationList;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public BigInteger getPaxTattoo() {
        return paxTattoo;
    }

    public void setPaxTattoo(BigInteger paxTattoo) {
        this.paxTattoo = paxTattoo;
    }

    public String getRefundedSegmentString() {
        return refundedSegmentString;
    }

    public void setRefundedSegmentString(String refundedSegmentString) {
        this.refundedSegmentString = refundedSegmentString;
    }

    public List<BigInteger> getRefundedSegmentTattoos() {
        return refundedSegmentTattoos;
    }

    public void setRefundedSegmentTattoos(List<BigInteger> refundedSegmentTattoos) {
        this.refundedSegmentTattoos = refundedSegmentTattoos;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public void setRefundedAmount(BigDecimal refundedAmount) {
        this.refundedAmount = refundedAmount;
    }

    public String getRefundedAmountCurrency() {
        return refundedAmountCurrency;
    }

    public void setRefundedAmountCurrency(String refundedAmountCurrency) {
        this.refundedAmountCurrency = refundedAmountCurrency;
    }

    public List<DetailedMonetaryInformation> getDetailedMonetaryInformationList() {
        return detailedMonetaryInformationList;
    }

    public void setDetailedMonetaryInformationList(List<DetailedMonetaryInformation> detailedMonetaryInformationList) {
        this.detailedMonetaryInformationList = detailedMonetaryInformationList;
    }

    public List<DetailedTaxInformation> getDetailedTaxInformationList() {
        return detailedTaxInformationList;
    }

    public void setDetailedTaxInformationList(List<DetailedTaxInformation> detailedTaxInformationList) {
        this.detailedTaxInformationList = detailedTaxInformationList;
    }

    public List<DetailedPenaltyInformation> getDetailedPenaltyInformationList() {
        return detailedPenaltyInformationList;
    }

    public void setDetailedPenaltyInformationList(List<DetailedPenaltyInformation> detailedPenaltyInformationList) {
        this.detailedPenaltyInformationList = detailedPenaltyInformationList;
    }

    public List<DetailedCommissionInformation> getDetailedCommissionInformationList() {
        return detailedCommissionInformationList;
    }

    public void setDetailedCommissionInformationList(List<DetailedCommissionInformation> detailedCommissionInformationList) {
        this.detailedCommissionInformationList = detailedCommissionInformationList;
    }

}
