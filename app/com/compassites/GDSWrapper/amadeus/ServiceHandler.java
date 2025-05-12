package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.AmadeusWebServices;
import com.amadeus.xml.AmadeusWebServicesPT;
import com.amadeus.xml.farqnq_07_1_1a.FareCheckRules;
import com.amadeus.xml.farqnr_07_1_1a.FareCheckRulesReply;
import com.amadeus.xml.fatceq_13_1_1a.TicketCheckEligibility;
import com.amadeus.xml.fatcer_13_1_1a.TicketCheckEligibilityReply;
import com.amadeus.xml.fatcer_13_1_1a.TravelFlightInformationType;
import com.amadeus.xml.flireq_07_1_1a.AirFlightInfo;
import com.amadeus.xml.flires_07_1_1a.AirFlightInfoReply;
import com.amadeus.xml.fmptbq_14_2_1a.FareMasterPricerTravelBoardSearch;
import com.amadeus.xml.fmptbr_14_2_1a.FareMasterPricerTravelBoardSearchReply;
import com.amadeus.xml.fmtctq_18_2_1a.TicketATCShopperMasterPricerTravelBoardSearch;
import com.amadeus.xml.fmtctr_18_2_1a.TicketATCShopperMasterPricerTravelBoardSearchReply;
import com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation;
import com.amadeus.xml.itares_05_2_ia.AirSellFromRecommendationReply;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.pnradd_11_3_1a.PNRAddMultiElements;
import com.amadeus.xml.pnrret_11_3_1a.PNRRetrieve;
import com.amadeus.xml.pnrxcl_11_3_1a.*;
import com.amadeus.xml.qdqlrq_11_1_1a.QueueList;
import com.amadeus.xml.qdqlrr_11_1_1a.QueueListReply;
import com.amadeus.xml.tarcpq_13_2_1a.TicketReissueConfirmedPricing;
import com.amadeus.xml.tarcpr_13_2_1a.TicketReissueConfirmedPricingReply;
import com.amadeus.xml.taripq_19_1_1a.TicketRepricePNRWithBookingClass;
import com.amadeus.xml.taripr_19_1_1a.TicketRepricePNRWithBookingClassReply;
import com.amadeus.xml.tatreq_20_1_1a.TicketProcessEDoc;
import com.amadeus.xml.tatres_20_1_1a.TicketProcessEDocReply;
import com.amadeus.xml.tautcq_04_1_1a.TicketCreateTSTFromPricing;
import com.amadeus.xml.tautcr_04_1_1a.TicketCreateTSTFromPricingReply;
import com.amadeus.xml.tipnrq_12_4_1a.FareInformativePricingWithoutPNR;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply;
import com.amadeus.xml.tmrxrq_18_1_1a.MiniRuleGetFromRec;
import com.amadeus.xml.tmrxrr_18_1_1a.MiniRuleGetFromRecReply;
import com.amadeus.xml.tpcbrq_12_4_1a.FarePricePNRWithBookingClass;
import com.amadeus.xml.tpcbrr_12_4_1a.FarePricePNRWithBookingClassReply;
import com.amadeus.xml.tpicgq_17_1_1a.ServiceIntegratedCatalogue;
import com.amadeus.xml.tpicgr_17_1_1a.ServiceIntegratedCatalogueReply;
import com.amadeus.xml.tplprq_12_4_1a.FarePricePNRWithLowestFare;
import com.amadeus.xml.tplprr_12_4_1a.FarePricePNRWithLowestFareReply;
import com.amadeus.xml.tpscgq_17_1_1a.ServiceStandaloneCatalogue;
import com.amadeus.xml.tpscgr_17_1_1a.ServiceStandaloneCatalogueReply;
import com.amadeus.xml.trcanq_14_1_1a.*;
import com.amadeus.xml.trcanr_14_1_1a.TicketCancelDocumentReply;
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
import dto.OpenTicketDTO;
import dto.reissue.ReIssueConfirmationRequest;
import dto.reissue.ReIssueSearchRequest;
import models.AmadeusSessionWrapper;
import models.AncillaryServiceRequest;
import models.FlightSearchOffice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import play.Play;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.handler.MessageContext;
import java.math.BigInteger;
import java.net.URL;
import java.util.*;

@Service
public class ServiceHandler {

    AmadeusWebServicesPT mPortType;
    //TicketGTPPT ticketGTPPT;
    //SessionHandler mSession;

    public static URL wsdlUrl;

    static Logger amadeusLogger = LoggerFactory.getLogger("amadeus");

    static Logger logger = LoggerFactory.getLogger("gds");
    static Logger loggerTemp = LoggerFactory.getLogger("gds_search");

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
        //mSession = new SessionHandler();
    }

//    public SessionHandler logIn(SessionHandler mSession) {
//        logger.debug("amadeus login called....................");
//        SecurityAuthenticate securityAuthenticateReq = MessageFactory.getInstance().getAuthenticationRequest();
//        logger.debug("amadeus login called at : " + new Date() + " " + mSession.getSessionId());
//        amadeusLogger.debug("securityAuthenticateReq " + new Date() + " ---->" + new XStream().toXML(securityAuthenticateReq));
//        SecurityAuthenticateReply securityAuthenticate = mPortType.securityAuthenticate(securityAuthenticateReq, mSession.getSession());
//        amadeusLogger.debug("securityAuthenticateRes " + new Date() + " ---->" + new XStream().toXML(securityAuthenticate));
//        return mSession;
//    }

    //todo
    public AmadeusSessionWrapper logIn() {
        logger.debug("amadeus login called....................");
        AmadeusSessionWrapper amadeusSessionWrapper = new AmadeusSessionWrapper();
        Holder<Session> sessionHolder = amadeusSessionWrapper.resetSession();
        FlightSearchOffice office = new FlightSearchOffice();
        SecurityAuthenticate securityAuthenticateReq = MessageFactory.getInstance().getAuthenticationRequest(office.getOfficeId());
        logger.debug("amadeus login called at : " + new Date() + " " + amadeusSessionWrapper.getSessionId());
        amadeusLogger.debug("securityAuthenticateReq " + new Date() + " ---->" + new XStream().toXML(securityAuthenticateReq));
        SecurityAuthenticateReply securityAuthenticate = mPortType.securityAuthenticate(securityAuthenticateReq, sessionHolder);

        amadeusLogger.debug("securityAuthenticateRes " + new Date() + " ---->" + new XStream().toXML(securityAuthenticate));
        amadeusSessionWrapper.setmSession(sessionHolder);
        amadeusSessionWrapper.setSequenceNumber("0");
        return amadeusSessionWrapper;
    }

    public AmadeusSessionWrapper logIn(FlightSearchOffice office) {
        logger.debug("amadeus login called....................");
        AmadeusSessionWrapper amadeusSessionWrapper = logIn(office.getOfficeId());
//        Holder<Session> sessionHolder = amadeusSessionWrapper.resetSession();
//        SecurityAuthenticate securityAuthenticateReq = MessageFactory.getInstance().getAuthenticationRequest(office.getOfficeId());
//        amadeusLogger.debug("securityAuthenticateReq " + new Date() + " ---->" + new XStream().toXML(securityAuthenticateReq));
//        SecurityAuthenticateReply securityAuthenticate = mPortType.securityAuthenticate(securityAuthenticateReq, sessionHolder);
//        amadeusLogger.debug("securityAuthenticateRes " + new Date() + " ---->" + new XStream().toXML(securityAuthenticate));
//        amadeusSessionWrapper.setmSession(sessionHolder);
//        amadeusSessionWrapper.setOfficeId(office.getOfficeId());
        amadeusSessionWrapper.setPartnerName(office.getName());

        return amadeusSessionWrapper;
    }

    public AmadeusSessionWrapper logIn(String officeId) {
        logger.debug("amadeus login called....................");
        AmadeusSessionWrapper amadeusSessionWrapper = new AmadeusSessionWrapper();
        Holder<Session> sessionHolder = amadeusSessionWrapper.resetSession();
        SecurityAuthenticate securityAuthenticateReq = MessageFactory.getInstance().getAuthenticationRequest(officeId);
        amadeusLogger.debug("securityAuthenticateReq " + new Date() + " ---->" + new XStream().toXML(securityAuthenticateReq));
        SecurityAuthenticateReply securityAuthenticate = mPortType.securityAuthenticate(securityAuthenticateReq, sessionHolder);
        amadeusLogger.debug("securityAuthenticateRes " + new Date() + " ---->" + new XStream().toXML(securityAuthenticate));
        amadeusSessionWrapper.setmSession(sessionHolder);
        amadeusSessionWrapper.setOfficeId(officeId);
        amadeusSessionWrapper.setSequenceNumber("0");
        return amadeusSessionWrapper;
    }

    public synchronized SecuritySignOutReply logOut(AmadeusSessionWrapper amadeusSessionWrapper) {
        if(amadeusSessionWrapper != null) {
            SecuritySignOutReply signOutReply = null;
            amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
            logger.debug("AmadeusFlightSearch securitySignOut at : " + new Date() + " " + amadeusSessionWrapper.getSessionId());
            signOutReply = mPortType.securitySignOut(new SecuritySignOut(), amadeusSessionWrapper.getmSession());
            amadeusLogger.debug("signOutReplyRes " + new Date() + " ---->" + new XStream().toXML(signOutReply));
            amadeusSessionWrapper.resetSession();
            return signOutReply;
        }
        return null;
    }

    //search flights with 2 cities- faremastertravelboard service
    public FareMasterPricerTravelBoardSearchReply searchAirlines(SearchParameters searchParameters, AmadeusSessionWrapper amadeusSessionWrapper, String searchType) {
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);

        logger.debug("AmadeusFlightSearch called at : {} {}", new Date(), amadeusSessionWrapper.getSessionId());
        FareMasterPricerTravelBoardSearch fareMasterPricerTravelBoardSearch = new SearchFlights().createSearchQuery(searchParameters, amadeusSessionWrapper.getOfficeId());
        amadeusLogger.debug("Amadeus Search request {} SessionId: {} Office Id: {} {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), amadeusSessionWrapper.getOfficeId(), searchType, new XStream().toXML(fareMasterPricerTravelBoardSearch));
        FareMasterPricerTravelBoardSearchReply SearchReply = mPortType.fareMasterPricerTravelBoardSearch(fareMasterPricerTravelBoardSearch, amadeusSessionWrapper.getmSession());

        if(Play.application().configuration().getBoolean("amadeus.DEBUG_SEARCH_LOG") && searchParameters.getBookingType().equals(BookingType.SEAMEN))
            loggerTemp.debug("\nAmadeusSearchReq {} :AmadeusFlightSearch response returned  at : {}session: {} ---->\n{}", amadeusSessionWrapper.getOfficeId(), new Date(), amadeusSessionWrapper.printSession(), new XStream().toXML(SearchReply));//todo
        logger.debug("AmadeusFlightSearch response returned  at : {}", new Date());
        amadeusLogger.debug(" SessionId: {}", amadeusSessionWrapper.getSessionId());
        return  SearchReply;
    }

    public FareMasterPricerTravelBoardSearchReply searchSplitAirlines(SearchParameters searchParameters, AmadeusSessionWrapper amadeusSessionWrapper,boolean isDestinationDomestic) {
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);

        logger.debug("AmadeusFlightSearch Split called at : {} {}", new Date(), amadeusSessionWrapper.getSessionId());
        FareMasterPricerTravelBoardSearch fareMasterPricerTravelBoardSearch = new SplitTicketSearchFlights().createSearchQuery(searchParameters, amadeusSessionWrapper.getOfficeId(),isDestinationDomestic);
        amadeusLogger.debug("AmadeusSearchReq Split {} SessionId: {} Office Id: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), amadeusSessionWrapper.getOfficeId(), new XStream().toXML(fareMasterPricerTravelBoardSearch));
        FareMasterPricerTravelBoardSearchReply SearchReply = mPortType.fareMasterPricerTravelBoardSearch(fareMasterPricerTravelBoardSearch, amadeusSessionWrapper.getmSession());

        if(Play.application().configuration().getBoolean("amadeus.DEBUG_SEARCH_LOG") && searchParameters.getBookingType().equals(BookingType.SEAMEN))
            loggerTemp.debug("\nAmadeusSearchReq split {} :AmadeusFlightSearch response returned  at : {}session: {} ---->\n{}", amadeusSessionWrapper.getOfficeId(), new Date(), amadeusSessionWrapper.printSession(), new XStream().toXML(SearchReply));//todo
        logger.debug("AmadeusFlightSearch split response returned  at : {}", new Date());
        amadeusLogger.debug("SessionId: {}", amadeusSessionWrapper.getSessionId());
        return  SearchReply;
    }

    public AirSellFromRecommendationReply checkFlightAvailability(TravellerMasterInfo travellerMasterInfo, AmadeusSessionWrapper amadeusSessionWrapper) {
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        logger.debug("amadeus checkFlightAvailability called at : " + new Date()  + "....................Session Id: " + amadeusSessionWrapper.getSessionId());
        AirSellFromRecommendation sellFromRecommendation = new BookFlights().sellFromRecommendation(travellerMasterInfo);

        amadeusLogger.debug("sellFromRecommendationReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId() + " ---->" + new XStream().toXML(sellFromRecommendation));

        AirSellFromRecommendationReply sellFromRecommendationReply = mPortType.airSellFromRecommendation(sellFromRecommendation, amadeusSessionWrapper.getmSession());

        amadeusLogger.debug("sellFromRecommendation Response " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId() + " ---->" + new XStream().toXML(sellFromRecommendationReply));
        return   sellFromRecommendationReply;
    }

    public PNRReply addTravellerInfoToPNR(TravellerMasterInfo travellerMasterInfo, AmadeusSessionWrapper amadeusSessionWrapper){
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        logger.debug("amadeus addTravellerInfoToPNR called   at " + new Date() + "....................Session Id: " + amadeusSessionWrapper.getSessionId());
        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().getMultiElements(travellerMasterInfo);

        amadeusLogger.debug("pnrAddMultiElementsReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrAddMultiElements));

        PNRReply pnrReply = mPortType.pnrAddMultiElements(pnrAddMultiElements, amadeusSessionWrapper.getmSession());

        amadeusLogger.debug("pnrAddMultiElementsRes " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrReply));
        return  pnrReply;
    }

    //pricing transaction
    public FarePricePNRWithBookingClassReply pricePNR(String carrrierCode, PNRReply pnrReply, boolean isSeamen, boolean isDomesticFlight, FlightItinerary flightItinerary, List<AirSegmentInformation> airSegmentList, boolean isSegmentWisePricing, AmadeusSessionWrapper amadeusSessionWrapper, boolean isAddBooking) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        logger.debug("amadeus pricePNR called at " + new Date() + "....................Session Id: " + amadeusSessionWrapper.getSessionId());
        FarePricePNRWithBookingClass pricePNRWithBookingClass = new PricePNR().getPNRPricingOption(carrrierCode, pnrReply, isSeamen, isDomesticFlight, flightItinerary, airSegmentList, isSegmentWisePricing,isAddBooking);

        amadeusLogger.debug("pricePNRWithBookingClassReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pricePNRWithBookingClass));

        FarePricePNRWithBookingClassReply pricePNRWithBookingClassReply = mPortType.farePricePNRWithBookingClass(pricePNRWithBookingClass, amadeusSessionWrapper.getmSession());

        amadeusLogger.debug("pricePNRWithBookingClassRes " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pricePNRWithBookingClassReply));
        return pricePNRWithBookingClassReply;
    }

    public TicketCreateTSTFromPricingReply createTST(int numberOfTST, AmadeusSessionWrapper amadeusSessionWrapper) {
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        logger.debug("amadeus createTST called at " + new Date() + "...................Session Id:. " + amadeusSessionWrapper.getSessionId());
        TicketCreateTSTFromPricing ticketCreateTSTFromPricing = new CreateTST().createTSTReq(numberOfTST);
        amadeusLogger.debug("createTSTFromPricingReplyReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(ticketCreateTSTFromPricing));

        TicketCreateTSTFromPricingReply createTSTFromPricingReply = mPortType.ticketCreateTSTFromPricing(ticketCreateTSTFromPricing, amadeusSessionWrapper.getmSession());

        amadeusLogger.debug("createTSTFromPricingReplyRes " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(createTSTFromPricingReply));
        return createTSTFromPricingReply;
    }

    public TicketCreateTSTFromPricingReply createSplitTST(int numberOfTST, AmadeusSessionWrapper amadeusSessionWrapper) {
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        logger.debug("amadeus createSplitTST called at " + new Date() + "...................Session Id:. " + amadeusSessionWrapper.getSessionId());
        TicketCreateTSTFromPricing ticketCreateTSTFromPricing = new CreateTST().createSplitTSTReq(numberOfTST);
        amadeusLogger.debug("createSplitTSTFromPricingReplyReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(ticketCreateTSTFromPricing));

        TicketCreateTSTFromPricingReply createTSTFromPricingReply = mPortType.ticketCreateTSTFromPricing(ticketCreateTSTFromPricing, amadeusSessionWrapper.getmSession());

        amadeusLogger.debug("createSplitTSTFromPricingReplyRes " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(createTSTFromPricingReply));
        return createTSTFromPricingReply;
    }

    public PNRReply savePNR(AmadeusSessionWrapper amadeusSessionWrapper) {
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        logger.debug("amadeus savePNR called at " + new Date() + "....................Session Id: " + amadeusSessionWrapper.getSessionId());
        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().savePnr();
        amadeusLogger.debug("savePNRReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrAddMultiElements));
        PNRReply pnrReply =  mPortType.pnrAddMultiElements(new PNRAddMultiElementsh().savePnr(), amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("savePNRRes " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrReply));
        return pnrReply;
    }

    public PNRReply savePNRES(AmadeusSessionWrapper amadeusSessionWrapper, String officeId) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        logger.debug("amadeus savePNR called at " + new Date() + "....................Session Id: " + amadeusSessionWrapper.getSessionId());
        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().esxEntry(officeId);
        amadeusLogger.debug("savePNRReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrAddMultiElements));
        PNRReply pnrReply =  mPortType.pnrAddMultiElements(pnrAddMultiElements, amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("savePNRRes " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrReply));
        return pnrReply;
    }

    public PNRReply savePNRES(AmadeusSessionWrapper amadeusSessionWrapper) {
        amadeusSessionWrapper.incrementSequenceNumber();
        logger.debug("amadeus savePNR called at " + new Date() + "....................Session Id: " + amadeusSessionWrapper.getSessionId());
        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().savePnr();
        amadeusLogger.debug("savePNRReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrAddMultiElements));
        PNRReply pnrReply =  mPortType.pnrAddMultiElements(new PNRAddMultiElementsh().savePnr(), amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("savePNRRes " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrReply));
        return pnrReply;
    }

    public PNRReply saveChildPNR(String optionCode, AmadeusSessionWrapper amadeusSessionWrapper) {
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        logger.debug("amadeus saveChildPNR called at " + new Date() + "....................Session Id: " + amadeusSessionWrapper.getSessionId());
        PNRAddMultiElements pnrAddMultiElements = new com.compassites.GDSWrapper.amadeus.PNRSplit().saveChildPnr(optionCode);
        amadeusLogger.debug("saveChildPNRReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrAddMultiElements));
        PNRReply pnrReply =  mPortType.pnrAddMultiElements(new com.compassites.GDSWrapper.amadeus.PNRSplit().saveChildPnr(optionCode), amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("saveChildPNRRes " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrReply));
        return pnrReply;
    }

    //This method is used to get the information saved in the PNR params = num = GDS PNR
    public PNRReply retrivePNR(String num, AmadeusSessionWrapper amadeusSessionWrapper){
        //change here
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        logger.debug("amadeus retrievePNR called at " + new Date() + "....................Session Id: "+ amadeusSessionWrapper.getSessionId());
        PNRRetrieve pnrRetrieve = new PNRRetriev().retrieve(num);
        amadeusLogger.debug("pnrRetrieveReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrRetrieve));
        PNRReply pnrReply = mPortType.pnrRetrieve(pnrRetrieve, amadeusSessionWrapper.getmSession());

        amadeusLogger.debug("pnrRetrieveRes " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrReply));
        return pnrReply;
    }

    public PNRReply ignoreAndRetrievePNR(AmadeusSessionWrapper amadeusSessionWrapper){
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        logger.debug("amadeus ignoreAndRetrievePNR called at " + new Date() + "..................Session Id " + amadeusSessionWrapper.getSessionId());
        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().ignoreAndRetrievePNR();
        amadeusLogger.debug("ignoreAndRetrievePNR " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrAddMultiElements));
        PNRReply pnrReply =  mPortType.pnrAddMultiElements(pnrAddMultiElements, amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("ignoreAndRetrievePNR " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrReply));
        return pnrReply;

    }

    public PNRReply ignorePNRAddMultiElement(AmadeusSessionWrapper amadeusSessionWrapper){
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        logger.debug("amadeus ignorePNRAddMultiElement called at " + new Date() + "..................Session Id " + amadeusSessionWrapper.getSessionId());
        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().ignorePNRAddMultiElement();

        amadeusLogger.debug("ignorePNRAddMultiElementReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrAddMultiElements));

        PNRReply pnrReply =  mPortType.pnrAddMultiElements(pnrAddMultiElements, amadeusSessionWrapper.getmSession());

        amadeusLogger.debug("ignorePNRAddMultiElement Res" + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrReply));
        return pnrReply;

    }

    public PNRReply addSSRDetailsToPNR(TravellerMasterInfo travellerMasterInfo, List<String> segmentNumbers, Map<String,String> travellerMap, AmadeusSessionWrapper amadeusSessionWrapper){
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        logger.debug("amadeus addSSRDetailsToPNR called   at " + new Date() + "....................Session Id: " + amadeusSessionWrapper.getSessionId());
        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().addSSRDetails(travellerMasterInfo, segmentNumbers, travellerMap);

        amadeusLogger.debug("addSSRDetailsToPNR " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrAddMultiElements));

        PNRReply pnrReply = mPortType.pnrAddMultiElements(pnrAddMultiElements, amadeusSessionWrapper.getmSession());

        amadeusLogger.debug("addSSRDetailsToPNR " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrReply));
        return  pnrReply;
    }

    public DocIssuanceIssueTicketReply issueTicket(boolean sendTSTDataForIssuance, List<String> tstReferenceList, AmadeusSessionWrapper amadeusSessionWrapper){
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        logger.debug("amadeus issueTicket called at " + new Date() + "....................Session Id: "+ amadeusSessionWrapper.getSessionId());
        DocIssuanceIssueTicket docIssuanceIssueTicket = new IssueTicket().issue(sendTSTDataForIssuance, tstReferenceList);
        amadeusLogger.debug("docIssuanceReq " + new Date()  + " SessionId: " + amadeusSessionWrapper.getSessionId() + " ---->" + new XStream().toXML(docIssuanceIssueTicket));
        DocIssuanceIssueTicketReply docIssuanceIssueTicketReply = mPortType.docIssuanceIssueTicket(docIssuanceIssueTicket, amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("docIssuanceRes " + new Date()  + " SessionId: " + amadeusSessionWrapper.getSessionId() + " ---->" + new XStream().toXML(docIssuanceIssueTicketReply));
        return docIssuanceIssueTicketReply;
    }

    public FareInformativePricingWithoutPNRReply getFareInfo(List<Journey> journeys, boolean seamen, int adultCount, int childCount, int infantCount, List<PAXFareDetails> paxFareDetailsList, AmadeusSessionWrapper amadeusSessionWrapper) {
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        logger.debug("amadeus getFareInfo called at " + new Date() + "....................Session Id: " + amadeusSessionWrapper.getSessionId());
        FareInformativePricingWithoutPNR farePricingWithoutPNR = new FareInformation().getFareInfo(journeys,seamen, adultCount, childCount, infantCount, paxFareDetailsList);
        amadeusLogger.debug("farePricingWithoutPNRReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(farePricingWithoutPNR));
        FareInformativePricingWithoutPNRReply fareInformativePricingPNRReply  = mPortType.fareInformativePricingWithoutPNR(farePricingWithoutPNR, amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("farePricingWithoutPNRRes " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(fareInformativePricingPNRReply));
        return  fareInformativePricingPNRReply;
    }

    public com.amadeus.xml.tipnrr_13_2_1a.FareInformativePricingWithoutPNRReply getFareInfo_32(List<Journey> journeys, boolean seamen, int adultCount, int childCount, int infantCount, List<PAXFareDetails> paxFareDetailsList, AmadeusSessionWrapper amadeusSessionWrapper) {
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        logger.debug("amadeus getFareInfo called at " + new Date() + "....................Session Id: " + amadeusSessionWrapper.getSessionId());
        com.amadeus.xml.tipnrq_13_2_1a.FareInformativePricingWithoutPNR farePricingWithoutPNR = new FareInformation13_2().getPriceInfo(journeys,seamen, adultCount, childCount, infantCount, paxFareDetailsList);
        amadeusLogger.debug("farePricingWithoutPNRReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(farePricingWithoutPNR));
        com.amadeus.xml.tipnrr_13_2_1a.FareInformativePricingWithoutPNRReply fareInformativePricingPNRReply  = mPortType.fareInformativePricingWithoutPNR132(farePricingWithoutPNR, amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("farePricingWithoutPNRRes " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(fareInformativePricingPNRReply));
        return  fareInformativePricingPNRReply;
    }

    public AirFlightInfoReply getFlightInfo(AirSegmentInformation airSegment, AmadeusSessionWrapper amadeusSessionWrapper) {
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        logger.debug("amadeus getFlightInfo  called at " + new Date() + "....................Session Id: " + amadeusSessionWrapper.getSessionId());
        AirFlightInfo airFlightInfo = new FlightInformation().getAirFlightInfo(airSegment);
        amadeusLogger.debug("flightInfoReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(airFlightInfo));
        AirFlightInfoReply airFlightInfoReply = mPortType.airFlightInfo(airFlightInfo, amadeusSessionWrapper.getmSession());

        amadeusLogger.debug("flightInfoRes " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(airFlightInfo));
        return airFlightInfoReply;
    }

    public FareCheckRulesReply getFareRules(AmadeusSessionWrapper amadeusSessionWrapper){
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        logger.debug("amadeus getFareRules called at " + new Date() + "....................Session Id: " + amadeusSessionWrapper.getSessionId());
        FareCheckRules fareCheckRules = new FareRules().createFareRules();
        amadeusLogger.debug("fareRulesReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(fareCheckRules));
        FareCheckRulesReply fareCheckRulesReply = mPortType.fareCheckRules(fareCheckRules, amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("fareRulesRes " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(fareCheckRulesReply));
        return fareCheckRulesReply;
    }

    public FareCheckRulesReply getFareRulesFromFareComponent(AmadeusSessionWrapper amadeusSessionWrapper, Map<String, String> fareComponentsMap){
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        logger.debug("amadeus getFareRulesFromFareComponent called at " + new Date() + "....................Session Id: " + amadeusSessionWrapper.getSessionId());
        FareCheckRules fareCheckRules = new FareRules().getFareCheckRulesForFareComponents(fareComponentsMap);
        amadeusLogger.debug("fareRulesFromFareComponentsReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(fareCheckRules));
        FareCheckRulesReply fareCheckRulesReply = mPortType.fareCheckRules(fareCheckRules, amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("fareRulesFromFareComponentsRes " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(fareCheckRulesReply));
        return fareCheckRulesReply;
    }

    public FareCheckRulesReply getFareRulesForFCType(String fcNumber, AmadeusSessionWrapper amadeusSessionWrapper){
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        logger.debug("amadeus getFareRulesForFCType called at " + new Date() + "....................Session Id: " + amadeusSessionWrapper.getSessionId());
        FareCheckRules fareCheckRules = new FareRules().getFareInfoForFCType(fcNumber);
        amadeusLogger.debug("getFareRulesForFCTypeReq " + new Date()+ " SessionId: " + amadeusSessionWrapper.getSessionId() + " ---->" + new XStream().toXML(fareCheckRules));
        FareCheckRulesReply fareCheckRulesReply = mPortType.fareCheckRules(fareCheckRules, amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("getFareRulesForFCTypeRes " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(fareCheckRulesReply));
        return fareCheckRulesReply;
    }



    //    public FarePricePNRWithLowestFareReply getLowestFare(boolean isSeamen) {
    public FarePricePNRWithLowestFareReply getLowestFare(String carrrierCode, PNRReply pnrReply, boolean isSeamen, boolean isDomesticFlight, FlightItinerary flightItinerary, List<AirSegmentInformation> airSegmentList, boolean isSegmentWisePricing, AmadeusSessionWrapper amadeusSessionWrapper) {
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        logger.debug("amadeus getLowestFare called at " + new Date() + "....................Session Id: " + amadeusSessionWrapper.getSessionId());
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

        amadeusLogger.debug("FarePricePNRWithLowestFareReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(farePricePNRWithLowestFare));
        FarePricePNRWithLowestFareReply farePricePNRWithLowestFareReply = mPortType.farePricePNRWithLowestFare(farePricePNRWithLowestFare, amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("FarePricePNRWithLowestFareReplyRes " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(farePricePNRWithLowestFareReply));
        return farePricePNRWithLowestFareReply;
    }

    public PNRReply exitESPnr(PNRCancel pnrCancel, AmadeusSessionWrapper amadeusSessionWrapper) {
        logger.debug("exitESPNR called  at " + new Date() + "................Session Id: "+ amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        amadeusLogger.debug("ExitESpnrReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrCancel));
        //PNRReply pnrReply = new PNRReply();
        PNRReply pnrReply = mPortType.pnrCancel(pnrCancel, amadeusSessionWrapper.getmSession());

        amadeusLogger.debug("ExitESRes " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrReply));
        return pnrReply;
    }


    public PNRReply cancelPNR(String pnr, PNRReply gdsPNRReply, AmadeusSessionWrapper amadeusSessionWrapper){
        logger.debug("cancelPNR called  at " + new Date() + "................Session Id: "+ amadeusSessionWrapper.getSessionId());

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);

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

        amadeusLogger.debug("pnrCancelReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrCancel));
        //PNRReply pnrReply = new PNRReply();
        PNRReply pnrReply = mPortType.pnrCancel(pnrCancel, amadeusSessionWrapper.getmSession());

        amadeusLogger.debug("pnrCancelRes " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrReply));
        return pnrReply;

    }

    public QueueListReply queueListResponse(QueueList queueListReq, AmadeusSessionWrapper amadeusSessionWrapper){
        logger.debug("queueListResponse called at " + new Date() + "....................Session Id: " + amadeusSessionWrapper.getSessionId());
        amadeusLogger.debug("queueListReq" + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(queueListReq));
        QueueListReply queueListReply = mPortType.queueList(queueListReq,amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("queueListRes" + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(queueListReply));
        return queueListReply;
    }

    public TicketDisplayTSTReply ticketDisplayTST(AmadeusSessionWrapper amadeusSessionWrapper){
        logger.debug("ticketDisplayTST called at " +  new Date() + "....................Session Id: " + amadeusSessionWrapper.getSessionId());

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        TicketDisplayTST ticketDisplayTST  = new CreateTST().createTicketDisplayTSTReq();
        amadeusLogger.debug("ticketDisplayTSTReq" + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(ticketDisplayTST));

        TicketDisplayTSTReply ticketDisplayTSTReply = mPortType.ticketDisplayTST(ticketDisplayTST, amadeusSessionWrapper.getmSession());

        amadeusLogger.debug("ticketDisplayTSTRes" + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(ticketDisplayTSTReply));

        return ticketDisplayTSTReply;
    }

    public PNRReply splitPNR(com.amadeus.xml.pnrspl_11_3_1a.PNRSplit pnrSplit, AmadeusSessionWrapper amadeusSessionWrapper){
        logger.debug("splitPNR called at " +  new Date() + "....................Session Id: " + amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.incrementSequenceNumber();
        amadeusLogger.debug("splitPNRReq" + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrSplit));
        PNRReply pnrReply = mPortType.pnrSplit(pnrSplit,amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("splitPNRRes" + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrReply));
        return pnrReply;
    }

    //    //todo for time being removing it as compulsary steps in ticket booking
    public MiniRuleGetFromRecReply retriveMiniRuleFromPNR(AmadeusSessionWrapper amadeusSessionWrapper, String pnr){
        //change here
        try {
            amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
            logger.debug("amadeus retrievePNR called at " + new Date() + "....................Session Id: " + amadeusSessionWrapper.getSessionId());
            MiniRuleGetFromRec miniRuleGetFromPricingRec = new PNRRetriev().miniRuleGetFromPNR(pnr);
            amadeusLogger.debug("miniRuleGetFromPricingRecReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId() + " ---->" + new XStream().toXML(miniRuleGetFromPricingRec));
            MiniRuleGetFromRecReply miniRuleGetFromPricingRecReply = mPortType.miniRuleGetFromRec(miniRuleGetFromPricingRec, amadeusSessionWrapper.getmSession());

            amadeusLogger.debug("miniRuleGetFromPricingRecReply " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId() + " ---->" + new XStream().toXML(miniRuleGetFromPricingRecReply));
            return miniRuleGetFromPricingRecReply;
        }catch (Exception e){
            logger.error("error in retriveMiniRuleFromPNR:"+ e.getMessage());
        }
        return null;
    }
//
//
//    public MiniRuleGetFromETicketReply retriveMiniRuleFromEticket(String Eticket, AmadeusSessionWrapper amadeusSessionWrapper){
//        //change here
//        amadeusSessionWrapper.incrementSequenceNumber();
//        logger.debug("amadeus retrievePNR called at " + new Date() + "....................Session Id: "+ amadeusSessionWrapper.getSessionId());
//        MiniRuleGetFromETicket miniRuleGetFromETicket = new PNRRetriev().miniRuleGetFromETicket(Eticket);
//        amadeusLogger.debug("MiniRuleGetFromETicketReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(miniRuleGetFromETicket));
//        MiniRuleGetFromETicketReply miniRuleGetFromETicketReply = mPortType.miniRuleGetFromETicket(miniRuleGetFromETicket, amadeusSessionWrapper.getmSession());
//
//        amadeusLogger.debug("MiniRuleGetFromETicketReply " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(miniRuleGetFromETicketReply));
//        return miniRuleGetFromETicketReply;
//    }

    public MiniRuleGetFromRecReply retriveMiniRuleFromPricing(AmadeusSessionWrapper amadeusSessionWrapper){
        //change here
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        logger.debug("amadeus retrievePNR called at " + new Date() + "....................Session Id: "+ amadeusSessionWrapper.getSessionId());
        MiniRuleGetFromRec miniRuleGetFromPricing = new PNRRetriev().miniRuleGetFromPricing();
        amadeusLogger.debug("MiniRuleGetFromPricingReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(miniRuleGetFromPricing));
        MiniRuleGetFromRecReply miniRuleGetFromPricingReply = mPortType.miniRuleGetFromRec(miniRuleGetFromPricing, amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("MiniRuleGetFromPricingReply " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(miniRuleGetFromPricingReply));
        return miniRuleGetFromPricingReply;
    }


    public PNRReply cancelFullPNR(String pnr, PNRReply gdsPNRReply, AmadeusSessionWrapper amadeusSessionWrapper,Boolean setReservationInfo){
        logger.debug("cancelFullPNR called  at " + new Date() + "................Session Id: "+ amadeusSessionWrapper.getSessionId());
        PNRCancel pnrCancel = new PNRCancel();
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        if(setReservationInfo) {
            ReservationControlInformationDetailsTypeI reservationControlInformationDetailsTypeI = new ReservationControlInformationDetailsTypeI();
            reservationControlInformationDetailsTypeI.setControlNumber(pnr);
            ReservationControlInformationType reservationControlInformationType = new ReservationControlInformationType();
            reservationControlInformationType.setReservation(reservationControlInformationDetailsTypeI);
            pnrCancel.setReservationInfo(reservationControlInformationType);
        }


        OptionalPNRActionsType pnrActionsType = new OptionalPNRActionsType();
        pnrActionsType.getOptionCode().add(BigInteger.valueOf(11));
        pnrCancel.setPnrActions(pnrActionsType);
        CancelPNRElementType cancelPNRElementType = new CancelPNRElementType();
        cancelPNRElementType.setEntryType(AmadeusConstants.CANCEL_PNR_ITINERARY_TYPE);
        pnrCancel.getCancelElements().add(cancelPNRElementType);


        amadeusLogger.debug("pnrFullCancelReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrCancel));
        //PNRReply pnrReply = new PNRReply();
        PNRReply pnrReply = mPortType.pnrCancel(pnrCancel, amadeusSessionWrapper.getmSession());

        amadeusLogger.debug("pnrFullCancelRes " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrReply));
        return pnrReply;

    }

    public TicketProcessEDocReply ticketProcessEDoc(List<String> tickets,AmadeusSessionWrapper amadeusSessionWrapper){
        logger.debug("ticketProcessEdoc called  at " + new Date() + "................Session Id: "+ amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        TicketProcessEDoc ticketProcessEDocRQ = new RefundTicket().getTicketProcessEdocRQ(tickets);
        amadeusLogger.debug("ticketProcessEDocRQ " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(ticketProcessEDocRQ));
        TicketProcessEDocReply ticketProcessEDocReply = mPortType.ticketProcessEDoc(ticketProcessEDocRQ,amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("ticketProcessEDocResponse " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(ticketProcessEDocReply));
        return ticketProcessEDocReply;
    }

    public TicketCancelDocumentReply ticketCancelDocument(String pnr, List<String> ticketsList, PNRReply gdsPNRReply, String delOficID, AmadeusSessionWrapper amadeusSessionWrapper){
        logger.debug("Ticket cancel document called  at " + new Date() + "................Session Id: "+ amadeusSessionWrapper.getSessionId());

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);

        // Creating XML request for ticket cancel document
        TicketCancelDocument ticketCancelDocument  = new TicketCancelDocumentHandler().ticketCancelDocument(ticketsList,gdsPNRReply,delOficID);

        amadeusLogger.debug("ticketCancelDocReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(ticketCancelDocument));
        TicketCancelDocumentReply ticketCancelDocumentReply = mPortType.ticketCancelDocument(ticketCancelDocument, amadeusSessionWrapper.getmSession());

        amadeusLogger.debug("ticketCancelDocRes " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(ticketCancelDocumentReply));
        return ticketCancelDocumentReply;

    }

    public TicketProcessEDocReply reIssueCheckTicketStatus(ReIssueSearchRequest reIssueSearchRequest, AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        TicketProcessEDoc ticketProcessEDocReq = ReIssueTicket.ReIssueCheckTicketStatus.createReissueTicketStatusCheck(reIssueSearchRequest);
        amadeusLogger.info("ReIssueTicketStatusCheckRequest {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketProcessEDocReq));
        TicketProcessEDocReply ticketProcessEDocReply = mPortType.ticketProcessEDoc(ticketProcessEDocReq, amadeusSessionWrapper.getmSession());
        amadeusLogger.info("ReIssueTicketStatusCheckReply {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketProcessEDocReply));
        return ticketProcessEDocReply;

    }

    public TicketCheckEligibilityReply reIssueTicketCheckEligibility(ReIssueSearchRequest reIssueSearchRequest, AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        TicketCheckEligibility ticketCheckEligibility = ReIssueTicket.ReIssueCheckEligibility.createCheckEligibilityRequest(reIssueSearchRequest, amadeusSessionWrapper.getOfficeId());
        amadeusLogger.debug("ReIssueTicketCheckEligibilityRequest {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketCheckEligibility));
        TicketCheckEligibilityReply ticketCheckEligibilityReply = mPortType.ticketCheckEligibility(ticketCheckEligibility, amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("ReIssueTicketCheckEligibilityReply {} SessionId: {}  \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketCheckEligibilityReply));
        return ticketCheckEligibilityReply;

    }

    public TicketATCShopperMasterPricerTravelBoardSearchReply reIssueATCAirlineSearch(ReIssueSearchRequest reissueSearchRequest, TravelFlightInformationType allowedCarriers, AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        TicketATCShopperMasterPricerTravelBoardSearch reIssueATCSearchRequest = ReIssueTicket.ReIssueATCSearch.createReissueATCSearchRequest(reissueSearchRequest, allowedCarriers, amadeusSessionWrapper.getOfficeId());
        amadeusLogger.debug("ReIssueATCSearch Request on {}, SessionId: {}, Office ID: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), amadeusSessionWrapper.getOfficeId(), new XStream().toXML(reIssueATCSearchRequest));
        TicketATCShopperMasterPricerTravelBoardSearchReply reIssueATCSearchReply = mPortType.ticketATCShopperMasterPricerTravelBoardSearch(reIssueATCSearchRequest, amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("ReIssueATCSearch Response {} SessionId: {}, Office ID: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), amadeusSessionWrapper.getOfficeId(), new XStream().toXML(reIssueATCSearchReply));
        return reIssueATCSearchReply;

    }

    public ServiceIntegratedCatalogueReply getAdditionalBaggageInformationAmadeus(AmadeusSessionWrapper amadeusSessionWrapper){

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        ServiceIntegratedCatalogue serviceIntegratedCatalogue = AncillaryServiceReq.AdditionalPaidBaggage.createShowAdditionalBaggageInformationRequest();
        amadeusLogger.debug("ServiceIntegratedCatalogue Additional Baggage Request {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(serviceIntegratedCatalogue));
        ServiceIntegratedCatalogueReply serviceIntegratedCatalogueReply = mPortType.serviceIntegratedCatalogue(serviceIntegratedCatalogue, amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("ServiceIntegratedCatalogue Additional Baggage Response {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(serviceIntegratedCatalogueReply));
        return serviceIntegratedCatalogueReply;

    }

    public ServiceStandaloneCatalogueReply getAdditionalBaggageInfoStandalone(AmadeusSessionWrapper amadeusSessionWrapper, AncillaryServiceRequest ancillaryServiceRequest){

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        ServiceStandaloneCatalogue serviceStandaloneCatalogue = AncillaryServiceReq.AdditionalPaidBaggage.createShowAdditionalBaggageInformationRequestStandalone(ancillaryServiceRequest);
        amadeusLogger.debug("ServiceStandaloneCatalogue Additional Baggage Request {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(serviceStandaloneCatalogue));
        ServiceStandaloneCatalogueReply serviceStandaloneCatalogueReply = mPortType.serviceStandaloneCatalogue(serviceStandaloneCatalogue, amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("ServiceStandaloneCatalogue Additional Baggage Response {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(serviceStandaloneCatalogueReply));
        return serviceStandaloneCatalogueReply;

    }

    public ServiceStandaloneCatalogueReply getMealsInfoStandalone(AmadeusSessionWrapper amadeusSessionWrapper, AncillaryServiceRequest ancillaryServiceRequest){

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        ServiceStandaloneCatalogue serviceStandaloneCatalogue = AncillaryServiceReq.AdditionalPaidBaggage.createShowMealsInformationRequestStandalone(ancillaryServiceRequest);
        amadeusLogger.debug("ServiceStandaloneCatalogue Meals Request {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(serviceStandaloneCatalogue));
        ServiceStandaloneCatalogueReply serviceStandaloneCatalogueReply = mPortType.serviceStandaloneCatalogue(serviceStandaloneCatalogue, amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("ServiceStandaloneCatalogue Meals Response {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(serviceStandaloneCatalogueReply));
        return serviceStandaloneCatalogueReply;

    }


    public PNRReply partialCancelPNR(String pnr, PNRReply gdsPNRReply,Map<BigInteger, String> segmentMap, AmadeusSessionWrapper amadeusSessionWrapper){
        logger.debug("partialCancelPNR called  at " + new Date() + "................Session Id: "+ amadeusSessionWrapper.getSessionId());

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);

        PNRCancel pnrCancel = new PNRCancel();
        OptionalPNRActionsType pnrActionsType = new OptionalPNRActionsType();
        pnrActionsType.getOptionCode().add(BigInteger.valueOf(11));
        pnrCancel.setPnrActions(pnrActionsType);
        CancelPNRElementType cancelPNRElementType = new CancelPNRElementType();
        List<ElementIdentificationType> elementIdentificationTypeList = cancelPNRElementType.getElement();
        if(!segmentMap.isEmpty()) {
            // Iterating over the Map
            ElementIdentificationType elementIdentificationType = new ElementIdentificationType();
            for (Map.Entry<BigInteger, String> entry : segmentMap.entrySet()) {
                BigInteger key = entry.getKey();
                String value = entry.getValue();
                elementIdentificationType.setNumber(key);
                elementIdentificationType.setIdentifier(value);
                elementIdentificationTypeList.add(elementIdentificationType);
            }
        }else {
            for (PNRReply.OriginDestinationDetails originDestination : gdsPNRReply.getOriginDestinationDetails()) {
                for (PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestination.getItineraryInfo()) {
                    ElementIdentificationType elementIdentificationType = new ElementIdentificationType();
                    String segType = itineraryInfo.getElementManagementItinerary().getSegmentName();
                    if (segType.equalsIgnoreCase("AIR")) {
                        BigInteger segmentRef = itineraryInfo.getElementManagementItinerary().getReference().getNumber();
                        String segQualifier = itineraryInfo.getElementManagementItinerary().getReference().getQualifier();
                        elementIdentificationType.setNumber(segmentRef);
                        elementIdentificationType.setIdentifier(segQualifier);
                        elementIdentificationTypeList.add(elementIdentificationType);
                    }
                }
            }
        }
        cancelPNRElementType.setEntryType(AmadeusConstants.CANCEL_PNR_ELEMENT_TYPE);
        pnrCancel.getCancelElements().add(cancelPNRElementType);

        amadeusLogger.debug("pnrCancelReq " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrCancel));
        //PNRReply pnrReply = new PNRReply();
        PNRReply pnrReply = mPortType.pnrCancel(pnrCancel, amadeusSessionWrapper.getmSession());

        amadeusLogger.debug("pnrCancelRes " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(pnrReply));
        return pnrReply;

    }
    public TicketRepricePNRWithBookingClassReply repricePNRWithBookingClassReply(AmadeusSessionWrapper amadeusSessionWrapper, TravellerMasterInfo travellerMasterInfo, List<String> tickets) {
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        TicketRepricePNRWithBookingClass ticketRepricePNRWithBookingClass = ReIssueTicket.getTicketRepricePNRWithBookingClass(travellerMasterInfo, tickets);
        amadeusLogger.debug("TicketRepricePNRWithBookingClass Request {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketRepricePNRWithBookingClass));
        TicketRepricePNRWithBookingClassReply repricePNRWithBookingClassReply = mPortType.ticketRepricePNRWithBookingClass(ticketRepricePNRWithBookingClass, amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("TicketRepricePNRWithBookingClassReply Response {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(repricePNRWithBookingClassReply));
        return repricePNRWithBookingClassReply;
    }

    public TicketReissueConfirmedPricingReply ticketReissueConfirmedPricingReply(AmadeusSessionWrapper amadeusSessionWrapper, List<String> tickets) {
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        TicketReissueConfirmedPricing ticketReissueConfirmedPricing = ReIssueTicket.getTicketReissueConfirmedPricing(tickets);
        amadeusLogger.debug("TicketReissueConfirmedPricing Request {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketReissueConfirmedPricing));
        TicketReissueConfirmedPricingReply ticketReissueConfirmedPricingReply = mPortType.ticketReissueConfirmedPricing(ticketReissueConfirmedPricing, amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("TicketReissueConfirmedPricingReply Response {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketReissueConfirmedPricingReply));
        return ticketReissueConfirmedPricingReply;
    }

    public TicketProcessEDocReply ticketProcessEDocReply(AmadeusSessionWrapper amadeusSessionWrapper, List<OpenTicketDTO> openTicketDTOS) {
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        TicketProcessEDoc ticketProcessEDoc = OpenTicketReport.OpenTicketReportRequest.createOpenTicketRequest(openTicketDTOS);
        amadeusLogger.debug("Open Ticket Request Request {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketProcessEDoc));
        TicketProcessEDocReply ticketProcessEDocReply = mPortType.ticketProcessEDoc(ticketProcessEDoc, amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("Open Ticket Response body {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketProcessEDocReply));
        return ticketProcessEDocReply;
    }

    public PNRReply splitPNRForReissue(ReIssueConfirmationRequest reIssueConfirmationRequest, AmadeusSessionWrapper amadeusSessionWrapper){

        amadeusSessionWrapper.incrementSequenceNumber();
        com.amadeus.xml.pnrspl_11_3_1a.PNRSplit pnrSplit = ReIssueTicket.ReIssueConfirmation.splitPNRForReIssuedPax(reIssueConfirmationRequest);
        amadeusLogger.debug("Splitting PNR for ReIssue Request Body {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrSplit));
        PNRReply pnrReply = mPortType.pnrSplit(pnrSplit,amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("Splitting PNR for ReIssue Response Body {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrReply));

        return pnrReply;
    }

}