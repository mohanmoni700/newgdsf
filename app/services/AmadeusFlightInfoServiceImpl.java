package services;

import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply.MainGroup.PricingGroupLevelGroup;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply.MainGroup.PricingGroupLevelGroup.FareInfoGroup.SegmentLevelGroup;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply.MainGroup.PricingGroupLevelGroup.FareInfoGroup.SegmentLevelGroup.BaggageAllowance.BaggageDetails;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.*;
import com.sun.xml.ws.fault.ServerSOAPFaultException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Santhosh
 */

@Service
public class AmadeusFlightInfoServiceImpl implements FlightInfoService {

	@Override
	public FlightItinerary getFlightnfo(FlightItinerary flightItinerary, SearchParameters searchParams) {
		ServiceHandler serviceHandler = null;
		
		try {
			serviceHandler = new ServiceHandler();
			serviceHandler.logIn();
			FareInformativePricingWithoutPNRReply reply = serviceHandler.getFareInfo(flightItinerary, searchParams.getAdultCount(), searchParams.getChildCount(), searchParams.getInfantCount());
			addBaggageInfo(flightItinerary, reply.getMainGroup().getPricingGroupLevelGroup());
			
		} catch (ServerSOAPFaultException ssf) {
			ssf.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return flightItinerary;
	}
	
	private void addBaggageInfo(FlightItinerary flightItinerary, List<PricingGroupLevelGroup> pricingLevelGroup) {
		List<SegmentLevelGroup> segmentGrpList = new ArrayList<>();
		for(PricingGroupLevelGroup pricingLevelGrp : pricingLevelGroup) {
			for(SegmentLevelGroup segment : pricingLevelGrp.getFareInfoGroup().getSegmentLevelGroup()) {
				segmentGrpList.add(segment);
			}
		}
		for (Journey journey : flightItinerary.getJourneyList()) {
			for (AirSegmentInformation airSegment : journey.getAirSegmentList()) {
				for (SegmentLevelGroup segmentGrp : segmentGrpList) {
					String from  = segmentGrp.getSegmentInformation().getBoardPointDetails().getTrueLocationId();
					String to = segmentGrp.getSegmentInformation().getOffpointDetails().getTrueLocationId();
					if(airSegment.getFromLocation().equalsIgnoreCase(from) && airSegment.getToLocation().equalsIgnoreCase(to)) {
						BaggageInfo baggageInfo = new BaggageInfo();
						BaggageDetails baggageDetails = segmentGrp.getBaggageAllowance().getBaggageDetails();
						baggageInfo.setValue(baggageDetails.getFreeAllowance().toBigInteger());
						baggageInfo.setUnit(baggageDetails.getUnitQualifier());
						airSegment.setBaggageInfo(baggageInfo);
					}
				}
			}
		}
	}

}
