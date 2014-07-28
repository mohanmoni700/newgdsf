package com.compassites.model;

import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.Property;

import javax.xml.datatype.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Renu on 7/18/14.
 */
public class Journey{

    private Duration travelTime;
    private String travelTimeStr;
    private Long travelTimeMillis;

    @Property
    private List<AirSegmentInformation> airSegmentList;
    public Journey(){
        airSegmentList=new ArrayList<AirSegmentInformation>();
    }

    public List<AirSegmentInformation> getAirSegmentList() {
        return airSegmentList;
    }

    public void setAirSegmentList(List<AirSegmentInformation> airSegmentList) {
        this.airSegmentList = airSegmentList;
    }
    public Duration getTravelTime() {
        return travelTime;
    }

    public void setTravelTime(Duration travelTime) {
        this.travelTime = travelTime;
        this.setTravelTimeStr();
    }

    public void setTravelTimeStr() {
        String timeStr ="" ;
        if (this.travelTime == null){
            this.travelTimeStr = "NULL!!!";
            this.travelTimeMillis = Long.valueOf(0);
            return;
        }
        if (travelTime.getMonths() > 0){
            timeStr = timeStr + travelTime.getMonths() +" Month(s) ";
        }
        if (travelTime.getDays() > 0){
            timeStr = timeStr + travelTime.getDays() +" Day(s) ";
        }
        if (travelTime.getHours() > 0){
            timeStr = timeStr + travelTime.getHours() +" Hour(s) ";
        }
        if (travelTime.getMinutes() > 0){
            timeStr = timeStr + travelTime.getMinutes() +" Minutes ";
        }
        if (travelTime.getSeconds() > 0){
            timeStr = timeStr + travelTime.getSeconds() +" Seconds ";
        }
        this.travelTimeStr = timeStr;
        Calendar c = Calendar.getInstance();
        this.travelTimeMillis = travelTime.getTimeInMillis(c);
    }

    public String getTravelTimeStr(){
        return this.travelTimeStr;
    }

    @Override
    public boolean equals(Object obj) {
        return Pojomatic.equals(this, obj);
    }


    @Override
    public int hashCode() {
        return Pojomatic.hashCode(this);
    }

    @Override
    public String toString() {
        return Pojomatic.toString(this);
    }

    public Long getTravelTimeMillis() {
        return travelTimeMillis;
    }

    public void setTravelTimeMillis(Long travelTimeMillis) {
        this.travelTimeMillis = travelTimeMillis;
    }
}