package dto;

import java.math.BigInteger;

public class AirlineSpecificQueueAndTimeLimitDetails {

    private String airline;

    private String date;

    private String time;

    private String timeZone;

    private String mainQueueOffice;

    private BigInteger queueNumber;

    private BigInteger category;

    public String getAirline() {
        return airline;
    }

    public void setAirline(String airline) {
        this.airline = airline;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getMainQueueOffice() {
        return mainQueueOffice;
    }

    public void setMainQueueOffice(String mainQueueOffice) {
        this.mainQueueOffice = mainQueueOffice;
    }

    public BigInteger getQueueNumber() {
        return queueNumber;
    }

    public void setQueueNumber(BigInteger queueNumber) {
        this.queueNumber = queueNumber;
    }

    public BigInteger getCategory() {
        return category;
    }

    public void setCategory(BigInteger category) {
        this.category = category;
    }

}
