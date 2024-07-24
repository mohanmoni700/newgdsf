
package com.compassites.model.travelomatrix.ResponseModels.UpdatePNR;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

public class UpdatePNRResponse {

    @JsonProperty("BookingDetails")
    private BookingDetails bookingDetails;
    @JsonProperty("Message")
    private String message;
    @JsonProperty("Status")
    private Long status;

    public BookingDetails getBookingDetails() {
        return bookingDetails;
    }

    public void setBookingDetails(BookingDetails bookingDetails) {
        this.bookingDetails = bookingDetails;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getStatus() {
        return status;
    }

    public void setStatus(Long status) {
        this.status = status;
    }

}
