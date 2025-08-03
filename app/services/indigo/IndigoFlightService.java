package services.indigo;

import com.compassites.model.IssuanceRequest;
import com.compassites.model.IssuanceResponse;
import com.compassites.model.PNRResponse;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface IndigoFlightService {
    public PNRResponse checkFareChangeAndAvailability(TravellerMasterInfo travellerMasterInfo);
    public PNRResponse generatePNR(TravellerMasterInfo travellerMasterInfo);
    public IssuanceResponse priceBookedPNR(IssuanceRequest issuanceRequest);
    public IssuanceResponse issueTicket(IssuanceRequest issuanceRequest);
}
