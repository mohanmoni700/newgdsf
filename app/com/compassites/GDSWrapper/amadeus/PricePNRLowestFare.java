package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.tplprq_12_4_1a.*;
import com.amadeus.xml.tplprq_12_4_1a.FarePricePNRWithLowestFare.DiscountInformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
		List<String> codeAttributes = Arrays.asList("PTC","RU","RW");
		CodedAttributeType codedAttributeType = new CodedAttributeType();
		List<CodedAttributeInformationType> codedAttribute = new ArrayList<>();
		for(String code : codeAttributes){
            CodedAttributeInformationType codeAttrInfoType = new CodedAttributeInformationType();
			codeAttrInfoType.setAttributeType(code);
			if(code.equals("RW")){
				codeAttrInfoType.setAttributeDescription("029608");
			}
		    codedAttribute.add(codeAttrInfoType);
        }
		codedAttributeType.getAttributeDetails().addAll(codedAttribute);
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
