package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.tplprq_12_4_1a.CodedAttributeInformationType;
import com.amadeus.xml.tplprq_12_4_1a.CodedAttributeType;
import com.amadeus.xml.tplprq_12_4_1a.DiscountAndPenaltyInformationTypeI;
import com.amadeus.xml.tplprq_12_4_1a.DiscountPenaltyMonetaryInformationTypeI;
import com.amadeus.xml.tplprq_12_4_1a.FarePricePNRWithLowestFare;
import com.amadeus.xml.tplprq_12_4_1a.FarePricePNRWithLowestFare.DiscountInformation;

/**
 * @author Santhosh
 */
public class PricePNRLowestFare {

	public FarePricePNRWithLowestFare getPricePNRWithLowestSeamenFare() {
		FarePricePNRWithLowestFare farePricePNRWithLowestFare = new FarePricePNRWithLowestFare();

		DiscountInformation discountInfo = new DiscountInformation();
		DiscountAndPenaltyInformationTypeI penDisInfo = new DiscountAndPenaltyInformationTypeI();
		penDisInfo.setInfoQualifier("701");
		DiscountPenaltyMonetaryInformationTypeI penDisData = new DiscountPenaltyMonetaryInformationTypeI();
		penDisData.setDiscountCode("SEA");
		penDisInfo.getPenDisData().add(penDisData);
		discountInfo.setPenDisInformation(penDisInfo);
		farePricePNRWithLowestFare.getDiscountInformation().add(discountInfo);

		CodedAttributeType codedAttributeType = new CodedAttributeType();
		CodedAttributeInformationType codedAttribute = new CodedAttributeInformationType();
		codedAttribute.setAttributeType("PTC");
		codedAttributeType.getAttributeDetails().add(codedAttribute);
		farePricePNRWithLowestFare.setOverrideInformation(codedAttributeType);

		return farePricePNRWithLowestFare;
	}

	public FarePricePNRWithLowestFare getPricePNRWithLowestNonSeamenFare() {
		FarePricePNRWithLowestFare farePricePNRWithLowestFare = new FarePricePNRWithLowestFare();

		CodedAttributeType codedAttributeType = new CodedAttributeType();
		CodedAttributeInformationType codedAttribute = new CodedAttributeInformationType();
		codedAttribute.setAttributeType("NOP");
		codedAttributeType.getAttributeDetails().add(codedAttribute);
		farePricePNRWithLowestFare.setOverrideInformation(codedAttributeType);

		return farePricePNRWithLowestFare;
	}

}
