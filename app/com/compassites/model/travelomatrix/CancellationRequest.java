package com.compassites.model.travelomatrix;

import java.io.Serializable;
import java.util.ArrayList;

public class CancellationRequest implements Serializable {

    public String appReference;

    public int sequenceNumber;

    public String bookingId;

    public String pNR;

    public ArrayList<Integer> ticketId;

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

    public String getpNR() {
        return pNR;
    }

    public void setpNR(String pNR) {
        this.pNR = pNR;
    }

    public ArrayList<Integer> getTicketId() {
        return ticketId;
    }

    public void setTicketId(ArrayList<Integer> ticketId) {
        this.ticketId = ticketId;
    }

    public boolean isFullBookingCancel() {
        return isFullBookingCancel;
    }

    public void setFullBookingCancel(boolean fullBookingCancel) {
        isFullBookingCancel = fullBookingCancel;
    }
}
