
package com.compassites.model.travelomatrix;

import java.util.List;
import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommitBookingRequest {
    @JsonProperty("AppReference")
    private String appReference;
    @JsonProperty("Passengers")
    private List<Passenger> passengers;
    @JsonProperty("ResultToken")
    private String resultToken;
    @JsonProperty("SequenceNumber")
    private String sequenceNumber;

    public String getAppReference() {
        return appReference;
    }

    public void setAppReference(String appReference) {
        this.appReference = appReference;
    }

    public List<Passenger> getPassengers() {
        return passengers;
    }

    public void setPassengers(List<Passenger> passengers) {
        this.passengers = passengers;
    }

    public String getResultToken() {
        return resultToken;
    }

    public void setResultToken(String resultToken) {
        this.resultToken = resultToken;
    }

    public String getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(String sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

}
