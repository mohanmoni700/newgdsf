package com.compassites.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 16-09-2014.
 */
public class FareJourney implements Serializable {

    private List<FareSegment> fareSegmentList;

    public FareJourney() {
        fareSegmentList = new ArrayList<>();
    }

    public List<FareSegment> getFareSegmentList() {
        return fareSegmentList;
    }

    public void setFareSegmentList(List<FareSegment> fareSegmentList) {
        this.fareSegmentList = fareSegmentList;
    }


}
