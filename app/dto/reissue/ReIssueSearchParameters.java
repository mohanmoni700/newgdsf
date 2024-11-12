package dto.reissue;

import com.compassites.model.DateType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReIssueSearchParameters implements Serializable {

    private String actionRequestedCode;
    private String origin;
    private String destination;
    private Date travelDate;
    private DateType dateType;
    private List<String> transitPointList;
    private boolean isOriginChanged;
    private boolean isDestinationChanged;
    private boolean isDateChanged;
    private boolean isTransitPointAdded;
    private boolean isNonStop;

    public String getActionRequestedCode() {
        return actionRequestedCode;
    }

    public void setActionRequestedCode(String actionRequestedCode) {
        this.actionRequestedCode = actionRequestedCode;
    }

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

    public DateType getDateType() {
        return dateType;
    }

    public void setDateType(DateType dateType) {
        this.dateType = dateType;
    }

    public Date getTravelDate() {
        return travelDate;
    }

    public void setTravelDate(Date travelDate) {
        this.travelDate = travelDate;
    }

    @JsonProperty("isOriginChanged")
    public boolean isOriginChanged() {
        return isOriginChanged;
    }

    public void setOriginChanged(boolean originChanged) {
        isOriginChanged = originChanged;
    }

    @JsonProperty("isDestinationChanged")
    public boolean isDestinationChanged() {
        return isDestinationChanged;
    }

    public void setDestinationChanged(boolean destinationChanged) {
        isDestinationChanged = destinationChanged;
    }

    @JsonProperty("isDateChanged")
    public boolean isDateChanged() {
        return isDateChanged;
    }

    public void setDateChanged(boolean dateChanged) {
        isDateChanged = dateChanged;
    }

    public List<String> getTransitPointList() {
        return transitPointList;
    }

    public void setTransitPointList(List<String> transitPointList) {
        this.transitPointList = transitPointList;
    }

    @JsonProperty("isTransitPointAdded")
    public boolean isTransitPointAdded() {
        return isTransitPointAdded;
    }

    public void setTransitPointAdded(boolean transitPointAdded) {
        isTransitPointAdded = transitPointAdded;
    }

    @JsonProperty("isNonStop")
    public boolean isNonStop() {
        return isNonStop;
    }

    public void setNonStop(boolean nonStop) {
        isNonStop = nonStop;
    }

}
