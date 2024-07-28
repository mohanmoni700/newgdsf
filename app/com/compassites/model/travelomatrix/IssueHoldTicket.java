
package com.compassites.model.travelomatrix;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;


public class IssueHoldTicket {

    @JsonProperty("AppReference")
    private String appReference;
    @JsonProperty("BookingId")
    private String bookingId;
    @JsonProperty("Pnr")
    private String pnr;
    @JsonProperty("SequenceNumber")
    private String sequenceNumber;

    public String getAppReference() {
        return appReference;
    }

    public void setAppReference(String appReference) {
        this.appReference = appReference;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getPnr() {
        return pnr;
    }

    public void setPnr(String pnr) {
        this.pnr = pnr;
    }

    public String getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(String sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

}
