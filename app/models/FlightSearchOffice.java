package models;

public class FlightSearchOffice {

    String officeId;
    String name = "";
    boolean isPartner = false;


    public FlightSearchOffice(String officeId, String name, boolean isPartner) {
        this.officeId = officeId;
        this.name = name;
        this.isPartner = isPartner;
    }

    public FlightSearchOffice(String officeId) {
        this.officeId = officeId;
    }

    //todo
    public FlightSearchOffice() {
        this.officeId = "BOMVS34C3";
    }

    public String getGetOfficeId() {
        return officeId;
    }

    public void setGetOfficeId(String officeId) {
        this.officeId = officeId;
    }

    public boolean isPartner() {
        return isPartner;
    }

    public void setPartner(boolean partner) {
        isPartner = partner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}
