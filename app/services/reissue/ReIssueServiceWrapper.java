package services.reissue;

import com.compassites.model.SearchResponse;
import dto.reissue.ReIssueTicketRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReIssueServiceWrapper implements ReIssueService {

    @Autowired
    AmadeusReissueService amadeusReissueService;

    @Override
    public SearchResponse reIssueTicket(ReIssueTicketRequest reIssueTicketRequest){

        SearchResponse reIssueTicketResponse = null;
        if(reIssueTicketRequest.getProvider().equals("Amadeus")){
            reIssueTicketResponse = amadeusReissueService.reIssueTicket(reIssueTicketRequest);
        }

        return  reIssueTicketResponse;
    }


}
