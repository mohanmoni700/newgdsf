package com.compassites.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.joda.deser.DateTimeDeserializer;
import com.fasterxml.jackson.datatype.joda.ser.DateTimeSerializer;
import org.joda.time.DateTime;
import org.pojomatic.annotations.Property;
import utils.CustomDateTimeSerializer;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by mahendra-singh on 6/10/14.
 */
public class SearchJourney implements Serializable{

    @Property
    private String origin;
    @Property
    private String destination;
    @Property
    private Date travelDate;

    private String travelDateStr;

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

    public Date getTravelDate() {
        return travelDate;
    }

    public void setTravelDate(Date travelDate) {
        this.travelDate = travelDate;
    }

    public String getTravelDateStr() {
        return travelDateStr;
    }

    public void setTravelDateStr(String travelDateStr) {
        this.travelDateStr = travelDateStr;
    }
}
