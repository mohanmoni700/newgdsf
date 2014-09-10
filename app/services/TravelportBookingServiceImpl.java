package services;

import com.compassites.GDSWrapper.travelport.AirRequestClient;
import com.compassites.GDSWrapper.travelport.AirReservationClient;
import com.compassites.GDSWrapper.travelport.Helper;
import com.compassites.GDSWrapper.travelport.LowFareRequestClient;
import com.compassites.model.PNRResponse;
import com.compassites.model.Passenger;
import com.compassites.model.PassengerTypeCode;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.travelport.schema.air_v26_0.*;
import com.travelport.schema.common_v26_0.ProviderReservationInfoRef;
import com.travelport.schema.common_v26_0.TypeCabinClass;
import com.travelport.schema.universal_v26_0.AirCreateReservationRsp;
import com.travelport.schema.universal_v26_0.ProviderReservationInfo;
import com.travelport.service.air_v26_0.AirFaultMessage;
import org.springframework.stereotype.Service;
import utils.StringUtility;

import java.util.List;

/*
*
* Created by user on 02-09-2014.
*/

@Service
public class TravelPortBookingServiceImpl implements BookingService {

    @Override
    public PNRResponse generatePNR(TravellerMasterInfo travellerMasterInfo) {
        LowFareRequestClient lowFareRequestClient = new LowFareRequestClient();
        PNRResponse pnrResponse = null;
        LowFareSearchRsp responseTwo = null;
        try {
            //AirItinerary airItinerary = AirRequestClient.getItinerary(responseTwo, responseTwo.getAirPricingSolution().get(0));
            AirItinerary airItinerary = AirRequestClient.buildAirItinerary(travellerMasterInfo);
            PassengerTypeCode passengerType = null;
            if(travellerMasterInfo.isSeamen()){
                passengerType = PassengerTypeCode.SEA;
            }else {
                passengerType = PassengerTypeCode.ADT;
            }
            TypeCabinClass typeCabinClass = TypeCabinClass.valueOf(travellerMasterInfo.getCabinClass().upperValue());
            List<Passenger> passengerList = AirRequestClient.createPassengers(travellerMasterInfo.getTravellersList(),passengerType);

            AirPriceRsp priceRsp = AirRequestClient.priceItinerary(airItinerary, passengerType.toString(), "INR", typeCabinClass, passengerList);
            boolean fareIsValid =  checkFare(priceRsp,travellerMasterInfo);
            if(fareIsValid){
                AirPricingSolution airPriceSolution = AirReservationClient.stripNonXmitSections(AirRequestClient.getPriceSolution(priceRsp));
                AirCreateReservationRsp reservationRsp = AirReservationClient.reserve(airPriceSolution,travellerMasterInfo);
                pnrResponse = retrievePNR(reservationRsp);
            }

            System.out.println("Results");
         /*catch (DatatypeConfigurationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }*/
        } catch (AirFaultMessage airFaultMessage) {
            airFaultMessage.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }catch (Exception e){
            e.printStackTrace();
        }
        return pnrResponse;
    }


    public boolean checkFare(AirPriceRsp priceRsp,TravellerMasterInfo travellerMasterInfo){
        Long searchPrice = 0L;
        if(travellerMasterInfo.isSeamen()){
            searchPrice = travellerMasterInfo.getItinerary().getSeamanPricingInformation().getTotalPriceValue();
        }else {
            searchPrice = travellerMasterInfo.getItinerary().getPricingInformation().getTotalPriceValue();
        }
        Long totalPrice = new Long(StringUtility.getPriceFromString(priceRsp.getAirPriceResult().get(0).getAirPricingSolution().get(0).getTotalPrice()));
        if(totalPrice.equals(searchPrice)){
            return true;
        }

        return false;
    }

    public PNRResponse retrievePNR(AirCreateReservationRsp reservationRsp){
        PNRResponse pnrResponse =  new PNRResponse();
        Helper.ReservationInfoMap reservationInfoMap = Helper.createReservationInfoMap(reservationRsp.getUniversalRecord().getProviderReservationInfo());
        for(AirReservation airReservation : reservationRsp.getUniversalRecord().getAirReservation()){
            for(ProviderReservationInfoRef reservationInfoRef : airReservation.getProviderReservationInfoRef()){
                ProviderReservationInfo reservationInfo = reservationInfoMap.getByRef(reservationInfoRef);
                pnrResponse.setPnrNumber(reservationInfo.getLocatorCode());
            }
        }
        return pnrResponse;
    }

}
