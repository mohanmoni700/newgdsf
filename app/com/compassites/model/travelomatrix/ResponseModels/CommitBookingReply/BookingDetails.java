
package com.compassites.model.travelomatrix.ResponseModels.CommitBookingReply;

import java.util.List;
import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingDetails {

    @JsonProperty("Attr")
    private Attr attr;
    @JsonProperty("BookingId")
    private String bookingId;
    @JsonProperty("JourneyList")
    private JourneyList journeyList;
    @JsonProperty("PNR")
    private String pNR;
    @JsonProperty("PassengerDetails")
    private List<PassengerDetail> passengerDetails;
    @JsonProperty("Price")
    private Price price;

    public Attr getAttr() {
        return attr;
    }

    public void setAttr(Attr attr) {
        this.attr = attr;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public JourneyList getJourneyList() {
        return journeyList;
    }

    public void setJourneyList(JourneyList journeyList) {
        this.journeyList = journeyList;
    }

    public String getPNR() {
        return pNR;
    }

    public void setPNR(String pNR) {
        this.pNR = pNR;
    }

    public List<PassengerDetail> getPassengerDetails() {
        return passengerDetails;
    }

    public void setPassengerDetails(List<PassengerDetail> passengerDetails) {
        this.passengerDetails = passengerDetails;
    }

    public Price getPrice() {
        return price;
    }

    public void setPrice(Price price) {
        this.price = price;
    }

}
