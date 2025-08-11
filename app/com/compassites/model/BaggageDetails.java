package com.compassites.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public class BaggageDetails {

    private Long contactMasterId;

    private String segmentNumber;

    private String baggageId;

    private String code;

    private String destination;

    private String baggageDescription;

    private String origin;

    private Long price;

    private Long basePrice;

    private Long tax;

    private String weight;

    private String piece;

    private String rfic;

    private String rfisc;

    private boolean isRefundable;

    private boolean isMIF;

    private String bkm;

    private String serviceId;

    private String carrierCode;

    private boolean isFMT;

    private boolean isWVAL;

    private boolean isPVAL;

    private boolean isFTXT;

    private Long ticketId;

    private String tmxTicketNumber;

    private Boolean returnDetails;

    private String amadeusPaxRef;

    private List<String> segmentTattooList;

    public String getPiece() {
        return piece;
    }

    public void setPiece(String piece) {
        this.piece = piece;
    }

    public String getRfic() {
        return rfic;
    }

    public void setRfic(String rfic) {
        this.rfic = rfic;
    }

    public String getRfisc() {
        return rfisc;
    }

    public void setRfisc(String rfisc) {
        this.rfisc = rfisc;
    }

    public boolean isMIF() {
        return isMIF;
    }

    public void setMIF(boolean MIF) {
        isMIF = MIF;
    }

    public String getBkm() {
        return bkm;
    }

    public void setBkm(String bkm) {
        this.bkm = bkm;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getCarrierCode() {
        return carrierCode;
    }

    public void setCarrierCode(String carrierCode) {
        this.carrierCode = carrierCode;
    }

    public boolean isRefundable() {
        return isRefundable;
    }

    public void setRefundable(boolean refundable) {
        isRefundable = refundable;
    }

    public Boolean getReturnDetails() {
        return returnDetails;
    }

    public void setReturnDetails(Boolean returnDetails) {
        this.returnDetails = returnDetails;
    }

    public String getSegment() {
        return segment;
    }

    public String getBaggageId() {
        return baggageId;
    }

    public void setBaggageId(String baggageId) {
        this.baggageId = baggageId;
    }


    public void setSegment(String segment) {
        this.segment = segment;
    }

    private String segment;

    public String getTmxTicketNumber() {
        return tmxTicketNumber;
    }

    public void setTmxTicketNumber(String tmxTicketNumber) {
        this.tmxTicketNumber = tmxTicketNumber;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;

    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }


    public boolean isFMT() {
        return isFMT;
    }

    public void setFMT(boolean FMT) {
        isFMT = FMT;
    }

    public boolean isWVAL() {
        return isWVAL;
    }

    public void setWVAL(boolean WVAL) {
        isWVAL = WVAL;
    }

    public boolean isPVAL() {
        return isPVAL;
    }

    public void setPVAL(boolean PVAL) {
        isPVAL = PVAL;
    }

    public boolean isFTXT() {
        return isFTXT;
    }

    public void setFTXT(boolean FTXT) {
        isFTXT = FTXT;
    }

    public Long getContactMasterId() {
        return contactMasterId;
    }

    public void setContactMasterId(Long contactMasterId) {
        this.contactMasterId = contactMasterId;
    }

    public String getBaggageDescription() {
        return baggageDescription;
    }

    public void setBaggageDescription(String baggageDescription) {
        this.baggageDescription = baggageDescription;
    }

    public String getSegmentNumber() {
        return segmentNumber;
    }

    public void setSegmentNumber(String segmentNumber) {
        this.segmentNumber = segmentNumber;
    }

    public Long getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(Long basePrice) {
        this.basePrice = basePrice;
    }

    public Long getTax() {
        return tax;
    }

    public void setTax(Long tax) {
        this.tax = tax;
    }

    public String getAmadeusPaxRef() {
        return amadeusPaxRef;
    }

    public void setAmadeusPaxRef(String amadeusPaxRef) {
        this.amadeusPaxRef = amadeusPaxRef;
    }

    public List<String> getSegmentTattooList() {
        return segmentTattooList;
    }

    public void setSegmentTattooList(List<String> segmentTattooList) {
        this.segmentTattooList = segmentTattooList;
    }


}

