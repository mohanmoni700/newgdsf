package com.compassites.model;

import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.Property;

import java.io.Serializable;
import java.util.Date;


/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/23/14
 * Time: 4:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class AirSegmentInformation implements Serializable{
    public static final long serialVersionUID = 42L;
    @Property
    private String fromLocation;
    @Property
    private String toLocation;

    private String fromTerminal;

    public String getFromTerminal() {
        return fromTerminal;
    }

    public void setFromTerminal(String fromTerminal) {
        this.fromTerminal = fromTerminal;
    }

    public String getToTerminal() {
        return toTerminal;
    }

    public void setToTerminal(String toTerminal) {
        this.toTerminal = toTerminal;
    }

    private String toTerminal;

    private String fromDate;
    private String toDate;

    private String arrivalTime;

    private String departureTime;

    private String distanceTravelled;
    private String distanceUnit;
    private String travelTime;
    @Property
    private String flightNumber;
    @Property
    private String carrierCode;

    private Date departureDate;


    public String getFromLocation() {
        return fromLocation;
    }

    public void setFromLocation(String fromLocation) {
        this.fromLocation = fromLocation;
    }

    public String getToLocation() {
        return toLocation;
    }

    public void setToLocation(String toLocation) {
        this.toLocation = toLocation;
    }

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;

       /* SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        try {
            this.departureDate =  sdf.parse(departureTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }*/
    }

    public String getDistanceTravelled() {
        return distanceTravelled;
    }

    public void setDistanceTravelled(String distanceTravelled) {
        this.distanceTravelled = distanceTravelled;
    }

    public String getDistanceUnit() {
        return distanceUnit;
    }

    public void setDistanceUnit(String distanceUnit) {
        this.distanceUnit = distanceUnit;
    }

    public String getTravelTime() {
        return travelTime;
    }

    public void setTravelTime(String travelTime) {
        this.travelTime = travelTime;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getCarrierCode() {
        return carrierCode;
    }

    public void setCarrierCode(String carrierCode) {
        this.carrierCode = carrierCode;
    }

    public Date getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(Date departureDate) {
        this.departureDate = departureDate;
    }

    /* @Override
    public boolean equals(Object obj) {
        AirSegmentInformation airSegmentInformation = null;
        if(obj instanceof  AirSegmentInformation) {
            airSegmentInformation = (AirSegmentInformation) obj;
        }
        if(!(this.fromLocation.equals(airSegmentInformation.getFromLocation()) && this.toLocation.equals(airSegmentInformation.getToLocation()))){
            return  false;
        }
        if(!this.carrierCode.equals(airSegmentInformation.getCarrierCode())){
            return false;
        }
        if(!this.flightNumber.equals(airSegmentInformation.getFlightNumber())){
            return  false;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        Date date = null,date1 = null;
        try {
            date = sdf.parse(this.departureTime);
            date1 = sdf.parse(airSegmentInformation.getDepartureTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
        int dateComapareResult = date.compareTo(date1);
        if(dateComapareResult != 0){
          return false;
        }

        return true;
    }*/


    @Override
    public boolean equals(Object obj) {
        return Pojomatic.equals(this,obj);
    }


    @Override
    public int hashCode() {
        return Pojomatic.hashCode(this);
    }

    @Override
    public String toString() {
        return Pojomatic.toString(this);
    }
}
