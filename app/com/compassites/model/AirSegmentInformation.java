package com.compassites.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.Property;

import models.Airline;
import models.Airport;


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
	
    @Property
    private String flightNumber;
   
    @Property
    private String carrierCode;
    
    private String operatingCarrierCode;

    private String fromTerminal;

    private String toTerminal;

    private String fromDate;
    
    private String toDate;

    private String arrivalTime;

    private String departureTime;

    private String distanceTravelled;
    
    private String distanceUnit;
    
    private String travelTime;
    
    private Integer connectionTime;
    
    private String connectionTimeStr;

    private Airline airline;
    
    private Airline operatingAirline;
    
    private Airport fromAirport;
    
    private Airport toAirport;

    private Date departureDate;
    
    private Date arrivalDate;

    private String bookingClass;

    private String airSegmentKey;

    private String flightDetailsKey;
    
    private String equipment;
    
    private FlightInfo flightInfo;

    private String fareBasis;

    private String contextType;

    private List<HoppingFlightInformation> hoppingFlightInformations = null;
    
    public List<HoppingFlightInformation> getHoppingFlightInformations() {
		return hoppingFlightInformations;
	}

	public void setHoppingFlightInformations(List<HoppingFlightInformation> hoppingFlightInformations) {
		this.hoppingFlightInformations = hoppingFlightInformations;
	}

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

    public String getOperatingCarrierCode() {
		return operatingCarrierCode;
	}

	public void setOperatingCarrierCode(String operatingCarrierCode) {
		this.operatingCarrierCode = operatingCarrierCode;
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

    public Date getArrivalDate() {
		return arrivalDate;
	}

	public void setArrivalDate(Date arrivalDate) {
		this.arrivalDate = arrivalDate;
	}

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

    public Airline getAirline() {
        return airline;
    }

    public void setAirline(Airline airline) {
        this.airline = airline;
    }

    public Airline getOperatingAirline() {
		return operatingAirline;
	}

	public void setOperatingAirline(Airline operatingAirline) {
		this.operatingAirline = operatingAirline;
	}

	public Airport getFromAirport() {
        return fromAirport;
    }

    public void setFromAirport(Airport fromAirport) {
        this.fromAirport = fromAirport;
    }

    public Airport getToAirport() {
        return toAirport;
    }

    public void setToAirport(Airport toAirport) {
        this.toAirport = toAirport;
    }

    public Integer getConnectionTime() {
        return connectionTime;
    }

    public void setConnectionTime(Integer connectionTime) {
        this.connectionTime = connectionTime;
        this.setConnectionTimeStr();
    }

    public String getConnectionTimeStr() {
        return connectionTimeStr;
    }

    public void setConnectionTimeStr() {

        this.connectionTimeStr = "" ;
        if (this.connectionTime == null || this.connectionTime == 0){
            return;
        }
        int h = this.connectionTime / 60;
        int m = this.connectionTime % 60;
        if (h > 0)
            this.connectionTimeStr = h +" Hour(s) ";
        if (m > 0)
            this.connectionTimeStr = this.connectionTimeStr + m +" Minutes";

    }

    public String getBookingClass() {
        return bookingClass;
    }

    public void setBookingClass(String bookingClass) {
        this.bookingClass = bookingClass;
    }

    public String getFlightDetailsKey() {
        return flightDetailsKey;
    }

    public void setFlightDetailsKey(String flightDetailsKey) {
        this.flightDetailsKey = flightDetailsKey;
    }

    public String getAirSegmentKey() {
        return airSegmentKey;
    }

    public void setAirSegmentKey(String airSegmentKey) {
        this.airSegmentKey = airSegmentKey;
    }

	public FlightInfo getFlightInfo() {
		return flightInfo;
	}

	public void setFlightInfo(FlightInfo flightInfo) {
		this.flightInfo = flightInfo;
	}

	public String getEquipment() {
		return equipment;
	}

	public void setEquipment(String equipment) {
		this.equipment = equipment;
	}

    public String getFareBasis() {
        return fareBasis;
    }

    public void setFareBasis(String fareBasis) {
        this.fareBasis = fareBasis;
    }

    public String getContextType() {
        return contextType;
    }

    public void setContextType(String contextType) {
        this.contextType = contextType;
    }
}
