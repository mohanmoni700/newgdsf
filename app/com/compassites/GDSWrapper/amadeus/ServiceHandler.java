package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.AmadeusWebServices;
import com.amadeus.xml.AmadeusWebServicesPT;
import com.amadeus.xml.farqnq_07_1_1a.FareCheckRules;
import com.amadeus.xml.farqnr_07_1_1a.FareCheckRulesReply;
import com.amadeus.xml.flireq_07_1_1a.AirFlightInfo;
import com.amadeus.xml.flires_07_1_1a.AirFlightInfoReply;
import com.amadeus.xml.fmptbr_12_4_1a.FareMasterPricerTravelBoardSearchReply;
import com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation;
import com.amadeus.xml.itares_05_2_ia.AirSellFromRecommendationReply;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply;
import com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements;
import com.amadeus.xml.pnrret_10_1_1a.PNRRetrieve;
import com.amadeus.xml.pnrxcl_11_3_1a.CancelPNRElementType;
import com.amadeus.xml.pnrxcl_11_3_1a.OptionalPNRActionsType;
import com.amadeus.xml.pnrxcl_11_3_1a.PNRCancel;
import com.amadeus.xml.tautcq_04_1_1a.TicketCreateTSTFromPricing;
import com.amadeus.xml.tautcr_04_1_1a.TicketCreateTSTFromPricingReply;
import com.amadeus.xml.tipnrq_12_4_1a.FareInformativePricingWithoutPNR;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply;
import com.amadeus.xml.tpcbrq_07_3_1a.FarePricePNRWithBookingClass;
import com.amadeus.xml.tpcbrr_07_3_1a.FarePricePNRWithBookingClassReply;
import com.amadeus.xml.tplprq_12_4_1a.FarePricePNRWithLowestFare;
import com.amadeus.xml.tplprr_12_4_1a.FarePricePNRWithLowestFareReply;
import com.amadeus.xml.ttktiq_09_1_1a.DocIssuanceIssueTicket;
import com.amadeus.xml.ttktir_09_1_1a.DocIssuanceIssueTicketReply;
import com.amadeus.xml.vlsslr_06_1_1a.SecurityAuthenticateReply;
import com.amadeus.xml.vlssoq_04_1_1a.SecuritySignOut;
import com.amadeus.xml.vlssor_04_1_1a.SecuritySignOutReply;
import com.compassites.constants.AmadeusConstants;
import com.compassites.model.AirSegmentInformation;
import com.compassites.model.Journey;
import com.compassites.model.SearchParameters;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.thoughtworks.xstream.XStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;
import java.math.BigInteger;
import java.net.URL;
import java.util.*;

public class ServiceHandler {

    AmadeusWebServicesPT mPortType;

    SessionHandler mSession;

    static Logger amadeusLogger = LoggerFactory.getLogger("amadeus");

    static Logger logger = LoggerFactory.getLogger("gds");

    public ServiceHandler() throws Exception{
//        URL wsdlUrl=ServiceHandler.class.getResource("/wsdl/amadeus/1ASIWFLYFYH_PDT_20140429_052541.wsdl");
        URL wsdlUrl=ServiceHandler.class.getResource("/wsdl/amadeus/1ASIWFLYFYH_PDT_20141017_122132.wsdl");
        AmadeusWebServices service = new AmadeusWebServices(wsdlUrl);
        mPortType = service.getAmadeusWebServicesPort();

        //gzip compression headers
        HashMap httpHeaders = new HashMap();
        httpHeaders.put("Content-Encoding", Collections.singletonList("gzip"));
        httpHeaders.put("Accept-Encoding", Collections.singletonList("gzip"));
        Map reqContext = ((BindingProvider) mPortType).getRequestContext();
        reqContext.put(MessageContext.HTTP_REQUEST_HEADERS, httpHeaders);
        mSession = new SessionHandler();
    }

    public SessionReply logIn() {
        SecurityAuthenticateReply securityAuthenticate = mPortType.securityAuthenticate(new MessageFactory().buildAuthenticationRequest(), mSession.getSession());
        SessionReply sessionReply=new SessionReply();
        sessionReply.setSecurityAuthenticateReply(securityAuthenticate);
        sessionReply.setSession(mSession.getSession().value);
        return sessionReply;
    }

    public SecuritySignOutReply logOut() {
        SecuritySignOutReply signOutReply = null;
        mSession.incrementSequenceNumber();
        signOutReply = mPortType.securitySignOut(new SecuritySignOut(), mSession.getSession());
        mSession.resetSession();
        return signOutReply;
    }

    //search flights with 2 cities- faremastertravelboard service
    public FareMasterPricerTravelBoardSearchReply searchAirlines(SearchParameters searchParameters) {
        mSession.incrementSequenceNumber();
        amadeusLogger.debug("AmadeusFlightSearch called at : " + new Date());
        FareMasterPricerTravelBoardSearchReply SearchReply = mPortType.fareMasterPricerTravelBoardSearch(new SearchFlights().createSearchQuery(searchParameters), mSession.getSession());
        amadeusLogger.debug("AmadeusFlightSearch response returned  at : " + new Date());
        return  SearchReply;
    }
    
    public AirSellFromRecommendationReply checkFlightAvailability(TravellerMasterInfo travellerMasterInfo) {
        mSession.incrementSequenceNumber();
        AirSellFromRecommendation sellFromRecommendation = new BookFlights().sellFromRecommendation(travellerMasterInfo);

//        XMLFileUtility.createXMLFile(sellFromRecommendation, "sellFromRecommendationReq.xml");

        amadeusLogger.debug("sellFromRecommendationReq " + new Date() + " ---->" + new XStream().toXML(sellFromRecommendation));

        AirSellFromRecommendationReply sellFromRecommendationReply = mPortType.airSellFromRecommendation(sellFromRecommendation, mSession.getSession());


        amadeusLogger.debug("sellFromRecommendation Response " + new Date() + " ---->" + new XStream().toXML(sellFromRecommendationReply));

//        XMLFileUtility.createXMLFile(sellFromRecommendationReply, "sellFromRecommendationRes.xml");

        return   sellFromRecommendationReply;
    }

    public PNRReply addTravellerInfoToPNR(TravellerMasterInfo travellerMasterInfo){
        mSession.incrementSequenceNumber();

        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().getMultiElements(travellerMasterInfo);

//        XMLFileUtility.createXMLFile(pnrAddMultiElements, "pnrAddMultiElementsReq.xml");
        amadeusLogger.debug("pnrAddMultiElementsReq " + new Date() + " ---->" + new XStream().toXML(pnrAddMultiElements));

        PNRReply pnrReply = mPortType.pnrAddMultiElements(pnrAddMultiElements, mSession.getSession());

//        XMLFileUtility.createXMLFile(pnrReply, "pnrAddMultiElementsRes.xml");
        amadeusLogger.debug("pnrAddMultiElementsRes " + new Date() + " ---->" + new XStream().toXML(pnrReply));
        return  pnrReply;
    }

    //pricing transaction
    public FarePricePNRWithBookingClassReply pricePNR(String carrrierCode, PNRReply pnrReply) {
        mSession.incrementSequenceNumber();
        FarePricePNRWithBookingClass pricePNRWithBookingClass = new PricePNR().getPNRPricingOption(carrrierCode, pnrReply);

//        XMLFileUtility.createXMLFile(pricePNRWithBookingClass, "pricePNRWithBookingClassReq.xml");
        amadeusLogger.debug("pricePNRWithBookingClassReq " + new Date() + " ---->" + new XStream().toXML(pricePNRWithBookingClass));

        FarePricePNRWithBookingClassReply pricePNRWithBookingClassReply = mPortType.farePricePNRWithBookingClass(pricePNRWithBookingClass, mSession.getSession());

//        XMLFileUtility.createXMLFile(pricePNRWithBookingClassReply, "pricePNRWithBookingClassRes.xml");
        amadeusLogger.debug("pricePNRWithBookingClassRes " + new Date() + " ---->" + new XStream().toXML(pricePNRWithBookingClassReply));
        return pricePNRWithBookingClassReply;
    }

    public TicketCreateTSTFromPricingReply createTST(int numberOfTST) {
        mSession.incrementSequenceNumber();
        TicketCreateTSTFromPricing ticketCreateTSTFromPricing = new CreateTST().createTSTReq(numberOfTST);
//        XMLFileUtility.createXMLFile(ticketCreateTSTFromPricing, "createTSTFromPricingReplyReq.xml");
        amadeusLogger.debug("createTSTFromPricingReplyReq " + new Date() + " ---->" + new XStream().toXML(ticketCreateTSTFromPricing));

        TicketCreateTSTFromPricingReply createTSTFromPricingReply = mPortType.ticketCreateTSTFromPricing(ticketCreateTSTFromPricing, mSession.getSession());

//        XMLFileUtility.createXMLFile(createTSTFromPricingReply, "createTSTFromPricingReplyRes.xml");
        amadeusLogger.debug("createTSTFromPricingReplyRes " + new Date() + " ---->" + new XStream().toXML(createTSTFromPricingReply));
        return createTSTFromPricingReply;
    }

    public PNRReply savePNR() {
        mSession.incrementSequenceNumber();
        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().savePnr();
//        XMLFileUtility.createXMLFile(pnrAddMultiElements, "savePNRReq.xml");
        amadeusLogger.debug("savePNRReq " + new Date() + " ---->" + new XStream().toXML(pnrAddMultiElements));
        PNRReply pnrReply =  mPortType.pnrAddMultiElements(new PNRAddMultiElementsh().savePnr(), mSession.getSession());
        amadeusLogger.debug("savePNRRes " + new Date() + " ---->" + new XStream().toXML(pnrReply));
//        XMLFileUtility.createXMLFile(pnrReply, "savePNRRes.xml");
        return pnrReply;
    }
    
    public PNRReply retrivePNR(String num){
        mSession.incrementSequenceNumber();
        PNRRetrieve pnrRetrieve = new PNRRetriev().retrieve(num);
//        XMLFileUtility.createXMLFile(pnrRetrieve, "pnrRetrieveReq.xml");
        amadeusLogger.debug("pnrRetrieveReq " + new Date() + " ---->" + new XStream().toXML(pnrRetrieve));
        PNRReply pnrReply = mPortType.pnrRetrieve(pnrRetrieve,mSession.getSession());
//        XMLFileUtility.createXMLFile(pnrReply, "pnrRetrieveRes.xml");
        amadeusLogger.debug("pnrRetrieveRes " + new Date() + " ---->" + new XStream().toXML(pnrReply));
        return pnrReply;
    }
    
    public DocIssuanceIssueTicketReply issueTicket(){
        mSession.incrementSequenceNumber();
        DocIssuanceIssueTicket docIssuanceIssueTicket = new IssueTicket().issue();
//        XMLFileUtility.createXMLFile(docIssuanceIssueTicket, "docIssuanceReq.xml");
        amadeusLogger.debug("docIssuanceReq " + new Date() + " ---->" + new XStream().toXML(docIssuanceIssueTicket));
        DocIssuanceIssueTicketReply docIssuanceIssueTicketReply = mPortType.docIssuanceIssueTicket(docIssuanceIssueTicket, mSession.getSession());
//        XMLFileUtility.createXMLFile(docIssuanceIssueTicketReply, "docIssuanceRes.xml");
        amadeusLogger.debug("docIssuanceRes " + new Date() + " ---->" + new XStream().toXML(docIssuanceIssueTicketReply));
        return docIssuanceIssueTicketReply;
    }
    
	public FareInformativePricingWithoutPNRReply getFareInfo(List<Journey> journeys, int adultCount, int childCount, int infantCount) {
		mSession.incrementSequenceNumber();
		FareInformativePricingWithoutPNR farePricingWithoutPNR = new FareInformation().getFareInfo(journeys, adultCount, childCount, infantCount);
//        XMLFileUtility.createXMLFile(farePricingWithoutPNR, "farePricingWithoutPNRReq.xml");
        amadeusLogger.debug("farePricingWithoutPNRReq " + new Date() + " ---->" + new XStream().toXML(farePricingWithoutPNR));
        FareInformativePricingWithoutPNRReply fareInformativePricingPNRReply  = mPortType.fareInformativePricingWithoutPNR(farePricingWithoutPNR, mSession.getSession());
        amadeusLogger.debug("farePricingWithoutPNRRes " + new Date() + " ---->" + new XStream().toXML(fareInformativePricingPNRReply));
//        XMLFileUtility.createXMLFile(fareInformativePricingPNRReply, "farePricingWithoutPNRRes.xml");
        return  fareInformativePricingPNRReply;
	}
	
	public AirFlightInfoReply getFlightInfo(AirSegmentInformation airSegment) {
		mSession.incrementSequenceNumber();
		AirFlightInfo airFlightInfo = new FlightInformation().getAirFlightInfo(airSegment);
		return mPortType.airFlightInfo(airFlightInfo, mSession.getSession());
	}

    public FareCheckRulesReply getFareRules(){
        mSession.incrementSequenceNumber();
        FareCheckRules fareCheckRules = new FareRules().createFareRules();
//        XMLFileUtility.createXMLFile(fareCheckRules, "fareRulesReq.xml");
        amadeusLogger.debug("fareRulesReq " + new Date() + " ---->" + new XStream().toXML(fareCheckRules));
        FareCheckRulesReply fareCheckRulesReply = mPortType.fareCheckRules(fareCheckRules, mSession.getSession());
//        XMLFileUtility.createXMLFile(fareCheckRulesReply, "fareRulesRes.xml");
        amadeusLogger.debug("fareRulesRes " + new Date() + " ---->" + new XStream().toXML(fareCheckRulesReply));
        return fareCheckRulesReply;
    }
    
    public FarePricePNRWithLowestFareReply getLowestFare(boolean isSeamen) {
    	mSession.incrementSequenceNumber();
    	FarePricePNRWithLowestFare farePricePNRWithLowestFare = null;
    	if(isSeamen) {
    		farePricePNRWithLowestFare = new PricePNRLowestFare().getPricePNRWithLowestSeamenFare();
    	} else {
    		farePricePNRWithLowestFare = new PricePNRLowestFare().getPricePNRWithLowestNonSeamenFare();
    	}
//    	XMLFileUtility.createXMLFile(farePricePNRWithLowestFare, "FarePricePNRWithLowestFareReq.xml");
        amadeusLogger.debug("FarePricePNRWithLowestFareReq " + new Date() + " ---->" + new XStream().toXML(farePricePNRWithLowestFare));
    	FarePricePNRWithLowestFareReply farePricePNRWithLowestFareReply = mPortType.farePricePNRWithLowestFare(farePricePNRWithLowestFare, mSession.getSession());
//    	XMLFileUtility.createXMLFile(farePricePNRWithLowestFareReply, "FarePricePNRWithLowestFareReplyRes.xml");
        amadeusLogger.debug("FarePricePNRWithLowestFareReplyRes " + new Date() + " ---->" + new XStream().toXML(farePricePNRWithLowestFareReply));
    	return farePricePNRWithLowestFareReply;
    }


    public com.amadeus.xml.pnracc_11_3_1a.PNRReply cancelPNR(String pnr){
        logger.debug("cancelPNR called ................");

        mSession.incrementSequenceNumber();
        PNRCancel pnrCancel = new PNRCancel();
        OptionalPNRActionsType pnrActionsType = new OptionalPNRActionsType();
        pnrActionsType.getOptionCode().add(BigInteger.valueOf(0));
        pnrCancel.setPnrActions(pnrActionsType);
        CancelPNRElementType cancelPNRElementType = new CancelPNRElementType();
        cancelPNRElementType.setEntryType(AmadeusConstants.CANCEL_PNR_ITINERARY_TYPE);
        pnrCancel.getCancelElements().add(cancelPNRElementType);

        com.amadeus.xml.pnracc_11_3_1a.PNRReply pnrReply = mPortType.pnrCancel(pnrCancel, mSession.getSession());

        return pnrReply;

    }
}
