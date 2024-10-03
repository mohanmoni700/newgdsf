package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation;
import com.amadeus.xml.pnracc_11_3_1a.OriginatorDetailsTypeI;
import com.amadeus.xml.pnracc_11_3_1a.OriginatorIdentificationDetailsTypeI;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.pnracc_11_3_1a.POSGroupType;
import com.amadeus.xml.trcanq_14_1_1a.*;
import com.compassites.model.FareJourney;
import com.compassites.model.FlightItinerary;
import com.compassites.model.Journey;
import com.compassites.model.traveller.TravellerMasterInfo;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TicketCancelDocumentHandler {
    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

    public TicketCancelDocument ticketCancelDocument(List<String> ticketsList, PNRReply gdsPNRReply, String delOficID) {
        TicketCancelDocument ticketCancelDocument = new TicketCancelDocument();
        List<TicketNumberTypeI> ticketNumberList = ticketCancelDocument.getDocumentNumberDetails();
        for(String ticketNumber : ticketsList){
            TicketNumberTypeI ticketNumberTypeI = new TicketNumberTypeI();
            TicketNumberDetailsTypeI ticketNumberDetailsTypeI = new TicketNumberDetailsTypeI();

            ticketNumberDetailsTypeI.setNumber(ticketNumber.replaceAll("\\D", ""));
            ticketNumberTypeI.setDocumentDetails(ticketNumberDetailsTypeI);
            ticketNumberList.add(ticketNumberTypeI);
        }

        //set office id
        POSGroupType posGroupType = gdsPNRReply.getSbrCreationPosDetails();
        OriginatorIdentificationDetailsTypeI originatorIdentificationDetails = posGroupType.getSbrUserIdentificationOwn().getOriginIdentification();

        AdditionalBusinessSourceInformationType additionalBusinessSourceInformationType = new AdditionalBusinessSourceInformationType();
        OriginatorIdentificationDetailsType originatorIdentificationDetailsType = new OriginatorIdentificationDetailsType();

        String officeId = originatorIdentificationDetails.getInHouseIdentification1();
        originatorIdentificationDetailsType.setInHouseIdentification2(delOficID);
        additionalBusinessSourceInformationType.setOriginatorDetails(originatorIdentificationDetailsType);
        ticketCancelDocument.setTargetOfficeDetails(additionalBusinessSourceInformationType);

        // set country code
        OriginatorDetailsTypeI originatorDetailsTypeI = posGroupType.getSbrPreferences().getUserPreferences();

        OfficeSettingsDetailsType officeSettingsDetailsType = new OfficeSettingsDetailsType();
        DocumentInfoFromOfficeSettingType documentInfoFromOfficeSettingType = new DocumentInfoFromOfficeSettingType();

        String countryCode = originatorDetailsTypeI.getCodedCountry();
        documentInfoFromOfficeSettingType.setMarketIataCode(countryCode);
        officeSettingsDetailsType.setOfficeSettingsDetails(documentInfoFromOfficeSettingType);
        ticketCancelDocument.setStockProviderDetails(officeSettingsDetailsType);


        return ticketCancelDocument;
    }

}
