package com.compassites.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.Property;

import javax.xml.datatype.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Renu on 7/18/14.
 */
public class Journey
{

    private Duration travelTime;
    private String travelTimeStr;
    private Long travelTimeMillis;
    private String airlinesStrForFilter;
    private Integer noOfStops;
    @JsonIgnore
    private Integer hashCode;

    private String provider;

    public Integer getHashCode() {
        return hashCode;
    }
    @Property
    private List<AirSegmentInformation> airSegmentList;
    public Journey(){
        airSegmentList=new ArrayList<AirSegmentInformation>();
    }

    public List<AirSegmentInformation> getAirSegmentList() {
        this.hashCode=hashCode();
        return airSegmentList;
    }

    public void setAirSegmentList(List<AirSegmentInformation> airSegmentList) {
        this.hashCode=hashCode();
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
        int hours = 0;
        if (travelTime.getDays() > 0)
        	hours = travelTime.getDays() * 24;
        if (travelTime.getHours() > 0)
        	hours = hours + travelTime.getHours();
        if(hours > 0)
			timeStr = timeStr + hours + " Hour(s) ";
        if (travelTime.getMinutes() > 0)
            timeStr = timeStr + travelTime.getMinutes() +" Minutes ";
        if (travelTime.getSeconds() > 0)
            timeStr = timeStr + travelTime.getSeconds() +" Seconds ";
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

    public String getAirlinesStrForFilter() {
        return airlinesStrForFilter;
    }

    public void setAirlinesStrForFilter(String airlinesStrForFilter) {
        this.airlinesStrForFilter = airlinesStrForFilter;
    }

    public Integer getNoOfStops() {
        return noOfStops;
    }

    public void setNoOfStops(Integer noOfStops) {
        this.noOfStops = noOfStops;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}