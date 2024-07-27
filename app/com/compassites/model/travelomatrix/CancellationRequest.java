package com.compassites.model.travelomatrix;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;

public class CancellationRequest implements Serializable {

    @JsonProperty("AppReference")
    public String appReference;

    @JsonProperty("SequenceNumber")
    public int sequenceNumber;

    @JsonProperty("BookingId")
    public String bookingId;

    @JsonProperty("PNR")
    public String PNR;

    @JsonProperty("TicketId")
    public ArrayList<String> ticketId;

    @JsonProperty("IsFullBookingCancel")
    public boolean isFullBookingCancel;

    public String getAppReference() {
        return appReference;
    }

    public void setAppReference(String appReference) {
        this.appReference = appReference;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getPNR() {
        return PNR;
    }

    public void setpNR(String PNR) {
        this.PNR = PNR;
    }

    public ArrayList<String> getTicketId() {
        return ticketId;
    }

    public void setTicketId(ArrayList<String> ticketId) {
        this.ticketId = ticketId;
    }

    public boolean isFullBookingCancel() {
        return isFullBookingCancel;
    }

    public void setisFullBookingCancel(boolean isfullBookingCancel) {
        isFullBookingCancel = isfullBookingCancel;
    }
}
