package controllers;


import com.compassites.GDSWrapper.mystifly.AirMessageQueue;
import com.compassites.model.*;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirMessageQueueRS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import services.*;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
    AmadeusBookingServiceImpl amadeusBookingService;

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
    public Result getLowestFare() {
    	JsonNode json = request().body().asJson();
        String pnr = Json.fromJson(json.findPath("gdsPNR"), String.class);
        String provider = Json.fromJson(json.findPath("provider"), String.class);
        Boolean isSeamen = Json.fromJson(json.findPath("isSeamen"), Boolean.class);
    	LowFareResponse lowestFare = bookingService.getLowestFare(pnr, provider, isSeamen);
    	logger.debug("-----------------LowestFare:\n" + Json.toJson(lowestFare));
    	return ok(Json.toJson(lowestFare));
    }

    public Result cancelPNR(){
        logger.info("cancelPNR called ");
        JsonNode json = request().body().asJson();
        String pnr = Json.fromJson(json.findPath("gdsPNR"), String.class);
        String provider = Json.fromJson(json.findPath("provider"), String.class);
        logger.debug("Cacnel PNR called for PNR : " + pnr + " provider : " + provider);

        CancelPNRResponse cancelPNRResponse = cancelService.cancelPNR(pnr, provider);

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

        }else {
            TravelportBookingServiceImpl travelportBookingService = new TravelportBookingServiceImpl();
            jsonNode =  travelportBookingService.getBookingDetails(pnr);
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
        return ok(Json.toJson(messageItems));
    }

    public Result removeMessage() throws RemoteException {

        AirMessageQueue airMessageQueue = new AirMessageQueue();
        airMessageQueue.removeMessageQueueRQ();
        return ok("Success");
    }
}