package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.flireq_07_1_1a.AirFlightInfo;
import com.amadeus.xml.flireq_07_1_1a.AirFlightInfo.GeneralFlightInfo;
import com.amadeus.xml.flireq_07_1_1a.AirFlightInfo.GeneralFlightInfo.BoardPointDetails;
import com.amadeus.xml.flireq_07_1_1a.AirFlightInfo.GeneralFlightInfo.CompanyDetails;
import com.amadeus.xml.flireq_07_1_1a.AirFlightInfo.GeneralFlightInfo.FlightDate;
import com.amadeus.xml.flireq_07_1_1a.AirFlightInfo.GeneralFlightInfo.FlightIdentification;
import com.amadeus.xml.flireq_07_1_1a.AirFlightInfo.GeneralFlightInfo.OffPointDetails;
import com.compassites.model.AirSegmentInformation;

/**
 * @author Santhosh
 */
public class FlightInformation {

	public AirFlightInfo getAirFlightInfo(AirSegmentInformation airSegment) {
		AirFlightInfo airFlightInfo = new AirFlightInfo();
		GeneralFlightInfo generalFlightInfo = new GeneralFlightInfo();

		BoardPointDetails boardPointDetails = new BoardPointDetails();
		boardPointDetails.setTrueLocationId(airSegment.getFromLocation());
		generalFlightInfo.setBoardPointDetails(boardPointDetails);

		CompanyDetails companyDetails = new CompanyDetails();
		companyDetails.setMarketingCompany(airSegment.getCarrierCode());
		// TODO: get correct marketing & operating carrier code
		companyDetails.setOperatingCompany(airSegment.getCarrierCode());
		generalFlightInfo.setCompanyDetails(companyDetails);

		FlightDate flightDate = new FlightDate();
		flightDate.setDepartureDate(airSegment.getFromDate());
		generalFlightInfo.setFlightDate(flightDate);

		FlightIdentification flightId = new FlightIdentification();
		flightId.setFlightNumber(airSegment.getFlightNumber());
		generalFlightInfo.setFlightIdentification(flightId);

		OffPointDetails offPointDetails = new OffPointDetails();
		offPointDetails.setTrueLocationId(airSegment.getToLocation());
		generalFlightInfo.setOffPointDetails(offPointDetails);

		airFlightInfo.setGeneralFlightInfo(generalFlightInfo);
		return airFlightInfo;
	}

}
