package com.compassites.GDSWrapper.mystifly;

import static org.junit.Assert.*;

import java.rmi.RemoteException;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirOrderTicketRS;
import org.junit.Test;

import utils.XMLFileUtility;

public class AirOrderTicketTest {

	@Test
	public void testIssueTicket() {
		AirOrderTicketClient airOrderTicketClient = new AirOrderTicketClient();
		AirOrderTicketRS airOrderTicketRS = null;
		String pnr = "MF06856714"; 
		try {
			airOrderTicketRS = airOrderTicketClient.issueTicket(pnr);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		XMLFileUtility.createXMLFile(airOrderTicketRS, "airOrderTicketRS.xml");
		assertTrue(airOrderTicketRS.getSuccess());
	}

}
