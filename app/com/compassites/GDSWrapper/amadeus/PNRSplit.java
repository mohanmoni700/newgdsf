package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.pnradd_11_3_1a.*;

import java.math.BigInteger;

/**
 * Created by sathishkumarpalanisamy on 18/09/17.
 */
public class PNRSplit {

    public PNRAddMultiElements saveChildPnr(String optionCode) {
        PNRAddMultiElements element = new PNRAddMultiElements();
        OptionalPNRActionsType pnrActions = new OptionalPNRActionsType();
        pnrActions.getOptionCode().add(new BigInteger(optionCode));
        element.setPnrActions(pnrActions);

        PNRAddMultiElements.DataElementsMaster dataElementsMaster =  new PNRAddMultiElements.DataElementsMaster();
        PNRAddMultiElements.DataElementsMaster.DataElementsIndiv dataElementsIndiv = new PNRAddMultiElements.DataElementsMaster.DataElementsIndiv();

        ElementManagementSegmentType elementManagementData = new ElementManagementSegmentType();
        ReferencingDetailsType reference =  new ReferencingDetailsType();
        reference.setNumber("15");
        reference.setQualifier("OT");
        elementManagementData.setSegmentName("RF");
        elementManagementData.setReference(reference);
        dataElementsIndiv.setElementManagementData(elementManagementData);

        LongFreeTextType freetextData = new LongFreeTextType();
        FreeTextQualificationType freetextDetail = new FreeTextQualificationType();
        freetextDetail.setSubjectQualifier("3");
        freetextDetail.setType("P22");
        freetextData.setFreetextDetail(freetextDetail);
        freetextData.setLongFreetext("Internet");
        dataElementsIndiv.setFreetextData(freetextData);
        dataElementsMaster.getDataElementsIndiv().add(dataElementsIndiv);
        dataElementsMaster.setMarker1(new DummySegmentTypeI());
        element.setDataElementsMaster(dataElementsMaster);

        return element;
    }
}
