package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.AmadeusWebServices;
import com.amadeus.xml.AmadeusWebServicesPT;
import com.amadeus.xml.farqiq_08_2_1a.OriginAndDestinationDetails;
import com.amadeus.xml.farqnq_07_1_1a.FareCheckRules;
import com.amadeus.xml.farqnr_07_1_1a.FareCheckRulesReply;
import com.amadeus.xml.flireq_07_1_1a.AirFlightInfo;
import com.amadeus.xml.flires_07_1_1a.AirFlightInfoReply;
import com.amadeus.xml.fmptbq_14_2_1a.FareMasterPricerTravelBoardSearch;
import com.amadeus.xml.fmptbr_14_2_1a.FareMasterPricerTravelBoardSearchReply;
import com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation;
import com.amadeus.xml.itares_05_2_ia.AirSellFromRecommendationReply;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.pnradd_11_3_1a.PNRAddMultiElements;
import com.amadeus.xml.pnrret_11_3_1a.PNRRetrieve;
import com.amadeus.xml.pnrspl_10_1_1a.PNRSplit;
import com.amadeus.xml.pnrxcl_11_3_1a.CancelPNRElementType;
import com.amadeus.xml.pnrxcl_11_3_1a.ElementIdentificationType;
import com.amadeus.xml.pnrxcl_11_3_1a.OptionalPNRActionsType;
import com.amadeus.xml.pnrxcl_11_3_1a.PNRCancel;
import com.amadeus.xml.qdqlrq_11_1_1a.QueueList;
import com.amadeus.xml.qdqlrr_11_1_1a.QueueListReply;
import com.amadeus.xml.tautcq_04_1_1a.TicketCreateTSTFromPricing;
import com.amadeus.xml.tautcr_04_1_1a.TicketCreateTSTFromPricingReply;
import com.amadeus.xml.tipnrq_12_4_1a.FareInformativePricingWithoutPNR;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply;
import com.amadeus.xml.tpcbrq_12_4_1a.FarePricePNRWithBookingClass;
import com.amadeus.xml.tpcbrr_12_4_1a.FarePricePNRWithBookingClassReply;
import com.amadeus.xml.tplprq_12_4_1a.FarePricePNRWithLowestFare;
import com.amadeus.xml.tplprr_12_4_1a.FarePricePNRWithLowestFareReply;
import com.amadeus.xml.ttktiq_09_1_1a.DocIssuanceIssueTicket;
import com.amadeus.xml.ttktir_09_1_1a.DocIssuanceIssueTicketReply;
import com.amadeus.xml.ttstrq_13_1_1a.TicketDisplayTST;
import com.amadeus.xml.ttstrr_13_1_1a.TicketDisplayTSTReply;
import com.amadeus.xml.vlsslq_06_1_1a.SecurityAuthenticate;
import com.amadeus.xml.vlsslr_06_1_1a.SecurityAuthenticateReply;
import com.amadeus.xml.vlssoq_04_1_1a.SecuritySignOut;
import com.amadeus.xml.vlssor_04_1_1a.SecuritySignOutReply;
import com.amadeus.xml.ws._2009._01.wbs_session_2_0.Session;
import com.compassites.constants.AmadeusConstants;
import com.compassites.model.*;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.thoughtworks.xstream.XStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import utils.XMLFileUtility;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.handler.MessageContext;
import java.math.BigInteger;
import java.net.URL;
import java.util.*;

@Service
public class ServiceHandler {

    AmadeusWebServicesPT mPortType;

    SessionHandler mSession;

    public static URL wsdlUrl;

    static Logger amadeusLogger = LoggerFactory.getLogger("amadeus");

    static Logger logger = LoggerFactory.getLogger("gds");

    public static String endPoint = null;

    static {
        URL url = null;
        try{
            endPoint = play.Play.application().configuration().getString("amadeus.endPointURL");
            url = ServiceHandler.class.getResource("/wsdl/amadeus/amadeus.wsdl");
        }catch (Exception e){
            logger.debug("Error in loading Amadeus URL : ", e);
        }
        wsdlUrl = url;
    }

    public ServiceHandler() throws Exception{
//        URL wsdlUrl=ServiceHandler.class.getResource("/wsdl/amadeus/1ASIWFLYFYH_PDT_20140429_052541.wsdl");
//        URL wsdlUrl = ServiceHandler.class.getResource("/wsdl/amadeus/amadeus.wsdl");
        AmadeusWebServices service = new AmadeusWebServices(wsdlUrl);
        mPortType = service.getAmadeusWebServicesPort();

        //gzip compression headers
        HashMap httpHeaders = new HashMap();
        httpHeaders.put("Content-Encoding", Collections.singletonList("gzip"));
        httpHeaders.put("Accept-Encoding", Collections.singletonList("gzip"));
        Map reqContext = ((BindingProvider) mPortType).getRequestContext();
        reqContext.put(MessageContext.HTTP_REQUEST_HEADERS, httpHeaders);
        reqContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPoint);
        mSession = new SessionHandler();
    }

    public void setSession(Session session){
        mSession = new SessionHandler(new Holder<>(session));

    }

    public Session getSession(){
        return mSession.getSession().value;
    }

    public SessionReply logIn() {
        logger.debug("amadeus login called ....................");
        SecurityAuthenticateReply securityAuthenticate = mPortType.securityAuthenticate(new MessageFactory().buildAuthenticationRequest(), mSession.getSession());
        SessionReply sessionReply=new SessionReply();
        sessionReply.setSecurityAuthenticateReply(securityAuthenticate);
        sessionReply.setSession(mSession.getSession().value);
        return sessionReply;
    }

    public SessionHandler logIn(SessionHandler mSession) {
        logger.debug("amadeus login called....................");
        SecurityAuthenticate securityAuthenticateReq = new MessageFactory().buildAuthenticationRequest();
        logger.debug("amadeus login called at : " + new Date() + " " + mSession.getSessionId());
        amadeusLogger.debug("securityAuthenticateReq " + new Date() + " ---->" + new XStream().toXML(securityAuthenticateReq));
        SecurityAuthenticateReply securityAuthenticate = mPortType.securityAuthenticate(securityAuthenticateReq, mSession.getSession());

        amadeusLogger.debug("securityAuthenticateRes " + new Date() + " ---->" + new XStream().toXML(securityAuthenticate));
        return mSession;
    }

    public SecuritySignOutReply logOut() {
        SecuritySignOutReply signOutReply = null;
        mSession.incrementSequenceNumber();
        logger.debug("AmadeusFlightSearch securitySignOut at : " + new Date() + " " + mSession.getSessionId());

        signOutReply = mPortType.securitySignOut(new SecuritySignOut(), mSession.getSession());
        amadeusLogger.debug("signOutReplyRes " + new Date() + " ---->" + new XStream().toXML(signOutReply));
        mSession.resetSession();
        return signOutReply;
    }

    //search flights with 2 cities- faremastertravelboard service
    public FareMasterPricerTravelBoardSearchReply searchAirlines(SearchParameters searchParameters, SessionHandler mSession) {
        mSession.incrementSequenceNumber();
        logger.debug("AmadeusFlightSearch called at : " + new Date() + " " + mSession.getSessionId());
        FareMasterPricerTravelBoardSearch fareMasterPricerTravelBoardSearch = new SearchFlights().createSearchQuery(searchParameters);
        amadeusLogger.debug("AmadeusSearchReq " + new Date() + " SessionId: " + mSession.getSessionId() + " ---->" + new XStream().toXML(fareMasterPricerTravelBoardSearch));
        FareMasterPricerTravelBoardSearchReply SearchReply = mPortType.fareMasterPricerTravelBoardSearch(fareMasterPricerTravelBoardSearch, mSession.getSession());
        logger.debug("AmadeusFlightSearch response returned  at : " + new Date());
        return  SearchReply;
    }
    
    public AirSellFromRecommendationReply checkFlightAvailability(TravellerMasterInfo travellerMasterInfo) {
        mSession.incrementSequenceNumber();
        logger.debug("amadeus checkFlightAvailability called at : " + new Date()  + "....................Session Id: " + mSession.getSessionId());
        AirSellFromRecommendation sellFromRecommendation = new BookFlights().sellFromRecommendation(travellerMasterInfo);

        amadeusLogger.debug("sellFromRecommendationReq " + new Date() + " SessionId: " + mSession.getSessionId() + " ---->" + new XStream().toXML(sellFromRecommendation));

        AirSellFromRecommendationReply sellFromRecommendationReply = mPortType.airSellFromRecommendation(sellFromRecommendation, mSession.getSession());

        amadeusLogger.debug("sellFromRecommendation Response " + new Date() + " SessionId: " + mSession.getSessionId() + " ---->" + new XStream().toXML(sellFromRecommendationReply));
        return   sellFromRecommendationReply;
    }

    public PNRReply addTravellerInfoToPNR(TravellerMasterInfo travellerMasterInfo){
        mSession.incrementSequenceNumber();
        logger.debug("amadeus addTravellerInfoToPNR called   at " + new Date() + "....................Session Id: " + mSession.getSessionId());
        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().getMultiElements(travellerMasterInfo);

        amadeusLogger.debug("pnrAddMultiElementsReq " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(pnrAddMultiElements));

        PNRReply pnrReply = mPortType.pnrAddMultiElements(pnrAddMultiElements, mSession.getSession());

        amadeusLogger.debug("pnrAddMultiElementsRes " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(pnrReply));
        return  pnrReply;
    }

    //pricing transaction
    public FarePricePNRWithBookingClassReply pricePNR(String carrrierCode, PNRReply pnrReply, boolean isSeamen,
                                                      boolean isDomesticFlight, FlightItinerary flightItinerary,
                                                      List<AirSegmentInformation> airSegmentList, boolean isSegmentWisePricing) {
        mSession.incrementSequenceNumber();
        logger.debug("amadeus pricePNR called at " + new Date() + "....................Session Id: " + mSession.getSessionId());
        FarePricePNRWithBookingClass pricePNRWithBookingClass = new PricePNR().getPNRPricingOption(carrrierCode, pnrReply, isSeamen, isDomesticFlight, flightItinerary, airSegmentList, isSegmentWisePricing);

        amadeusLogger.debug("pricePNRWithBookingClassReq " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(pricePNRWithBookingClass));

        FarePricePNRWithBookingClassReply pricePNRWithBookingClassReply = mPortType.farePricePNRWithBookingClass(pricePNRWithBookingClass, mSession.getSession());

        amadeusLogger.debug("pricePNRWithBookingClassRes " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(pricePNRWithBookingClassReply));
        return pricePNRWithBookingClassReply;
    }

    public TicketCreateTSTFromPricingReply createTST(int numberOfTST) {
        mSession.incrementSequenceNumber();
        logger.debug("amadeus createTST called at " + new Date() + "...................Session Id:. " + mSession.getSessionId());
        TicketCreateTSTFromPricing ticketCreateTSTFromPricing = new CreateTST().createTSTReq(numberOfTST);
        amadeusLogger.debug("createTSTFromPricingReplyReq " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(ticketCreateTSTFromPricing));

        TicketCreateTSTFromPricingReply createTSTFromPricingReply = mPortType.ticketCreateTSTFromPricing(ticketCreateTSTFromPricing, mSession.getSession());

        amadeusLogger.debug("createTSTFromPricingReplyRes " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(createTSTFromPricingReply));
        return createTSTFromPricingReply;
    }

    public PNRReply savePNR() {
        mSession.incrementSequenceNumber();
        logger.debug("amadeus savePNR called at " + new Date() + "....................Session Id: " + mSession.getSessionId());
        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().savePnr();
        amadeusLogger.debug("savePNRReq " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(pnrAddMultiElements));
        PNRReply pnrReply =  mPortType.pnrAddMultiElements(new PNRAddMultiElementsh().savePnr(), mSession.getSession());
        amadeusLogger.debug("savePNRRes " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(pnrReply));
        return pnrReply;
    }

    public PNRReply saveChildPNR(String optionCode) {
        mSession.incrementSequenceNumber();
        logger.debug("amadeus saveChildPNR called at " + new Date() + "....................Session Id: " + mSession.getSessionId());
        PNRAddMultiElements pnrAddMultiElements = new com.compassites.GDSWrapper.amadeus.PNRSplit().saveChildPnr(optionCode);
        amadeusLogger.debug("saveChildPNRReq " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(pnrAddMultiElements));
        PNRReply pnrReply =  mPortType.pnrAddMultiElements(new com.compassites.GDSWrapper.amadeus.PNRSplit().saveChildPnr(optionCode), mSession.getSession());
        amadeusLogger.debug("saveChildPNRRes " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(pnrReply));
        return pnrReply;
    }

    public PNRReply retrivePNR(String num){
    	//change here
        mSession.incrementSequenceNumber();
        logger.debug("amadeus retrievePNR called at " + new Date() + "....................Session Id: "+ mSession.getSessionId());
        PNRRetrieve pnrRetrieve = new PNRRetriev().retrieve(num);
        amadeusLogger.debug("pnrRetrieveReq " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(pnrRetrieve));
        PNRReply pnrReply = mPortType.pnrRetrieve(pnrRetrieve, mSession.getSession());

        amadeusLogger.debug("pnrRetrieveRes " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(pnrReply));
        return pnrReply;
    }

    public PNRReply ignoreAndRetrievePNR(){
        mSession.incrementSequenceNumber();
        logger.debug("amadeus ignoreAndRetrievePNR called at " + new Date() + "..................Session Id " + mSession.getSessionId());
        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().ignoreAndRetrievePNR();
        amadeusLogger.debug("ignoreAndRetrievePNR " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(pnrAddMultiElements));
        PNRReply pnrReply =  mPortType.pnrAddMultiElements(pnrAddMultiElements, mSession.getSession());
        amadeusLogger.debug("ignoreAndRetrievePNR " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(pnrReply));
        return pnrReply;

    }

    public PNRReply ignorePNRAddMultiElement(){
        mSession.incrementSequenceNumber();
        logger.debug("amadeus ignorePNRAddMultiElement called at " + new Date() + "..................Session Id " + mSession.getSessionId());
        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().ignorePNRAddMultiElement();

        amadeusLogger.debug("ignorePNRAddMultiElementReq " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(pnrAddMultiElements));

        PNRReply pnrReply =  mPortType.pnrAddMultiElements(pnrAddMultiElements, mSession.getSession());

        amadeusLogger.debug("ignorePNRAddMultiElement Res" + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(pnrReply));
        return pnrReply;

    }

    public PNRReply addSSRDetailsToPNR(TravellerMasterInfo travellerMasterInfo, List<String> segmentNumbers, Map<String,String> travellerMap){
        mSession.incrementSequenceNumber();
        logger.debug("amadeus addSSRDetailsToPNR called   at " + new Date() + "....................Session Id: " + mSession.getSessionId());
        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().addSSRDetails(travellerMasterInfo, segmentNumbers, travellerMap);

        amadeusLogger.debug("addSSRDetailsToPNR " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(pnrAddMultiElements));

        PNRReply pnrReply = mPortType.pnrAddMultiElements(pnrAddMultiElements, mSession.getSession());

        amadeusLogger.debug("addSSRDetailsToPNR " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(pnrReply));
        return  pnrReply;
    }

    public DocIssuanceIssueTicketReply issueTicket(boolean sendTSTDataForIssuance, List<String> tstReferenceList){
        mSession.incrementSequenceNumber();
        logger.debug("amadeus issueTicket called at " + new Date() + "....................Session Id: "+ mSession.getSessionId());
        DocIssuanceIssueTicket docIssuanceIssueTicket = new IssueTicket().issue(sendTSTDataForIssuance, tstReferenceList);
        amadeusLogger.debug("docIssuanceReq " + new Date()  + " SessionId: " + mSession.getSessionId() + " ---->" + new XStream().toXML(docIssuanceIssueTicket));
        DocIssuanceIssueTicketReply docIssuanceIssueTicketReply = mPortType.docIssuanceIssueTicket(docIssuanceIssueTicket, mSession.getSession());
        amadeusLogger.debug("docIssuanceRes " + new Date()  + " SessionId: " + mSession.getSessionId() + " ---->" + new XStream().toXML(docIssuanceIssueTicketReply));
        return docIssuanceIssueTicketReply;
    }
    
	public FareInformativePricingWithoutPNRReply getFareInfo(List<Journey> journeys, boolean seamen, int adultCount, int childCount, int infantCount, List<PAXFareDetails> paxFareDetailsList) {
		mSession.incrementSequenceNumber();
        logger.debug("amadeus getFareInfo called at " + new Date() + "....................Session Id: " + mSession.getSessionId());
		FareInformativePricingWithoutPNR farePricingWithoutPNR = new FareInformation().getFareInfo(journeys,seamen, adultCount, childCount, infantCount, paxFareDetailsList);
        amadeusLogger.debug("farePricingWithoutPNRReq " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(farePricingWithoutPNR));
        FareInformativePricingWithoutPNRReply fareInformativePricingPNRReply  = mPortType.fareInformativePricingWithoutPNR(farePricingWithoutPNR, mSession.getSession());
        amadeusLogger.debug("farePricingWithoutPNRRes " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(fareInformativePricingPNRReply));
        return  fareInformativePricingPNRReply;
	}

	public AirFlightInfoReply getFlightInfo(AirSegmentInformation airSegment) {
		mSession.incrementSequenceNumber();
        logger.debug("amadeus getFlightInfo  called at " + new Date() + "....................Session Id: " + mSession.getSessionId());
		AirFlightInfo airFlightInfo = new FlightInformation().getAirFlightInfo(airSegment);
        amadeusLogger.debug("flightInfoReq " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(airFlightInfo));
        AirFlightInfoReply airFlightInfoReply = mPortType.airFlightInfo(airFlightInfo, mSession.getSession());

        amadeusLogger.debug("flightInfoRes " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(airFlightInfo));
		return airFlightInfoReply;
	}

    public FareCheckRulesReply getFareRules(){
        mSession.incrementSequenceNumber();
        logger.debug("amadeus getFareRules called at " + new Date() + "....................Session Id: " + mSession.getSessionId());
        FareCheckRules fareCheckRules = new FareRules().createFareRules();
        amadeusLogger.debug("fareRulesReq " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(fareCheckRules));
        FareCheckRulesReply fareCheckRulesReply = mPortType.fareCheckRules(fareCheckRules, mSession.getSession());
        amadeusLogger.debug("fareRulesRes " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(fareCheckRulesReply));
        return fareCheckRulesReply;
    }

    public FareCheckRulesReply getFareRulesForFCType(String fcNumber){
        mSession.incrementSequenceNumber();
        logger.debug("amadeus getFareRulesForFCType called at " + new Date() + "....................Session Id: " + mSession.getSessionId());
        FareCheckRules fareCheckRules = new FareRules().getFareInfoForFCType(fcNumber);
        amadeusLogger.debug("getFareRulesForFCTypeReq " + new Date()+ " SessionId: " + mSession.getSessionId() + " ---->" + new XStream().toXML(fareCheckRules));
        FareCheckRulesReply fareCheckRulesReply = mPortType.fareCheckRules(fareCheckRules, mSession.getSession());
        amadeusLogger.debug("getFareRulesForFCTypeRes " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(fareCheckRulesReply));
        return fareCheckRulesReply;
    }



//    public FarePricePNRWithLowestFareReply getLowestFare(boolean isSeamen) {
        public FarePricePNRWithLowestFareReply getLowestFare(String carrrierCode, PNRReply pnrReply, boolean isSeamen,
                                                        boolean isDomesticFlight, FlightItinerary flightItinerary,
                                                        List<AirSegmentInformation> airSegmentList, boolean isSegmentWisePricing)
        {
    	mSession.incrementSequenceNumber();
        logger.debug("amadeus getLowestFare called at " + new Date() + "....................Session Id: " + mSession.getSessionId());
    	FarePricePNRWithLowestFare farePricePNRWithLowestFare = null;
        if(isSegmentWisePricing){
            farePricePNRWithLowestFare = new LowestPricePNR().getPNRPricingOption(carrrierCode,pnrReply, isSeamen,
                    isDomesticFlight, flightItinerary, airSegmentList, isSegmentWisePricing);
        }else {
            if(isSeamen) {
                farePricePNRWithLowestFare = new PricePNRLowestFare().getPricePNRWithLowestSeamenFare();
            } else {
                farePricePNRWithLowestFare = new PricePNRLowestFare().getPricePNRWithLowestNonSeamenFare();
            }
        }

        amadeusLogger.debug("FarePricePNRWithLowestFareReq " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(farePricePNRWithLowestFare));
    	FarePricePNRWithLowestFareReply farePricePNRWithLowestFareReply = mPortType.farePricePNRWithLowestFare(farePricePNRWithLowestFare, mSession.getSession());
        amadeusLogger.debug("FarePricePNRWithLowestFareReplyRes " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(farePricePNRWithLowestFareReply));
    	return farePricePNRWithLowestFareReply;
    }


    public PNRReply cancelPNR(String pnr, PNRReply gdsPNRReply){
        logger.debug("cancelPNR called  at " + new Date() + "................Session Id: "+ mSession.getSessionId());

        mSession.incrementSequenceNumber();

        PNRCancel pnrCancel = new PNRCancel();
        OptionalPNRActionsType pnrActionsType = new OptionalPNRActionsType();
        pnrActionsType.getOptionCode().add(BigInteger.valueOf(0));
        pnrCancel.setPnrActions(pnrActionsType);
        CancelPNRElementType cancelPNRElementType = new CancelPNRElementType();
        List<ElementIdentificationType> elementIdentificationTypeList = cancelPNRElementType.getElement();
        for(PNRReply.OriginDestinationDetails originDestination : gdsPNRReply.getOriginDestinationDetails()){
            for(PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestination.getItineraryInfo()) {
                ElementIdentificationType elementIdentificationType = new ElementIdentificationType();
                String segType = itineraryInfo.getElementManagementItinerary().getSegmentName();
                if(segType.equalsIgnoreCase("AIR")) {
                    BigInteger segmentRef = itineraryInfo.getElementManagementItinerary().getReference().getNumber();
                    String segQualifier = itineraryInfo.getElementManagementItinerary().getReference().getQualifier();
                    elementIdentificationType.setNumber(segmentRef);
                    elementIdentificationType.setIdentifier(segQualifier);
                    elementIdentificationTypeList.add(elementIdentificationType);
                }
            }
        }
        cancelPNRElementType.setEntryType(AmadeusConstants.CANCEL_PNR_ELEMENT_TYPE);
        pnrCancel.getCancelElements().add(cancelPNRElementType);


        amadeusLogger.debug("pnrCancelReq " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(pnrCancel));
        //PNRReply pnrReply = new PNRReply();
        PNRReply pnrReply = mPortType.pnrCancel(pnrCancel, mSession.getSession());

        amadeusLogger.debug("pnrCancelRes " + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(pnrReply));
        return pnrReply;

    }

    public QueueListReply queueListResponse(QueueList queueListReq){
        logger.debug("queueListResponse called at " + new Date() + "....................Session Id: " + mSession.getSessionId());
        amadeusLogger.debug("queueListReq" + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(queueListReq));
        QueueListReply queueListReply = mPortType.queueList(queueListReq,mSession.getSession());
        amadeusLogger.debug("queueListRes" + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(queueListReply));
        return queueListReply;
    }

    public TicketDisplayTSTReply ticketDisplayTST(){
        logger.debug("ticketDisplayTST called at " +  new Date() + "....................Session Id: " + mSession.getSessionId());

        mSession.incrementSequenceNumber();
        TicketDisplayTST ticketDisplayTST  = new CreateTST().createTicketDisplayTSTReq();
        amadeusLogger.debug("ticketDisplayTSTReq" + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(ticketDisplayTST));

        TicketDisplayTSTReply ticketDisplayTSTReply = mPortType.ticketDisplayTST(ticketDisplayTST, mSession.getSession());

        amadeusLogger.debug("ticketDisplayTSTRes" + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(ticketDisplayTSTReply));

        return ticketDisplayTSTReply;
    }

    public PNRReply splitPNR(com.amadeus.xml.pnrspl_11_3_1a.PNRSplit pnrSplit){
        logger.debug("splitPNR called at " +  new Date() + "....................Session Id: " + mSession.getSessionId());
        mSession.incrementSequenceNumber();
        amadeusLogger.debug("splitPNRReq" + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(pnrSplit));
        PNRReply pnrReply = mPortType.pnrSplit(pnrSplit,mSession.getSession());
        amadeusLogger.debug("splitPNRRes" + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(pnrReply));
        return pnrReply;
    }
}
