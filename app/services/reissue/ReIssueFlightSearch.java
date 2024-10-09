package services.reissue;

import com.amadeus.xml.fatcer_13_1_1a.TravelFlightInformationType;
import com.compassites.model.SearchResponse;
import dto.reissue.ReIssueTicketRequest;
import models.AmadeusSessionWrapper;
import models.FlightSearchOffice;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ReIssueFlightSearch {

    SearchResponse reIssueFlightSearch(ReIssueTicketRequest reIssueTicketRequest, TravelFlightInformationType allowedCarriers, AmadeusSessionWrapper amadeusSessionWrapper);
    String provider();
    List<FlightSearchOffice> getOfficeList();

}
