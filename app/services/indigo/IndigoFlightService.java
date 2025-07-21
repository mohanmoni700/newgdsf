package services.indigo;

import com.compassites.model.PNRResponse;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface IndigoFlightService {
    public PNRResponse checkFareChangeAndAvailability(TravellerMasterInfo travellerMasterInfo);
    public PNRResponse generatePNR(TravellerMasterInfo travellerMasterInfo);
}
