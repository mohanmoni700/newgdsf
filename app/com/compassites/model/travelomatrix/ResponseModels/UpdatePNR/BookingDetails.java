
package com.compassites.model.travelomatrix.ResponseModels.UpdatePNR;

import java.util.List;
import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingDetails {

    @JsonProperty("AppReference")
    private String appReference;
    @JsonProperty("BookingItineraryDetails")
    private List<BookingItineraryDetail> bookingItineraryDetails;
    @JsonProperty("BoookingTransaction")
    private List<BoookingTransaction> boookingTransaction;
    @JsonProperty("MasterBookingStatus")
    private String masterBookingStatus;

    public String getAppReference() {
        return appReference;
    }

    public void setAppReference(String appReference) {
        this.appReference = appReference;
    }

    public List<BookingItineraryDetail> getBookingItineraryDetails() {
        return bookingItineraryDetails;
    }

    public void setBookingItineraryDetails(List<BookingItineraryDetail> bookingItineraryDetails) {
        this.bookingItineraryDetails = bookingItineraryDetails;
    }

    public List<BoookingTransaction> getBoookingTransaction() {
        return boookingTransaction;
    }

    public void setBoookingTransaction(List<BoookingTransaction> boookingTransaction) {
        this.boookingTransaction = boookingTransaction;
    }

    public String getMasterBookingStatus() {
        return masterBookingStatus;
    }

    public void setMasterBookingStatus(String masterBookingStatus) {
        this.masterBookingStatus = masterBookingStatus;
    }

}
