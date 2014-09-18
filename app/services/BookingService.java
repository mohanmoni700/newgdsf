package services;

import com.compassites.model.PNRResponse;
import com.compassites.model.traveller.TravellerMasterInfo;

/**
 * Created by user on 07-08-2014.
 */
public interface BookingService {

    public PNRResponse generatePNR(TravellerMasterInfo travellerMasterInfo);

    public PNRResponse priceChangePNR(TravellerMasterInfo travellerMasterInfo);
}
