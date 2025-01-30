package com.compassites.model.amadeus.reissue;

import com.compassites.model.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ReIssuePricingInformation implements Serializable {

    private boolean isLCC;

    private int adtCount;

    private int chdCount;

    private int infCount;

    private BigDecimal basePrice;

    private BigDecimal tax;

    private BigDecimal totalPrice;

    private BigDecimal grandTotalDifferenceAmountD;

    private BigDecimal totalTaxDifferenceB;

    private BigDecimal totalPenaltyAmountP;

    private BigDecimal totalAdditionalCollectionA;

    private BigDecimal mcoResidualValueC;

    private BigDecimal grandTotalValueM;

    private BigDecimal newTaxN;

    private String currency;

    private String gdsCurrency;

    private BigDecimal totalPriceValue;

    private Map<String, BigDecimal> taxMap;

    private BigDecimal discount;

    private BigDecimal totalCalculatedValue;

    private String provider;

    private boolean segmentWisePricing;

    private List<SegmentPricing> segmentPricingList;

    private boolean isTotalAmountConverted;

    private Map<String, TSTPrice> tstPriceMap;

    private String fareSourceCode;

    private String pricingOfficeId;

    private MnrSearchBaggage mnrSearchBaggage;

    private List<ReIssuePerPaxPricingInfo> paxWisePricing;



    private BigDecimal adtBasePrice;

    private BigDecimal chdBasePrice;

    private BigDecimal infBasePrice;

    private BigDecimal adtTotalPrice;

    private BigDecimal chdTotalPrice;

    private BigDecimal infTotalPrice;

    private List<PassengerTax> passengerTaxes;

    private List<PAXFareDetails> paxFareDetailsList;

    private BigDecimal totalBasePrice;

    private BigDecimal totalTax;

    public boolean isLCC() {
        return isLCC;
    }

    public void setLCC(boolean LCC) {
        isLCC = LCC;
    }

    public int getAdtCount() {
        return adtCount;
    }

    public void setAdtCount(int adtCount) {
        this.adtCount = adtCount;
    }

    public int getChdCount() {
        return chdCount;
    }

    public void setChdCount(int chdCount) {
        this.chdCount = chdCount;
    }

    public int getInfCount() {
        return infCount;
    }

    public void setInfCount(int infCount) {
        this.infCount = infCount;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public BigDecimal getGrandTotalDifferenceAmountD() {
        return grandTotalDifferenceAmountD;
    }

    public void setGrandTotalDifferenceAmountD(BigDecimal grandTotalDifferenceAmountD) {
        this.grandTotalDifferenceAmountD = grandTotalDifferenceAmountD;
    }

    public BigDecimal getTotalTaxDifferenceB() {
        return totalTaxDifferenceB;
    }

    public void setTotalTaxDifferenceB(BigDecimal totalTaxDifferenceB) {
        this.totalTaxDifferenceB = totalTaxDifferenceB;
    }

    public BigDecimal getTotalPenaltyAmountP() {
        return totalPenaltyAmountP;
    }

    public void setTotalPenaltyAmountP(BigDecimal totalPenaltyAmountP) {
        this.totalPenaltyAmountP = totalPenaltyAmountP;
    }

    public BigDecimal getTotalAdditionalCollectionA() {
        return totalAdditionalCollectionA;
    }

    public void setTotalAdditionalCollectionA(BigDecimal totalAdditionalCollectionA) {
        this.totalAdditionalCollectionA = totalAdditionalCollectionA;
    }

    public BigDecimal getMcoResidualValueC() {
        return mcoResidualValueC;
    }

    public void setMcoResidualValueC(BigDecimal mcoResidualValueC) {
        this.mcoResidualValueC = mcoResidualValueC;
    }

    public BigDecimal getGrandTotalValueM() {
        return grandTotalValueM;
    }

    public void setGrandTotalValueM(BigDecimal grandTotalValueM) {
        this.grandTotalValueM = grandTotalValueM;
    }

    public BigDecimal getNewTaxN() {
        return newTaxN;
    }

    public void setNewTaxN(BigDecimal newTaxN) {
        this.newTaxN = newTaxN;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getGdsCurrency() {
        return gdsCurrency;
    }

    public void setGdsCurrency(String gdsCurrency) {
        this.gdsCurrency = gdsCurrency;
    }

    public BigDecimal getTotalPriceValue() {
        return totalPriceValue;
    }

    public void setTotalPriceValue(BigDecimal totalPriceValue) {
        this.totalPriceValue = totalPriceValue;
    }

    public Map<String, BigDecimal> getTaxMap() {
        return taxMap;
    }

    public void setTaxMap(Map<String, BigDecimal> taxMap) {
        this.taxMap = taxMap;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getTotalCalculatedValue() {
        return totalCalculatedValue;
    }

    public void setTotalCalculatedValue(BigDecimal totalCalculatedValue) {
        this.totalCalculatedValue = totalCalculatedValue;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isSegmentWisePricing() {
        return segmentWisePricing;
    }

    public void setSegmentWisePricing(boolean segmentWisePricing) {
        this.segmentWisePricing = segmentWisePricing;
    }

    public List<SegmentPricing> getSegmentPricingList() {
        return segmentPricingList;
    }

    public void setSegmentPricingList(List<SegmentPricing> segmentPricingList) {
        this.segmentPricingList = segmentPricingList;
    }

    public boolean isTotalAmountConverted() {
        return isTotalAmountConverted;
    }

    public void setTotalAmountConverted(boolean totalAmountConverted) {
        isTotalAmountConverted = totalAmountConverted;
    }

    public Map<String, TSTPrice> getTstPriceMap() {
        return tstPriceMap;
    }

    public void setTstPriceMap(Map<String, TSTPrice> tstPriceMap) {
        this.tstPriceMap = tstPriceMap;
    }

    public String getFareSourceCode() {
        return fareSourceCode;
    }

    public void setFareSourceCode(String fareSourceCode) {
        this.fareSourceCode = fareSourceCode;
    }

    public String getPricingOfficeId() {
        return pricingOfficeId;
    }

    public void setPricingOfficeId(String pricingOfficeId) {
        this.pricingOfficeId = pricingOfficeId;
    }

    public MnrSearchBaggage getMnrSearchBaggage() {
        return mnrSearchBaggage;
    }

    public void setMnrSearchBaggage(MnrSearchBaggage mnrSearchBaggage) {
        this.mnrSearchBaggage = mnrSearchBaggage;
    }

    public List<ReIssuePerPaxPricingInfo> getPaxWisePricing() {
        return paxWisePricing;
    }

    public void setPaxWisePricing(List<ReIssuePerPaxPricingInfo> paxWisePricing) {
        this.paxWisePricing = paxWisePricing;
    }


    public BigDecimal getAdtBasePrice() {
        return adtBasePrice;
    }

    public void setAdtBasePrice(BigDecimal adtBasePrice) {
        this.adtBasePrice = adtBasePrice;
    }

    public BigDecimal getChdBasePrice() {
        return chdBasePrice;
    }

    public void setChdBasePrice(BigDecimal chdBasePrice) {
        this.chdBasePrice = chdBasePrice;
    }

    public BigDecimal getInfBasePrice() {
        return infBasePrice;
    }

    public void setInfBasePrice(BigDecimal infBasePrice) {
        this.infBasePrice = infBasePrice;
    }

    public BigDecimal getAdtTotalPrice() {
        return adtTotalPrice;
    }

    public void setAdtTotalPrice(BigDecimal adtTotalPrice) {
        this.adtTotalPrice = adtTotalPrice;
    }

    public BigDecimal getChdTotalPrice() {
        return chdTotalPrice;
    }

    public void setChdTotalPrice(BigDecimal chdTotalPrice) {
        this.chdTotalPrice = chdTotalPrice;
    }

    public BigDecimal getInfTotalPrice() {
        return infTotalPrice;
    }

    public void setInfTotalPrice(BigDecimal infTotalPrice) {
        this.infTotalPrice = infTotalPrice;
    }

    public List<PassengerTax> getPassengerTaxes() {
        return passengerTaxes;
    }

    public void setPassengerTaxes(List<PassengerTax> passengerTaxes) {
        this.passengerTaxes = passengerTaxes;
    }

    public List<PAXFareDetails> getPaxFareDetailsList() {
        return paxFareDetailsList;
    }

    public void setPaxFareDetailsList(List<PAXFareDetails> paxFareDetailsList) {
        this.paxFareDetailsList = paxFareDetailsList;
    }

    public BigDecimal getTotalBasePrice() {
        return totalBasePrice;
    }

    public void setTotalBasePrice(BigDecimal totalBasePrice) {
        this.totalBasePrice = totalBasePrice;
    }

    public BigDecimal getTotalTax() {
        return totalTax;
    }

    public void setTotalTax(BigDecimal totalTax) {
        this.totalTax = totalTax;
    }

}
