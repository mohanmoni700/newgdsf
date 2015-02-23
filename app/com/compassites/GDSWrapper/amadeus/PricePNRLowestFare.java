package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.tplprq_12_4_1a.CodedAttributeInformationType;
import com.amadeus.xml.tplprq_12_4_1a.CodedAttributeType;
import com.amadeus.xml.tplprq_12_4_1a.FarePricePNRWithLowestFare;

/**
 * @author Santhosh
 */
public class PricePNRLowestFare {
	
	public FarePricePNRWithLowestFare getFarePricePNRWithLowestFare() {
		FarePricePNRWithLowestFare farePricePNRWithLowestFare = new FarePricePNRWithLowestFare();
		
		CodedAttributeType codedAttributeType = new CodedAttributeType();
		CodedAttributeInformationType codedAttribute = new CodedAttributeInformationType();
		codedAttribute.setAttributeType("NOP");
		codedAttributeType.getAttributeDetails().add(codedAttribute);
		farePricePNRWithLowestFare.setOverrideInformation(codedAttributeType);
		
//		TransportIdentifierType transportIdentifierType = new TransportIdentifierType();
//		CompanyIdentificationTypeI company = new CompanyIdentificationTypeI();
//		company.setCarrierCode(carrierCode);
//		transportIdentifierType.setCarrierInformation(company);
//		farePricePNRWithLowestFare.setValidatingCarrier(transportIdentifierType);
		
		return farePricePNRWithLowestFare;
	}

}
