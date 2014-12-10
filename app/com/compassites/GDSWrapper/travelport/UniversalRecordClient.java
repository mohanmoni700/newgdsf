package com.compassites.GDSWrapper.travelport;

import com.travelport.schema.air_v26_0.AirReservation;
import com.travelport.schema.common_v26_0.BillingPointOfSaleInfo;
import com.travelport.schema.common_v26_0.ProviderReservationInfoRef;
import com.travelport.schema.universal_v26_0.AirCreateReservationRsp;
import com.travelport.schema.universal_v26_0.ProviderReservationInfo;
import com.travelport.schema.universal_v26_0.UniversalRecordRetrieveReq;
import com.travelport.schema.universal_v26_0.UniversalRecordRetrieveRsp;
import com.travelport.service.universal_v26_0.UniversalRecordArchivedFaultMessage;
import com.travelport.service.universal_v26_0.UniversalRecordFaultMessage;
import com.travelport.service.universal_v26_0.UniversalRecordRetrieveServicePortType;
import com.travelport.service.universal_v26_0.UniversalRecordService;
import org.apache.commons.logging.LogFactory;
import utils.XMLFileUtility;

import javax.xml.ws.BindingProvider;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Created by user on 11-09-2014.
 */
public class UniversalRecordClient extends TravelPortClient {

    static final String ServiceName =  "/UniversalRecordService";
    static UniversalRecordService universalRecordService = null;
    static UniversalRecordRetrieveServicePortType universalRecordRetrieveServicePortType = null;

    static void  init(){
        if (universalRecordService == null){
            try {
                String path = new File(".").getCanonicalPath();
                universalRecordService = new UniversalRecordService(new java.net.URL("http://localhost:9000/wsdl/galileo/universal_v26_0/UniversalRecord.wsdl"));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (universalRecordRetrieveServicePortType == null){
            universalRecordRetrieveServicePortType  = universalRecordService.getUniversalRecordRetrieveServicePort();
            LogFactory.getLog(UniversalRecordClient.class).info("Initializing universalRecordRetrieveServicePortType....");
            setRequestContext((BindingProvider) universalRecordRetrieveServicePortType, ServiceName);
            LogFactory.getLog(UniversalRecordClient.class).info("Initialized");
        }
    }

    public static UniversalRecordRetrieveRsp retrievePNR(AirCreateReservationRsp reservationRsp) {
        String locatorCode = "";
        Helper.ReservationInfoMap reservationInfoMap = Helper.createReservationInfoMap(reservationRsp.getUniversalRecord().getProviderReservationInfo());
        for(AirReservation airReservation : reservationRsp.getUniversalRecord().getAirReservation()){
            for(ProviderReservationInfoRef reservationInfoRef : airReservation.getProviderReservationInfoRef()){
                ProviderReservationInfo reservationInfo1 = reservationInfoMap.getByRef(reservationInfoRef);
                locatorCode = reservationInfo1.getLocatorCode();
            }
        }
        return retrievePNR(locatorCode);
    }

	public static UniversalRecordRetrieveRsp retrievePNR(String locatorCode) {
		UniversalRecordRetrieveReq recordRetrieveReq = new UniversalRecordRetrieveReq();
		recordRetrieveReq.setAuthorizedBy("TEST");
		recordRetrieveReq.setTargetBranch(BRANCH);

		BillingPointOfSaleInfo billingPointOfSaleInfo = new BillingPointOfSaleInfo();
		billingPointOfSaleInfo.setOriginApplication("UAPI");
		recordRetrieveReq.setBillingPointOfSaleInfo(billingPointOfSaleInfo);
		UniversalRecordRetrieveRsp recordRetrieveRsp = null;
		UniversalRecordRetrieveReq.ProviderReservationInfo reservationInfo = new UniversalRecordRetrieveReq.ProviderReservationInfo();
		reservationInfo.setProviderCode(GDS);
		reservationInfo.setProviderLocatorCode(locatorCode);
		recordRetrieveReq.setProviderReservationInfo(reservationInfo);
		try {
			init();
			XMLFileUtility.createXMLFile(recordRetrieveReq,
					"UniversalRecordRetrieveReq.xml");
			recordRetrieveRsp = universalRecordRetrieveServicePortType.service(
					recordRetrieveReq, null);
			XMLFileUtility.createXMLFile(recordRetrieveRsp,
					"UniversalRecordRetrieveRes.xml");
		} catch (UniversalRecordFaultMessage universalRecordFaultMessage) {
			universalRecordFaultMessage.printStackTrace();
		} catch (UniversalRecordArchivedFaultMessage universalRecordArchivedFaultMessage) {
			universalRecordArchivedFaultMessage.printStackTrace();
		}
		return recordRetrieveRsp;
	}
	
}
