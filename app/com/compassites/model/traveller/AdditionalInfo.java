package com.compassites.model.traveller;

/**
 * Created by user on 07-08-2014.
 */
public class AdditionalInfo {

    private String vesselId;

    private String purposeOfTravel;

    private Boolean addBooking;
    private String originalPNR;

    public Boolean getAddBooking() {
        return addBooking;
    }

    public void setAddBooking(Boolean addBooking) {
        this.addBooking = addBooking;
    }

    public String getOriginalPNR() {
        return originalPNR;
    }

    public void setOriginalPNR(String originalPNR) {
        this.originalPNR = originalPNR;
    }

    public String getPurposeOfTravel() {
        return purposeOfTravel;
    }

    public void setPurposeOfTravel(String purposeOfTravel) {
        this.purposeOfTravel = purposeOfTravel;
    }

    public String getVesselId() {
        return vesselId;
    }

    public void setVesselId(String vesselId) {
        this.vesselId = vesselId;
    }
}
