
package com.compassites.model.travelomatrix.ResponseModels.CommitBookingReply;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;


public class CommitBooking {

    @JsonProperty("BookingDetails")
    private BookingDetails bookingDetails;

    public BookingDetails getBookingDetails() {
        return bookingDetails;
    }

    public void setBookingDetails(BookingDetails bookingDetails) {
        this.bookingDetails = bookingDetails;
    }

}
