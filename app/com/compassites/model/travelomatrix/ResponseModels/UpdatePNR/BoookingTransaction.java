
package com.compassites.model.travelomatrix.ResponseModels.UpdatePNR;

import java.util.List;
import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
@JsonIgnoreProperties(ignoreUnknown = true)
public class BoookingTransaction {

    @JsonProperty("BookingCustomer")
    private List<BookingCustomer> bookingCustomer;
    @JsonProperty("BookingID")
    private String bookingID;
    @JsonProperty("PNR")
    private String pNR;
    @JsonProperty("SequenceNumber")
    private String sequenceNumber;
    @JsonProperty("Status")
    private String status;

    public List<BookingCustomer> getBookingCustomer() {
        return bookingCustomer;
    }

    public void setBookingCustomer(List<BookingCustomer> bookingCustomer) {
        this.bookingCustomer = bookingCustomer;
    }

    public String getBookingID() {
        return bookingID;
    }

    public void setBookingID(String bookingID) {
        this.bookingID = bookingID;
    }

    public String getPNR() {
        return pNR;
    }

    public void setPNR(String pNR) {
        this.pNR = pNR;
    }

    public String getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(String sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
