package services.reissue;

import com.compassites.model.FlightItinerary;
import com.compassites.model.PNRResponse;
import com.compassites.model.SearchResponse;
import com.compassites.model.traveller.TravellerMasterInfo;
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

    @Override
    public PNRResponse ticketRebookAndRepricePNR(TravellerMasterInfo travellerMasterInfo, ReIssueSearchRequest reIssueTicketRequest) {
        String provider = getProvider(travellerMasterInfo);
        PNRResponse pnrResponse = null;
        if ("Amadeus".equalsIgnoreCase(provider)) {
            pnrResponse = amadeusReissueService
                    .ticketRebookAndRepricePNR(travellerMasterInfo, reIssueTicketRequest);
        } else {
            throw new RuntimeException("Invalid Provider");
        }
        return pnrResponse;
    }

    private String getProvider(TravellerMasterInfo travellerMasterInfo) {
        FlightItinerary itinerary = travellerMasterInfo.getItinerary();
        return travellerMasterInfo.isSeamen() ? itinerary
                .getSeamanPricingInformation().getProvider() : itinerary
                .getPricingInformation().getProvider();
    }


}
