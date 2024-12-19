package dto.reissue;

import com.compassites.model.BookingType;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReIssueConfirmationRequest {

    private Long bookingId;

    private boolean isSeaman;

    private String officeId;

    private String originalGdsPnr;

    private List<AmadeusPaxRefAndTicket> paxAndTicketList;

    private List<Integer> selectedSegmentList;

    private TravellerMasterInfo newTravellerMasterInfo;

    private TravellerMasterInfo originalTravellerMasterInfo;

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public boolean isSeaman() {
        return isSeaman;
    }

    public void setSeaman(boolean seaman) {
        isSeaman = seaman;
    }

    public String getOfficeId() {
        return officeId;
    }

    public void setOfficeId(String officeId) {
        this.officeId = officeId;
    }

    public String getOriginalGdsPnr() {
        return originalGdsPnr;
    }

    public void setOriginalGdsPnr(String originalGdsPnr) {
        this.originalGdsPnr = originalGdsPnr;
    }

    public List<AmadeusPaxRefAndTicket> getPaxAndTicketList() {
        return paxAndTicketList;
    }

    public void setPaxAndTicketList(List<AmadeusPaxRefAndTicket> paxAndTicketList) {
        this.paxAndTicketList = paxAndTicketList;
    }

    public List<Integer> getSelectedSegmentList() {
        return selectedSegmentList;
    }

    public void setSelectedSegmentList(List<Integer> selectedSegmentList) {
        this.selectedSegmentList = selectedSegmentList;
    }

    public TravellerMasterInfo getNewTravellerMasterInfo() {
        return newTravellerMasterInfo;
    }

    public void setNewTravellerMasterInfo(TravellerMasterInfo newTravellerMasterInfo) {
        this.newTravellerMasterInfo = newTravellerMasterInfo;
    }

    public TravellerMasterInfo getOriginalTravellerMasterInfo() {
        return originalTravellerMasterInfo;
    }

    public void setOriginalTravellerMasterInfo(TravellerMasterInfo originalTravellerMasterInfo) {
        this.originalTravellerMasterInfo = originalTravellerMasterInfo;
    }

    public BookingType getBookingType() {
        if (this.isSeaman()) {
            return BookingType.SEAMEN;
        } else {
            return BookingType.NON_MARINE;
        }
    }

}
