package com.compassites.GDSWrapper.travelport;

import java.net.MalformedURLException;
import java.util.List;

import javax.xml.ws.BindingProvider;

import utils.XMLFileUtility;

import com.compassites.model.AirSegmentInformation;
import com.compassites.model.FlightItinerary;
import com.compassites.model.Journey;
import com.travelport.schema.air_v26_0.FlightDetailsReq;
import com.travelport.schema.air_v26_0.FlightDetailsRsp;
import com.travelport.schema.air_v26_0.TypeBaseAirSegment;
import com.travelport.schema.common_v26_0.BillingPointOfSaleInfo;
import com.travelport.service.air_v26_0.AirFaultMessage;
import com.travelport.service.air_v26_0.AirService;
import com.travelport.service.air_v26_0.FlightDetailsPortType;
import com.travelport.service.air_v26_0.FlightService;

/**
 * @author Santhosh
 */
public class FlightDetailsClient extends TravelPortClient {
	
	public static final String SERVICE_NAME = "/FlightService";
	public static final String WSDL_URL = "http://localhost:9000/wsdl/galileo/air_v26_0/Air.wsdl";
	static FlightService flightService = null;
    static FlightDetailsPortType flightDetailsPortType = null;
    
    public FlightDetailsClient() {
    	init();
    }

    void  init() {
        if (flightService == null) {
            java.net.URL url = null;
            try {
                java.net.URL baseUrl;
                baseUrl = AirService.class.getResource(".");
                url = new java.net.URL(baseUrl, WSDL_URL);
                flightService = new FlightService(url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        if (flightDetailsPortType == null) {
        	flightDetailsPortType = flightService.getFlightDetailsPort();
            setRequestContext((BindingProvider) flightDetailsPortType, SERVICE_NAME);
        }
    }
    
    public FlightDetailsRsp getFlightDetails(List<Journey> journeyList) {
    	FlightDetailsReq request = new FlightDetailsReq();
    	FlightDetailsRsp response = null;
    	request.setAuthorizedBy("TEST");
		request.setTargetBranch(BRANCH);
		BillingPointOfSaleInfo billInfo = new BillingPointOfSaleInfo();
		billInfo.setOriginApplication(UAPI);
		request.setBillingPointOfSaleInfo(billInfo);
		
		
//		for(int i = 0; i < flightItinerary.getJourneyList().size(); i++) {
		for(Journey journey : journeyList) {
//			Journey journey = flightItinerary.getJourneyList().get(i);
//			for(int j = 0; j < journey.getAirSegmentList().size(); j++) {
			for(AirSegmentInformation segment : journey.getAirSegmentList()) {
//				AirSegmentInformation segment = journey.getAirSegmentList().get(j);
				TypeBaseAirSegment airSegment = new TypeBaseAirSegment();
				airSegment.setKey(segment.getAirSegmentKey());
				airSegment.setGroup(journeyList.indexOf(journey));
				airSegment.setOrigin(segment.getFromLocation());
				airSegment.setDestination(segment.getToLocation());
				airSegment.setCarrier(segment.getCarrierCode());
//				airSegment.setClassOfService("");
				airSegment.setEquipment(segment.getEquipment());
				airSegment.setFlightNumber(segment.getFlightNumber());
				airSegment.setDepartureTime(segment.getDepartureTime());
				request.getAirSegment().add(airSegment);
			}
		}
//		TypeBaseAirSegment airSegment = new TypeBaseAirSegment();
//		airSegment.setKey("FWWVm7IQSu6h8rzHgnM0CA==");
//		airSegment.setGroup(0);
//		airSegment.setOrigin("BLR");
//		airSegment.setDestination("BOM");
//		airSegment.setCarrier("AI");
//		airSegment.setClassOfService("Y");
//		airSegment.setEquipment("319");
//		airSegment.setProviderCode("1G");
//		airSegment.setFlightNumber("640");
//		airSegment.setDepartureTime("2015-03-01T06:45:00.000+05:30");
//		request.getAirSegment().add(airSegment);
		
		XMLFileUtility.createXMLFile(request, "FlightDetailsReq.xml");
		try {
			response = flightDetailsPortType.service(request);
		} catch (AirFaultMessage e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		XMLFileUtility.createXMLFile(response, "FlightDetailsRsp.xml");
		return response;
    }

}
