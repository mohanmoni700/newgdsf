package services;

import com.compassites.GDSWrapper.travelport.AirRequestClient;
import com.compassites.GDSWrapper.travelport.AirReservationClient;
import com.compassites.GDSWrapper.travelport.Helper;
import com.compassites.GDSWrapper.travelport.LowFareRequestClient;
import com.compassites.model.PNRResponse;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.travelport.schema.air_v26_0.AirItinerary;
import com.travelport.schema.air_v26_0.AirPriceRsp;
import com.travelport.schema.air_v26_0.AirPricingSolution;
import com.travelport.schema.air_v26_0.LowFareSearchRsp;
import com.travelport.schema.common_v26_0.TypeCabinClass;
import com.travelport.schema.universal_v26_0.AirCreateReservationRsp;
import com.travelport.service.air_v26_0.AirFaultMessage;

import javax.xml.datatype.DatatypeConfigurationException;

/*
*
* Created by user on 02-09-2014.
*/


public class TravelportBookingServiceImpl implements BookingService {

    @Override
    public PNRResponse generatePNR(TravellerMasterInfo travellerMasterInfo) {
       LowFareRequestClient lowFareRequestClient = new LowFareRequestClient();
        LowFareSearchRsp responseTwo = null;
        try {
            AirItinerary airItinerary = AirRequestClient.getItinerary(responseTwo, responseTwo.getAirPricingSolution().get(0));
            try {
                AirPriceRsp priceRsp = AirRequestClient.priceItinerary(airItinerary, "SEA", "INR", TypeCabinClass.ECONOMY, null);
                AirPricingSolution airPriceSolution = AirReservationClient.stripNonXmitSections(AirRequestClient.getPriceSolution(priceRsp));
                AirCreateReservationRsp response = AirReservationClient.reserve(airPriceSolution);
                System.out.println("Results");
            } catch (DatatypeConfigurationException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        } catch (AirFaultMessage airFaultMessage) {
            airFaultMessage.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }

}
