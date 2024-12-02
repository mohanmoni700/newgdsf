package com.compassites.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mahendra-singh on 1/10/14.
 */
public class PAXFareDetails implements Serializable {

    private List<FareJourney> fareJourneyList;

    private PassengerTypeCode passengerTypeCode;

    public PAXFareDetails() {
        fareJourneyList = new ArrayList<>();
    }

    public List<FareJourney> getFareJourneyList() {
        return fareJourneyList;
    }

    public void setFareJourneyList(List<FareJourney> fareJourneyList) {
        this.fareJourneyList = fareJourneyList;
    }

    public PassengerTypeCode getPassengerTypeCode() {
        return passengerTypeCode;
    }

    public void setPassengerTypeCode(PassengerTypeCode passengerTypeCode) {
        this.passengerTypeCode = passengerTypeCode;
    }
}
