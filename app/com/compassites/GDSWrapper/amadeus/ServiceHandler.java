package com.compassites.GDSWrapper.amadeus;

import javax.xml.ws.handler.Handler;

import com.amadeus.xml.AmadeusWebServices;
import com.amadeus.xml.AmadeusWebServicesPT;
import com.amadeus.xml._2010._06.session_v3.Session;
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
import com.amadeus.xml.pnracc_14_1_1a.PNRReply;
import com.amadeus.xml.pnradd_14_1_1a.PNRAddMultiElements;
import com.amadeus.xml.pnrret_14_1_1a.PNRRetrieve;
import com.amadeus.xml.pnrxcl_14_1_1a.*;
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
import com.amadeus.xml.trcanq_14_1_1a.TicketCancelDocument;
import com.amadeus.xml.trcanr_14_1_1a.TicketCancelDocumentReply;
import com.amadeus.xml.ttktiq_09_1_1a.DocIssuanceIssueTicket;
import com.amadeus.xml.ttktir_09_1_1a.DocIssuanceIssueTicketReply;
import com.amadeus.xml.ttstrq_13_1_1a.TicketDisplayTST;
import com.amadeus.xml.ttstrr_13_1_1a.TicketDisplayTSTReply;
import com.amadeus.xml.vlssoq_04_1_1a.SecuritySignOut;
import com.amadeus.xml.vlssor_04_1_1a.SecuritySignOutReply;
import com.amadeus.xml.pnrspl_14_1_1a.PNRSplit;

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
    private BindingProvider bindingProvider;

    public static URL wsdlUrl;

    static Logger amadeusLogger = LoggerFactory.getLogger("amadeus");
    static Logger logger = LoggerFactory.getLogger("gds");
    static Logger loggerTemp = LoggerFactory.getLogger("gds_search");

    public static String endPoint = null;

    static {
        URL url = null;
        try {
            endPoint = play.Play.application().configuration().getString("amadeus.endPointURL");
            url = ServiceHandler.class.getResource("/META-INF/wsdl/amadeus4/1ASIWFLYFYH_PDT_20250121_070055.wsdl");
        } catch (Exception e) {
            logger.debug("Error in loading Amadeus URL : ", e);
        }
        wsdlUrl = url;
    }

    public ServiceHandler() throws Exception {

        AmadeusWebServices service = new AmadeusWebServices(wsdlUrl);
        mPortType = service.getAmadeusWebServicesPort();
        bindingProvider = (BindingProvider) mPortType;

        HashMap<String, List<String>> httpHeaders = new HashMap<>();
        httpHeaders.put("Accept", Collections.singletonList("text/xml, multipart/related"));
        httpHeaders.put("Accept-Encoding", Collections.singletonList("gzip"));
        httpHeaders.put("Content-Encoding", Collections.singletonList("gzip"));

        Map<String, Object> reqContext = bindingProvider.getRequestContext();
        reqContext.put(MessageContext.HTTP_REQUEST_HEADERS, httpHeaders);
        reqContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPoint);

        if (bindingProvider.getBinding().getHandlerChain().isEmpty()) {
            List<Handler> handlerChain = new ArrayList<>();
            handlerChain.add(new AmadeusSOAPHeaderHandler());
            bindingProvider.getBinding().setHandlerChain(handlerChain);
        }

    }

    //This Method is used to log in with only BOM Office ID all the time
    public AmadeusSessionWrapper logIn(boolean isStateful) {

        logger.debug("Amadeus Normal Login called {}", new Date());

        AmadeusSessionWrapper amadeusSessionWrapper = new AmadeusSessionWrapper();
        Holder<Session> sessionHolder = amadeusSessionWrapper.resetSession();
        FlightSearchOffice office = new FlightSearchOffice();

        amadeusSessionWrapper.setmSession(sessionHolder);
        amadeusSessionWrapper.setStateful(isStateful);
        amadeusSessionWrapper.setOfficeId(office.getOfficeId());

        return amadeusSessionWrapper;
    }

    // Log in with FlightSearchOffice
    public AmadeusSessionWrapper logIn(FlightSearchOffice office, boolean isStateful) {
        logger.debug("Amadeus Flight Search Office ID login called {} ---> {}", office.getOfficeId(), new Date());

        AmadeusSessionWrapper amadeusSessionWrapper = logIn(office.getOfficeId(), isStateful);
        amadeusSessionWrapper.setPartnerName(office.getName());

        return amadeusSessionWrapper;
    }

    // Log in with specific office ID
    public AmadeusSessionWrapper logIn(String officeId, boolean isStateful) {

        logger.debug("Amadeus Office ID login called {} ---> {}", officeId, new Date());

        AmadeusSessionWrapper amadeusSessionWrapper = new AmadeusSessionWrapper();
        Holder<Session> session = amadeusSessionWrapper.resetSession();

        amadeusSessionWrapper.setmSession(session);
        amadeusSessionWrapper.setStateful(isStateful);
        amadeusSessionWrapper.setOfficeId(officeId);

        amadeusLogger.debug("Office ID Login Setup for officeId: {}", officeId);

        return amadeusSessionWrapper;
    }

    //This Method Logs out of a session
    public synchronized SecuritySignOutReply logOut(AmadeusSessionWrapper amadeusSessionWrapper) {

        if (amadeusSessionWrapper != null) {

            SecuritySignOutReply signOutReply;

            amadeusSessionWrapper.setLogout(true);
            amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);

            logger.debug("Amadeus Logout Request called at : {} {}", new Date(), amadeusSessionWrapper.getSessionId());
            signOutReply = mPortType.securitySignOut(new SecuritySignOut(), amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
            amadeusLogger.debug("Amadeus Logout Response {} ---->{}", new Date(), new XStream().toXML(signOutReply));

            amadeusSessionWrapper.resetSession();
            return signOutReply;
        }

        return null;
    }

    //This method searches for Airlines
    public FareMasterPricerTravelBoardSearchReply searchAirlines(SearchParameters searchParameters, AmadeusSessionWrapper amadeusSessionWrapper, String searchType) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("AmadeusFlightSearch called at : {} {}", new Date(), amadeusSessionWrapper.getSessionId());

        FareMasterPricerTravelBoardSearch fareMasterPricerTravelBoardSearch = new SearchFlights().createSearchQuery(searchParameters, amadeusSessionWrapper.getOfficeId());

        amadeusLogger.debug("Amadeus Search request {} SessionId: {} Office Id: {} {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), amadeusSessionWrapper.getOfficeId(), searchType, new XStream().toXML(fareMasterPricerTravelBoardSearch));
        FareMasterPricerTravelBoardSearchReply SearchReply = mPortType.fareMasterPricerTravelBoardSearch(fareMasterPricerTravelBoardSearch, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());

        if (Play.application().configuration().getBoolean("amadeus.DEBUG_SEARCH_LOG") && searchParameters.getBookingType().equals(BookingType.SEAMEN)) {
            loggerTemp.debug("\nAmadeusSearchReq {} :AmadeusFlightSearch response returned  at : {}session: {} ---->\n{}", amadeusSessionWrapper.getOfficeId(), new Date(), amadeusSessionWrapper.printSession(), new XStream().toXML(SearchReply));
        }
        logger.debug("AmadeusFlightSearch response returned  at : {} for : {} from : {}", new Date(), searchType, amadeusSessionWrapper.getOfficeId());
        amadeusLogger.debug(" SessionId: {}", amadeusSessionWrapper.getSessionId());

        return SearchReply;

    }

    //This method is used for search airlines for split airline workflow
    public FareMasterPricerTravelBoardSearchReply searchSplitAirlines(SearchParameters searchParameters, AmadeusSessionWrapper amadeusSessionWrapper, boolean isDestinationDomestic) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("AmadeusFlightSearch Split called at : {} {}", new Date(), amadeusSessionWrapper.getSessionId());

        FareMasterPricerTravelBoardSearch fareMasterPricerTravelBoardSearch = new SplitTicketSearchFlights().createSearchQuery(searchParameters, amadeusSessionWrapper.getOfficeId(), isDestinationDomestic);
        amadeusLogger.debug("AmadeusSearchReq Split {} SessionId: {} Office Id: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), amadeusSessionWrapper.getOfficeId(), new XStream().toXML(fareMasterPricerTravelBoardSearch));
        FareMasterPricerTravelBoardSearchReply SearchReply = mPortType.fareMasterPricerTravelBoardSearch(fareMasterPricerTravelBoardSearch, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());

        if (Play.application().configuration().getBoolean("amadeus.DEBUG_SEARCH_LOG") && searchParameters.getBookingType().equals(BookingType.SEAMEN))
            loggerTemp.debug("\nAmadeusSearchReq split {} :AmadeusFlightSearch response returned  at : {}session: {} ---->\n{}", amadeusSessionWrapper.getOfficeId(), new Date(), amadeusSessionWrapper.printSession(), new XStream().toXML(SearchReply));
        logger.debug("AmadeusFlightSearch split response returned  at : {}", new Date());
        amadeusLogger.debug("SessionId: {}", amadeusSessionWrapper.getSessionId());

        return SearchReply;
    }

    //This Method is used to check the availability and fetch baggage information at generate PNR time
    public AirSellFromRecommendationReply checkFlightAvailability(TravellerMasterInfo travellerMasterInfo, AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("Amadeus Check Flight Availability called at : {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());

        AirSellFromRecommendation sellFromRecommendation = new BookFlights().sellFromRecommendation(travellerMasterInfo);
        amadeusLogger.debug("AirSellFromRecommendation Request {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(sellFromRecommendation));
        AirSellFromRecommendationReply sellFromRecommendationReply = mPortType.airSellFromRecommendation(sellFromRecommendation, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("AirSellFromRecommendationReply Response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(sellFromRecommendationReply));

        amadeusSessionWrapper.updateSessionFromResponse(amadeusSessionWrapper.getmSession());

        return sellFromRecommendationReply;
    }

    //This method added the traveller information to the PNR
    public PNRReply addTravellerInfoToPNR(TravellerMasterInfo travellerMasterInfo, AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("Amadeus Add Traveller Infor to PNR called   at {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());

        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().getMultiElements(travellerMasterInfo);
        amadeusLogger.debug("PNRAddMultiElements Request -- Add Traveller to PNR {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrAddMultiElements));
        PNRReply pnrReply = mPortType.pnrAddMultiElements(pnrAddMultiElements, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("PNRReply -- Add Traveller to PNR {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrReply));

        return pnrReply;
    }

    //This method is used to Price the PNR
    public FarePricePNRWithBookingClassReply pricePNR(String carrrierCode, PNRReply pnrReply, boolean isSeamen, boolean isDomesticFlight, FlightItinerary flightItinerary, List<AirSegmentInformation> airSegmentList, boolean isSegmentWisePricing, AmadeusSessionWrapper amadeusSessionWrapper, boolean isAddBooking) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("Amadeus pricePNR called at {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());

        FarePricePNRWithBookingClass pricePNRWithBookingClass = new PricePNR().getPNRPricingOption(carrrierCode, pnrReply, isSeamen, isDomesticFlight, flightItinerary, airSegmentList, isSegmentWisePricing, isAddBooking, false, 0);
        amadeusLogger.debug("FarePricePNRWithBookingClass Request {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pricePNRWithBookingClass));
        FarePricePNRWithBookingClassReply pricePNRWithBookingClassReply = mPortType.farePricePNRWithBookingClass124(pricePNRWithBookingClass, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("FarePricePNRWithBookingClass response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pricePNRWithBookingClassReply));

        return pricePNRWithBookingClassReply;
    }

    //This method creates a TST
    public TicketCreateTSTFromPricingReply createTST(int numberOfTST, AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("Amadeus createTST called at {}...................Session Id:. {}", new Date(), amadeusSessionWrapper.getSessionId());

        TicketCreateTSTFromPricing ticketCreateTSTFromPricing = new CreateTST().createTSTReq(numberOfTST);
        amadeusLogger.debug("TicketCreateTSTFromPricing Request {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketCreateTSTFromPricing));
        TicketCreateTSTFromPricingReply createTSTFromPricingReply = mPortType.ticketCreateTSTFromPricing(ticketCreateTSTFromPricing, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("TicketCreateTSTFromPricingReply Response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(createTSTFromPricingReply));

        return createTSTFromPricingReply;
    }

    //This method creates Split TST's
    public TicketCreateTSTFromPricingReply createSplitTST(int numberOfTST, AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("Amadeus createSplitTST called at {}...................Session Id:. {}", new Date(), amadeusSessionWrapper.getSessionId());

        TicketCreateTSTFromPricing ticketCreateTSTFromPricing = new CreateTST().createSplitTSTReq(numberOfTST);
        amadeusLogger.debug("TicketCreateTSTFromPricing - Split TST - Request {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketCreateTSTFromPricing));
        TicketCreateTSTFromPricingReply createTSTFromPricingReply = mPortType.ticketCreateTSTFromPricing(ticketCreateTSTFromPricing, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("TicketCreateTSTFromPricingReply - Split TST - Response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(createTSTFromPricingReply));

        return createTSTFromPricingReply;
    }

    //This method saves a PNR
    public PNRReply savePNR(AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("Amadeus savePNR called at {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());

        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().savePnr();
        amadeusLogger.debug("Save PNR request {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrAddMultiElements));
        PNRReply pnrReply = mPortType.pnrAddMultiElements(new PNRAddMultiElementsh().savePnr(), amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("Save PNR Response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrReply));


        return pnrReply;
    }


    //This Method is used for ES Entry
    public PNRReply savePNRES(AmadeusSessionWrapper amadeusSessionWrapper, String officeId) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("Amadeus savePNRES -ESX Entry- called at {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());

        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().esxEntry(officeId);
        amadeusLogger.debug("Amadeus savePNRES -ESX Entry- Request {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrAddMultiElements));
        PNRReply pnrReply = mPortType.pnrAddMultiElements(pnrAddMultiElements, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("Amadeus savePNRES -ESX Entry- Response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrReply));

        return pnrReply;
    }

    //Deprecated
    public PNRReply savePNRES(AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("amadeus savePNR called at {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());
        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().savePnr();

        amadeusLogger.debug("savePNRReq {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrAddMultiElements));
        PNRReply pnrReply = mPortType.pnrAddMultiElements(new PNRAddMultiElementsh().savePnr(), amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("savePNRRes {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrReply));

        return pnrReply;
    }

    //This Method Saves a child PNR after orignal PNR split
    public PNRReply saveChildPNR(String optionCode, AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("Amadeus saveChildPNR called at {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());

        PNRAddMultiElements pnrAddMultiElements = new com.compassites.GDSWrapper.amadeus.PNRSplit().saveChildPnr(optionCode);
        amadeusLogger.debug("Save Child PNR Request {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrAddMultiElements));
        PNRReply pnrReply = mPortType.pnrAddMultiElements(new com.compassites.GDSWrapper.amadeus.PNRSplit().saveChildPnr(optionCode), amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("Save Child PNR Response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrReply));

        return pnrReply;
    }

    //This method is used to get the information saved in the PNR params = num = GDS PNR
    public PNRReply retrievePNR(String num, AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("Amadeus retrievePNR called at {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());

        PNRRetrieve pnrRetrieve = new PNRRetriev().retrieve(num);
        amadeusLogger.debug("PNRRetrieve request {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrRetrieve));
        PNRReply pnrReply = mPortType.pnrRetrieve(pnrRetrieve, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("PNRRetrieve Response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrReply));

        amadeusSessionWrapper.updateSessionFromResponse(amadeusSessionWrapper.getmSession());

        return pnrReply;
    }

    //This method Ignores the changes and retrieves the PNR
    public PNRReply ignoreAndRetrievePNR(AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("Amadeus ignoreAndRetrievePNR called at {}..................Session Id {}", new Date(), amadeusSessionWrapper.getSessionId());

        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().ignoreAndRetrievePNR();
        amadeusLogger.debug("Ignore And Retrieve PNR Request{} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrAddMultiElements));
        PNRReply pnrReply = mPortType.pnrAddMultiElements(pnrAddMultiElements, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("Ignore And Retrieve PNR Response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrReply));

        amadeusSessionWrapper.updateSessionFromResponse(amadeusSessionWrapper.getmSession());

        return pnrReply;
    }

    //This method Ignores the changes and Adds elements to the PNR
    public PNRReply ignorePNRAddMultiElement(AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("Amadeus ignorePNRAddMultiElement called at {}..................Session Id {}", new Date(), amadeusSessionWrapper.getSessionId());

        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().ignorePNRAddMultiElement();
        amadeusLogger.debug("Ignore PNR Add MultiElements Request {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrAddMultiElements));
        PNRReply pnrReply = mPortType.pnrAddMultiElements(pnrAddMultiElements, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("Ignore PNR Add MultiElements Response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrReply));

        return pnrReply;
    }

    //This Method Adds Special Service Request to the PNR
    public PNRReply addSSRDetailsToPNR(TravellerMasterInfo travellerMasterInfo, List<String> segmentNumbers, Map<String, String> travellerMap, AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("amadeus addSSRDetailsToPNR called   at {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());

        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().addSSRDetails(travellerMasterInfo, segmentNumbers, travellerMap);
        amadeusLogger.debug("Add SSR Details To PNR Request{} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrAddMultiElements));
        PNRReply pnrReply = mPortType.pnrAddMultiElements(pnrAddMultiElements, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("Add SSR Details To PNR Response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrReply));

        return pnrReply;
    }

    //This method for issuance of Ticket
    public DocIssuanceIssueTicketReply issueTicket(boolean sendTSTDataForIssuance, List<String> tstReferenceList, AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("Amadeus issueTicket called at {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());

        DocIssuanceIssueTicket docIssuanceIssueTicket = new IssueTicket().issue(sendTSTDataForIssuance, tstReferenceList);
        amadeusLogger.debug("DocIssuanceIssueTicket Request - Issue Ticket -  {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(docIssuanceIssueTicket));
        DocIssuanceIssueTicketReply docIssuanceIssueTicketReply = mPortType.docIssuanceIssueTicket(docIssuanceIssueTicket, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("DocIssuanceIssueTicket Response - Issue Ticket -  {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(docIssuanceIssueTicketReply));

        return docIssuanceIssueTicketReply;
    }

    //This method gets the Fare information without PNR
    public FareInformativePricingWithoutPNRReply getFareInfo(List<Journey> journeys, boolean seamen, int adultCount, int childCount, int infantCount, List<PAXFareDetails> paxFareDetailsList, AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("Amadeus getFareInfo called at {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());

        FareInformativePricingWithoutPNR farePricingWithoutPNR = new FareInformation().getFareInfo(journeys, seamen, adultCount, childCount, infantCount, paxFareDetailsList);
        amadeusLogger.debug("FareInformativePricingWithoutPNR -Get Fare Info- Request {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(farePricingWithoutPNR));
        FareInformativePricingWithoutPNRReply fareInformativePricingPNRReply = mPortType.fareInformativePricingWithoutPNR124(farePricingWithoutPNR, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("FareInformativePricingWithoutPNR -Get Fare Info- Response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(fareInformativePricingPNRReply));

        return fareInformativePricingPNRReply;
    }

    //This Method gets the flight info
    public AirFlightInfoReply getFlightInfo(AirSegmentInformation airSegment, AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("Amadeus getFlightInfo  called at {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());

        AirFlightInfo airFlightInfo = new FlightInformation().getAirFlightInfo(airSegment);
        amadeusLogger.debug("AirFlightInfo Request {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(airFlightInfo));
        AirFlightInfoReply airFlightInfoReply = mPortType.airFlightInfo(airFlightInfo, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("AirFlightInfo Response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(airFlightInfo));

        return airFlightInfoReply;
    }

    //This method get the Face Rules in String Format (Category 16)
    public FareCheckRulesReply getFareRules(AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("Amadeus getFareRules called at {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());

        FareCheckRules fareCheckRules = new FareRules().createFareRules();
        amadeusLogger.debug("FareCheckRules Request {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(fareCheckRules));
        FareCheckRulesReply fareCheckRulesReply = mPortType.fareCheckRules(fareCheckRules, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("FareCheckRules Response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(fareCheckRulesReply));

        return fareCheckRulesReply;
    }


    //Fetching fare components for different fare components of a Booking
    public FareCheckRulesReply getFareRulesFromFareComponent(AmadeusSessionWrapper amadeusSessionWrapper, Map<String, String> fareComponentsMap) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("amadeus getFareRulesFromFareComponent called at {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());

        FareCheckRules fareCheckRules = new FareRules().getFareCheckRulesForFareComponents(fareComponentsMap);
        amadeusLogger.debug("fareRulesFromFareComponentsReq {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(fareCheckRules));
        FareCheckRulesReply fareCheckRulesReply = mPortType.fareCheckRules(fareCheckRules, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("fareRulesFromFareComponentsRes {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(fareCheckRulesReply));

        return fareCheckRulesReply;
    }

    //Getting Fare check rules for one fare component
    public FareCheckRulesReply getFareRulesForFCType(String fcNumber, AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("Amadeus getFareRulesForFCType called at {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());

        FareCheckRules fareCheckRules = new FareRules().getFareInfoForFCType(fcNumber);
        amadeusLogger.debug("FareCheckRules -FareRulesForFCType- Request{} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(fareCheckRules));
        FareCheckRulesReply fareCheckRulesReply = mPortType.fareCheckRules(fareCheckRules, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("FareCheckRules -FareRulesForFCType- Response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(fareCheckRulesReply));

        return fareCheckRulesReply;
    }


    //This method gets the fare for the future dates
    public FarePricePNRWithLowestFareReply getLowestFare(String carrrierCode, PNRReply pnrReply, boolean isSeamen, boolean isDomesticFlight, FlightItinerary flightItinerary, List<AirSegmentInformation> airSegmentList, boolean isSegmentWisePricing, AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("Amadeus getLowestFare called at {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());

        FarePricePNRWithLowestFare farePricePNRWithLowestFare;
        if (isSegmentWisePricing) {
            farePricePNRWithLowestFare = new LowestPricePNR().getPNRPricingOption(carrrierCode, pnrReply, isSeamen,
                    isDomesticFlight, flightItinerary, airSegmentList, true);
        } else {
            if (isSeamen) {
                farePricePNRWithLowestFare = new PricePNRLowestFare().getPricePNRWithLowestSeamenFare();
            } else {
                farePricePNRWithLowestFare = new PricePNRLowestFare().getPricePNRWithLowestNonSeamenFare();
            }
        }

        amadeusLogger.debug("FarePricePNRWithLowestFare Request {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(farePricePNRWithLowestFare));
        FarePricePNRWithLowestFareReply farePricePNRWithLowestFareReply = mPortType.farePricePNRWithLowestFare124(farePricePNRWithLowestFare, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("FarePricePNRWithLowestFare Response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(farePricePNRWithLowestFareReply));

        return farePricePNRWithLowestFareReply;
    }

    //This Method Exits the ESX entry
    public PNRReply exitESPnr(PNRCancel pnrCancel, AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("Amadeus exitESPNR called  at {}................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());

        amadeusLogger.debug("Exit ESX Entry Request {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrCancel));
        PNRReply pnrReply = mPortType.pnrCancel(pnrCancel, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("Exit ESX Entry Response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrReply));

        return pnrReply;
    }


    //This Method Cancels a PNR
    public PNRReply cancelPNR(String pnr, PNRReply gdsPNRReply, AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("Amadeus cancelPNR called  at {}................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());

        PNRCancel pnrCancel = new PNRCancel();
        OptionalPNRActionsType pnrActionsType = new OptionalPNRActionsType();
        pnrActionsType.getOptionCode().add(BigInteger.valueOf(0));
        pnrCancel.setPnrActions(pnrActionsType);
        CancelPNRElementType cancelPNRElementType = new CancelPNRElementType();
        List<ElementIdentificationType> elementIdentificationTypeList = cancelPNRElementType.getElement();
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
        cancelPNRElementType.setEntryType(AmadeusConstants.CANCEL_PNR_ELEMENT_TYPE);
        pnrCancel.getCancelElements().add(cancelPNRElementType);

        amadeusLogger.debug("Cancel PNR Request {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrCancel));
        PNRReply pnrReply = mPortType.pnrCancel(pnrCancel, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("Cancel PNR Response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrReply));

        return pnrReply;

    }

    public QueueListReply queueListResponse(QueueList queueListReq, AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("Amadeus queueListResponse called at {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());

        amadeusLogger.debug("QueueList Request{} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(queueListReq));
        QueueListReply queueListReply = mPortType.queueList(queueListReq, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("QueueList Response{} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(queueListReply));

        return queueListReply;
    }

    //This Method gets the TST
    public TicketDisplayTSTReply ticketDisplayTST(AmadeusSessionWrapper amadeusSessionWrapper) {

        logger.debug("Amadeus ticketDisplayTST called at {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);

        TicketDisplayTST ticketDisplayTST = new CreateTST().createTicketDisplayTSTReq();
        amadeusLogger.debug("TicketDisplayTST Request{} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketDisplayTST));
        TicketDisplayTSTReply ticketDisplayTSTReply = mPortType.ticketDisplayTST(ticketDisplayTST, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("TicketDisplayTST Response{} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketDisplayTSTReply));

        return ticketDisplayTSTReply;
    }

    //This Methos Splits a PNR
    public PNRReply splitPNR(PNRSplit pnrSplit, AmadeusSessionWrapper amadeusSessionWrapper) {

        logger.debug("Amadeus splitPNR called at {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);

        amadeusLogger.debug("Split PNR Request{} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrSplit));
        PNRReply pnrReply = mPortType.pnrSplit(pnrSplit, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("Split PNR Response{} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrReply));

        return pnrReply;
    }

    //This Method gets the Mini Rule for the PNR
    public MiniRuleGetFromRecReply retrieveMiniRuleFromPNR(AmadeusSessionWrapper amadeusSessionWrapper, String pnr) {

        try {
            logger.debug("Amadeus retrieveMiniRuleFromPNR called at {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());
            amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);

            MiniRuleGetFromRec miniRuleGetFromPricingRec = new PNRRetriev().miniRuleGetFromPNR(pnr);
            amadeusLogger.debug("MiniRuleGetFromRec Request {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(miniRuleGetFromPricingRec));
            MiniRuleGetFromRecReply miniRuleGetFromPricingRecReply = mPortType.miniRuleGetFromRec(miniRuleGetFromPricingRec, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
            amadeusLogger.debug("MiniRuleGetFromRec Response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(miniRuleGetFromPricingRecReply));

            return miniRuleGetFromPricingRecReply;
        } catch (Exception e) {
            logger.error("Error in retrieveMiniRuleFromPNR:{}", e.getMessage());
        }
        return null;
    }

    //This method gets the Mini Rule from the pricing information
    public MiniRuleGetFromRecReply retrieveMiniRuleFromPricing(AmadeusSessionWrapper amadeusSessionWrapper) {

        logger.debug("Amadeus retrievePNR  called at {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);

        MiniRuleGetFromRec miniRuleGetFromPricing = new PNRRetriev().miniRuleGetFromPricing();
        amadeusLogger.debug("MiniRuleGetFromRec - From Pricing - Request{} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(miniRuleGetFromPricing));
        MiniRuleGetFromRecReply miniRuleGetFromPricingReply = mPortType.miniRuleGetFromRec(miniRuleGetFromPricing, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("MiniRuleGetFromRec - From Pricing - Response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(miniRuleGetFromPricingReply));

        return miniRuleGetFromPricingReply;
    }


    //This Method cancels the whole PNR
    public PNRReply cancelFullPNR(String pnr, PNRReply gdsPNRReply, AmadeusSessionWrapper amadeusSessionWrapper, Boolean setReservationInfo) {

        logger.debug("Amadeus cancelFullPNR called  at {}................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);

        PNRCancel pnrCancel = new PNRCancel();
        if (setReservationInfo) {
            com.amadeus.xml.pnrxcl_14_1_1a.ReservationControlInformationDetailsTypeI reservationControlInformationDetailsTypeI = new com.amadeus.xml.pnrxcl_14_1_1a.ReservationControlInformationDetailsTypeI();
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


        amadeusLogger.debug("PNRCancel -Full Cancel - Request{} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrCancel));
        PNRReply pnrReply = mPortType.pnrCancel(pnrCancel, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("PNRCancel -Full Cancel - Response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrReply));

        return pnrReply;
    }

    //This method is called to check the ticket status for Refund
    public TicketProcessEDocReply ticketProcessEDoc(List<String> tickets, AmadeusSessionWrapper amadeusSessionWrapper) {

        logger.debug("Amadeus ticketProcessEdoc called  at {}................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);

        TicketProcessEDoc ticketProcessEDocRQ = new RefundTicket().getTicketProcessEdocRQ(tickets);
        amadeusLogger.debug("TicketProcessEDoc - Refund - Request {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketProcessEDocRQ));
        TicketProcessEDocReply ticketProcessEDocReply = mPortType.ticketProcessEDoc(ticketProcessEDocRQ, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("TicketProcessEDoc - Refund - Response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketProcessEDocReply));

        return ticketProcessEDocReply;
    }

    //This method creates a ticket cancel document
    public TicketCancelDocumentReply ticketCancelDocument(String pnr, List<String> ticketsList, PNRReply gdsPNRReply, String delOficID, AmadeusSessionWrapper amadeusSessionWrapper) {

        logger.debug("Ticket cancel document called  at {}................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);

        // Creating XML request for ticket cancel document
        TicketCancelDocument ticketCancelDocument = new TicketCancelDocumentHandler().ticketCancelDocument(ticketsList, gdsPNRReply, delOficID);
        amadeusLogger.debug("TicketCancelDocument Request {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketCancelDocument));
        TicketCancelDocumentReply ticketCancelDocumentReply = mPortType.ticketCancelDocument(ticketCancelDocument, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("TicketCancelDocument Response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketCancelDocumentReply));

        return ticketCancelDocumentReply;

    }

    //This Method checks the Status of the ticket to be reissued
    public TicketProcessEDocReply reIssueCheckTicketStatus(ReIssueSearchRequest reIssueSearchRequest, AmadeusSessionWrapper amadeusSessionWrapper) {

        logger.debug("TicketProcessEDoc for Reissue called at {}................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);

        TicketProcessEDoc ticketProcessEDocReq = ReIssueTicket.ReIssueCheckTicketStatus.createReissueTicketStatusCheck(reIssueSearchRequest);
        amadeusLogger.info("ReIssue Ticket Status Check(TicketProcessEDoc) Request {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketProcessEDocReq));
        TicketProcessEDocReply ticketProcessEDocReply = mPortType.ticketProcessEDoc(ticketProcessEDocReq, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.info("ReIssue Ticket Status Check(TicketProcessEDoc) Response {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketProcessEDocReply));

        return ticketProcessEDocReply;

    }

    //This method checks if the ticket number/ numbers are eligible for reissue
    public TicketCheckEligibilityReply reIssueTicketCheckEligibility(ReIssueSearchRequest reIssueSearchRequest, AmadeusSessionWrapper amadeusSessionWrapper) {

        logger.debug("TicketCheckEligibility for Reissue called  at {}................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);

        TicketCheckEligibility ticketCheckEligibility = ReIssueTicket.ReIssueCheckEligibility.createCheckEligibilityRequest(reIssueSearchRequest, amadeusSessionWrapper.getOfficeId());
        amadeusLogger.debug("ReIssue TicketCheckEligibility Request {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketCheckEligibility));
        TicketCheckEligibilityReply ticketCheckEligibilityReply = mPortType.ticketCheckEligibility(ticketCheckEligibility, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("ReIssue TicketCheckEligibility Response {} SessionId: {}  \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketCheckEligibilityReply));

        return ticketCheckEligibilityReply;
    }

    //This Method runs a ATC search to fetch flights for reissue
    public TicketATCShopperMasterPricerTravelBoardSearchReply reIssueATCAirlineSearch(ReIssueSearchRequest reissueSearchRequest, TravelFlightInformationType allowedCarriers, AmadeusSessionWrapper amadeusSessionWrapper) {

        logger.debug("TicketATCShopperMasterPricerTravelBoardSearch for Reissue called  at {}................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);

        TicketATCShopperMasterPricerTravelBoardSearch reIssueATCSearchRequest = ReIssueTicket.ReIssueATCSearch.createReissueATCSearchRequest(reissueSearchRequest, allowedCarriers, amadeusSessionWrapper.getOfficeId());
        amadeusLogger.debug("ReIssueATCSearch Request on {}, SessionId: {}, Office ID: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), amadeusSessionWrapper.getOfficeId(), new XStream().toXML(reIssueATCSearchRequest));
        TicketATCShopperMasterPricerTravelBoardSearchReply reIssueATCSearchReply = mPortType.ticketATCShopperMasterPricerTravelBoardSearch(reIssueATCSearchRequest, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("ReIssueATCSearch Response {} SessionId: {}, Office ID: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), amadeusSessionWrapper.getOfficeId(), new XStream().toXML(reIssueATCSearchReply));

        return reIssueATCSearchReply;

    }

    //Deprecated(Using Integrated catalogue here)
    public ServiceIntegratedCatalogueReply getAdditionalBaggageInformationAmadeus(AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        ServiceIntegratedCatalogue serviceIntegratedCatalogue = AncillaryServiceReq.AdditionalPaidBaggage.createShowAdditionalBaggageInformationRequest();

        amadeusLogger.debug("ServiceIntegratedCatalogue Additional Baggage Request {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(serviceIntegratedCatalogue));
        ServiceIntegratedCatalogueReply serviceIntegratedCatalogueReply = mPortType.serviceIntegratedCatalogue(serviceIntegratedCatalogue, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("ServiceIntegratedCatalogue Additional Baggage Response {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(serviceIntegratedCatalogueReply));

        return serviceIntegratedCatalogueReply;

    }

    //This method gets the baggage catalogue
    public ServiceStandaloneCatalogueReply getAdditionalBaggageInfoStandalone(AmadeusSessionWrapper amadeusSessionWrapper, AncillaryServiceRequest ancillaryServiceRequest) {

        logger.debug("ServiceStandaloneCatalogue Baggage called  at {}................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);

        ServiceStandaloneCatalogue serviceStandaloneCatalogue = AncillaryServiceReq.AdditionalPaidBaggage.createShowAdditionalBaggageInformationRequestStandalone(ancillaryServiceRequest);
        amadeusLogger.debug("ServiceStandaloneCatalogue Additional Baggage Request {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(serviceStandaloneCatalogue));
        ServiceStandaloneCatalogueReply serviceStandaloneCatalogueReply = mPortType.serviceStandaloneCatalogue(serviceStandaloneCatalogue, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("ServiceStandaloneCatalogue Additional Baggage Response {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(serviceStandaloneCatalogueReply));

        return serviceStandaloneCatalogueReply;

    }

    //This method gets the meal catalogue
    public ServiceStandaloneCatalogueReply getMealsInfoStandalone(AmadeusSessionWrapper amadeusSessionWrapper, AncillaryServiceRequest ancillaryServiceRequest) {

        logger.debug("ServiceStandaloneCatalogue Meals called  at {}................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);

        ServiceStandaloneCatalogue serviceStandaloneCatalogue = AncillaryServiceReq.AdditionalPaidBaggage.createShowMealsInformationRequestStandalone(ancillaryServiceRequest);
        amadeusLogger.debug("ServiceStandaloneCatalogue Meals Request {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(serviceStandaloneCatalogue));
        ServiceStandaloneCatalogueReply serviceStandaloneCatalogueReply = mPortType.serviceStandaloneCatalogue(serviceStandaloneCatalogue, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("ServiceStandaloneCatalogue Meals Response {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(serviceStandaloneCatalogueReply));

        return serviceStandaloneCatalogueReply;

    }

    //Partial cancellation
    public PNRReply partialCancelPNR(String pnr, PNRReply gdsPNRReply, Map<BigInteger, String> segmentMap, AmadeusSessionWrapper amadeusSessionWrapper) {

        logger.debug("Amadeus partialCancelPNR called  at {}................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);

        PNRCancel pnrCancel = new PNRCancel();
        OptionalPNRActionsType pnrActionsType = new OptionalPNRActionsType();
        pnrActionsType.getOptionCode().add(BigInteger.valueOf(11));
        pnrCancel.setPnrActions(pnrActionsType);
        CancelPNRElementType cancelPNRElementType = new CancelPNRElementType();
        List<ElementIdentificationType> elementIdentificationTypeList = cancelPNRElementType.getElement();
        if (!segmentMap.isEmpty()) {
            // Iterating over the Map
            ElementIdentificationType elementIdentificationType = new ElementIdentificationType();
            for (Map.Entry<BigInteger, String> entry : segmentMap.entrySet()) {
                BigInteger key = entry.getKey();
                String value = entry.getValue();
                elementIdentificationType.setNumber(key);
                elementIdentificationType.setIdentifier(value);
                elementIdentificationTypeList.add(elementIdentificationType);
            }
        } else {
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

        amadeusLogger.debug("Partial cancellation Request {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrCancel));
        PNRReply pnrReply = mPortType.pnrCancel(pnrCancel, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("Partial cancellation Response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrReply));

        return pnrReply;

    }

    //Mostly deprecated as reissue is using different API's
    public TicketRepricePNRWithBookingClassReply repricePNRWithBookingClassReply(AmadeusSessionWrapper amadeusSessionWrapper, TravellerMasterInfo travellerMasterInfo, List<String> tickets) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        TicketRepricePNRWithBookingClass ticketRepricePNRWithBookingClass = ReIssueTicket.getTicketRepricePNRWithBookingClass(travellerMasterInfo, tickets);

        amadeusLogger.debug("TicketRepricePNRWithBookingClass Request {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketRepricePNRWithBookingClass));
        TicketRepricePNRWithBookingClassReply repricePNRWithBookingClassReply = mPortType.ticketRepricePNRWithBookingClass(ticketRepricePNRWithBookingClass, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("TicketRepricePNRWithBookingClassReply Response {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(repricePNRWithBookingClassReply));

        return repricePNRWithBookingClassReply;
    }

    //Mostly deprecated as reissue is using different API's
    public TicketReissueConfirmedPricingReply ticketReissueConfirmedPricingReply(AmadeusSessionWrapper amadeusSessionWrapper, List<String> tickets) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        TicketReissueConfirmedPricing ticketReissueConfirmedPricing = ReIssueTicket.getTicketReissueConfirmedPricing(tickets);

        amadeusLogger.debug("TicketReissueConfirmedPricing Request {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketReissueConfirmedPricing));
        TicketReissueConfirmedPricingReply ticketReissueConfirmedPricingReply = mPortType.ticketReissueConfirmedPricing(ticketReissueConfirmedPricing, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("TicketReissueConfirmedPricingReply Response {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketReissueConfirmedPricingReply));

        return ticketReissueConfirmedPricingReply;
    }

    //This method gets the ticket status for open ticket report
    public TicketProcessEDocReply ticketProcessEDocReply(AmadeusSessionWrapper amadeusSessionWrapper, List<OpenTicketDTO> openTicketDTOS) {

        logger.debug("Amadeus Open Ticket Report called  at {}................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);

        TicketProcessEDoc ticketProcessEDoc = OpenTicketReport.OpenTicketReportRequest.createOpenTicketRequest(openTicketDTOS);
        amadeusLogger.debug("Open Ticket Request Request {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketProcessEDoc));
        TicketProcessEDocReply ticketProcessEDocReply = mPortType.ticketProcessEDoc(ticketProcessEDoc, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("Open Ticket Response body {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(ticketProcessEDocReply));

        return ticketProcessEDocReply;
    }

    //This method splits the PNR for which reissue is to be performed.
    public PNRReply splitPNRForReissue(ReIssueConfirmationRequest reIssueConfirmationRequest, AmadeusSessionWrapper amadeusSessionWrapper) {

        logger.debug("Split PNR called for reissue booking  at {}................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);

        PNRSplit pnrSplit = ReIssueTicket.ReIssueConfirmation.splitPNRForReIssuedPax(reIssueConfirmationRequest);
        amadeusLogger.debug("Splitting PNR for ReIssue Request Body {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrSplit));
        PNRReply pnrReply = mPortType.pnrSplit(pnrSplit, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("Splitting PNR for ReIssue Response Body {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrReply));

        return pnrReply;
    }

    //This uses the latest pricing API
    public com.amadeus.xml.tipnrr_13_2_1a.FareInformativePricingWithoutPNRReply getFareInfo_32(List<Journey> journeys, boolean seamen, int adultCount, int childCount, int infantCount, List<PAXFareDetails> paxFareDetailsList, AmadeusSessionWrapper amadeusSessionWrapper) {

        logger.debug("amadeus getFareInfo 13.2 called at {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);

        com.amadeus.xml.tipnrq_13_2_1a.FareInformativePricingWithoutPNR farePricingWithoutPNR = new FareInformation13_2().getPriceInfo(journeys, seamen, adultCount, childCount, infantCount, paxFareDetailsList);
        amadeusLogger.debug("farePricingWithoutPNRReq  13.2 {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(farePricingWithoutPNR));
        com.amadeus.xml.tipnrr_13_2_1a.FareInformativePricingWithoutPNRReply fareInformativePricingPNRReply = mPortType.fareInformativePricingWithoutPNR(farePricingWithoutPNR, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("farePricingWithoutPNRRes 13.2 {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(fareInformativePricingPNRReply));
        return fareInformativePricingPNRReply;
    }

    //This method saves a PNR for Ancillary Payment
    public PNRReply savePNRForAncillaryServices(AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("Amadeus savePNR for Ancillary Payment called at {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());

        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().savePnrForAncillaryPayment();
        amadeusLogger.debug("Save PNR request for Ancillary Payment {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrAddMultiElements));
        PNRReply pnrReply = mPortType.pnrAddMultiElements(new PNRAddMultiElementsh().savePnrForAncillaryPayment(), amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("Save PNR Response for Ancillary Payment {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrReply));

        return pnrReply;
    }

    //Pricing for split ticket flows are done here
    public FarePricePNRWithBookingClassReply priceSplitTicketPNR(String carrierCode, PNRReply pnrReply, boolean isSeamen, boolean isDomesticFlight, FlightItinerary flightItinerary, List<AirSegmentInformation> airSegmentList, boolean isSegmentWisePricing, AmadeusSessionWrapper amadeusSessionWrapper, boolean isAddBooking, int journeyIndex) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("Amadeus priceSplitTicketPNR called at {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());

        FarePricePNRWithBookingClass pricePNRWithBookingClass = new PricePNR().getPNRPricingOption(carrierCode, pnrReply, isSeamen, isDomesticFlight, flightItinerary, airSegmentList, isSegmentWisePricing, isAddBooking, true, journeyIndex);
        amadeusLogger.debug("Amadeus price SplitTicket PNR WithBookingClass Request {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pricePNRWithBookingClass));

        FarePricePNRWithBookingClassReply pricePNRWithBookingClassReply = mPortType.farePricePNRWithBookingClass124(pricePNRWithBookingClass, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("Amadeus price SplitTicket PNR WithBookingClass Response {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pricePNRWithBookingClassReply));

        return pricePNRWithBookingClassReply;
    }


    //This method is used to add Joco Pnr post booking success PNR
    public PNRReply addJocoPnrBookingInfoToPNR(String jocoPnr, AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        logger.debug("Amadeus Add Joco PNR Info to PNR called   at {}....................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());

        PNRAddMultiElements pnrAddMultiElements = new PNRAddMultiElementsh().addJocoPnrNumberEntryToGdsPnr(jocoPnr);
        amadeusLogger.debug("PNRAddMultiElements Request -- Add Joco PNR Info to PNR {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrAddMultiElements));

        PNRReply pnrReply = mPortType.pnrAddMultiElements(pnrAddMultiElements, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("PNRReply -- Add Joco PNR Info to PNR {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(pnrReply));

        return pnrReply;
    }


}