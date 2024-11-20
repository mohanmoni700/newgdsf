package controllers;

import com.compassites.GDSWrapper.mystifly.AirMessageQueue;
import com.compassites.model.*;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.compassites.model.travelomatrix.ResponseModels.UpdatePNR.UpdatePNRResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import dto.reissue.ReIssueSearchRequest;
import models.MiniRule;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirMessageQueueRS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import services.*;

import services.ancillary.AncillaryService;
import services.reissue.ReIssueService;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static com.compassites.constants.StaticConstatnts.CANCEL_PNR;
import static com.compassites.constants.StaticConstatnts.VOID_TICKET;
import static play.mvc.Controller.request;
import static play.mvc.Results.ok;

@org.springframework.stereotype.Controller
public class Application {

    @Autowired
    private FlightSearchWrapper flightSearchWrapper;

    @Autowired
    private MergeSearchResults mergeSearchResults;

    @Autowired
    private BookingServiceWrapper bookingService;

    @Autowired
    private FlightInfoServiceWrapper flightInfoService;

    @Autowired
    private CancelServiceWrapper cancelService;

    @Autowired
    private QueueListServiceWrapper queueListServiceWrapper;

    @Autowired
    private AmadeusBookingServiceImpl amadeusBookingService;

    @Autowired
    private MystiflyBookingServiceImpl mystiflyBookingService;

    @Autowired
    private TraveloMatrixBookingServiceImpl traveloMatrixBookingService;

    @Autowired
    private AncillaryService ancillaryService;

    @Autowired
    private ReIssueService reIssueService;

    @Autowired
    private RefundServiceWrapper refundServiceWrapper;

    @Autowired
    private AmadeusTicketCancelDocumentServiceImpl amadeusTicketCancelDocumentServiceImpl;


    static Logger logger = LoggerFactory.getLogger("gds");

    public Result flightSearch(){
        logger.debug("Request recieved");
        String json = request().body().asText();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JodaModule());
        SearchParameters  searchParameters = null;
        try {
            searchParameters = mapper.readValue(json, SearchParameters.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        SearchParameters  searchParameters = Json.fromJson(json, SearchParameters.class);
        logger.debug("SearchParamerters: " + json.toString());
        flightSearchWrapper.search(searchParameters);
//        mergeSearchResults.searchAndMerge(searchParameters);
        return Controller.ok(Json.toJson(searchParameters.redisKey()));
    }

    public Result generatePNR(){
        JsonNode json = request().body().asJson();
        logger.debug("----------------- generatePNR PNR Request: " + json);
        TravellerMasterInfo travellerMasterInfo = Json.fromJson(json, TravellerMasterInfo.class);
        PNRResponse pnrResponse = bookingService.generatePNR(travellerMasterInfo);
        logger.debug("-----------------PNR Response: " + Json.toJson(pnrResponse));
        return Controller.ok(Json.toJson(pnrResponse));
    }

    public Result generateTempPNR() {
        JsonNode json = request().body().asJson();
        logger.debug("----------------- generateTempPNR PNR Request: " + json);
        TravellerMasterInfo travellerMasterInfo = Json.fromJson(json, TravellerMasterInfo.class);
        PNRResponse pnrResponse = bookingService.createTempPNR(travellerMasterInfo);
        logger.debug("-----------------PNR Response: " + Json.toJson(pnrResponse));
        return Controller.ok(Json.toJson(pnrResponse));
    }

    public Result splitPNR(){
        logger.debug("-----------------splitPNR called: ");
        JsonNode json = request().body().asJson();
        logger.debug("split PNR........ " + Json.toJson(json));
        IssuanceRequest issuanceRequest = Json.fromJson(json, IssuanceRequest.class);
        logger.debug("split PNR called " + Json.toJson(issuanceRequest));
        logger.debug("getProvider..."+issuanceRequest.getProvider());
        SplitPNRResponse splitPNRResponse = bookingService.splitPNR(issuanceRequest,CANCEL_PNR);
        logger.debug("-----------------splitPNR Response: " + Json.toJson(splitPNRResponse));
        return ok(Json.toJson(splitPNRResponse));
    }

    public Result ticketCancelDocuments() {
        logger.info("ticketCancelDocuments called ");
        JsonNode json = request().body().asJson();
        IssuanceRequest issuanceRequest = Json.fromJson(json, IssuanceRequest.class);
        SplitPNRResponse splitPNRResponse = bookingService.splitPNR(issuanceRequest, VOID_TICKET);
        logger.debug("-----------------splitPNR Response: " + Json.toJson(splitPNRResponse));
        return ok(Json.toJson(splitPNRResponse));
    }
    public Result checkFareChangeAndAvailability(){
        JsonNode json = request().body().asJson();
        logger.debug("----------------- checkFareChangeAndAvailability PNR Request: " + json);
        TravellerMasterInfo travellerMasterInfo = Json.fromJson(json, TravellerMasterInfo.class);
        PNRResponse pnrResponse = bookingService.checkFareChangeAndAvailability(travellerMasterInfo);
        logger.debug("-----------------PNR Response: " + Json.toJson(pnrResponse));
        return Controller.ok(Json.toJson(pnrResponse));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result priceChangePNR(){
        JsonNode json = request().body().asJson();
        logger.debug("----------------- priceChangePNR PNR Request: " + json);
        TravellerMasterInfo travellerMasterInfo = Json.fromJson(json, TravellerMasterInfo.class);
        PNRResponse pnrResponse = bookingService.priceChangePNR(travellerMasterInfo);
        logger.debug("-----------------PNR Response: " + Json.toJson(pnrResponse));
        return Controller.ok(Json.toJson(pnrResponse));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result getBaggageInfo() {

    	JsonNode json = request().body().asJson();
    	SearchParameters searchParams = Json.fromJson(json.findPath("searchParams"), SearchParameters.class);
    	FlightItinerary flightItinerary = Json.fromJson(json.findPath("flightItinerary"), FlightItinerary.class);
    	String provider = json.get("provider").asText();
    	Boolean seamen = Json.fromJson(json.findPath("travellerInfo").findPath("seamen"), Boolean.class);
    	FlightItinerary response = null;
    	try {
    		response = flightInfoService.getBaggageInfo(flightItinerary, searchParams, provider, seamen);
		} catch (Exception e) {
			e.printStackTrace();
		}

    	return Controller.ok(Json.toJson(response));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result getCancellationFee() {
    	JsonNode json = request().body().asJson();
    	SearchParameters searchParams = Json.fromJson(json.findPath("searchParams"), SearchParameters.class);
    	FlightItinerary flightItinerary = Json.fromJson(json.findPath("flightItinerary"), FlightItinerary.class);
    	String provider = json.get("provider").asText();
    	Boolean seamen = Json.fromJson(json.findPath("travellerInfo").findPath("seamen"), Boolean.class);

    	String fareRules = flightInfoService.getCancellationFee(flightItinerary, searchParams, provider, seamen);
    	return Controller.ok(Json.toJson(fareRules));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result getMiniRuleFromFlightItenary() {

        List<HashMap> miniRules = new ArrayList<>();
        JsonNode json = request().body().asJson();
        SearchParameters searchParams = Json.fromJson(json.findPath("searchParams"), SearchParameters.class);
        FlightItinerary flightItinerary = Json.fromJson(json.findPath("flightItinerary"), FlightItinerary.class);
        String provider = json.get("provider").asText();
        Boolean seamen = Json.fromJson(json.findPath("travellerInfo").findPath("seamen"), Boolean.class);
        Boolean isBenzyFare = amadeusBookingService.isBenzyFare(flightItinerary,seamen);
        if(isBenzyFare){
            miniRules = flightInfoService.getGenericFareRuleFlightItenary(flightItinerary,searchParams,seamen,provider);
        }else {
            miniRules = flightInfoService.getMiniRuleFeeFromFlightItenary(flightItinerary, searchParams, provider, seamen);
        }
        return Controller.ok(Json.toJson(miniRules));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result getMiniRuleFromPNR() {
        JsonNode json = request().body().asJson();
        List<HashMap> miniRules = new ArrayList<>();
        String provider = json.get("provider").asText();
        String pnr = json.get("pnr").asText();
        if (PROVIDERS.AMADEUS.toString().equalsIgnoreCase(provider)) {
            miniRules = amadeusBookingService.getMiniRuleFeeFromPNR(pnr);
        }
        return Controller.ok(Json.toJson(miniRules));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result getMiniRuleFromEticket() {
        MiniRule miniRule = new MiniRule();
        JsonNode json = request().body().asJson();
        String provider = json.get("provider").asText();
        String pnr = json.get("pnr").asText();
        String Eticket = json.get("eticket").asText();
        if(PROVIDERS.AMADEUS.toString().equalsIgnoreCase(provider)){
            miniRule = amadeusBookingService.getMiniRuleFeeFromEticket(pnr,Eticket,miniRule);
        }
        return Controller.ok(Json.toJson(miniRule));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result getFlightDetails() {
    	JsonNode json = request().body().asJson();
    	FlightItinerary flightItinerary = Json.fromJson(json.findPath("flightItinerary"), FlightItinerary.class);
    	String provider = json.get("provider").asText();
    	Boolean seamen = Json.fromJson(json.findPath("travellerInfo").findPath("seamen"), Boolean.class);
    	FlightItinerary response = flightInfoService.getInFlightDetails(flightItinerary, provider, seamen);
    	return Controller.ok(Json.toJson(response));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result issueTicket(){
        JsonNode json = request().body().asJson();
        IssuanceRequest issuanceRequest = Json.fromJson(json, IssuanceRequest.class);
        logger.debug("issueTicket request : " + json);
        IssuanceResponse issuanceResponse = bookingService.issueTicket(issuanceRequest);
        logger.debug("-----------------IssuanceResponse:\n" + Json.toJson(issuanceResponse));
        return ok(Json.toJson(issuanceResponse));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result getPnrDetails(){
    	JsonNode json = request().body().asJson();
    	IssuanceRequest issuanceRequest = Json.fromJson(json, IssuanceRequest.class);
    	String gdsPNR = issuanceRequest.getGdsPNR();
    	String provider = issuanceRequest.getProvider();
        logger.debug("==== in Application RQ ==== >>>>>>" + Json.toJson(issuanceRequest));
        TravellerMasterInfo masterInfo = bookingService.getPnrDetails(issuanceRequest, gdsPNR, provider);
    	logger.debug("==== in Application INFO ==== >>>>>>" + Json.toJson(masterInfo));
		return ok(Json.toJson(masterInfo));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result getBookingDetails() {
    	JsonNode json = request().body().asJson();
    	String pnr = Json.fromJson(json.findPath("gdsPNR"), String.class);
        String provider = Json.fromJson(json.findPath("provider"), String.class);
        logger.debug("getBookingDetails request : "+ json);
        JsonNode res = bookingService.getBookingDetails(provider, pnr);
        logger.debug("getBookingDetails response =>>>>>>>>>>>> " + res);
		return ok(res);
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result getBookingDetailsByOfficeId() {
    	JsonNode json = request().body().asJson();
    	String pnr = Json.fromJson(json.findPath("gdsPNR"), String.class);
        String provider = Json.fromJson(json.findPath("provider"), String.class);
        String officeId = Json.fromJson(json.findPath("officeId"), String.class);
        logger.debug("getBookingDetails request : "+ json);
        logger.debug("officeId : "+ officeId);
        JsonNode res = bookingService.getBookingDetailsByOfficeId(provider, pnr, officeId);
        logger.debug("getBookingDetails response =>>>>>>>>>>>> " + res);
		return ok(res);
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result getLowestFare() {
    	JsonNode json = request().body().asJson();
        String pnr = Json.fromJson(json.findPath("gdsPNR"), String.class);
        String provider = Json.fromJson(json.findPath("provider"), String.class);
        Boolean isSeamen = Json.fromJson(json.findPath("isSeamen"), Boolean.class);
        IssuanceRequest issuanceRequest = Json.fromJson(json.findPath("issuanceRequest"), IssuanceRequest.class);
    	LowFareResponse lowestFare = bookingService.getLowestFare(issuanceRequest);
    	logger.debug("-----------------LowestFare:\n" + Json.toJson(lowestFare));
    	return ok(Json.toJson(lowestFare));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result commitBooking(){
        JsonNode json = request().body().asJson();
        IssuanceRequest issuanceRequest = Json.fromJson(json,IssuanceRequest.class);
        IssuanceResponse issuanceResponse = traveloMatrixBookingService.commitBooking(issuanceRequest);
        return ok(Json.toJson(issuanceResponse));
    }
    @BodyParser.Of(BodyParser.Json.class)
    public Result getFareRuleFromTmx() {
        List<HashMap> miniRules = new ArrayList<>();
        JsonNode json = request().body().asJson();
        //String resultToken = Json.fromJson(json,String.class);
        String resultToken = json.get("resultToken").asText();
        String returnResultToken = null;
        if(json.get("returnResultToken") != null)
        returnResultToken = json.get("returnResultToken").asText();
        miniRules = flightInfoService.getFareRuleFromTmx(resultToken,returnResultToken);
        return Controller.ok(Json.toJson(miniRules));
    }

    public Result cancelPNR(){
        logger.info("cancelPNR called ");
        JsonNode json = request().body().asJson();
        String pnr = Json.fromJson(json.findPath("gdsPNR"), String.class);
        String provider = Json.fromJson(json.findPath("provider"), String.class);
        String appRef = Json.fromJson(json.findPath("appRef"), String.class);
        String bookingId = Json.fromJson(json.findPath("bookingId"), String.class);
        Boolean fullPNR = Json.fromJson(json.findPath("fullPNR"), Boolean.class);
        logger.debug("Cacnel PNR called for PNR : " + pnr + " provider : " + provider);

        CancelPNRResponse cancelPNRResponse = cancelService.cancelPNR(pnr, provider,appRef,bookingId,fullPNR);

        logger.debug("cancel pnr response " + Json.toJson(cancelPNRResponse));
        return ok(Json.toJson(cancelPNRResponse));
    }

    public Result getQueueListInfo(){
        return ok(Json.toJson(queueListServiceWrapper.getQueueListResponse()));
    }

    public Result getBookingDetailsForPNR(){
        JsonNode json = request().body().asJson();
        logger.debug("getBookingDetailsForPNR request : "+ json);
        HashMap<String, Object> jsonMap = bookingService.getBookingDetailsForPNR(json);
        logger.debug("getBookingDetailsForPNR response : "+ jsonMap);
        return ok(Json.toJson(jsonMap));
    }


    //todo-- added for testing need to remove
    public Result displayTST(String pnr,String provider){
        JsonNode jsonNode = null;
        if(PROVIDERS.AMADEUS.toString().equalsIgnoreCase(provider)){
//            AmadeusBookingServiceImpl amadeusBookingService = new AmadeusBookingServiceImpl();
//        amadeusBookingService.getDisplayTicketDetails(pnr);
            jsonNode = amadeusBookingService.getBookingDetails(pnr);

        } else {
            TravelportBookingServiceImpl travelportBookingService = new TravelportBookingServiceImpl();
            jsonNode =  travelportBookingService.getBookingDetails(pnr);
        }

        return ok(jsonNode);

    }

    public Result displayPNR(String pnr,String provider) throws RemoteException{
        JsonNode jsonNode = null;
        if (PROVIDERS.MYSTIFLY.toString().equalsIgnoreCase(provider)){
            jsonNode = mystiflyBookingService.getBookingDetails(pnr);
        }
        return ok(jsonNode);

    }

    public Result priceBookedPNR(){
        JsonNode json = request().body().asJson();
        IssuanceRequest issuanceRequest = Json.fromJson(json, IssuanceRequest.class);
        logger.debug("priceBookedPNR issuance Request : " + json);
        IssuanceResponse issuanceResponse = bookingService.priceBookedPNR(issuanceRequest);
        logger.debug("-----------------IssuanceResponse:\n" + Json.toJson(issuanceResponse));
        return ok(Json.toJson(issuanceResponse));
    }

    public Result getTicktedMessage() throws RemoteException {
        JsonNode json = request().body().asJson();
        AirMessageQueue airMessageQueue = new AirMessageQueue();
        AirMessageQueueRS airMessageQueueRS = airMessageQueue.addMessage();
        MessageItems messageItems = getMessagesIitem(airMessageQueueRS);
        return ok(Json.toJson(messageItems));
    }

    public MessageItems getMessagesIitem(AirMessageQueueRS airMessageQueueRS){
        MessageItems messageItems = new MessageItems();
        List<MessageItem> messageItemList = new ArrayList<>();
        for ( org.datacontract.schemas._2004._07.mystifly_onepoint.MessageItem items : airMessageQueueRS.getMessageItems().getMessageItemArray()) {
            MessageItem messageItem = new MessageItem();
            messageItem.setBookingMode(items.getBookingMode());
            messageItem.setUniqueId(items.getUniqueID());
            messageItem.setMessage(items.getMessages().getStringArray(0));
            messageItem.setRph(items.getRPH());
            messageItem.setTkeTimeLimit(items.getTktTimeLimit().toString());
            messageItemList.add(messageItem);
        }
        messageItems.setMessageItem(messageItemList);
        return messageItems;
    }
    public Result getTripDetails() throws RemoteException{
        JsonNode json = request().body().asJson();
        IssuanceRequest issuanceRequest = Json.fromJson(json, IssuanceRequest.class);
        IssuanceResponse issuanceResponse = bookingService.readTripDetails(issuanceRequest);
        logger.debug("-----------------Trip Details Response:\n" + Json.toJson(issuanceResponse));
        return ok(Json.toJson(issuanceResponse));
    }

    public Result getMessagesFromQueue() throws RemoteException{
        JsonNode json = request().body().asJson();
        String category = Json.fromJson(json.findPath("queueCategory"), String.class);
        AirMessageQueue airMessageQueue = new AirMessageQueue();
        AirMessageQueueRS airMessageQueueRS = airMessageQueue.getAllMessages(category);
        MessageItems messageItems = getMessagesIitem(airMessageQueueRS);
        return ok(Json.toJson(messageItems));
    }
    public Result removeTicktedMessage() throws RemoteException {
        JsonNode json = request().body().asJson();
        Items items = Json.fromJson(json, Items.class);
        AirMessageQueue airMessageQueue = new AirMessageQueue();
        for (com.compassites.model.Item itemList : items.getItemList()) {
            airMessageQueue.removeMessageQueueRQ(itemList.getUniqueId());
        }
        return ok(Json.toJson(airMessageQueue));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result uploadTicket(){
        UpdatePNRResponse updatePNRResponse = null;
        JsonNode json = request().body().asJson();
        String provider = json.get("provider").asText();
        String appRef = json.get("appRef").asText();
        if(PROVIDERS.TRAVELOMATRIX.toString().equalsIgnoreCase(provider)){
            updatePNRResponse = new TraveloMatrixBookingServiceImpl().getUpdatePnr(appRef);

        }
        return ok(Json.toJson(updatePNRResponse));
    }
    public Result ticketCancelDocument(){
        logger.info("ticketCancelDocument called ");
        JsonNode jsonBody = request().body().asJson();
        String pnr = Json.fromJson(jsonBody.findPath("gdsPNR"), String.class);
        String provider = Json.fromJson(jsonBody.findPath("provider"), String.class);
        String appRef = Json.fromJson(jsonBody.findPath("appRef"), String.class);
        String bookingId = Json.fromJson(jsonBody.findPath("bookingId"), String.class);

        // Retrieve the 'numbers' array node
        ArrayNode ticketsNode = (ArrayNode) jsonBody.get("ticketId");

        // Convert ArrayNode to List of Integers
        List<String> ticketsList = new ArrayList<>();
        for (JsonNode ticketNode : ticketsNode) {
            // Extract integer value from each node and add to the list
            ticketsList.add(ticketNode.asText());
        }
        logger.debug("Cancel ticket document called : " + pnr + " provider : " + provider);
        TicketCancelDocumentResponse  ticketCancelDocumentResponse = amadeusTicketCancelDocumentServiceImpl.ticketCancelDocument(pnr,ticketsList);

        logger.debug("cancel ticket document response " + Json.toJson(ticketCancelDocumentResponse));
        return ok(Json.toJson(ticketCancelDocumentResponse));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result reIssueTicket() {
        JsonNode json = request().body().asJson();

        ReIssueSearchRequest reIssueSearchRequest = Json.fromJson(json, ReIssueSearchRequest.class);
        logger.debug("ReissueTicket Request Body{}", Json.toJson(reIssueSearchRequest));

        //Complete the response Body later
        SearchResponse reIssueTicketResponse = reIssueService.reIssueTicket(reIssueSearchRequest);
        logger.debug("ReissueTicket response Body{}", Json.toJson(reIssueTicketResponse));

        return ok(Json.toJson(reIssueTicketResponse));
    }


    @BodyParser.Of(BodyParser.Json.class)
    public Result checkRefundEligibiliy(){
        TicketCheckEligibilityRes ticketCheckEligibilityRes = null;
        JsonNode json = request().body().asJson();
        String provider = json.get("provider").asText();
        String gdspnr = json.get("gdsPnr").asText();
        String searchOfficeId = json.get("searchOffice").asText();
        ticketCheckEligibilityRes = refundServiceWrapper.checkTicketEligibility(provider,gdspnr,searchOfficeId);
        return ok(Json.toJson(ticketCheckEligibilityRes));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result processFullRefund(){
        TicketProcessRefundRes ticketProcessRefundRes = null;
        JsonNode json = request().body().asJson();
        String provider = json.get("provider").asText();
        String gdspnr = json.get("gdsPnr").asText();
        String searchOfficeId = json.get("searchOffice").asText();
        ticketProcessRefundRes = refundServiceWrapper.processFullRefund(provider,gdspnr,searchOfficeId);
        return ok(Json.toJson(ticketProcessRefundRes));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result checkPartRefundEligibility(){
        TicketCheckEligibilityRes ticketCheckEligibilityRes = null;
        JsonNode json = request().body().asJson();
        String provider = json.get("provider").asText();
        String gdspnr = json.get("gdsPnr").asText();
        String searchOfficeId = json.get("searchOffice").asText();
        JsonNode ticketsNode = json.get("tickets");
        List<String> ticketList = new LinkedList<>();
        if (ticketsNode != null && ticketsNode.isArray()) {
            for (JsonNode ticketNode : ticketsNode) {
                ticketList.add(ticketNode.asText());
            }
        }
        ticketCheckEligibilityRes = refundServiceWrapper.checkPartRefundTicketEligibility(provider,gdspnr,ticketList,searchOfficeId);
        return ok(Json.toJson(ticketCheckEligibilityRes));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result processPartialRefund(){
        TicketProcessRefundRes ticketProcessRefundRes = null;
        JsonNode json = request().body().asJson();
        String provider = json.get("provider").asText();
        String gdspnr = json.get("gdsPnr").asText();
        String searchOfficeId = json.get("searchOffice").asText();
        JsonNode ticketsNode = json.get("tickets");
        List<String> ticketList = new LinkedList<>();
        if (ticketsNode != null && ticketsNode.isArray()) {
            for (JsonNode ticketNode : ticketsNode) {
                ticketList.add(ticketNode.asText());
            }
        }
        ticketProcessRefundRes = refundServiceWrapper.processPartialRefund(provider,gdspnr,ticketList,searchOfficeId);
        return ok(Json.toJson(ticketProcessRefundRes));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result fetchTmxExtraServices() {
        JsonNode json = request().body().asJson();
        String resultToken = json.get("resultToken").asText();
        String reResulttoken = json.get("reResulttoken").asText();
        String journeyType = json.get("journeyType").asText();
        Boolean isLCC = json.get("isLCC").asBoolean();

        AncillaryServicesResponse ancillaryServicesResponse =ancillaryService.getTmxExtraServices(resultToken,reResulttoken,journeyType,isLCC);
        return ok(Json.toJson(ancillaryServicesResponse));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result addAdditionalBaggageRequest() {

        JsonNode json = request().body().asJson();
        String gdsPnr = json.get("gdsPnr").asText();
        String provider = json.get("provider").asText();

        AncillaryServicesResponse baggageDetails = ancillaryService.getAdditionalBaggageInfo(gdsPnr, provider);
        logger.debug("Ancillary - Baggage response {} ", Json.toJson(baggageDetails));

        return ok(Json.toJson(baggageDetails));

    }

    public Result ticketRebookAndRepricePNR(){
        JsonNode json = request().body().asJson();
        logger.debug("----------------- ticketRebookAndRepricePNR PNR Request: " + json);
        TravellerMasterInfo travellerMasterInfo = Json.fromJson(json, TravellerMasterInfo.class);
        ReIssueSearchRequest reIssueTicketRequest = new ReIssueSearchRequest();
        logger.debug("ReissueTicket Request Body{}", Json.toJson(reIssueTicketRequest));
        PNRResponse pnrResponse = reIssueService.ticketRebookAndRepricePNR(travellerMasterInfo, reIssueTicketRequest);
        logger.debug("-----------------PNR Response for ticketRebookAndRepricePNR: " + Json.toJson(pnrResponse));
        return Controller.ok(Json.toJson(pnrResponse));
    }

    public Result home(){
        return ok("GDS Service running.....");
    }


}