package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.AmadeusWebServices;
import com.amadeus.xml.AmadeusWebServicesPT;
import com.amadeus.xml.fmptbr_12_4_1a.FareMasterPricerTravelBoardSearchReply;
import com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation;
import com.amadeus.xml.itares_05_2_ia.AirSellFromRecommendationReply;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply;
import com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements;
import com.amadeus.xml.pnrret_10_1_1a.PNRRetrieve;
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
import utils.JSONFileUtility;

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

        JSONFileUtility.createJsonFile(sellFromRecommendation,"sellFromRecommendationReq.json");

        AirSellFromRecommendationReply sellFromRecommendationReply = mPortType.airSellFromRecommendation(sellFromRecommendation, mSession.getSession());

        JSONFileUtility.createJsonFile(sellFromRecommendationReply,"sellFromRecommendationRes.json");

        return   sellFromRecommendationReply;
    }

    public PNRReply addTravellerInfoToPNR(TravellerMasterInfo travellerMasterInfo){
        mSession.incrementSequenceNumber();

        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().getMultiElements(travellerMasterInfo);


        JSONFileUtility.createJsonFile(pnrAddMultiElements,"pnrAddMultiElementsReq.json");
        PNRReply pnrReply = mPortType.pnrAddMultiElements(pnrAddMultiElements, mSession.getSession());


        JSONFileUtility.createJsonFile(pnrReply,"pnrAddMultiElementsRes.json");
        return  pnrReply;
    }

    //pricing transaction
    public FarePricePNRWithBookingClassReply pricePNR(TravellerMasterInfo travellerMasterInfo, PNRReply pnrReply) {
        mSession.incrementSequenceNumber();
        FarePricePNRWithBookingClass pricePNRWithBookingClass = new PricePNR().getPNRPricingOption(travellerMasterInfo, pnrReply);

        JSONFileUtility.createJsonFile(pricePNRWithBookingClass,"pricePNRWithBookingClassReq.json");

        FarePricePNRWithBookingClassReply pricePNRWithBookingClassReply = mPortType.farePricePNRWithBookingClass(pricePNRWithBookingClass, mSession.getSession());

        JSONFileUtility.createJsonFile(pricePNRWithBookingClassReply,"pricePNRWithBookingClassRes.json");

        return pricePNRWithBookingClassReply;
    }

    public TicketCreateTSTFromPricingReply createTST() {
        mSession.incrementSequenceNumber();
        return mPortType.ticketCreateTSTFromPricing(new CreateTST().createTSTReq(), mSession.getSession());
    }

    public PNRReply savePNR() {
        mSession.incrementSequenceNumber();
        return mPortType.pnrAddMultiElements(new PNRAddMultiElementsh().savePnr(), mSession.getSession());
    }
    
    public PNRReply retrivePNR(String num){
        mSession.incrementSequenceNumber();
        PNRRetrieve pnrRetrieve = new PNRRetriev().retrieve(num);
        JSONFileUtility.createJsonFile(pnrRetrieve,"pnrRetrieveReq.json");
        PNRReply pnrReply = mPortType.pnrRetrieve(pnrRetrieve,mSession.getSession());
        JSONFileUtility.createJsonFile(pnrReply,"pnrRetrieveRes.json");
        return pnrReply;

    }
    
    public DocIssuanceIssueTicketReply issueTicket(){
        mSession.incrementSequenceNumber();
        DocIssuanceIssueTicket docIssuanceIssueTicket = new IssueTicket().issue();
        JSONFileUtility.createJsonFile(docIssuanceIssueTicket,"docIssuanceReq.json");
        DocIssuanceIssueTicketReply docIssuanceIssueTicketReply = mPortType.docIssuanceIssueTicket(docIssuanceIssueTicket, mSession.getSession());
        JSONFileUtility.createJsonFile(docIssuanceIssueTicketReply,"docIssuanceRes.json");
        return docIssuanceIssueTicketReply;
    }
    
    public FareInformativePricingWithoutPNRReply fareInfo(FlightItinerary fligtItinerary, SearchParameters searchParams) {
    	mSession.incrementSequenceNumber();
		// FareInformativePricingWithoutPNR farePricingWithoutPNR = new FareInformation().getFareInfo(fligtItinerary, searchParams);
		
		//JSONFileUtility.createJsonFile(farePricingWithoutPNR, "farePricingWithoutPNRReq.json");
//		FareInformativePricingWithoutPNR fareInformativePricingWithoutPNR = new FareInformativePricingWithoutPNR();
		FareInformativePricingWithoutPNRReply reply = null;
//		FareInformativePricingWithoutPNRReply reply = mPortType.fareInformativePricingWithoutPNR(fareInformativePricingWithoutPNR, mSession.getSession());
		return reply;
    }
}
