package com.compassites.model;

import com.compassites.model.amadeus.reissue.ReIssuePerPaxPricingInfo;
import models.PreloadedSeamanFareRules;

import javax.persistence.Transient;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by mahendra-singh on 23/5/14.
 */
public class PricingInformation implements Serializable {

    private boolean isLCC;

    private BigDecimal basePrice;

    private BigDecimal adtBasePrice;

    private BigDecimal chdBasePrice;

    private BigDecimal infBasePrice;

    private BigDecimal adtOnwardBasePrice;

    private BigDecimal chdOnwardBasePrice;

    private BigDecimal infOnwardBasePrice;

    private BigDecimal adtReturnBasePrice;

    private BigDecimal chdReturnBasePrice;

    private BigDecimal infReturnBasePrice;

    private PreloadedSeamanFareRules preloadedSeamanFareRules;


    @Transient
    private BigDecimal grandTotalDifferenceAmountD;

    @Transient
    private BigDecimal totalTaxDifferenceB;

    @Transient
    private BigDecimal totalPenaltyAmountP;

    @Transient
    private BigDecimal totalAdditionalCollectionA;

    @Transient
    private BigDecimal mcoResidualValueC;

    @Transient
    private BigDecimal grandTotalValueM;

    @Transient
    private List<ReIssuePerPaxPricingInfo> paxWisePricing;
    private Map<String, Object> preloadedSeamanFareRulesMap; // Adjust the type as necessary

    // Getters and Setters
    public Map<String, Object> getPreloadedSeamanFareRulesMap() {
        return preloadedSeamanFareRulesMap;
    }

    public void setPreloadedSeamanFareRulesMap(Map<String, Object> preloadedSeamanFareRulesMap) {
        this.preloadedSeamanFareRulesMap = preloadedSeamanFareRulesMap;
    }

    public BigDecimal getAdtOnwardBasePrice() {
        return adtOnwardBasePrice;
    }

    public void setAdtOnwardBasePrice(BigDecimal adtOnwardBasePrice) {
        this.adtOnwardBasePrice = adtOnwardBasePrice;
    }

    public BigDecimal getChdOnwardBasePrice() {
        return chdOnwardBasePrice;
    }

    public void setChdOnwardBasePrice(BigDecimal chdOnwardBasePrice) {
        this.chdOnwardBasePrice = chdOnwardBasePrice;
    }

    public BigDecimal getInfOnwardBasePrice() {
        return infOnwardBasePrice;
    }

    public void setInfOnwardBasePrice(BigDecimal infOnwardBasePrice) {
        this.infOnwardBasePrice = infOnwardBasePrice;
    }

    public BigDecimal getAdtReturnBasePrice() {
        return adtReturnBasePrice;
    }

    public void setAdtReturnBasePrice(BigDecimal adtReturnBasePrice) {
        this.adtReturnBasePrice = adtReturnBasePrice;
    }

    public BigDecimal getChdReturnBasePrice() {
        return chdReturnBasePrice;
    }

    public void setChdReturnBasePrice(BigDecimal chdReturnBasePrice) {
        this.chdReturnBasePrice = chdReturnBasePrice;
    }

    public BigDecimal getInfReturnBasePrice() {
        return infReturnBasePrice;
    }

    public void setInfReturnBasePrice(BigDecimal infReturnBasePrice) {
        this.infReturnBasePrice = infReturnBasePrice;
    }

    private BigDecimal adtTotalPrice;

    private BigDecimal chdTotalPrice;

    private BigDecimal infTotalPrice;

    private BigDecimal tax;

    private BigDecimal totalPrice;

    private String currency;

    private String gdsCurrency;

    private BigDecimal totalPriceValue;

    private List<PassengerTax> passengerTaxes;

    private List<PAXFareDetails> paxFareDetailsList;

    private Map<String, BigDecimal> taxMap;

    private BigDecimal totalBasePrice;

    private BigDecimal totalTax;

    private BigDecimal discount;

    private BigDecimal totalCalculatedValue;

    private BigDecimal cancelFee;

    private String fareRules;

    private String provider;

    private boolean segmentWisePricing;

    private List<SegmentPricing> segmentPricingList;

    private boolean isTotalAmountConverted;

    private Map<String, TSTPrice> tstPriceMap;

    private String fareSourceCode;

    private String pricingOfficeId;

    private BigDecimal onwardTotalBasePrice;

    private BigDecimal returnTotalBasePrice;

    private Boolean isPriceChanged;

    private MnrSearchFareRules mnrSearchFareRules;

    private MnrSearchBaggage mnrSearchBaggage;

    public List<ReIssuePerPaxPricingInfo> getPaxWisePricing() {
        return paxWisePricing;
    }

    public PreloadedSeamanFareRules getPreloadedSeamanFareRules() {
        return preloadedSeamanFareRules;
    }

    public void setPreloadedSeamanFareRules(PreloadedSeamanFareRules preloadedSeamanFareRules) {
        this.preloadedSeamanFareRules = preloadedSeamanFareRules;
    }

    public void setPaxWisePricing(List<ReIssuePerPaxPricingInfo> paxWisePricing) {
        this.paxWisePricing = paxWisePricing;
    }

    public PricingInformation() {
        paxFareDetailsList = new ArrayList<>();
        segmentWisePricing = false;
        segmentPricingList = new ArrayList<>();
        adtBasePrice = new BigDecimal(0);
        chdBasePrice = new BigDecimal(0);
        infBasePrice = new BigDecimal(0);
    }

    public Boolean getPriceChanged() {
        return isPriceChanged;
    }

    public void setPriceChanged(Boolean priceChanged) {
        isPriceChanged = priceChanged;
    }

    public BigDecimal getOnwardTotalBasePrice() {
        return onwardTotalBasePrice;
    }

    public void setOnwardTotalBasePrice(BigDecimal onwardTotalBasePrice) {
        this.onwardTotalBasePrice = onwardTotalBasePrice;
    }

    public BigDecimal getReturnTotalBasePrice() {
        return returnTotalBasePrice;
    }

    public void setReturnTotalBasePrice(BigDecimal returnTotalBasePrice) {
        this.returnTotalBasePrice = returnTotalBasePrice;
    }


    public String getPricingOfficeId() {
        return pricingOfficeId;
    }

    public void setPricingOfficeId(String pricingOfficeId) {
        this.pricingOfficeId = pricingOfficeId;
    }

    public boolean isLCC() {
        return isLCC;
    }

    public void setLCC(boolean isLCC) {
        this.isLCC = isLCC;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getTotalPriceValue() {
        return totalPriceValue;
    }

    public void setTotalPriceValue(BigDecimal totalPriceValue) {
        this.totalPriceValue = totalPriceValue;
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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Map<String, BigDecimal> getTaxMap() {
        return taxMap;
    }

    public void setTaxMap(Map<String, BigDecimal> taxMap) {
        this.taxMap = taxMap;
    }

    public BigDecimal getTotalBasePrice() {
        return totalBasePrice;
    }

    public void setTotalBasePrice(BigDecimal totalBasePrice) {
        this.totalBasePrice = totalBasePrice;
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

    public String getFareRules() {
        return fareRules;
    }

    public void setFareRules(String fareRules) {
        this.fareRules = fareRules;
    }

    public String getGdsCurrency() {
        return gdsCurrency;
    }

    public void setGdsCurrency(String gdsCurrency) {
        this.gdsCurrency = gdsCurrency;
    }

    public BigDecimal getTotalTax() {
        return totalTax;
    }

    public void setTotalTax(BigDecimal totalTax) {
        this.totalTax = totalTax;
    }

    public BigDecimal getCancelFee() {
        return cancelFee;
    }

    public void setCancelFee(BigDecimal cancelFee) {
        this.cancelFee = cancelFee;
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

    public void setInfTotalPrice(BigDecimal infTotalPrice) {
        this.infTotalPrice = infTotalPrice;
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

    public String getFareSourceCode() {
        return fareSourceCode;
    }

    public void setFareSourceCode(String fareSourceCode) {
        this.fareSourceCode = fareSourceCode;
    }

    @Override
    public String toString() {
        return "PricingInformation [isLCC=" + isLCC + ", basePrice=" + basePrice + ", adtBasePrice=" + adtBasePrice
                + ", chdBasePrice=" + chdBasePrice + ", infBasePrice=" + infBasePrice + ", adtTotalPrice="
                + adtTotalPrice + ", chdTotalPrice=" + chdTotalPrice + ", infTotalPrice=" + infTotalPrice + ", tax="
                + tax + ", totalPrice=" + totalPrice + ", currency=" + currency + ", gdsCurrency=" + gdsCurrency
                + ", totalPriceValue=" + totalPriceValue + ", passengerTaxes=" + passengerTaxes
                + ", paxFareDetailsList=" + paxFareDetailsList + ", taxMap=" + taxMap + ", totalBasePrice="
                + totalBasePrice + ", totalTax=" + totalTax + ", discount=" + discount + ", totalCalculatedValue="
                + totalCalculatedValue + ", cancelFee=" + cancelFee + ", fareRules=" + fareRules + ", provider="
                + provider + ", segmentWisePricing=" + segmentWisePricing + ", segmentPricingList=" + segmentPricingList
                + "]";
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

    public MnrSearchFareRules getMnrSearchFareRules() {
        return mnrSearchFareRules;
    }

    public void setMnrSearchFareRules(MnrSearchFareRules mnrSearchFareRules) {
        this.mnrSearchFareRules = mnrSearchFareRules;
    }

    public MnrSearchBaggage getMnrSearchBaggage() {
        return mnrSearchBaggage;
    }

    public void setMnrSearchBaggage(MnrSearchBaggage mnrSearchBaggage) {
        this.mnrSearchBaggage = mnrSearchBaggage;
    }
}
