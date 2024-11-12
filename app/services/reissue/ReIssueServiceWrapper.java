package services.reissue;

import com.compassites.model.SearchResponse;
import dto.reissue.ReIssueSearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReIssueServiceWrapper implements ReIssueService {

    @Autowired
    AmadeusReissueService amadeusReissueService;

    @Override
    public SearchResponse reIssueTicket(ReIssueSearchRequest reIssueSearchRequest){

        SearchResponse reIssueTicketResponse = null;
        if(reIssueSearchRequest.getProvider().equals("Amadeus")){
            reIssueTicketResponse = amadeusReissueService.reIssueTicket(reIssueSearchRequest);
        }

        return  reIssueTicketResponse;
    }


}
