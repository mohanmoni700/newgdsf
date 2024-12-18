package dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenTicketDTO implements Serializable {
    private String ticketNo;

    private String originalTicketNo;

    public String getOriginalTicketNo() {
        return originalTicketNo;
    }

    public void setOriginalTicketNo(String originalTicketNo) {
        this.originalTicketNo = originalTicketNo;
    }
    public String getTicketNo() {
        return ticketNo;
    }

    public void setTicketNo(String ticketNo) {
        this.ticketNo = ticketNo;
    }
}
