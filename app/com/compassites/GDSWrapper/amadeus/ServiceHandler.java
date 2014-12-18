package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.AmadeusWebServices;
import com.amadeus.xml.AmadeusWebServicesPT;
import com.amadeus.xml.farqnq_07_1_1a.FareCheckRules;
import com.amadeus.xml.farqnr_07_1_1a.FareCheckRulesReply;
import com.amadeus.xml.fmptbr_12_4_1a.FareMasterPricerTravelBoardSearchReply;
import com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation;
import com.amadeus.xml.itares_05_2_ia.AirSellFromRecommendationReply;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply;
import com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements;
import com.amadeus.xml.pnrret_10_1_1a.PNRRetrieve;
import com.amadeus.xml.tautcq_04_1_1a.TicketCreateTSTFromPricing;
import com.amadeus.xml.tautcr_04_1_1a.TicketCreateTSTFromPricingReply;
import com.amadeus.xml.tipnrq_12_4_1a.FareInformativePricingWithoutPNR;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply;
import com.amadeus.xml.tpcbrq_07_3_1a.FarePricePNRWithBookingClass;
import com.amadeus.xml.tpcbrr_07_3_1a.FarePricePNRWithBookingClassReply;
import com.amadeus.xml.ttktiq_09_1_1a.DocIssuanceIssueTicket;
import com.amadeus.xml.ttktir_09_1_1a.DocIssuanceIssueTicketReply;
import com.amadeus.xml.vlsslr_06_1_1a.SecurityAuthenticateReply;
import com.amadeus.xml.vlssoq_04_1_1a.SecuritySignOut;
import com.amadeus.xml.vlssor_04_1_1a.SecuritySignOutReply;
import com.compassites.model.FlightItinerary;
import com.compassites.model.SearchParameters;
import com.compassites.model.traveller.TravellerMasterInfo;
import utils.XMLFileUtility;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ServiceHandler {

    AmadeusWebServicesPT mPortType;
    SessionHandler mSession;

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
        return mPortType.fareMasterPricerTravelBoardSearch(new SearchFlights().createSearchQuery(searchParameters), mSession.getSession());
    }
    
    public AirSellFromRecommendationReply checkFlightAvailability(TravellerMasterInfo travellerMasterInfo) {
        mSession.incrementSequenceNumber();
        AirSellFromRecommendation sellFromRecommendation = new BookFlights().sellFromRecommendation(travellerMasterInfo);

        XMLFileUtility.createXMLFile(sellFromRecommendation, "sellFromRecommendationReq.xml");

        AirSellFromRecommendationReply sellFromRecommendationReply = mPortType.airSellFromRecommendation(sellFromRecommendation, mSession.getSession());

        XMLFileUtility.createXMLFile(sellFromRecommendationReply, "sellFromRecommendationRes.xml");

        return   sellFromRecommendationReply;
    }

    public PNRReply addTravellerInfoToPNR(TravellerMasterInfo travellerMasterInfo){
        mSession.incrementSequenceNumber();

        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().getMultiElements(travellerMasterInfo);

        XMLFileUtility.createXMLFile(pnrAddMultiElements, "pnrAddMultiElementsReq.xml");
        PNRReply pnrReply = mPortType.pnrAddMultiElements(pnrAddMultiElements, mSession.getSession());

        XMLFileUtility.createXMLFile(pnrReply, "pnrAddMultiElementsRes.xml");
        return  pnrReply;
    }

    //pricing transaction
    public FarePricePNRWithBookingClassReply pricePNR(String carrrierCode, PNRReply pnrReply) {
        mSession.incrementSequenceNumber();
        FarePricePNRWithBookingClass pricePNRWithBookingClass = new PricePNR().getPNRPricingOption(carrrierCode, pnrReply);

        XMLFileUtility.createXMLFile(pricePNRWithBookingClass, "pricePNRWithBookingClassReq.xml");

        FarePricePNRWithBookingClassReply pricePNRWithBookingClassReply = mPortType.farePricePNRWithBookingClass(pricePNRWithBookingClass, mSession.getSession());

        XMLFileUtility.createXMLFile(pricePNRWithBookingClassReply, "pricePNRWithBookingClassRes.xml");

        return pricePNRWithBookingClassReply;
    }

    public TicketCreateTSTFromPricingReply createTST() {
        mSession.incrementSequenceNumber();
        TicketCreateTSTFromPricing ticketCreateTSTFromPricing = new CreateTST().createTSTReq();
        XMLFileUtility.createXMLFile(ticketCreateTSTFromPricing, "createTSTFromPricingReplyReq.xml");
        TicketCreateTSTFromPricingReply createTSTFromPricingReply = mPortType.ticketCreateTSTFromPricing(ticketCreateTSTFromPricing, mSession.getSession());
        XMLFileUtility.createXMLFile(createTSTFromPricingReply, "createTSTFromPricingReplyRes.xml");
        return createTSTFromPricingReply;
    }

    public PNRReply savePNR() {
        mSession.incrementSequenceNumber();
        return mPortType.pnrAddMultiElements(new PNRAddMultiElementsh().savePnr(), mSession.getSession());
    }
    
    public PNRReply retrivePNR(String num){
        mSession.incrementSequenceNumber();
        PNRRetrieve pnrRetrieve = new PNRRetriev().retrieve(num);
        XMLFileUtility.createXMLFile(pnrRetrieve, "pnrRetrieveReq.xml");
        PNRReply pnrReply = mPortType.pnrRetrieve(pnrRetrieve,mSession.getSession());
        XMLFileUtility.createXMLFile(pnrReply, "pnrRetrieveRes.xml");
        return pnrReply;
    }
    
    public DocIssuanceIssueTicketReply issueTicket(){
        mSession.incrementSequenceNumber();
        DocIssuanceIssueTicket docIssuanceIssueTicket = new IssueTicket().issue();
        XMLFileUtility.createXMLFile(docIssuanceIssueTicket, "docIssuanceReq.xml");
        DocIssuanceIssueTicketReply docIssuanceIssueTicketReply = mPortType.docIssuanceIssueTicket(docIssuanceIssueTicket, mSession.getSession());
        XMLFileUtility.createXMLFile(docIssuanceIssueTicketReply, "docIssuanceRes.xml");
        return docIssuanceIssueTicketReply;
    }
    
	public FareInformativePricingWithoutPNRReply getFareInfo(FlightItinerary fligtItinerary, int adultCount, int childCount, int infantCount) {
		mSession.incrementSequenceNumber();
		FareInformativePricingWithoutPNR farePricingWithoutPNR = new FareInformation().getFareInfo(fligtItinerary, adultCount, childCount, infantCount);
        XMLFileUtility.createXMLFile(farePricingWithoutPNR, "farePricingWithoutPNRReq.xml");
		FareInformativePricingWithoutPNRReply fareInformativePricingPNRReply  = mPortType.fareInformativePricingWithoutPNR(farePricingWithoutPNR, mSession.getSession());
        XMLFileUtility.createXMLFile(fareInformativePricingPNRReply, "farePricingWithoutPNRRes.xml");
        return  fareInformativePricingPNRReply;
	}

    public FareCheckRulesReply getFareRules(){
        mSession.incrementSequenceNumber();
        FareCheckRules fareCheckRules = new FareRules().createFareRules();
        XMLFileUtility.createXMLFile(fareCheckRules, "fareRulesReq.xml");
        FareCheckRulesReply fareCheckRulesReply = mPortType.fareCheckRules(fareCheckRules, mSession.getSession());
        XMLFileUtility.createXMLFile(fareCheckRulesReply, "fareRulesRes.xml");
        return fareCheckRulesReply;
    }
}
