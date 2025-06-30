package dto;

public class FreeSeatDetails {

    private String origin;
    private String destination;
    private String seatStatus;
    private String seatStatusDescription;
    private String seatNumber;
    private String seatName;
    private String seatType;
    private String seatCode;
    private String amadeusPaxReference;
    private String paxName;
    private String comments;
    private String airlineName;
    private String airlineCode;
    private String flightNumber;
    private String amadeusSegmentRef;
    private String departureDate;

    public String getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(String departureDate) {
        this.departureDate = departureDate;
    }

    public String getAmadeusSegmentRef() {
        return amadeusSegmentRef;
    }

    public void setAmadeusSegmentRef(String amadeusSegmentRef) {
        this.amadeusSegmentRef = amadeusSegmentRef;
    }

    public String getAirlineName() {
        return airlineName;
    }

    public void setAirlineName(String airlineName) {
        this.airlineName = airlineName;
    }

    public String getAirlineCode() {
        return airlineCode;
    }

    public void setAirlineCode(String airlineCode) {
        this.airlineCode = airlineCode;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
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

    public String getSeatStatus() {
        return seatStatus;
    }

    public void setSeatStatus(String seatStatus) {
        this.seatStatus = seatStatus;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getSeatName() {
        return seatName;
    }

    public void setSeatName(String seatName) {
        this.seatName = seatName;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public String getSeatCode() {
        return seatCode;
    }

    public void setSeatCode(String seatCode) {
        this.seatCode = seatCode;
    }

    public String getAmadeusPaxReference() {
        return amadeusPaxReference;
    }

    public void setAmadeusPaxReference(String amadeusPaxReference) {
        this.amadeusPaxReference = amadeusPaxReference;
    }

    public String getPaxName() {
        return paxName;
    }

    public void setPaxName(String paxName) {
        this.paxName = paxName;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getSeatStatusDescription() {
        return seatStatusDescription;
    }

    public void setSeatStatusDescription(String seatStatusDescription) {
        this.seatStatusDescription = seatStatusDescription;
    }

}
