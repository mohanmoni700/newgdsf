package services;

import com.compassites.model.PNRResponse;
import com.compassites.model.traveller.TravellerMasterInfo;

import java.util.List;

public interface SplitTicketBookingService {
    public List<PNRResponse> generatePNR(List<TravellerMasterInfo> travellerMasterInfo);
    public List<PNRResponse> priceChangePNR(List<TravellerMasterInfo> travellerMasterInfo);
    public List<PNRResponse> checkFareChangeAndAvailability(List<TravellerMasterInfo> travellerMasterInfos);
    public List<PNRResponse> generateSplitTicketPNR(List<TravellerMasterInfo> travellerMasterInfos);
    public PNRResponse checkFareChangeAndAvailabilityForSplitTicket(List<TravellerMasterInfo> travellerMasterInfos);
    public PNRResponse generateSplitTicketWithSinglePNR(TravellerMasterInfo travellerMasterInfo);
}
