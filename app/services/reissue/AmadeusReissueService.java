package services.reissue;

import com.compassites.model.SearchResponse;
import dto.reissue.ReIssueTicketRequest;
import org.springframework.stereotype.Service;

@Service
public interface AmadeusReissueService {

    SearchResponse reIssueTicket(ReIssueTicketRequest reIssueTicketRequest);

}

