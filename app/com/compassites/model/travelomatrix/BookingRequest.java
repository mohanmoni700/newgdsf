package com.compassites.model.travelomatrix;

import java.io.Serializable;
import java.util.ArrayList;

public class BookingRequest implements Serializable {

    public String appReference;

    public int sequenceNumber;

    public String resultToken;

    public ArrayList<TMPassenger> passengers;

    public String getAppReference() {
        return appReference;
    }

    public void setAppReference(String appReference) {
        this.appReference = appReference;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getResultToken() {
        return resultToken;
    }

    public void setResultToken(String resultToken) {
        this.resultToken = resultToken;
    }

    public ArrayList<TMPassenger> getPassengers() {
        return passengers;
    }

    public void setPassengers(ArrayList<TMPassenger> passengers) {
        this.passengers = passengers;
    }
}
