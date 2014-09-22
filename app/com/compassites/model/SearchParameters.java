package com.compassites.model;


import org.pojomatic.annotations.Property;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/20/14
 * Time: 4:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class SearchParameters implements Serializable,Cloneable {
    @Property
    private String origin;
    @Property
    private String destination;
    private String stopOver;
    private String currency;
    private List<Passenger> passengers;
    private Boolean withReturnJourney;
    private Integer noOfStops;
    @Property
    private Integer adultCount;
    @Property
    private Integer childCount;
    @Property
    private Integer infantCount;
    @Property
    private Boolean refundableFlights;
    @Property
    private Boolean directFlights;
    private DateType dateType;
    private JourneyType journeyType;
    private String searchBookingType;
    @Property
    private BookingType bookingType;
    @Property
    private CabinClass cabinClass;

    private String preferredAirlines;
    private String preferredFood;
    @Property
    private Date fromDate;
    @Property
    private Date returnDate;
    private String transit;
    private String nationality;

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public SearchParameters(){
        withReturnJourney = false;
        currency = "INR";
        passengers = new ArrayList<Passenger>();
        refundableFlights = false;
        directFlights = false;
        dateType = DateType.DEPARTURE; //Arrival = arrival, Departure = departure
        bookingType = BookingType.SEAMEN;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getStopOver() {
        return stopOver;
    }

    public void setStopOver(String stopOver) {
        this.stopOver = stopOver;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<Passenger> getPassengers() {
        return passengers;
    }

    public void setPassengers(List<Passenger> passengers) {
        this.passengers = passengers;
    }

    public Boolean getWithReturnJourney() {
        return withReturnJourney;
    }

    public void setWithReturnJourney(Boolean withReturnJourney) {
        this.withReturnJourney = withReturnJourney;
    }

    public Integer getNoOfStops() {
        return noOfStops;
    }

    public void setNoOfStops(Integer noOfStops) {
        this.noOfStops = noOfStops;
    }

    public Integer getAdultCount() {
        return adultCount;
    }

    public void setAdultCount(Integer adultCount) {
        this.adultCount = adultCount;
    }

    public Integer getChildCount() {
        return childCount;
    }

    public void setChildCount(Integer childCount) {
        this.childCount = childCount;
    }

    public Integer getInfantCount() {
        return infantCount;
    }

    public void setInfantCount(Integer infantCount) {
        this.infantCount = infantCount;
    }

    public Boolean getRefundableFlights() {
        return refundableFlights;
    }

    public void setRefundableFlights(Boolean refundableFlights) {
        this.refundableFlights = refundableFlights;
    }

    public Boolean getDirectFlights() {
        return directFlights;
    }

    public void setDirectFlights(Boolean directFlights) {
        this.directFlights = directFlights;
    }

    public DateType getDateType() {
        return dateType;
    }

    public void setDateType(DateType dateType) {
        this.dateType = dateType;
    }

    public JourneyType getJourneyType() {
        return journeyType;
    }

    public void setJourneyType(JourneyType journeyType) {
        this.journeyType = journeyType;
    }

    public String getSearchBookingType() {
        return searchBookingType;
    }

    public void setSearchBookingType(String searchBookingType) {
        this.searchBookingType = searchBookingType;
    }

    public BookingType getBookingType() {
        return bookingType;
    }

    public void setBookingType(BookingType bookingType) {
        this.bookingType = bookingType;
    }

    public CabinClass getCabinClass() {
        return cabinClass;
    }

    public void setCabinClass(CabinClass cabinClass) {
        this.cabinClass = cabinClass;
    }

    public String getPreferredAirlines() {
        return preferredAirlines;
    }

    public void setPreferredAirlines(String preferredAirlines) {
        this.preferredAirlines = preferredAirlines;
    }

    public String getPreferredFood() {
        return preferredFood;
    }

    public void setPreferredFood(String preferredFood) {
        this.preferredFood = preferredFood;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(Date returnDate) {
        this.returnDate = returnDate;
    }

    public String getTransit() {
        return transit;
    }

    public void setTransit(String transit) {
        this.transit = transit;
    }

    public String redisKey(){
        Date journeyDate = this.fromDate;
        String key = "";
        key = key+ this.origin+ this.destination +"ADT:"+ this.adultCount +"CHD:"+ this.childCount +"INF:"+ this.infantCount+
                journeyDate.toString()+ this.cabinClass;
        key = key + "RF:"+this.refundableFlights + "DR:" + this.directFlights + "PA:" + this.preferredAirlines;
        key = key + "TR:"+this.transit+"DT:" + this.dateType + "BK" + this.bookingType+"JT"+this.journeyType+"RD"+this.returnDate.toString();
        return key;
    }

    public SearchParameters clone(){
        try {
            return (SearchParameters)super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*public String redisKey(){
        return Integer.toString(Pojomatic.hashCode(this));
    }*/
}
