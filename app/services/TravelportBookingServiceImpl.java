package services;

import com.compassites.GDSWrapper.travelport.AirRequestClient;
import com.compassites.GDSWrapper.travelport.AirReservationClient;
import com.compassites.GDSWrapper.travelport.Helper;
import com.compassites.GDSWrapper.travelport.UniversalRecordClient;
import com.compassites.model.ErrorMessage;
import com.compassites.model.PNRResponse;
import com.compassites.model.Passenger;
import com.compassites.model.PassengerTypeCode;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.travelport.schema.air_v26_0.AirItinerary;
import com.travelport.schema.air_v26_0.AirPriceRsp;
import com.travelport.schema.air_v26_0.AirPricingSolution;
import com.travelport.schema.air_v26_0.AirReservation;
import com.travelport.schema.common_v26_0.ProviderReservationInfoRef;
import com.travelport.schema.common_v26_0.TypeCabinClass;
import com.travelport.schema.universal_v26_0.AirCreateReservationRsp;
import com.travelport.schema.universal_v26_0.ProviderReservationInfo;
import com.travelport.schema.universal_v26_0.UniversalRecordRetrieveRsp;
import com.travelport.service.air_v26_0.AirFaultMessage;
import org.springframework.stereotype.Service;
import utils.ErrorMessageHelper;
import utils.StringUtility;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/*
*
* Created by user on 02-09-2014.
*/

@Service
public class TravelportBookingServiceImpl implements BookingService {

    @Override
    public PNRResponse generatePNR(TravellerMasterInfo travellerMasterInfo) {
        PNRResponse pnrResponse = new PNRResponse();
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
            pnrResponse =  checkFare(priceRsp,travellerMasterInfo);
            if(!pnrResponse.isPriceChanged()){
                AirPricingSolution airPriceSolution = AirReservationClient.stripNonXmitSections(AirRequestClient.getPriceSolution(priceRsp));
                AirCreateReservationRsp reservationRsp = AirReservationClient.reserve(AirRequestClient.getPriceSolution(priceRsp), travellerMasterInfo);
                UniversalRecordRetrieveRsp universalRecordRetrieveRsp = UniversalRecordClient.retrievePNR(reservationRsp);
                pnrResponse = retrievePNR(universalRecordRetrieveRsp,pnrResponse);
            }


        } catch (AirFaultMessage airFaultMessage) {
            airFaultMessage.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("error", ErrorMessage.ErrorType.ERROR,"TravelPort");
            pnrResponse.setErrorMessage(errorMessage);
        }catch (Exception e){
            e.printStackTrace();
            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("error", ErrorMessage.ErrorType.ERROR,"TravelPort");
            pnrResponse.setErrorMessage(errorMessage);
        }

        return pnrResponse;
    }

    @Override
    public PNRResponse priceChangePNR(TravellerMasterInfo travellerMasterInfo) {

        return generatePNR(travellerMasterInfo);
    }


    public PNRResponse checkFare(AirPriceRsp priceRsp,TravellerMasterInfo travellerMasterInfo){
        PNRResponse pnrResponse = new PNRResponse();
        BigDecimal searchPrice = new BigDecimal(0);
        if(travellerMasterInfo.isSeamen()){
            searchPrice = travellerMasterInfo.getItinerary().getSeamanPricingInformation().getTotalPriceValue();
        }else {
            searchPrice = travellerMasterInfo.getItinerary().getPricingInformation().getTotalPriceValue();
        }
        BigDecimal totalPrice = new BigDecimal(StringUtility.getPriceFromString(priceRsp.getAirPriceResult().get(0).getAirPricingSolution().get(0).getTotalPrice()));
        BigDecimal changedBasePrice =   new BigDecimal(StringUtility.getPriceFromString(priceRsp.getAirPriceResult().get(0).getAirPricingSolution().get(0).getBasePrice()));
        if(totalPrice.equals(searchPrice)){

            pnrResponse.setPriceChanged(false);
            pnrResponse.setFlightAvailable(true);
            return pnrResponse;
        }
        pnrResponse.setChangedPrice(totalPrice);
        pnrResponse.setChangedBasePrice(changedBasePrice);
        pnrResponse.setOriginalPrice(searchPrice);
        pnrResponse.setFlightAvailable(true);
        pnrResponse.setPriceChanged(true);
        return pnrResponse;
    }

    public PNRResponse retrievePNR(UniversalRecordRetrieveRsp universalRecordRetrieveRsp, PNRResponse pnrResponse){
//        PNRResponse pnrResponse =  new PNRResponse();
        Helper.ReservationInfoMap reservationInfoMap = Helper.createReservationInfoMap(universalRecordRetrieveRsp.getUniversalRecord().getProviderReservationInfo());
        Date lastDate = null;
        for(AirReservation airReservation : universalRecordRetrieveRsp.getUniversalRecord().getAirReservation()){
            for(ProviderReservationInfoRef reservationInfoRef : airReservation.getProviderReservationInfoRef()){
                ProviderReservationInfo reservationInfo = reservationInfoMap.getByRef(reservationInfoRef);
                pnrResponse.setPnrNumber(reservationInfo.getLocatorCode());
            }
        }
        try {
            String remarkData = universalRecordRetrieveRsp.getUniversalRecord().getGeneralRemark().get(0).getRemarkData();
            int i = remarkData.lastIndexOf("BY");
            String subString = remarkData.substring(i+2);

            subString = subString.trim();
            String[] args1 = subString.split("/");
            String dateString = args1[0]+"/"+args1[1];
            dateString = dateString+ Calendar.getInstance().get(Calendar.YEAR);
            SimpleDateFormat sdf = new SimpleDateFormat("HHmm/ddMMMyyyy");

            lastDate = sdf.parse(dateString);
        } catch (NullPointerException e){
            e.printStackTrace();
            pnrResponse.setFlightAvailable(false);

        }catch (ParseException e) {
            e.printStackTrace();
        }

        pnrResponse.setValidTillDate(lastDate);
        return pnrResponse;
    }

}
