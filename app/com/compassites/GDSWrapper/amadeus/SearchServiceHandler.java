package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.AmadeusWebServices;
import com.amadeus.xml.AmadeusWebServicesPT;
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

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;
import java.math.BigInteger;
import java.net.URL;
import java.util.*;

@Service
public class SearchServiceHandler {

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
            url = SearchServiceHandler.class.getResource("/wsdl/amadeus/1ASIWFLYFYH_PDT_TicketGTP_3.1_2.0.wsdl");
        }catch (Exception e){
            logger.debug("Error in loading Amadeus URL : ", e);
        }
        wsdlUrl = url;
    }

    public SearchServiceHandler() throws Exception{
//        URL wsdlUrl=ServiceHandler.class.getResource("/wsdl/amadeus/1ASIWFLYFYH_PDT_20140429_052541.wsdl");
//        URL wsdlUrl = ServiceHandler.class.getResource("/wsdl/amadeus/amadeus.wsdl");
        AmadeusWebServices service = new AmadeusWebServices(wsdlUrl);
        mPortType = service.getAmadeusWebServicesPort();
        WSSecurityHeaderSOAPHandler handler = new WSSecurityHeaderSOAPHandler();
        List<Handler> handlerChain = new ArrayList<Handler>();
        handlerChain.add(handler);
        //gzip compression headers
        HashMap httpHeaders = new HashMap();
        httpHeaders.put("Content-Encoding", Collections.singletonList("gzip"));
        httpHeaders.put("Accept-Encoding", Collections.singletonList("gzip"));
        Map reqContext = ((BindingProvider) mPortType).getRequestContext();
        reqContext.put(MessageContext.HTTP_REQUEST_HEADERS, httpHeaders);
        reqContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPoint);
        ((BindingProvider) mPortType).getBinding().setHandlerChain(handlerChain);
        mSession = new SessionHandler();
    }

    public void setSession(Session session){
        mSession = new SessionHandler(new Holder<>(session));

    }

    public Session getSession(){
        return mSession.getSession().value;
    }

//    public SessionReply logIn() {
//        logger.debug("amadeus login called ....................");
//        SecurityAuthenticate securityAuthenticateReq = MessageFactory.getInstance().getAuthenticationRequest();
//        amadeusLogger.debug("securityAuthenticateReq " + new Date() + " ---->" + new XStream().toXML(securityAuthenticateReq));
//        SecurityAuthenticateReply securityAuthenticate = mPortType.securityAuthenticate(securityAuthenticateReq, mSession.getSession());
//        amadeusLogger.debug("securityAuthenticateRes " + new Date() + " ---->" + new XStream().toXML(securityAuthenticate));
//
//        SessionReply sessionReply=new SessionReply();
//        sessionReply.setSecurityAuthenticateReply(securityAuthenticate);
//        sessionReply.setSession(mSession.getSession().value);
//
//        return sessionReply;
//    }

//    public SessionHandler logIn(SessionHandler mSession) {
//        logger.debug("amadeus login called....................");
//        SecurityAuthenticate securityAuthenticateReq = MessageFactory.getInstance().getAuthenticationRequest();
//        logger.debug("amadeus login called at : " + new Date() + " " + mSession.getSessionId());
//        amadeusLogger.debug("securityAuthenticateReq " + new Date() + " ---->" + new XStream().toXML(securityAuthenticateReq));
//        SecurityAuthenticateReply securityAuthenticate = mPortType.securityAuthenticate(securityAuthenticateReq, mSession.getSession());
//
//        amadeusLogger.debug("securityAuthenticateRes " + new Date() + " ---->" + new XStream().toXML(securityAuthenticate));
//        return mSession;
//    }

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
//    public FareMasterPricerTravelBoardSearchReply searchAirlines(SearchParameters searchParameters, SessionHandler mSession) {
////        mSession.incrementSequenceNumber();
//        logger.debug("AmadeusFlightSearch called at : " + new Date() + " " + mSession.getSessionId());
////        FareMasterPricerTravelBoardSearch fareMasterPricerTravelBoardSearch = new SearchFlights().createSearchQuery(searchParameters);
//        amadeusLogger.debug("AmadeusSearchReq " + new Date() + " SessionId: " + mSession.getSessionId() + " ---->" + new XStream().toXML(fareMasterPricerTravelBoardSearch));
//        FareMasterPricerTravelBoardSearchReply SearchReply = mPortType.fareMasterPricerTravelBoardSearch(fareMasterPricerTravelBoardSearch, null);
//        logger.debug("AmadeusFlightSearch response returned  at : " + new Date());
//        return  SearchReply;
//    }


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


    public QueueListReply queueListResponse(QueueList queueListReq){
        logger.debug("queueListResponse called at " + new Date() + "....................Session Id: " + mSession.getSessionId());
        amadeusLogger.debug("queueListReq" + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(queueListReq));
        QueueListReply queueListReply = mPortType.queueList(queueListReq,mSession.getSession());
        amadeusLogger.debug("queueListRes" + new Date() + " SessionId: " + mSession.getSessionId()+ " ---->" + new XStream().toXML(queueListReply));
        return queueListReply;
    }

}
