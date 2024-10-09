package dto.reissue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReIssueSearchParameters implements Serializable {

    private String origin;
    private String destination;
    private Date changeDate;
    private String requestedCode;
    private Boolean isDateChanged;

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Date getChangeDate() {
        return changeDate;
    }

    public void setChangeDate(Date changeDate) {
        this.changeDate = changeDate;
    }

    public String getRequestedCode() {
        return requestedCode;
    }

    public void setRequestedCode(String requestedCode) {
        this.requestedCode = requestedCode;
    }

    @JsonProperty("isDateChanged")
    public boolean isDateChanged() {
        return isDateChanged;
    }

    public void setDateChanged(Boolean dateChanged) {
        isDateChanged = dateChanged;
    }

}
