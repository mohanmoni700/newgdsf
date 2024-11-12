package services.reissue;

import com.compassites.model.SearchResponse;
import dto.reissue.ReIssueSearchRequest;
import org.springframework.stereotype.Service;

@Service
public interface ReIssueService {

    SearchResponse reIssueTicket(ReIssueSearchRequest reIssueSearchRequest);

}
