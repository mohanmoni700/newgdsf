package com.compassites.GDSWrapper.amadeus;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.amadeus.xml.tipnrq_12_4_1a.FareInformativePricingWithoutPNR;
import com.amadeus.xml.tipnrq_12_4_1a.FareInformativePricingWithoutPNR.MessageDetails;
import com.amadeus.xml.tipnrq_12_4_1a.FareInformativePricingWithoutPNR.MessageDetails.MessageFunctionDetails;
import com.amadeus.xml.tipnrq_12_4_1a.FareInformativePricingWithoutPNR.PassengersGroup;
import com.amadeus.xml.tipnrq_12_4_1a.FareInformativePricingWithoutPNR.PassengersGroup.PtcGroup;
import com.amadeus.xml.tipnrq_12_4_1a.FareInformativePricingWithoutPNR.PassengersGroup.PtcGroup.DiscountPtc;
import com.amadeus.xml.tipnrq_12_4_1a.FareInformativePricingWithoutPNR.PassengersGroup.SegmentRepetitionControl;
import com.amadeus.xml.tipnrq_12_4_1a.FareInformativePricingWithoutPNR.PassengersGroup.SegmentRepetitionControl.SegmentControlDetails;
import com.amadeus.xml.tipnrq_12_4_1a.FareInformativePricingWithoutPNR.PassengersGroup.TravellersID;
import com.amadeus.xml.tipnrq_12_4_1a.FareInformativePricingWithoutPNR.PassengersGroup.TravellersID.TravellerDetails;
import com.amadeus.xml.tipnrq_12_4_1a.FareInformativePricingWithoutPNR.TripsGroup;
import com.amadeus.xml.tipnrq_12_4_1a.FareInformativePricingWithoutPNR.TripsGroup.OriginDestination;
import com.amadeus.xml.tipnrq_12_4_1a.FareInformativePricingWithoutPNR.TripsGroup.SegmentGroup;
import com.amadeus.xml.tipnrq_12_4_1a.FareInformativePricingWithoutPNR.TripsGroup.SegmentGroup.SegmentInformation;
import com.amadeus.xml.tipnrq_12_4_1a.FareInformativePricingWithoutPNR.TripsGroup.SegmentGroup.SegmentInformation.BoardPointDetails;
import com.amadeus.xml.tipnrq_12_4_1a.FareInformativePricingWithoutPNR.TripsGroup.SegmentGroup.SegmentInformation.CompanyDetails;
import com.amadeus.xml.tipnrq_12_4_1a.FareInformativePricingWithoutPNR.TripsGroup.SegmentGroup.SegmentInformation.FlightDate;
import com.amadeus.xml.tipnrq_12_4_1a.FareInformativePricingWithoutPNR.TripsGroup.SegmentGroup.SegmentInformation.FlightIdentification;
import com.amadeus.xml.tipnrq_12_4_1a.FareInformativePricingWithoutPNR.TripsGroup.SegmentGroup.SegmentInformation.OffpointDetails;
import com.amadeus.xml.tipnrq_12_4_1a.FareInformativePricingWithoutPNR.TripsGroup.SegmentGroup.Trigger;
import com.compassites.model.*;

import play.libs.Json;
import views.html.main;

/**
 * @author Santhosh
 */
public class FareInformation {

	public FareInformativePricingWithoutPNR getFareInfo(
			List<Journey> journeys, boolean seamen, int adultCount, int childCount, int infantCount, List<PAXFareDetails> paxFareDetailsList) {

		FareInformativePricingWithoutPNR fareInfo = new FareInformativePricingWithoutPNR();

		MessageDetails messageDetails = new MessageDetails();
		MessageFunctionDetails messageFunctionDetails = new MessageFunctionDetails();
		messageFunctionDetails.setBusinessFunction("1");
		messageFunctionDetails.setMessageFunction("741");
		messageFunctionDetails.setResponsibleAgency("1A");
		messageDetails.setMessageFunctionDetails(messageFunctionDetails);
		fareInfo.setMessageDetails(messageDetails);

		List<PassengersGroup> passengers = fareInfo.getPassengersGroup();
		//System.out.println("seamen value inside getFareInfo"+seamen);
		if(seamen){
			passengers.add(getPassengerGroup(PassengerTypeCode.SEA,adultCount));
	        if(childCount > 0){
	            passengers.add(getPassengerGroup(PassengerTypeCode.SEA,childCount));
	        }
			if(infantCount > 0){
	            passengers.add(getPassengerGroup(PassengerTypeCode.SEA,infantCount));
	        }
		}else{
			passengers.add(getPassengerGroup(PassengerTypeCode.ADT,adultCount));
	        if(childCount > 0){
	            passengers.add(getPassengerGroup(PassengerTypeCode.CHD,childCount));
	        }
			if(infantCount > 0){
	            passengers.add(getPassengerGroup(PassengerTypeCode.INF,infantCount));
	        }
		}
		


		TripsGroup tripsGroup = new TripsGroup();
		fareInfo.setTripsGroup(tripsGroup);
		List<SegmentGroup> segmentGroups = tripsGroup.getSegmentGroup();
		List<AirSegmentInformation> airSegments = new ArrayList<>();
		List<FareSegment> fareSegments = new ArrayList<>();
		int i = 0 ;
		for (Journey journey : journeys) {
			FareJourney fareJourney = paxFareDetailsList.get(0).getFareJourneyList().get(i);
			int j = 0;
			for (AirSegmentInformation airSegment : journey.getAirSegmentList()) {

				airSegments.add(airSegment);
				fareSegments.add(fareJourney.getFareSegmentList().get(j));
                j++;
			}
            i++;
		}
		//System.out.println("airSegments json\n"+Json.toJson(airSegments));
        i = 0;
		for (AirSegmentInformation airSegment : airSegments) {
			SegmentGroup segmentGroup = new SegmentGroup();
			SegmentInformation segmentInfo = new SegmentInformation();
			BoardPointDetails boardingDetails = new BoardPointDetails();
			boardingDetails.setTrueLocationId(airSegment.getFromLocation());
			OffpointDetails offpointDetails = new OffpointDetails();
			offpointDetails.setTrueLocationId(airSegment.getToLocation());
			segmentInfo.setBoardPointDetails(boardingDetails);
			segmentInfo.setOffpointDetails(offpointDetails);
			FlightDate flightDate = new FlightDate();
			SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"); 
			Date date = null;
			try {
				date = dateParser.parse((airSegment.getDepartureTime()));
			} catch (ParseException e) {
				e.printStackTrace();
			}
			SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyy");
			String dateString = dateFormat.format(date);
			flightDate.setDepartureDate(dateString);
			
			segmentInfo.setFlightDate(flightDate);
			CompanyDetails companyDetails = new CompanyDetails();
			companyDetails.setMarketingCompany(airSegment.getCarrierCode());
			segmentInfo.setCompanyDetails(companyDetails);
			FlightIdentification flightIdentification = new FlightIdentification();
			// TODO: Change hard coded value
			flightIdentification.setBookingClass(fareSegments.get(i).getBookingClass());
			flightIdentification.setFlightNumber(airSegment.getFlightNumber());
			segmentInfo.setFlightIdentification(flightIdentification);

			segmentGroup.setSegmentInformation(segmentInfo);
			Trigger trigger = new Trigger();
			segmentGroup.setTrigger(trigger);
			segmentGroups.add(segmentGroup);
		}
		OriginDestination originDestination = new OriginDestination();
		originDestination.setDestination(airSegments
				.get(airSegments.size() - 1).getToLocation());
		originDestination.setOrigin(airSegments.get(0).getFromLocation());
		tripsGroup.setOriginDestination(originDestination);
		fareInfo.setTripsGroup(tripsGroup);

		return fareInfo;
	}

	
	private PassengersGroup getPassengerGroup(PassengerTypeCode passengerType,
			int passengerQuantity) {
		PassengersGroup passengerGroup = new PassengersGroup();

		SegmentRepetitionControl segmentRepetitionControl = new SegmentRepetitionControl();
		SegmentControlDetails segmentControlDetails = new SegmentControlDetails();
		segmentControlDetails.setQuantity(new BigDecimal(1));
		segmentControlDetails.setNumberOfUnits(new BigDecimal(1));
		segmentRepetitionControl.getSegmentControlDetails().add(
				segmentControlDetails);
		passengerGroup.setSegmentRepetitionControl(segmentRepetitionControl);

		/*TravellersID travellersId = new TravellersID();
		TravellerDetails travellerDetails = new TravellerDetails();
		travellerDetails.setMeasurementValue(new BigDecimal(1));
		travellersId.getTravellerDetails().add(travellerDetails);
		passengerGroup.setTravellersID(travellersId);*/

		PtcGroup ptcGroup = new PtcGroup();
		DiscountPtc discountPtc = new DiscountPtc();
		discountPtc.setValueQualifier(passengerType.name());
		ptcGroup.setDiscountPtc(discountPtc);
		passengerGroup.getPtcGroup().add(ptcGroup);

		return passengerGroup;
	}

}
