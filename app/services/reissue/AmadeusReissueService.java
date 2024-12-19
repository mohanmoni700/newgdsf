package services.reissue;

import com.compassites.model.PNRResponse;
import com.compassites.model.SearchResponse;
import com.compassites.model.traveller.TravellerMasterInfo;
import dto.reissue.ReIssueConfirmationRequest;
import dto.reissue.ReIssueSearchRequest;
import org.springframework.stereotype.Service;

@Service
public interface AmadeusReissueService {

    SearchResponse reIssueTicket(ReIssueSearchRequest reIssueSearchRequest);

    PNRResponse confirmReIssue(ReIssueConfirmationRequest reIssueConfirmationRequest);

    PNRResponse ticketRebookAndRepricePNR(TravellerMasterInfo travellerMasterInfo, ReIssueSearchRequest reIssueTicketRequest);

}

