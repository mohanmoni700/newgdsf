package services;

import com.compassites.GDSWrapper.travelomatrix.BookingFlights;
import com.compassites.constants.StaticConstatnts;
import com.compassites.constants.TraveloMatrixConstants;
import com.compassites.model.*;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.compassites.model.travelomatrix.ResponseModels.CommitBookingReply.CommitBookingReply;
import com.compassites.model.travelomatrix.ResponseModels.CommitBookingReply.Detail;
import com.compassites.model.travelomatrix.ResponseModels.CommitBookingReply.PassengerDetail;
import com.compassites.model.travelomatrix.ResponseModels.HoldTicket.HoldTicketResponse;
import com.compassites.model.travelomatrix.ResponseModels.IssueTicket.IssueTicketResponse;
import com.compassites.model.travelomatrix.ResponseModels.UpdateFareQuotes.Price;
import com.compassites.model.travelomatrix.ResponseModels.UpdateFareQuotes.UpdateFareQuotesReply;

import com.compassites.GDSWrapper.travelomatrix.HoldTicketTMX;
import com.compassites.GDSWrapper.travelomatrix.IssueHoldTicketTMX;
import com.compassites.model.travelomatrix.ResponseModels.UpdatePNR.UpdatePNRResponse;
import com.compassites.model.travelomatrix.UpdatePNRRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


@Service
public class TraveloMatrixBookingServiceImpl implements BookingService  {

    static org.slf4j.Logger travelomatrixLogger = LoggerFactory.getLogger("travelomatrix");

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

    public BookingFlights bookingFlights = new BookingFlights();

    public HoldTicketTMX holdTicketTMX = new HoldTicketTMX();

    public IssueHoldTicketTMX issueHoldTicketTMX = new IssueHoldTicketTMX();

    @Override
    public PNRResponse priceChangePNR(TravellerMasterInfo travellerMasterInfo) {
        return generatePNR(travellerMasterInfo);
    }

    @Override
    public SplitPNRResponse splitPNR(IssuanceRequest issuanceRequest) {
        return null;
    }

    @Override
    public PNRResponse generatePNR(TravellerMasterInfo travellerMasterInfo) {
        PNRResponse pnrResponse = null;
        travelomatrixLogger.debug("generatePNR called...........");

        if(travellerMasterInfo.isBookAndHold()) {
            //Book and Hold
            JsonNode jsonResponse = holdTicketTMX.HoldBooking(travellerMasterInfo);
            try {
                travelomatrixLogger.debug("Response for generatePNR: " + jsonResponse);
                HoldTicketResponse response = new ObjectMapper().treeToValue(jsonResponse, HoldTicketResponse.class);
                if (response.getStatus().equalsIgnoreCase("0")) {
                    travelomatrixLogger.debug("Response is not valid for HoldBooking: " + response.getMessage());
                } else {
                    pnrResponse = getPNRResponseFromHoldTicket(travellerMasterInfo, response);
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }else{
            pnrResponse = getBookingPNRResponse(travellerMasterInfo);
        }

        return pnrResponse;
    }


    public IssuanceResponse commitBooking(IssuanceRequest issuanceRequest)  {
        IssuanceResponse issuanceResponse = new IssuanceResponse();
        JsonNode jsonResponse = bookingFlights.commitBooking(issuanceRequest);
        travelomatrixLogger.debug("Response for generatePNR: " + jsonResponse);
        CommitBookingReply commitBookingReply = null;
        try {
            commitBookingReply = new ObjectMapper().treeToValue(jsonResponse, CommitBookingReply.class);
            if (commitBookingReply.getStatus().equalsIgnoreCase("0")) {
                travelomatrixLogger.debug("No Response receieved for CommitBooking from Travelomatrix : " + commitBookingReply);
            } else {
             issuanceResponse = getCommitBookingResponse(commitBookingReply);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return issuanceResponse;
    }

    public IssuanceResponse issueTicket(IssuanceRequest issuanceRequest) {
        IssuanceResponse issuanceResponse = new IssuanceResponse();
        IssueTicketResponse issueTicketResponse = null;
        travelomatrixLogger.debug("IssueTicket called...........");
        JsonNode jsonResponse = issueHoldTicketTMX.issueHoldTicket(issuanceRequest);
        try{
            travelomatrixLogger.debug("Response for IssuanceTicket:"+ jsonResponse);
             issueTicketResponse = new ObjectMapper().treeToValue(jsonResponse, IssueTicketResponse.class);
            if(issueTicketResponse.getStatus() == 0){
                travelomatrixLogger.debug("Issuance is failed");
                issuanceResponse.setSuccess(false);
            }else{
                issuanceResponse = getIssuanceResponse(issuanceRequest,issueTicketResponse);
            }
        }catch(JsonProcessingException e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }

      return issuanceResponse;
    }
    public PNRResponse checkFareChangeAndAvailability(
            TravellerMasterInfo travellerMasterInfo) {
        PNRResponse pnrResponse = null;
        travelomatrixLogger.debug("checkFareChangeAndAvailability called...........");
        String resultToken = travellerMasterInfo.getItinerary().getResultToken();
        JsonNode jsonResponse = bookingFlights.getUpdatedFares(resultToken);
        try {
            travelomatrixLogger.debug("Response for checkFareChangeAndAvailability: ResultToken:"+ resultToken +" ----  Response: \n"+ jsonResponse);
            UpdateFareQuotesReply response = new ObjectMapper().treeToValue(jsonResponse, UpdateFareQuotesReply.class);
            if(response.getStatus() == 0){
                travelomatrixLogger.debug("FareRule Respose is not Reeceived for ResultToken :" + resultToken);
            }else{
                pnrResponse = getPNRResponse(travellerMasterInfo,response);
            }

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return pnrResponse;
    }

    public PNRResponse getPNRResponse(TravellerMasterInfo travellerMasterInfo,UpdateFareQuotesReply updateFareQuotesReply){
        PNRResponse pnrResponse = new PNRResponse();
        Boolean availbleFlights = false;
       // pnrResponse.setCreationOfficeId(TraveloMatrixConstants.tmofficeId.toString());
        List<AirSegmentInformation> journeyList= travellerMasterInfo.getItinerary().getNonSeamenJourneyList().get(0).getAirSegmentList();
        for(AirSegmentInformation airSegmentInformation : journeyList){
            if(airSegmentInformation.getAvailbleSeats() != null && airSegmentInformation.getAvailbleSeats() > 0){
                availbleFlights = true;
            }else {
                availbleFlights = false;
            }
        }

       String currencyFromReply =  updateFareQuotesReply.getUpdateFareQuote().getFareQuoteDetails().getJourneyList().getPrice().getCurrency();
       Double totalFareFromReply =  updateFareQuotesReply.getUpdateFareQuote().getFareQuoteDetails().getJourneyList().getPrice().getTotalDisplayFare();
       String currency = travellerMasterInfo.getItinerary().getPricingInformation().getCurrency();
       BigDecimal totalPrice = travellerMasterInfo.getItinerary().getPricingInformation().getTotalPrice();
       if(currency.equalsIgnoreCase(currencyFromReply) && totalFareFromReply == totalPrice.doubleValue()){
           pnrResponse.setPriceChanged(false);
       }else{
        pnrResponse.setPriceChanged(true);
        pnrResponse.setChangedBasePrice(new BigDecimal(updateFareQuotesReply.getUpdateFareQuote().getFareQuoteDetails().getJourneyList().getPrice().getPriceBreakup().getBasicFare()));
        pnrResponse.setChangedPrice(new BigDecimal(updateFareQuotesReply.getUpdateFareQuote().getFareQuoteDetails().getJourneyList().getPrice().getTotalDisplayFare()));
       }
        pnrResponse.setFlightAvailable(availbleFlights);
        pnrResponse.setResultToken(updateFareQuotesReply.getUpdateFareQuote().getFareQuoteDetails().getJourneyList().getResultToken());
        Price price = updateFareQuotesReply.getUpdateFareQuote().getFareQuoteDetails().getJourneyList().getPrice();
        PricingInformation pricingInformation = new PricingInformation();
        pricingInformation.setBasePrice(new BigDecimal(price.getPriceBreakup().getBasicFare()));
        pricingInformation.setTotalBasePrice(new BigDecimal(price.getTotalDisplayFare()));
        pricingInformation.setAdtBasePrice(new BigDecimal(price.getPassengerBreakup().getADT().getBasePrice()));
        pricingInformation.setAdtTotalPrice(new BigDecimal(price.getPassengerBreakup().getADT().getTotalPrice()));
        pricingInformation.setGdsCurrency(price.getCurrency());
        pricingInformation.setTax(new BigDecimal(price.getPriceBreakup().getTax()));
        if(price.getPassengerBreakup().getCHD() != null)
            pricingInformation.setChdBasePrice(new BigDecimal(price.getPassengerBreakup().getCHD().getBasePrice()));
        else
            pricingInformation.setChdBasePrice(new BigDecimal(0));
        if(price.getPassengerBreakup().getINF() != null)
            pricingInformation.setInfBasePrice(new BigDecimal(price.getPassengerBreakup().getINF().getBasePrice()));
        else
         pricingInformation.setInfBasePrice(new BigDecimal(0));
        pricingInformation.setCurrency(price.getCurrency());
        pricingInformation.setTotalPrice(new BigDecimal(price.getTotalDisplayFare()));
        pricingInformation.setTotalPriceValue(new BigDecimal(price.getTotalDisplayFare()));
       List<PassengerTax> passengerTaxList = new LinkedList<>();
        PassengerTax adtPassengerTax = new PassengerTax();
        adtPassengerTax.setPassengerType("ADT");
        adtPassengerTax.setPassengerCount(Integer.parseInt(price.getPassengerBreakup().getADT().getPassengerCount()));
        adtPassengerTax.setTotalTax(new BigDecimal(price.getPassengerBreakup().getADT().getTax()));
        passengerTaxList.add(adtPassengerTax);
        if(price.getPassengerBreakup().getCHD() != null){
            PassengerTax chdPassengerTax = new PassengerTax();
            chdPassengerTax.setPassengerType("CHD");
            chdPassengerTax.setPassengerCount(Integer.parseInt(price.getPassengerBreakup().getCHD().getPassengerCount()));
            chdPassengerTax.setTotalTax(new BigDecimal(price.getPassengerBreakup().getCHD().getTax()));
            passengerTaxList.add(chdPassengerTax);
        }
        if(price.getPassengerBreakup().getINF() != null){
            PassengerTax infPassengerTax = new PassengerTax();
            infPassengerTax.setPassengerType("INF");
            infPassengerTax.setPassengerCount(Integer.parseInt(price.getPassengerBreakup().getINF().getPassengerCount()));
            infPassengerTax.setTotalTax(new BigDecimal(price.getPassengerBreakup().getINF().getTax()));
            passengerTaxList.add(infPassengerTax);
        }
        pricingInformation.setPassengerTaxes(passengerTaxList);
        pricingInformation.setLCC(updateFareQuotesReply.getUpdateFareQuote().getFareQuoteDetails().getJourneyList().getAttr().getIsLCC());
        pricingInformation.setProvider(TraveloMatrixConstants.provider);
        pnrResponse.setPricingInfo(pricingInformation);
        pnrResponse.setCappingLimitReached(false);
        pnrResponse.setHoldTime(updateFareQuotesReply.getUpdateFareQuote().getFareQuoteDetails().getJourneyList().getHoldTicket());
        return pnrResponse;
    }

    public IssuanceResponse getCommitBookingResponse(CommitBookingReply commitBookingReply){
        IssuanceResponse issuanceResponse = new IssuanceResponse();
        String airlinePNR=commitBookingReply.getCommitBooking().getBookingDetails().getJourneyList().getFlightDetails().getDetails().get(0).get(0).getAirlinePNR();
        issuanceResponse.setAirlinePnr(airlinePNR);
        issuanceResponse.setSuccess(true);
        List<Detail> details = commitBookingReply.getCommitBooking().getBookingDetails().getJourneyList().getFlightDetails().getDetails().get(0);
        int segmentnumber = 1;
        Map<String,String> airlinePNRMap = new HashMap<>();
        for(Detail detail:details) {
            String segments = detail.getOrigin().getAirportCode().toLowerCase() + detail.getDestination().getAirportCode().toLowerCase() + String.valueOf(segmentnumber);
            String airlinePnr = detail.getAirlinePNR();
            airlinePNRMap.put(segments,airlinePnr);
            segmentnumber++;
        }
        issuanceResponse.setAirlinePNRMap(airlinePNRMap);
        issuanceResponse.setIssued(true);
        Map<String,String> tickenetNumberMap = new HashMap<>();
        List<PassengerDetail> passengerDetailList =  commitBookingReply.getCommitBooking().getBookingDetails().getPassengerDetails();
        for(PassengerDetail passengerDetail:passengerDetailList){
            String passengerType = passengerDetail.getPassengerType();
            String ticketNumber  = passengerDetail.getTicketNumber();
            tickenetNumberMap.put(passengerType,ticketNumber);
        }
        issuanceResponse.setTicketNumberMap(tickenetNumberMap);
        issuanceResponse.setBookingId(commitBookingReply.getCommitBooking().getBookingDetails().getBookingId());
        //issuanceResponse.setBaggage(commitBookingReply.getCommitBooking().getBookingDetails().getAttr().getBaggage().toString());
        return issuanceResponse;
    }

    public PNRResponse getBookingPNRResponse(TravellerMasterInfo travellerMasterInfo){
        PNRResponse pnrResponse = new PNRResponse();
       // String airlinePNR=commitBookingReply.getCommitBooking().getBookingDetails().getJourneyList().getFlightDetails().getDetails().get(0).get(0).getAirlinePNR();
      // pnrResponse.setAirlinePNR("NA");
       // pnrResponse.setPnrNumber("NA");
        pnrResponse.setFlightAvailable(true);
        pnrResponse.setCreationOfficeId(TraveloMatrixConstants.tmofficeId);
        pnrResponse.setAirlinePNRError(false);
        String resultToken = travellerMasterInfo.getItinerary().getResultToken();
        pnrResponse.setResultToken(resultToken);
        pnrResponse.setBookingId("NA");
        String appidReference = travellerMasterInfo.getAppReference();
        pnrResponse.setAppReference(appidReference);
        List<AirSegmentInformation> details = travellerMasterInfo.getItinerary().getJourneyList().get(0).getAirSegmentList();
        int segmentnumber = 1;
        Map<String,String> airlinePNRMap = new HashMap<>();
        for(AirSegmentInformation detail:details) {
            String segments = detail.getFromLocation().toLowerCase() + detail.getToLocation().toLowerCase() + String.valueOf(segmentnumber);
            String airlinePnr = "NA";
            airlinePNRMap.put(segments,airlinePnr);
            segmentnumber++;
        }
        pnrResponse.setAirlinePNRMap(airlinePNRMap);
        Map<String,String> tickenetNumberMap = new HashMap<>();
        List<Traveller> passengerDetailList =  travellerMasterInfo.getTravellersList();
            for(Traveller passengerDetail:passengerDetailList){
                String passengerType = "ADT";
                String ticketNumber  = "NA";
                tickenetNumberMap.put(passengerType,ticketNumber);
            }
        pnrResponse.setTicketNumberMap(tickenetNumberMap);
        return pnrResponse;
    }

    public PNRResponse getPNRResponseFromHoldTicket(TravellerMasterInfo travellerMasterInfo,HoldTicketResponse holdTicketResponse){
        PNRResponse pnrResponse = new PNRResponse();
        String airlinePNR=holdTicketResponse.getHoldTicket().getBookingDetails().getPNR();
        pnrResponse.setAirlinePNR(airlinePNR);
        pnrResponse.setPnrNumber(holdTicketResponse.getHoldTicket().getBookingDetails().getPNR());
        pnrResponse.setFlightAvailable(true);
        pnrResponse.setCreationOfficeId(TraveloMatrixConstants.tmofficeId);
        pnrResponse.setAirlinePNRError(false);
        String resultToken = travellerMasterInfo.getItinerary().getResultToken();
        pnrResponse.setResultToken(resultToken);
        pnrResponse.setBookingId(holdTicketResponse.getHoldTicket().getBookingDetails().getBookingId());
        String appidReference = travellerMasterInfo.getAppReference();
        pnrResponse.setAppReference(appidReference);
        List<com.compassites.model.travelomatrix.ResponseModels.HoldTicket.Detail> details = holdTicketResponse.getHoldTicket().getBookingDetails().getJourneyList().getFlightDetails().getDetails().get(0);
        int segmentnumber = 1;
        Map<String,String> airlinePNRMap = new HashMap<>();
        for(com.compassites.model.travelomatrix.ResponseModels.HoldTicket.Detail detail:details) {
            String segments = detail.getOrigin().getAirportCode().toLowerCase() + detail.getDestination().getAirportCode().toLowerCase() + String.valueOf(segmentnumber);
            String airlinePnr = detail.getAirlinePNR();
            airlinePNRMap.put(segments,airlinePnr);
            segmentnumber++;
        }
        pnrResponse.setAirlinePNRMap(airlinePNRMap);
       // Map<String,String> tickenetNumberMap = new HashMap<>();
        List<com.compassites.model.travelomatrix.ResponseModels.HoldTicket.PassengerDetail> passengerDetailList =  holdTicketResponse.getHoldTicket().getBookingDetails().getPassengerDetails();
        for(com.compassites.model.travelomatrix.ResponseModels.HoldTicket.PassengerDetail passengerDetail:passengerDetailList){
            String passengerType = passengerDetail.getPassengerType();
           // String ticketNumber  = passengerDetail.getTicketNumber();
           // tickenetNumberMap.put(passengerType,ticketNumber);
        }
       // pnrResponse.setTicketNumberMap(tickenetNumberMap);
        return pnrResponse;
    }

    public IssuanceResponse getIssuanceResponse(IssuanceRequest issuanceRequest,IssueTicketResponse issueTicketResponse){
       IssuanceResponse issuanceResponse = new IssuanceResponse();
       issuanceResponse.setSuccess(true);
       issuanceResponse.setAirlinePnr(issuanceRequest.getGdsPNR());
       return issuanceResponse;
    }

    public  UpdatePNRResponse getUpdatePnr(String appRef){
      UpdatePNRResponse updatePNRResponse = null;
        travelomatrixLogger.debug("UpdatePNR called...........");
       JsonNode jsonResponse  =   issueHoldTicketTMX.getUpdatePNRResponse(appRef);
       try{
            travelomatrixLogger.debug("Response for IssuanceTicket:"+ jsonResponse);
            updatePNRResponse = new ObjectMapper().treeToValue(jsonResponse, UpdatePNRResponse.class);
            if(updatePNRResponse.getStatus() == 0){
                travelomatrixLogger.debug("updatePNRResponse is failed");
            }
        }catch(JsonProcessingException e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return updatePNRResponse;
    }
}
