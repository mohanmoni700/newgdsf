package com.compassites.model;

import java.io.Serializable;

/**
 * Created by user on 16-09-2014.
 */
public class FareSegment implements Serializable {

    private String bookingClass;

    private String fareBasis;

    private String cabinClass;

    public String getBookingClass() {
        return bookingClass;
    }

    public void setBookingClass(String bookingClass) {
        this.bookingClass = bookingClass;
    }

    public String getFareBasis() {
        return fareBasis;
    }

    public void setFareBasis(String fareBasis) {
        this.fareBasis = fareBasis;
    }

    public String getCabinClass() {
        return cabinClass;
    }

    public void setCabinClass(String cabinClass) {
        this.cabinClass = cabinClass;
    }
}
