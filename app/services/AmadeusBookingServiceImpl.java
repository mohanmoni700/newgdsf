package services;

import com.amadeus.xml.farqnr_07_1_1a.FareCheckRulesReply;
import com.amadeus.xml.itares_05_2_ia.AirSellFromRecommendationReply;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply;
import com.amadeus.xml.tautcr_04_1_1a.TicketCreateTSTFromPricingReply;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply;
import com.amadeus.xml.tpcbrr_07_3_1a.FarePricePNRWithBookingClassReply;
import com.amadeus.xml.ttktir_09_1_1a.DocIssuanceIssueTicketReply;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.*;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;
import org.springframework.stereotype.Service;
import utils.ErrorMessageHelper;
import utils.XMLFileUtility;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


@Service
public class AmadeusBookingServiceImpl implements BookingService {

    private final String amadeusFlightAvailibilityCode = "OK";

    private final String totalFareIdentifier = "712";

    private final String issuenceOkStatus = "O";

    private final String cappingLimitString = "CT RJT";

    @Override
    public PNRResponse generatePNR(TravellerMasterInfo travellerMasterInfo) {

        ServiceHandler serviceHandler = null;
        PNRResponse pnrResponse = new PNRResponse();
        FarePricePNRWithBookingClassReply pricePNRReply = null;
        try {
            serviceHandler = new ServiceHandler();

            serviceHandler.logIn();
            AirSellFromRecommendationReply sellFromRecommendation = serviceHandler.checkFlightAvailability(travellerMasterInfo);

            if(sellFromRecommendation.getErrorAtMessageLevel() != null && sellFromRecommendation.getErrorAtMessageLevel().size() > 0 && (sellFromRecommendation.getItineraryDetails() == null)){

                ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("error", ErrorMessage.ErrorType.ERROR, "Amadeus");
                pnrResponse.setErrorMessage(errorMessage);
            }

            boolean flightAvailable = validateFlightAvailability(sellFromRecommendation);

            if (flightAvailable) {

                PNRReply gdsPNRReply = serviceHandler.addTravellerInfoToPNR(travellerMasterInfo);
                pricePNRReply = serviceHandler.pricePNR(travellerMasterInfo, gdsPNRReply);

                pnrResponse = checkFare(pricePNRReply, travellerMasterInfo);
                if (!pnrResponse.isPriceChanged()) {
                    TicketCreateTSTFromPricingReply ticketCreateTSTFromPricingReply = serviceHandler.createTST();
                    if(ticketCreateTSTFromPricingReply.getApplicationError() != null){
                        String errorCode = ticketCreateTSTFromPricingReply.getApplicationError().getApplicationErrorInfo().getApplicationErrorDetail().getApplicationErrorCode();

                        ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("error", ErrorMessage.ErrorType.ERROR, "Amadeus");
                        pnrResponse.setErrorMessage(errorMessage);
                        pnrResponse.setFlightAvailable(false);
                        return pnrResponse;
                    }
                    gdsPNRReply = serviceHandler.savePNR();
                    String tstRefNo = getPNRNoFromResponse(gdsPNRReply);
                    gdsPNRReply = serviceHandler.retrivePNR(tstRefNo);
                    //pnrResponse.setPnrNumber(gdsPNRReply.getPnrHeader().get(0).getReservationInfo().getReservation().getControlNumber());
                    pnrResponse = createPNRResponse(gdsPNRReply, pricePNRReply);

                    //getCancellationFee(travellerMasterInfo, serviceHandler);
                }
            } else {

                pnrResponse.setFlightAvailable(false);
            }


        } catch (Exception e) {
            e.printStackTrace();
            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("error", ErrorMessage.ErrorType.ERROR, "Amadeus");
            pnrResponse.setErrorMessage(errorMessage);
        }

        return pnrResponse;
    }

    @Override
    public PNRResponse priceChangePNR(TravellerMasterInfo travellerMasterInfo) {
            return generatePNR(travellerMasterInfo);
    }




    public boolean validateFlightAvailability(AirSellFromRecommendationReply sellFromRecommendation){
        boolean errors = true;
        for (AirSellFromRecommendationReply.ItineraryDetails itinerary : sellFromRecommendation.getItineraryDetails()){
            for(AirSellFromRecommendationReply.ItineraryDetails.SegmentInformation segmentInformation : itinerary.getSegmentInformation()){
                for(String statusCode : segmentInformation.getActionDetails().getStatusCode()){
                    if(!amadeusFlightAvailibilityCode.equals(statusCode)){
                        errors = false;
                    }
                }
            }
        }
        return errors;
    }


    public PNRResponse checkFare(FarePricePNRWithBookingClassReply pricePNRReply,TravellerMasterInfo travellerMasterInfo){
        BigDecimal totalFare = new BigDecimal(0);
        PNRResponse pnrResponse = new PNRResponse();
        List<FarePricePNRWithBookingClassReply.FareList.FareDataInformation.FareDataSupInformation> fareList = pricePNRReply.getFareList().get(0).getFareDataInformation().getFareDataSupInformation();
        for (FarePricePNRWithBookingClassReply.FareList.FareDataInformation.FareDataSupInformation fareData : fareList){

            if(totalFareIdentifier.equals(fareData.getFareDataQualifier())){
                totalFare = new BigDecimal(fareData.getFareAmount());
                break;
            }

        }
        BigDecimal searchPrice = new BigDecimal(0);
        if(travellerMasterInfo.isSeamen()){
            searchPrice = travellerMasterInfo.getItinerary().getSeamanPricingInformation().getTotalPriceValue();
        }else {
            searchPrice = travellerMasterInfo.getItinerary().getPricingInformation().getTotalPriceValue();
        }


        if(totalFare.toBigIntegerExact().equals(searchPrice.toBigIntegerExact())) {
            return pnrResponse;
        }
        pnrResponse.setChangedPrice(totalFare);
        pnrResponse.setOriginalPrice(searchPrice);
        pnrResponse.setPriceChanged(true);
        pnrResponse.setFlightAvailable(true);
        return pnrResponse;

    }

    public String getPNRNoFromResponse(PNRReply gdsPNRReply){
        String pnrNumber = null;
        for(PNRReply.PnrHeader pnrHeader: gdsPNRReply.getPnrHeader()){
            pnrNumber = pnrHeader.getReservationInfo().getReservation().getControlNumber();
        }

        return pnrNumber;
    }

    public PNRResponse createPNRResponse(PNRReply gdsPNRReply,FarePricePNRWithBookingClassReply pricePNRReply){
        PNRResponse pnrResponse = new PNRResponse();

        for(PNRReply.PnrHeader pnrHeader: gdsPNRReply.getPnrHeader()){
            pnrResponse.setPnrNumber(pnrHeader.getReservationInfo().getReservation().getControlNumber());
        }
        FarePricePNRWithBookingClassReply.FareList.LastTktDate.DateTime dateTime = pricePNRReply.getFareList().get(0).getLastTktDate().getDateTime();
        String day = ((dateTime.getDay().toString().length() == 1) ? "0"+dateTime.getDay(): dateTime.getDay().toString());
        String month = ((dateTime.getMonth().toString().length() == 1) ? "0"+dateTime.getMonth(): dateTime.getMonth().toString());
        String year = dateTime.getYear().toString();
        //pnrResponse.setValidTillDate(""+dateTime.getDay()+dateTime.getMonth()+dateTime.getYear());
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
        Date lastTicketingDate = null;
        try {
           lastTicketingDate =  sdf.parse(day+month+year);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        pnrResponse.setValidTillDate(lastTicketingDate);
        pnrResponse.setFlightAvailable(true);
        pnrResponse.setTaxDetailsList(getTaxDetails(pricePNRReply));
        return pnrResponse;
    }

    public IssuanceResponse issueTicket(IssuanceRequest issuanceRequest) {
        ServiceHandler serviceHandler = null;
        IssuanceResponse issuanceResponse = new IssuanceResponse();
        issuanceResponse.setPnrNumber(issuanceRequest.getGdsPNR());
        try {
            serviceHandler = new ServiceHandler();
            serviceHandler.logIn();
            PNRReply gdsPNRReply = serviceHandler.retrivePNR(issuanceRequest.getGdsPNR());

            XMLFileUtility.createXMLFile(gdsPNRReply, "retrievePNRRes1.json");
            DocIssuanceIssueTicketReply issuanceIssueTicketReply = serviceHandler.issueTicket();
            if(issuenceOkStatus.equals(issuanceIssueTicketReply.getProcessingStatus().getStatusCode())){
                gdsPNRReply = serviceHandler.retrivePNR(issuanceRequest.getGdsPNR());
                Map<String,Object> airSegmentRefMap = new HashMap<>();
                Map<String,Object> travellerMap = new HashMap<>();
                for(PNRReply.OriginDestinationDetails originDestination : gdsPNRReply.getOriginDestinationDetails()){
                    for(PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestination.getItineraryInfo()){
                        String segmentRef = itineraryInfo.getElementManagementItinerary().getReference().getQualifier()+itineraryInfo.getElementManagementItinerary().getReference().getNumber();
                        airSegmentRefMap.put(segmentRef,itineraryInfo);
                    }
                }
                for(PNRReply.TravellerInfo travellerInfo : gdsPNRReply.getTravellerInfo()){
                    String key = travellerInfo.getElementManagementPassenger().getReference().getQualifier() + travellerInfo.getElementManagementPassenger().getReference().getNumber();
                    travellerMap.put(key,travellerInfo);
                }
                for(PNRReply.DataElementsMaster.DataElementsIndiv dataElementsDiv :gdsPNRReply.getDataElementsMaster().getDataElementsIndiv()){
                    if("FA".equals(dataElementsDiv.getElementManagementData().getSegmentName())){
                        String passengerRef = "";
                        List<String> segmentRefList = new ArrayList<>();
                        String travellerKey = "";
                        for(PNRReply.DataElementsMaster.DataElementsIndiv.ReferenceForDataElement.Reference reference :dataElementsDiv.getReferenceForDataElement().getReference()){
                            travellerKey = reference.getQualifier()+ reference.getNumber();
                            if(travellerMap.containsKey(travellerKey)){
                                passengerRef = travellerKey;
                            }else {
                                segmentRefList.add(travellerKey);
                            }
                        }
                        PNRReply.TravellerInfo traveller = (PNRReply.TravellerInfo)travellerMap.get(travellerKey);
                        String lastName = traveller.getPassengerData().get(0).getTravellerInformation().getTraveller().getSurname();
                        String name = traveller.getPassengerData().get(0).getTravellerInformation().getPassenger().get(0).getFirstName();
                        String[] nameArray = name.split(" ");
                        String firstName = nameArray[0];
                        String middleName = (nameArray.length > 1)? nameArray[1]: "";

                        for(Traveller traveller1 : issuanceRequest.getTravellerList()){
                            if(firstName.equalsIgnoreCase(traveller1.getPersonalDetails().getFirstName()) && middleName.equalsIgnoreCase(traveller1.getPersonalDetails().getMiddleName())
                                    && lastName.equalsIgnoreCase(traveller1.getPersonalDetails().getLastName())){
                                String freeText = dataElementsDiv.getOtherDataFreetext().get(0).getLongFreetext();
                                String[] freeTextArr = freeText.split("/");
                                String ticketNumber = freeTextArr[0].substring(3);
                                Map<String,String> ticketMap = new HashMap<>();
                                for(String segmentRef : segmentRefList){
                                    PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo = (PNRReply.OriginDestinationDetails.ItineraryInfo)airSegmentRefMap.get(segmentRef);
                                    String key = itineraryInfo.getTravelProduct().getProduct().getDepDate() + itineraryInfo.getTravelProduct().getProduct().getDepTime();
                                    ticketMap.put(key,ticketNumber);
                                }
                                traveller1.setTicketNumberMap(ticketMap);
                                issuanceResponse.setTravellerList(issuanceRequest.getTravellerList());
                            }
                        }
                        issuanceResponse.setSuccess(true);
                    }
                }
            }else {
                String errorDescription = issuanceIssueTicketReply.getErrorGroup().getErrorWarningDescription().getFreeText();
                if(errorDescription.contains(cappingLimitString)){
                    System.out.println("Send Email to operator saying capping limit is reached");
                    issuanceResponse.setCappingLimitReached(true);
                }
            }
            getCancellationFee(issuanceRequest, serviceHandler);

            System.out.println("");
        } catch (Exception e) {
            e.printStackTrace();
        }

      return issuanceResponse;
    }


    public List<TaxDetails> getTaxDetails(FarePricePNRWithBookingClassReply pricePNRReply){
        List<TaxDetails> taxDetailsList = new ArrayList<>();
        for(FarePricePNRWithBookingClassReply.FareList fareList : pricePNRReply.getFareList()){
            for(FarePricePNRWithBookingClassReply.FareList.TaxInformation taxInformation :fareList.getTaxInformation()){
                TaxDetails taxDetails = new TaxDetails();
                taxDetails.setTaxCode(taxInformation.getTaxDetails().getTaxType().getIsoCountry());
                taxDetails.setTaxAmount(new BigDecimal(taxInformation.getAmountDetails().getFareDataMainInformation().getFareAmount()));
                taxDetailsList.add(taxDetails);
            }
        }
        return taxDetailsList;
    }

    public void getCancellationFee(IssuanceRequest issuanceRequest,ServiceHandler serviceHandler){
        //ServiceHandler serviceHandler = null;
        try {
            //serviceHandler = new ServiceHandler();
            //serviceHandler.logIn();
            /*int adultCount  = 0,childCount  = 0,infantCount = 0;
            for(Traveller traveller : travellerMasterInfo.getTravellersList()){
                PassengerTypeCode passengerType = DateUtility.getPassengerTypeFromDOB(traveller.getPersonalDetails().getDateOfBirth());
                if(passengerType.equals(PassengerTypeCode.ADT)){
                    adultCount++;
                } else if(passengerType.equals(PassengerTypeCode.CHD)){
                    childCount++;
                } else {
                    infantCount++;
                }
            }*/
            FareInformativePricingWithoutPNRReply fareInfoReply = serviceHandler.getFareInfo(issuanceRequest.getFlightItinerary(), issuanceRequest.getAdultCount(), issuanceRequest.getChildCount(), issuanceRequest.getInfantCount());

            FareCheckRulesReply fareCheckRulesReply = serviceHandler.getFareRules();

            StringBuilder fareRule = new StringBuilder();
            for(FareCheckRulesReply.TariffInfo tariffInfo : fareCheckRulesReply.getTariffInfo()){
                if("(16)".equals(tariffInfo.getFareRuleInfo().getRuleCategoryCode())){
                    for(FareCheckRulesReply.TariffInfo.FareRuleText text : tariffInfo.getFareRuleText() ) {
                        fareRule.append(text.getFreeText().get(0));
                    }
                }
            }
            System.out.println("---------------------------------------Fare Rules------------------------------------\n"+fareRule.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
