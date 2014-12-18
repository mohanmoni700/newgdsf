package com.compassites.GDSWrapper.mystifly;

import java.rmi.RemoteException;

import junit.framework.Assert;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirTripDetailsRS;
import org.junit.Test;

import utils.XMLFileUtility;

public class AirTripDetailsTest {

	@Test
	public void testGetAirTripDetails() {
		String pnr = "MF06856714";
		AirTripDetailsClient tripDetailsClient = new AirTripDetailsClient();
		AirTripDetailsRS response = null;
		try {
			response = tripDetailsClient.getAirTripDetails(pnr);
		} catch (RemoteException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		Assert.assertNotNull(response);
		Assert.assertTrue(response.getSuccess());
		XMLFileUtility.createXMLFile(response, "AirTripDetailsRS.xml");
	}

}
