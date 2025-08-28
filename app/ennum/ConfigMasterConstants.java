package ennum;

public enum ConfigMasterConstants {
    SPLIT_TICKET("splitTicket"),
    SPLIT_TICKET_CONNECTION_TIME("split.minConnectionTime"),
    SPLIT_TICKET_AMADEUS_NO_OF_RESULTS("split.amadeus.noOfSearchResults"),
    SPLIT_TICKET_AMADEUS_DESTINATION_DOMESTIC_RESULTS("split.amadeus.destination.domestic"),
    SPLIT_TICKET_AMADEUS_OFFICE_ID_GLOBAL("split.ticket.officeId"),
    SPLIT_TICKET_TRANSIT_ENABLED("split.transitpoint.enabled"),
    SPLIT_TICKET_TRANSIT_CONNECTION_TIME("split.transitpoint.connectionTime");

    private final String key;

    ConfigMasterConstants(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
