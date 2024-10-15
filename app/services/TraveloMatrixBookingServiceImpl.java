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
import java.util.*;


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
            JsonNode jsonResponse = holdTicketTMX.HoldBooking(travellerMasterInfo,0L);
            JsonNode returnjsonResponse = null;
            if(travellerMasterInfo.getItinerary().getReturnResultToken() != null && travellerMasterInfo.getJourneyType().equalsIgnoreCase("ROUND_TRIP"))
            returnjsonResponse = holdTicketTMX.HoldBooking(travellerMasterInfo,1L);
            try {
                travelomatrixLogger.debug("Response for generatePNR: " + jsonResponse);
                HoldTicketResponse response = new ObjectMapper().treeToValue(jsonResponse, HoldTicketResponse.class);
                HoldTicketResponse returnresponse = null;
                if(travellerMasterInfo.getItinerary().getReturnResultToken() != null && travellerMasterInfo.getJourneyType().equalsIgnoreCase("ROUND_TRIP"))
                    returnresponse = new ObjectMapper().treeToValue(returnjsonResponse, HoldTicketResponse.class);
                if (response.getStatus().equalsIgnoreCase("0")) {
                    ErrorMessage errorMessage = new ErrorMessage();
                    errorMessage.setMessage(response.getMessage());
                    pnrResponse = new PNRResponse();
                    pnrResponse.setErrorMessage(errorMessage);
                    travelomatrixLogger.debug("Response is not valid for HoldBooking: " + response.getMessage());
                }else if (returnresponse != null && returnresponse.getStatus().equalsIgnoreCase("0") ) {
                    ErrorMessage errorMessage = new ErrorMessage();
                    errorMessage.setMessage(returnresponse.getMessage());
                    pnrResponse = new PNRResponse();
                    pnrResponse.setErrorMessage(errorMessage);
                    travelomatrixLogger.debug("Return Response is not valid for HoldBooking: " + response.getMessage());
                } else {
                    if(travellerMasterInfo.getItinerary().getReturnResultToken() != null && travellerMasterInfo.getJourneyType().equalsIgnoreCase("ROUND_TRIP")){
                        pnrResponse = getMergedPNRResponseFromHoldTicket(travellerMasterInfo, response,returnresponse);
                    }else{
                       pnrResponse = getPNRResponseFromHoldTicket(travellerMasterInfo, response);
                    }
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                travelomatrixLogger.error("Error in json parsor :HoldBooking: " + e.getMessage());
                throw new RuntimeException(e);

            }
        }else{
            if(travellerMasterInfo.getItinerary().getReturnResultToken() != null && travellerMasterInfo.getJourneyType().equalsIgnoreCase("ROUND_TRIP")) {
                pnrResponse = getRoundTripBookingPNRResponse(travellerMasterInfo);
            } else {
                pnrResponse = getBookingPNRResponse(travellerMasterInfo);
            }
        }


        return pnrResponse;
    }


    public IssuanceResponse commitBooking(IssuanceRequest issuanceRequest)  {
        IssuanceResponse issuanceResponse = new IssuanceResponse();
        JsonNode jsonResponse = bookingFlights.commitBooking(issuanceRequest,false);
        travelomatrixLogger.debug("Response for commitBooking: " + jsonResponse);
        CommitBookingReply commitBookingReply = null;
        try {
            commitBookingReply = new ObjectMapper().treeToValue(jsonResponse, CommitBookingReply.class);
            if (commitBookingReply.getStatus().equalsIgnoreCase("0")) {
                issuanceResponse = new IssuanceResponse();
                ErrorMessage em = new ErrorMessage();
                em.setMessage(commitBookingReply.getMessage());
                issuanceResponse.setErrorMessage(em);
                travelomatrixLogger.debug("No Response receieved for CommitBooking from Travelomatrix : " + commitBookingReply);
            } else {
                if(issuanceRequest.getReResultToken() != null) {
                    JsonNode rejsonResponse = bookingFlights.commitBooking(issuanceRequest, true);
                    travelomatrixLogger.debug("ReturnResponse for commitBooking: " + jsonResponse);
                    CommitBookingReply recommitBookingReply =  new ObjectMapper().treeToValue(rejsonResponse, CommitBookingReply.class);
                    if (recommitBookingReply.getStatus().equalsIgnoreCase("0")) {
                        travelomatrixLogger.debug("No Response receieved for CommitBooking from Travelomatrix : " + recommitBookingReply);
                        issuanceResponse = getCommitBookingResponse(commitBookingReply);
                    } else {
                        issuanceResponse = getMergedCommitBookingResponse(commitBookingReply,recommitBookingReply);
                    }
                }else{
                    issuanceResponse = getCommitBookingResponse(commitBookingReply);
                }
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
        PNRResponse returnPnrResponse = null;
        travelomatrixLogger.debug("checkFareChangeAndAvailability called...........");
        String resultToken = travellerMasterInfo.getItinerary().getResultToken();
        JsonNode jsonResponse = bookingFlights.getUpdatedFares(resultToken);
        try {
            travelomatrixLogger.debug("Response for checkFareChangeAndAvailability: ResultToken:"+ resultToken +" ----  Response: \n"+ jsonResponse);
            UpdateFareQuotesReply response = new ObjectMapper().treeToValue(jsonResponse, UpdateFareQuotesReply.class);
            if(response.getStatus() == 0){
                travelomatrixLogger.debug("checkFareChangeAndAvailability Respose is not Reeceived for ResultToken :" + resultToken);
                pnrResponse = new PNRResponse();
                ErrorMessage em = new ErrorMessage();
                em.setMessage(response.getMessage());
                pnrResponse.setErrorMessage(em);
            }else{
                pnrResponse = getPNRResponse(travellerMasterInfo,response);
            }

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        if(travellerMasterInfo.getJourneyType().equalsIgnoreCase("ROUND_TRIP") && travellerMasterInfo.getItinerary().getReturnResultToken() != null){
            String returResultToken = travellerMasterInfo.getItinerary().getReturnResultToken();
            JsonNode returnjsonResponse = bookingFlights.getUpdatedFares(returResultToken);
            try {
                travelomatrixLogger.debug("Return Response for checkFareChangeAndAvailability: ResultToken:"+ resultToken +" ----  Response: \n"+ jsonResponse);
                UpdateFareQuotesReply response = new ObjectMapper().treeToValue(returnjsonResponse, UpdateFareQuotesReply.class);
                if(response.getStatus() == 0){
                    travelomatrixLogger.debug("checkFareChangeAndAvailability Return Respose is not Reeceived for ResultToken :" + resultToken);
                    pnrResponse = new PNRResponse();
                    ErrorMessage em = new ErrorMessage();
                    em.setMessage(response.getMessage());
                    pnrResponse.setErrorMessage(em);
                }else{
                    returnPnrResponse = getPNRResponse(travellerMasterInfo,response);
                    pnrResponse = getMergedPNRResponse(pnrResponse,returnPnrResponse,travellerMasterInfo);
                }

            } catch (JsonProcessingException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        return pnrResponse;
    }

    public PNRResponse getPNRResponse(TravellerMasterInfo travellerMasterInfo,UpdateFareQuotesReply updateFareQuotesReply){
        PNRResponse pnrResponse = new PNRResponse();
        Boolean availbleFlights = false;
        pnrResponse.setCreationOfficeId(TraveloMatrixConstants.tmofficeId.toString());
        List<AirSegmentInformation> journeyList= travellerMasterInfo.getItinerary().getNonSeamenJourneyList().get(0).getAirSegmentList();
        for(AirSegmentInformation airSegmentInformation : journeyList){
            if(airSegmentInformation.getAvailbleSeats() != null && airSegmentInformation.getAvailbleSeats() > 0){
                availbleFlights = true;
            }else{
                availbleFlights = false;
            }
        }

        availbleFlights = true;
       String currencyFromReply =  updateFareQuotesReply.getUpdateFareQuote().getFareQuoteDetails().getJourneyList().getPrice().getCurrency();
       Double totalFareFromReply =  updateFareQuotesReply.getUpdateFareQuote().getFareQuoteDetails().getJourneyList().getPrice().getTotalDisplayFare();
       Double agencyCommision = updateFareQuotesReply.getUpdateFareQuote().getFareQuoteDetails().getJourneyList().getPrice().getPriceBreakup().getAgentCommission();
       Double tdsonAgency = updateFareQuotesReply.getUpdateFareQuote().getFareQuoteDetails().getJourneyList().getPrice().getPriceBreakup().getAgentTdsOnCommision();
       Double finalfare = totalFareFromReply-agencyCommision+tdsonAgency;

       String currency = travellerMasterInfo.getItinerary().getPricingInformation().getCurrency();
       BigDecimal totalPrice = travellerMasterInfo.getItinerary().getPricingInformation().getTotalPrice();
       Double difference = Math.abs(finalfare.doubleValue() - totalPrice.doubleValue());
       if(difference <= 50 ){
           pnrResponse.setPriceChanged(false);
       }else{
        pnrResponse.setPriceChanged(true);
        pnrResponse.setChangedBasePrice(new BigDecimal(updateFareQuotesReply.getUpdateFareQuote().getFareQuoteDetails().getJourneyList().getPrice().getPriceBreakup().getBasicFare()));
        pnrResponse.setChangedPrice(new BigDecimal(updateFareQuotesReply.getUpdateFareQuote().getFareQuoteDetails().getJourneyList().getPrice().getTotalDisplayFare()));
       }
        pnrResponse.setFlightAvailable(availbleFlights);
        pnrResponse.setSearchResultToken(travellerMasterInfo.getItinerary().getResultToken());
        pnrResponse.setReturnSearchResultToken(travellerMasterInfo.getItinerary().getReturnResultToken());
        pnrResponse.setResultToken(updateFareQuotesReply.getUpdateFareQuote().getFareQuoteDetails().getJourneyList().getResultToken());
        Price price = updateFareQuotesReply.getUpdateFareQuote().getFareQuoteDetails().getJourneyList().getPrice();
        PricingInformation pricingInformation = new PricingInformation();
        pricingInformation.setBasePrice(new BigDecimal(price.getPriceBreakup().getBasicFare()));
        pricingInformation.setTotalBasePrice(new BigDecimal(price.getTotalDisplayFare()));
        pricingInformation.setAdtBasePrice(new BigDecimal(price.getPassengerBreakup().getADT().getBasePrice()));
        pricingInformation.setAdtTotalPrice(new BigDecimal(price.getPassengerBreakup().getADT().getTotalPrice()));
        //pricingInformation.setAdtTotalPrice(totalPrice);
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
        Map<String,String> baggageMap = new HashMap<>();

        for(List<com.compassites.model.travelomatrix.ResponseModels.UpdateFareQuotes.Detail> details :updateFareQuotesReply.getUpdateFareQuote().getFareQuoteDetails().getJourneyList().getFlightDetails().getDetails()) {
            for (com.compassites.model.travelomatrix.ResponseModels.UpdateFareQuotes.Detail detail : details) {
                if (detail.getAttr() != null) {
                    String key = detail.getOrigin().getAirportCode().toUpperCase().toString() + detail.getDestination().getAirportCode().toUpperCase().toString();
                    String baggage = detail.getAttr().getBaggage();
                    String updateBaggage = updateBaggeUnits(baggage);
                    baggageMap.put(key,updateBaggage);
                }

            }
        }
        pnrResponse.setSegmentBaggageMap(baggageMap);
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
        String baggage = commitBookingReply.getCommitBooking().getBookingDetails().getJourneyList().getFlightDetails().getDetails().get(0).get(0).getAttr().getBaggage().toString();
        String updatedBagunits = updateBaggeUnits(baggage);
        issuanceResponse.setBaggage(updatedBagunits);
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
        for(Journey journey : travellerMasterInfo.getItinerary().getJourneyList()) {
            for (AirSegmentInformation detail : journey.getAirSegmentList()) {
                String segments = detail.getFromLocation().toLowerCase() + detail.getToLocation().toLowerCase() + String.valueOf(segmentnumber);
                String airlinePnr = "NA";
                airlinePNRMap.put(segments, airlinePnr);
                segmentnumber++;
            }
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
        pnrResponse.setSearchResultToken(travellerMasterInfo.getSearchResultToken());
        pnrResponse.setReturnSearchResultToken(travellerMasterInfo.getReturnSearchResultToken());
        return pnrResponse;
    }

    public PNRResponse getRoundTripBookingPNRResponse(TravellerMasterInfo travellerMasterInfo){
        PNRResponse pnrResponse = new PNRResponse();
        // String airlinePNR=commitBookingReply.getCommitBooking().getBookingDetails().getJourneyList().getFlightDetails().getDetails().get(0).get(0).getAirlinePNR();
        // pnrResponse.setAirlinePNR("NA");
        // pnrResponse.setPnrNumber("NA");
        pnrResponse.setFlightAvailable(true);
        pnrResponse.setCreationOfficeId(TraveloMatrixConstants.tmofficeId);
        pnrResponse.setAirlinePNRError(false);
        String resultToken = travellerMasterInfo.getItinerary().getResultToken();
        pnrResponse.setResultToken(resultToken);
        pnrResponse.setReturnResultToken(travellerMasterInfo.getItinerary().getReturnResultToken());
        pnrResponse.setBookingId("NA");
        pnrResponse.setReturnBookingId("NA");
        String appidReference = travellerMasterInfo.getAppReference();
        pnrResponse.setAppReference(appidReference);
        pnrResponse.setReturnAppReference(travellerMasterInfo.getReturnAppRef());
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

        PricingInformation pricingInformation = new PricingInformation();
        pricingInformation.setSegmentWisePricing(true);
        pnrResponse.setPricingInfo(pricingInformation);

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


    public String updateBaggeUnits(String baggage){

        String updatedBagunits = null;
        String pattern = "^KG\\d{3}$";
        if(baggage != null && (baggage.contains("Kg") || baggage.contains("Kilograms") || baggage.contains("kg"))){
            updatedBagunits = baggage.replaceAll("(?i)\\b(kilograms|kg)\\b", "KG");
            if(updatedBagunits.contains("(")){
                int index =   updatedBagunits.indexOf('(');
                if(index != -1){
                    updatedBagunits =   updatedBagunits.substring(0,index).trim();
                }
            }
        }else if(baggage != null && baggage.contains("Piece")){
            updatedBagunits = baggage.replaceAll("^0+", "").replaceAll("\\s*Piece\\s*", " PC");
        }else if (baggage != null && baggage.matches(pattern)) {
            String number = baggage.replaceAll("[^0-9]", "");  // Extract numeric part
            updatedBagunits = Integer.parseInt(number) + " KG";  // Combine with "KG"
        }else{
            updatedBagunits = baggage;
        }
        return updatedBagunits;

    }
    public PNRResponse getMergedPNRResponse(PNRResponse onwordPnrResponse,PNRResponse returnPnrResponse,TravellerMasterInfo travellerMasterInfo){
        PNRResponse pnrResponse = new PNRResponse();
        Boolean availbleFlights = false;
        pnrResponse.setAirlinePNR(onwordPnrResponse.getAirlinePNR());
        pnrResponse.setReturnGdsPNR(returnPnrResponse.getReturnGdsPNR());
        pnrResponse.setCreationOfficeId(TraveloMatrixConstants.tmofficeId.toString());
        if(onwordPnrResponse.isFlightAvailable() && returnPnrResponse.isFlightAvailable()){
            availbleFlights = true;
        }

        if(travellerMasterInfo.getItinerary().getPricingInformation().getOnwardTotalBasePrice() == onwordPnrResponse.getPricingInfo().getTotalBasePrice()
           && travellerMasterInfo.getItinerary().getPricingInformation().getOnwardTotalBasePrice() == onwordPnrResponse.getPricingInfo().getTotalBasePrice()){
            pnrResponse.setPriceChanged(false);
        }else{
            pnrResponse.setPriceChanged(false);
            pnrResponse.setChangedBasePrice(onwordPnrResponse.getChangedBasePrice().add(returnPnrResponse.getChangedBasePrice()));
            pnrResponse.setChangedPrice(onwordPnrResponse.getChangedPrice().add(returnPnrResponse.getChangedPrice()));
        }
        pnrResponse.setFlightAvailable(availbleFlights);
        pnrResponse.setResultToken(onwordPnrResponse.getResultToken());
        pnrResponse.setReturnResultToken(returnPnrResponse.getResultToken());
        pnrResponse.setAppReference(travellerMasterInfo.getAppReference());
        pnrResponse.setReturnAppReference(travellerMasterInfo.getReturnAppRef());
        PricingInformation pricingInformation = new PricingInformation();
        pricingInformation.setBasePrice(onwordPnrResponse.getPricingInfo().getBasePrice().add(returnPnrResponse.getPricingInfo().getBasePrice()));
        pricingInformation.setTotalBasePrice(onwordPnrResponse.getPricingInfo().getTotalBasePrice().add(returnPnrResponse.getPricingInfo().getTotalBasePrice()));
        pricingInformation.setAdtBasePrice(onwordPnrResponse.getPricingInfo().getAdtBasePrice().add(returnPnrResponse.getPricingInfo().getAdtBasePrice()));
        pricingInformation.setAdtTotalPrice(onwordPnrResponse.getPricingInfo().getAdtTotalPrice().add(returnPnrResponse.getPricingInfo().getAdtTotalPrice()));
        //pricingInformation.setAdtTotalPrice(totalPrice);
        pricingInformation.setGdsCurrency(onwordPnrResponse.getPricingInfo().getGdsCurrency());
        pricingInformation.setTax(onwordPnrResponse.getPricingInfo().getTax().add(returnPnrResponse.getPricingInfo().getTax()));
        if(!onwordPnrResponse.getPricingInfo().getChdBasePrice().equals(0) && !returnPnrResponse.getPricingInfo().getChdBasePrice().equals(0) )
            pricingInformation.setChdBasePrice(onwordPnrResponse.getPricingInfo().getChdBasePrice().add(returnPnrResponse.getPricingInfo().getChdBasePrice()));
        else
            pricingInformation.setChdBasePrice(new BigDecimal(0));
        if(!onwordPnrResponse.getPricingInfo().getInfBasePrice().equals(0) && !returnPnrResponse.getPricingInfo().getInfBasePrice().equals(0))
            pricingInformation.setInfBasePrice(onwordPnrResponse.getPricingInfo().getInfBasePrice().add(returnPnrResponse.getPricingInfo().getInfBasePrice()));
        else
            pricingInformation.setInfBasePrice(new BigDecimal(0));
        pricingInformation.setCurrency(onwordPnrResponse.getPricingInfo().getCurrency());
        pricingInformation.setTotalPrice(onwordPnrResponse.getPricingInfo().getTotalPrice().add(returnPnrResponse.getPricingInfo().getTotalPrice()));
        pricingInformation.setTotalPriceValue(onwordPnrResponse.getPricingInfo().getTotalPriceValue().add(returnPnrResponse.getPricingInfo().getTotalPriceValue()));
        List<PassengerTax> passengerTaxList = new LinkedList<>();
        PassengerTax adtPassengerTax = new PassengerTax();
        adtPassengerTax.setPassengerType("ADT");
        adtPassengerTax.setPassengerCount(onwordPnrResponse.getPricingInfo().getPassengerTaxes().get(0).getPassengerCount());
        adtPassengerTax.setTotalTax(onwordPnrResponse.getPricingInfo().getPassengerTaxes().get(0).getTotalTax().add(returnPnrResponse.getPricingInfo().getPassengerTaxes().get(0).getTotalTax()));
        passengerTaxList.add(adtPassengerTax);

        PassengerTax onchdPassengerTax = new PassengerTax();
        PassengerTax oninfPassengerTax = new PassengerTax();
        PassengerTax rechdPassengerTax = new PassengerTax();
        PassengerTax reinfPassengerTax = new PassengerTax();
        if(onwordPnrResponse.getPricingInfo().getPassengerTaxes().size() > 1 && onwordPnrResponse.getPricingInfo().getPassengerTaxes().get(1).getPassengerType().equalsIgnoreCase("CHD")){
            onchdPassengerTax = onwordPnrResponse.getPricingInfo().getPassengerTaxes().get(1);
            rechdPassengerTax = returnPnrResponse.getPricingInfo().getPassengerTaxes().get(1);
        }
        if(onwordPnrResponse.getPricingInfo().getPassengerTaxes().size() > 1 && onwordPnrResponse.getPricingInfo().getPassengerTaxes().get(1).getPassengerType().equalsIgnoreCase("INF")){
            oninfPassengerTax = onwordPnrResponse.getPricingInfo().getPassengerTaxes().get(1);
            reinfPassengerTax = returnPnrResponse.getPricingInfo().getPassengerTaxes().get(1);
        }
        if(onwordPnrResponse.getPricingInfo().getPassengerTaxes().size() > 2 && onwordPnrResponse.getPricingInfo().getPassengerTaxes().get(2).getPassengerType().equalsIgnoreCase("INF")){
            oninfPassengerTax = onwordPnrResponse.getPricingInfo().getPassengerTaxes().get(2);
            reinfPassengerTax = returnPnrResponse.getPricingInfo().getPassengerTaxes().get(2);
        }
        if(onchdPassengerTax.getPassengerType() != null && onchdPassengerTax.getPassengerType().equalsIgnoreCase("CHD")){
            PassengerTax chdPassengerTax = new PassengerTax();
            chdPassengerTax.setPassengerType("CHD");
            chdPassengerTax.setPassengerCount(onchdPassengerTax.getPassengerCount());
            chdPassengerTax.setTotalTax(onchdPassengerTax.getTotalTax().add(rechdPassengerTax.getTotalTax()));
            passengerTaxList.add(chdPassengerTax);
        }
        if(oninfPassengerTax.getPassengerType() != null && oninfPassengerTax.getPassengerType().equalsIgnoreCase("INF")){
            PassengerTax infPassengerTax = new PassengerTax();
            infPassengerTax.setPassengerType("INF");
            infPassengerTax.setPassengerCount(oninfPassengerTax.getPassengerCount());
            infPassengerTax.setTotalTax(oninfPassengerTax.getTotalTax().add(reinfPassengerTax.getTotalTax()));
            passengerTaxList.add(infPassengerTax);
        }
        pricingInformation.setPassengerTaxes(passengerTaxList);
        pricingInformation.setLCC(onwordPnrResponse.getPricingInfo().isLCC());
        pricingInformation.setProvider(TraveloMatrixConstants.provider);
        pnrResponse.setPricingInfo(pricingInformation);
        pnrResponse.setCappingLimitReached(false);
        Map<String,String> baggageMap = new HashMap<>();
        Map<String,String> onwordBaggageMap  = onwordPnrResponse.getSegmentBaggageMap();
        Map<String,String> returnBaggageMap  = returnPnrResponse.getSegmentBaggageMap();
        baggageMap.putAll(onwordBaggageMap);
        baggageMap.putAll(returnBaggageMap);
        pnrResponse.setSegmentBaggageMap(baggageMap);
        pnrResponse.setHoldTime(pnrResponse.isHoldTime());
        return pnrResponse;
    }

    public PNRResponse getMergedPNRResponseFromHoldTicket(TravellerMasterInfo travellerMasterInfo, HoldTicketResponse onwordresponse,HoldTicketResponse returnresponse){
        PNRResponse pnrResponse = new PNRResponse();
        String airlinePNR=onwordresponse.getHoldTicket().getBookingDetails().getPNR();
        pnrResponse.setAirlinePNR(airlinePNR);
        pnrResponse.setReturnGdsPNR(returnresponse.getHoldTicket().getBookingDetails().getPNR());
        pnrResponse.setPnrNumber(onwordresponse.getHoldTicket().getBookingDetails().getPNR());
        pnrResponse.setFlightAvailable(true);
        pnrResponse.setCreationOfficeId(TraveloMatrixConstants.tmofficeId);
        pnrResponse.setAirlinePNRError(false);
        String resultToken = travellerMasterInfo.getItinerary().getResultToken();
        pnrResponse.setResultToken(resultToken);
        pnrResponse.setReturnResultToken(travellerMasterInfo.getItinerary().getReturnResultToken());
        pnrResponse.setBookingId(onwordresponse.getHoldTicket().getBookingDetails().getBookingId());
        pnrResponse.setReturnBookingId(returnresponse.getHoldTicket().getBookingDetails().getBookingId());
        String appidReference = travellerMasterInfo.getAppReference();
        pnrResponse.setAppReference(appidReference);
        pnrResponse.setReturnAppReference(travellerMasterInfo.getReturnAppRef());
        List<com.compassites.model.travelomatrix.ResponseModels.HoldTicket.Detail> details = onwordresponse.getHoldTicket().getBookingDetails().getJourneyList().getFlightDetails().getDetails().get(0);
        int segmentnumber = 1;

        List<com.compassites.model.travelomatrix.ResponseModels.HoldTicket.Detail> redetails = returnresponse.getHoldTicket().getBookingDetails().getJourneyList().getFlightDetails().getDetails().get(0);

        Map<String,String> airlinePNRMap = new HashMap<>();
        for(com.compassites.model.travelomatrix.ResponseModels.HoldTicket.Detail detail:details) {
            String segments = detail.getOrigin().getAirportCode().toLowerCase() + detail.getDestination().getAirportCode().toLowerCase() + String.valueOf(segmentnumber);
            String airlinePnr = detail.getAirlinePNR();
            airlinePNRMap.put(segments,airlinePnr);
            segmentnumber++;
        }
        Map<String,String> returnAirlinePNRMap = new HashMap<>();
        for(com.compassites.model.travelomatrix.ResponseModels.HoldTicket.Detail detail:redetails) {
            String segments = detail.getOrigin().getAirportCode().toLowerCase() + detail.getDestination().getAirportCode().toLowerCase() + String.valueOf(segmentnumber);
            String airlinePnr = detail.getAirlinePNR();
            returnAirlinePNRMap.put(segments,airlinePnr);
            segmentnumber++;
        }
        airlinePNRMap.putAll(returnAirlinePNRMap);
        pnrResponse.setAirlinePNRMap(airlinePNRMap);
        // Map<String,String> tickenetNumberMap = new HashMap<>();
        List<com.compassites.model.travelomatrix.ResponseModels.HoldTicket.PassengerDetail> passengerDetailList =  onwordresponse.getHoldTicket().getBookingDetails().getPassengerDetails();
        for(com.compassites.model.travelomatrix.ResponseModels.HoldTicket.PassengerDetail passengerDetail:passengerDetailList){
            String passengerType = passengerDetail.getPassengerType();
            // String ticketNumber  = passengerDetail.getTicketNumber();
            // tickenetNumberMap.put(passengerType,ticketNumber);
        }
        // pnrResponse.setTicketNumberMap(tickenetNumberMap);
        return pnrResponse;
    }

    public IssuanceResponse getMergedCommitBookingResponse(CommitBookingReply onwardJourney,CommitBookingReply returnJourney){
        IssuanceResponse issuanceResponse = new IssuanceResponse();
        String airlinePNR=onwardJourney.getCommitBooking().getBookingDetails().getJourneyList().getFlightDetails().getDetails().get(0).get(0).getAirlinePNR();
        issuanceResponse.setAirlinePnr(airlinePNR);
        String reairlinePNR=returnJourney.getCommitBooking().getBookingDetails().getJourneyList().getFlightDetails().getDetails().get(0).get(0).getAirlinePNR();
        issuanceResponse.setReturnAirlinePnr(reairlinePNR);
        issuanceResponse.setSuccess(true);
        List<Detail> details = onwardJourney.getCommitBooking().getBookingDetails().getJourneyList().getFlightDetails().getDetails().get(0);
        int segmentnumber = 1;
        Map<String,String> airlinePNRMap = new HashMap<>();
        for(Detail detail:details) {
            String segments = detail.getOrigin().getAirportCode().toLowerCase() + detail.getDestination().getAirportCode().toLowerCase() + String.valueOf(segmentnumber);
            String airlinePnr = detail.getAirlinePNR();
            airlinePNRMap.put(segments,airlinePnr);
            segmentnumber++;
        }
        List<Detail> redetails = returnJourney.getCommitBooking().getBookingDetails().getJourneyList().getFlightDetails().getDetails().get(0);
        Map<String,String> reairlinePNRMap = new HashMap<>();
        for(Detail detail:redetails) {
            String segments = detail.getOrigin().getAirportCode().toLowerCase() + detail.getDestination().getAirportCode().toLowerCase() + String.valueOf(segmentnumber);
            String airlinePnr = detail.getAirlinePNR();
            airlinePNRMap.put(segments,airlinePnr);
            segmentnumber++;
        }
        issuanceResponse.setAirlinePNRMap(airlinePNRMap);
        issuanceResponse.setIssued(true);
        Map<String,String> tickenetNumberMap = new HashMap<>();
        Map<String,String> reTickenetNumberMap = new HashMap<>();
        List<PassengerDetail> passengerDetailList =  onwardJourney.getCommitBooking().getBookingDetails().getPassengerDetails();
        for(PassengerDetail passengerDetail:passengerDetailList){
            String passengerType = passengerDetail.getPassengerType();
            String ticketNumber  = passengerDetail.getTicketNumber();
            tickenetNumberMap.put(passengerType,ticketNumber);
        }
        List<PassengerDetail> repassengerDetailList =  returnJourney.getCommitBooking().getBookingDetails().getPassengerDetails();
        for(PassengerDetail passengerDetail:passengerDetailList){
            String passengerType = passengerDetail.getPassengerType();
            String ticketNumber  = passengerDetail.getTicketNumber();
            reTickenetNumberMap.put(passengerType,ticketNumber);
        }
        issuanceResponse.setTicketNumberMap(tickenetNumberMap);
        issuanceResponse.setTicketNumberMap(reTickenetNumberMap);
        issuanceResponse.setBookingId(onwardJourney.getCommitBooking().getBookingDetails().getBookingId());
        issuanceResponse.setBookingId(returnJourney.getCommitBooking().getBookingDetails().getBookingId());
        String baggage = onwardJourney.getCommitBooking().getBookingDetails().getJourneyList().getFlightDetails().getDetails().get(0).get(0).getAttr().getBaggage().toString();
        String updatedBagunits = updateBaggeUnits(baggage);
        issuanceResponse.setBaggage(updatedBagunits);
        return issuanceResponse;

    }
}
