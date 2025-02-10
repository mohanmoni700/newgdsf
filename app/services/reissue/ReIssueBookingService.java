package services.reissue;

import com.compassites.model.PNRResponse;
import dto.reissue.ReIssueConfirmationRequest;
import models.AmadeusSessionWrapper;
import models.FlightSearchOffice;
import org.springframework.stereotype.Service;

@Service
public interface ReIssueBookingService {

    PNRResponse confirmReissue(String newPnrToBeReIssued, ReIssueConfirmationRequest reIssueConfirmationRequest, FlightSearchOffice officeId);

}
