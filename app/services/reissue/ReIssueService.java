package services.reissue;

import com.compassites.model.SearchResponse;
import dto.reissue.ReIssueTicketRequest;
import org.springframework.stereotype.Service;

@Service
public interface ReIssueService {

    SearchResponse reIssueTicket(ReIssueTicketRequest reIssueTicketRequest);

}
