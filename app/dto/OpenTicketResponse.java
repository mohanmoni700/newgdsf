package dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenTicketResponse implements Serializable {

    private String ticketNumber;
    private String status;

    private String type;

    private String cpnNumber;

    private String cpnStatus;

    public String getCpnStatus() {
        return cpnStatus;
    }

    public void setCpnStatus(String cpnStatus) {
        this.cpnStatus = cpnStatus;
    }

    public String getCpnNumber() {
        return cpnNumber;
    }

    public void setCpnNumber(String cpnNumber) {
        this.cpnNumber = cpnNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
