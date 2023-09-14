package models;

import play.Play;

import java.util.List;

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

    //todo
    public FlightSearchOffice() {
        List<String> officeIdList = Play.application().configuration().getStringList("amadeus.SOURCE_OFFICE");
        this.getOfficeId = officeIdList.get(0);
    }

    public String getGetOfficeId() {
        return getOfficeId;
    }

    public boolean isPartner() {
        return isPartner;
    }

}
