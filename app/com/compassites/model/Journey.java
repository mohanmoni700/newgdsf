package com.compassites.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.Property;

import javax.xml.datatype.Duration;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Renu on 7/18/14.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Journey implements Serializable
{

    private Duration travelTime;
    private String travelTimeStr;
    private Long travelTimeMillis;
    private String airlinesStrForFilter;
    private Integer noOfStops;
    private String segmentKey;

    private String fromLocation;

    private String toLocation;

    private String fullSegmentKey;

    private Boolean isRefundable;

    public String getFullSegmentKey() {
        return fullSegmentKey;
    }

    public void setFullSegmentKey(String fullSegmentKey) {
        this.fullSegmentKey = fullSegmentKey;
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

    public String getSegmentKey() {
        return segmentKey;
    }

    public void setSegmentKey(String segmentKey) {
        this.segmentKey = segmentKey;
    }

    @JsonIgnore
    private Integer hashCode;

    private String provider;

    private String groupingKey;

    private String fareDescription;

    private List<String> lastTktDate;

    public List<String> getLastTktDate() {
        return lastTktDate;
    }

    public void setLastTktDate(List<String> lastTktDate) {
        this.lastTktDate = lastTktDate;
    }

    public String getFareDescription() {
        return fareDescription;
    }

    public void setFareDescription(String fareDescription) {
        this.fareDescription = fareDescription;
    }

    public String getGroupingKey() {
        return groupingKey;
    }

    public void setGroupingKey(String groupingKey) {
        this.groupingKey = groupingKey;
    }

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

   /* @Override
    public String toString() {
        return Pojomatic.toString(this);
    }*/

    
    public Long getTravelTimeMillis() {
        return travelTimeMillis;
    }

    @Override
	public String toString() {
		return "Journey [travelTime=" + travelTime + ", travelTimeStr=" + travelTimeStr + ", travelTimeMillis="
				+ travelTimeMillis + ", airlinesStrForFilter=" + airlinesStrForFilter + ", noOfStops=" + noOfStops
				+ ", hashCode=" + hashCode + ", provider=" + provider + ", airSegmentList=" + airSegmentList + "]";
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

    public Boolean getRefundable() {
        return isRefundable;
    }

    public void setRefundable(Boolean refundable) {
        isRefundable = refundable;
    }
}