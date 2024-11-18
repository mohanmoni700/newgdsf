package services.reissue;

import com.compassites.model.PNRResponse;
import com.compassites.model.SearchResponse;
import com.compassites.model.traveller.TravellerMasterInfo;
import dto.reissue.ReIssueSearchRequest;
import org.springframework.stereotype.Service;

@Service
public interface ReIssueService {

    SearchResponse reIssueTicket(ReIssueSearchRequest reIssueSearchRequest);
    PNRResponse ticketRebookAndRepricePNR(TravellerMasterInfo travellerMasterInfo, ReIssueSearchRequest reIssueTicketRequest);
}
