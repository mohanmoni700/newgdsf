package services;

import com.compassites.model.IssuanceRequest;
import com.compassites.model.PNRResponse;
import com.compassites.model.SplitPNRResponse;
import com.compassites.model.traveller.TravellerMasterInfo;
import dto.AddElementsToPnrDTO;

/**
 * Created by user on 07-08-2014.
 */
public interface BookingService {

    PNRResponse generatePNR(TravellerMasterInfo travellerMasterInfo);

    PNRResponse priceChangePNR(TravellerMasterInfo travellerMasterInfo);

    SplitPNRResponse splitPNR(IssuanceRequest issuanceRequest, String type);

    boolean addJocoPnrToGdsPnr(AddElementsToPnrDTO addElementsToPnrDTO);

}
