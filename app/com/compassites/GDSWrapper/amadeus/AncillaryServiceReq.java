package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.tpicgq_17_1_1a.AttributeInformationTypeU;
import com.amadeus.xml.tpicgq_17_1_1a.AttributeType;
import com.amadeus.xml.tpicgq_17_1_1a.PricingOptionKeyType;
import com.amadeus.xml.tpicgq_17_1_1a.ServiceIntegratedCatalogue;
import com.amadeus.xml.tpicgr_17_1_1a.ServiceIntegratedCatalogueReply;

import java.util.ArrayList;
import java.util.List;

public class AncillaryServiceReq {

    public static class AdditionalPaidBaggage {

        //This Created the request body for amadeus for additional baggage information
        public static ServiceIntegratedCatalogue createShowAdditionalBaggageInformationRequest() {

            ServiceIntegratedCatalogue serviceIntegratedCatalogue = new ServiceIntegratedCatalogue();
            List<ServiceIntegratedCatalogue.PricingOption> pricingOption = new ArrayList<>();

            //Requesting only baggage related information here
            ServiceIntegratedCatalogue.PricingOption groupBaggagePricingOption = new ServiceIntegratedCatalogue.PricingOption();

            PricingOptionKeyType groupPricingOptionKey = new PricingOptionKeyType();
            groupPricingOptionKey.setPricingOptionKey("GRP");
            groupBaggagePricingOption.setPricingOptionKey(groupPricingOptionKey);

            AttributeType optionDetail = new AttributeType();
            AttributeInformationTypeU criteriaDetails = new AttributeInformationTypeU();
            criteriaDetails.setAttributeType("BG");
            optionDetail.getCriteriaDetails().add(criteriaDetails);
            groupBaggagePricingOption.setOptionDetail(optionDetail);

            pricingOption.add(groupBaggagePricingOption);


            //MIF - > To identify special airlines
            ServiceIntegratedCatalogue.PricingOption mifPricingOption = new ServiceIntegratedCatalogue.PricingOption();
            PricingOptionKeyType mifPricingOptionKey = new PricingOptionKeyType();
            mifPricingOptionKey.setPricingOptionKey("MIF");
            mifPricingOption.setPricingOptionKey(mifPricingOptionKey);

            pricingOption.add(mifPricingOption);


            //OIS -> Show Only Issuable recommendation
            ServiceIntegratedCatalogue.PricingOption oisPricingOption = new ServiceIntegratedCatalogue.PricingOption();
            PricingOptionKeyType oisPricingOptionKey = new PricingOptionKeyType();
            oisPricingOptionKey.setPricingOptionKey("OIS");
            oisPricingOption.setPricingOptionKey(oisPricingOptionKey);

            pricingOption.add(oisPricingOption);


            //SCD -> Show Commercial Description
            ServiceIntegratedCatalogue.PricingOption scdPricingOption = new ServiceIntegratedCatalogue.PricingOption();
            PricingOptionKeyType scdPricingOptionKey = new PricingOptionKeyType();
            scdPricingOptionKey.setPricingOptionKey("SCD");
            scdPricingOption.setPricingOptionKey(scdPricingOptionKey);

            pricingOption.add(scdPricingOption);

            serviceIntegratedCatalogue.getPricingOption().addAll(pricingOption);

            return serviceIntegratedCatalogue;
        }

    }

}
