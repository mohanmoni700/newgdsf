package models;

public class FlightSearchOffice {
    String getOfficeId;
    boolean isPartner = false;

    public FlightSearchOffice(String getOfficeId, boolean isPartner) {
        this.getOfficeId = getOfficeId;
        this.isPartner = isPartner;
    }

    public FlightSearchOffice(String getOfficeId) {
        this.getOfficeId = getOfficeId;
    }

    public String getGetOfficeId() {
        return getOfficeId;
    }

    public boolean isPartner() {
        return isPartner;
    }

}
