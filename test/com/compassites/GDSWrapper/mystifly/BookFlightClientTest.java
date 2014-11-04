package com.compassites.GDSWrapper.mystifly;

import static org.junit.Assert.assertTrue;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirBookRS;
import org.junit.Test;

import com.compassites.helpers.TravellerMasterInfoHelper;
import com.compassites.model.traveller.TravellerMasterInfo;

public class BookFlightClientTest {

	@Test
	public void test() {
		BookFlightClient bookFlightClient = new BookFlightClient();
		TravellerMasterInfo travellerMaster = TravellerMasterInfoHelper.getTravellerMasterInfo();
		AirBookRS bookRS = bookFlightClient.bookFlight(travellerMaster);
    	assertTrue(bookRS.getSuccess());
	}

}
