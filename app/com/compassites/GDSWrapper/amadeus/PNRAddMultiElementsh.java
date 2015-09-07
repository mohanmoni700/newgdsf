package com.compassites.GDSWrapper.amadeus;


import com.amadeus.xml.pnradd_11_3_1a.*;
import com.amadeus.xml.pnradd_11_3_1a.PNRAddMultiElements.DataElementsMaster;
import com.amadeus.xml.pnradd_11_3_1a.PNRAddMultiElements.DataElementsMaster.DataElementsIndiv;
import com.amadeus.xml.pnradd_11_3_1a.PNRAddMultiElements.TravellerInfo;
import com.compassites.model.PassengerTypeCode;
import com.compassites.model.traveller.PassportDetails;
import com.compassites.model.traveller.PersonalDetails;
import com.compassites.model.traveller.Preferences;
import com.compassites.model.traveller.TravellerMasterInfo;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.springframework.util.StringUtils;
import utils.StringUtility;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PNRAddMultiElementsh {

    public PNRAddMultiElements getMultiElements(TravellerMasterInfo travellerMasterInfo) {
        PNRAddMultiElements element = new PNRAddMultiElements();
        OptionalPNRActionsType pnrActions = new OptionalPNRActionsType();
        pnrActions.getOptionCode().add(new BigInteger("0"));
        element.setPnrActions(pnrActions);

        //element.getTravellerInfo().add(addPassenger());
        //element.getTravellerInfo().add(addChildPassenger());
        //element.getTravellerInfo().add(addInfantPassenger());
        element.getTravellerInfo().addAll(getPassengersList(travellerMasterInfo))  ;
        PNRAddMultiElements.DataElementsMaster dem = new PNRAddMultiElements.DataElementsMaster();
        dem.setMarker1(new DummySegmentTypeI());
        int qualifierNumber = 0;
        dem.getDataElementsIndiv().add(addCreditCardData(qualifierNumber));
        dem.getDataElementsIndiv().add(addReceivedFrom(qualifierNumber));
        dem.getDataElementsIndiv().add(addTckArr(qualifierNumber));
        dem.getDataElementsIndiv().addAll(addContactInfo(travellerMasterInfo, qualifierNumber));
//        dem.getDataElementsIndiv().addAll(addAdditionalPassengerDetails(travellerMasterInfo, qualifierNumber));
        //dem.getDataElementsIndiv().add(addEOTInfo());

        element.setDataElementsMaster(dem);
        return element;
    }

    public PNRAddMultiElements addSSRDetails(TravellerMasterInfo travellerMasterInfo) {
        PNRAddMultiElements element = new PNRAddMultiElements();
        OptionalPNRActionsType pnrActions = new OptionalPNRActionsType();
        pnrActions.getOptionCode().add(new BigInteger("0"));
        element.setPnrActions(pnrActions);

        element.getTravellerInfo().addAll(getPassengersList(travellerMasterInfo))  ;
        DataElementsMaster dem = new DataElementsMaster();
        dem.setMarker1(new DummySegmentTypeI());
        int qualifierNumber = 0;
        dem.getDataElementsIndiv().addAll(addAdditionalPassengerDetails(travellerMasterInfo, qualifierNumber));
        element.setDataElementsMaster(dem);
        return element;
    }
    private List<TravellerInfo> getPassengersList(TravellerMasterInfo travellerMasterInfo){

        int passengerCount = 1;
        List<TravellerInfo> travellerInfoList = new ArrayList<>();

        for (com.compassites.model.traveller.Traveller traveller : travellerMasterInfo.getTravellersList()){
            TravellerInfo travellerInfo = new TravellerInfo();
            ElementManagementSegmentType emp = new ElementManagementSegmentType();
            ReferencingDetailsType rf = new ReferencingDetailsType();
            rf.setNumber(String.valueOf(passengerCount++));
            rf.setQualifier("PR");
            emp.setReference(rf);
            emp.setSegmentName("NM");
            travellerInfo.setElementManagementPassenger(emp);

            TravellerInfo.PassengerData passengerData = new TravellerInfo.PassengerData();
            TravellerInformationTypeI ti = new TravellerInformationTypeI();
            TravellerSurnameInformationTypeI tr = new TravellerSurnameInformationTypeI();
            tr.setSurname(traveller.getPersonalDetails().getLastName());
            tr.setQuantity(new BigInteger("1"));

            TravellerDetailsTypeI p = new TravellerDetailsTypeI();
            p.setFirstName(traveller.getPersonalDetails().getFirstName()+" "+traveller.getPersonalDetails().getMiddleName());
            //p.setIdentificationCode("ID1234");
            //p.setType("SEA");
            if(travellerMasterInfo.isSeamen()){
                p.setType(PassengerTypeCode.SEA.toString());

            }else {
                p.setType(getPassengerType(traveller.getPassportDetails().getDateOfBirth()));
            }

            ti.getPassenger().add(p);

            ti.setTraveller(tr);
            passengerData.setTravellerInformation(ti);
            travellerInfo.getPassengerData().add(passengerData);

            travellerInfoList.add(travellerInfo);
        }

        return travellerInfoList;
    }

    //TODO-- Add Seaman type to the adult passenger
    private String getPassengerType(Date passengerDOB){
        LocalDate birthdate = new LocalDate (passengerDOB);          //Birth date
        LocalDate now = new LocalDate();                    //Today's date
        Period period = new Period(birthdate, now,  PeriodType.yearMonthDay());
        int age = period.getYears();
        String passengerType;
        if(age <= 2){
            passengerType = "INF";
        }else if (age <= 12){
            passengerType = "CHD";
        }else{
            passengerType = "ADT";
        }

        return passengerType;

    }
    public TravellerInfo addPassenger(){
        TravellerInfo travellerInfo = new TravellerInfo();
        ElementManagementSegmentType emp = new ElementManagementSegmentType();
        ReferencingDetailsType rf = new ReferencingDetailsType();
        rf.setNumber("1");
        rf.setQualifier("PR");
        emp.setReference(rf);
        emp.setSegmentName("NM");
        travellerInfo.setElementManagementPassenger(emp);

        TravellerInfo.PassengerData passengerData = new TravellerInfo.PassengerData();
        TravellerInformationTypeI ti = new TravellerInformationTypeI();
        TravellerSurnameInformationTypeI tr = new TravellerSurnameInformationTypeI();
        tr.setSurname("DUPONT");
        tr.setQuantity(new BigInteger("1"));

        TravellerDetailsTypeI p = new TravellerDetailsTypeI();
        p.setFirstName("MATHIEU");
        p.setIdentificationCode("ID1234");
        p.setType("SEA");

        ti.getPassenger().add(p);
        ti.setTraveller(tr);
        passengerData.setTravellerInformation(ti);
        travellerInfo.getPassengerData().add(passengerData);
        return travellerInfo;
    }

    public TravellerInfo addChildPassenger(){
        TravellerInfo travellerInfo = new TravellerInfo();
        ElementManagementSegmentType emp = new ElementManagementSegmentType();
        ReferencingDetailsType rf = new ReferencingDetailsType();
        rf.setNumber("2");
        rf.setQualifier("PR");
        emp.setReference(rf);
        emp.setSegmentName("NM");
        travellerInfo.setElementManagementPassenger(emp);

        TravellerInfo.PassengerData passengerData = new TravellerInfo.PassengerData();
        TravellerInformationTypeI ti = new TravellerInformationTypeI();
        TravellerSurnameInformationTypeI tr = new TravellerSurnameInformationTypeI();
        tr.setSurname("DUPONTER");
        tr.setQuantity(BigInteger.valueOf(1));

        TravellerDetailsTypeI p = new TravellerDetailsTypeI();
        p.setFirstName("ADAM");
        p.setIdentificationCode("ID12345");
        p.setType("CH");

        ti.getPassenger().add(p);
        ti.setTraveller(tr);
        passengerData.setTravellerInformation(ti);
        travellerInfo.getPassengerData().add(passengerData);
        return travellerInfo;
    }

    public TravellerInfo addInfantPassenger(){
        TravellerInfo travellerInfo = new TravellerInfo();
        ElementManagementSegmentType emp = new ElementManagementSegmentType();
        ReferencingDetailsType rf = new ReferencingDetailsType();
        rf.setNumber("3");
        rf.setQualifier("PR");
        emp.setReference(rf);
        emp.setSegmentName("NM");
        travellerInfo.setElementManagementPassenger(emp);

        TravellerInfo.PassengerData passengerData = new TravellerInfo.PassengerData();
        TravellerInformationTypeI ti = new TravellerInformationTypeI();
        TravellerSurnameInformationTypeI tr = new TravellerSurnameInformationTypeI();
        tr.setSurname("DUPONTERINF");
        tr.setQuantity(new BigInteger("1"));

        TravellerDetailsTypeI p = new TravellerDetailsTypeI();
        p.setFirstName("ADAMER");
        p.setType("INF");

        ti.getPassenger().add(p);
        ti.setTraveller(tr);
        passengerData.setTravellerInformation(ti);
        travellerInfo.getPassengerData().add(passengerData);
        return travellerInfo;
    }



    public PNRAddMultiElements savePnr() {
        PNRAddMultiElements element = new PNRAddMultiElements();
        OptionalPNRActionsType pnrActions = new OptionalPNRActionsType();
        pnrActions.getOptionCode().add(new BigInteger("10"));
        element.setPnrActions(pnrActions);

        DataElementsMaster dataElementsMaster =  new DataElementsMaster();
        DataElementsIndiv dataElementsIndiv = new DataElementsIndiv();

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

    public PNRAddMultiElements addEotTimeElement() {
        PNRAddMultiElements element = new PNRAddMultiElements();
        OptionalPNRActionsType pnrActions = new OptionalPNRActionsType();
        pnrActions.getOptionCode().add(new BigInteger("0"));
        element.setPnrActions(pnrActions);
        DataElementsMaster dem = new DataElementsMaster();
        dem.setMarker1(new DummySegmentTypeI());
        dem.getDataElementsIndiv().add(addEOTInfo());
        element.setDataElementsMaster(dem);
        return element;
    }

    //contact information
    public List<DataElementsIndiv> addContactInfo(TravellerMasterInfo travellerMasterInfo, int qualifierNumber) {
        //email info
        PersonalDetails personalDetails = travellerMasterInfo.getTravellersList().get(0).getPersonalDetails();
        List<DataElementsIndiv> dataElementsDivList = new ArrayList<>();
        DataElementsIndiv de = new DataElementsIndiv();
        ElementManagementSegmentType elementManagementData = new ElementManagementSegmentType();

        elementManagementData.setSegmentName("AP");
        ReferencingDetailsType rf = new ReferencingDetailsType();
        rf.setQualifier("OT");
        rf.setNumber((++qualifierNumber)+"");
        elementManagementData.setReference(rf);
        de.setElementManagementData(elementManagementData);

        LongFreeTextType ftd = new LongFreeTextType();
        de.setFreetextData(ftd);
        FreeTextQualificationType ftdt = new FreeTextQualificationType();
        ftdt.setSubjectQualifier("3");
        ftdt.setType("P02");
        ftd.setFreetextDetail(ftdt);
        ftd.setLongFreetext(personalDetails.getEmail());
        dataElementsDivList.add(de);

        //home contact number
        de = new DataElementsIndiv();
        elementManagementData = new ElementManagementSegmentType();

        elementManagementData.setSegmentName("AP");
        rf = new ReferencingDetailsType();
        rf.setQualifier("OT");
        rf.setNumber(++qualifierNumber+"");
        elementManagementData.setReference(rf);
        de.setElementManagementData(elementManagementData);

        ftd = new LongFreeTextType();
        de.setFreetextData(ftd);
        ftdt = new FreeTextQualificationType();
        ftdt.setSubjectQualifier("3");
        ftdt.setType("3");
        ftd.setFreetextDetail(ftdt);
        ftd.setLongFreetext(personalDetails.getMobileNumber());
        dataElementsDivList.add(de);

        //emergency contact number
        de = new DataElementsIndiv();
        elementManagementData = new ElementManagementSegmentType();

        elementManagementData.setSegmentName("OS");
        rf = new ReferencingDetailsType();
        rf.setQualifier("OT");
        rf.setNumber((++qualifierNumber)+"");
        elementManagementData.setReference(rf);
        de.setElementManagementData(elementManagementData);

        ftd = new LongFreeTextType();
        de.setFreetextData(ftd);
        ftdt = new FreeTextQualificationType();
        ftdt.setSubjectQualifier("3");
        ftdt.setType("3");
        ftd.setFreetextDetail(ftdt);
        ftd.setLongFreetext("Emergency Contact Number "+personalDetails.getEmergencyContactNumber());
        dataElementsDivList.add(de);
        return dataElementsDivList;
    }
    //credit card info
    public DataElementsMaster.DataElementsIndiv addCreditCardData(int qualifierNumber) {
        DataElementsMaster.DataElementsIndiv de = new DataElementsMaster.DataElementsIndiv();
        ElementManagementSegmentType emd = new ElementManagementSegmentType();
        emd.setSegmentName("FP");
        ReferencingDetailsType rf = new ReferencingDetailsType();
        rf.setQualifier("OT");
        rf.setNumber((++qualifierNumber)+"");
        emd.setReference(rf);

        FormOfPaymentTypeI fop = new FormOfPaymentTypeI();
        FormOfPaymentDetailsTypeI fopd = new FormOfPaymentDetailsTypeI();
        fop.getFop().add(fopd);
        //fopd.setAccountNumber("4111111111111111");
        fopd.setIdentification("CA");
        //fopd.setCreditCardCode("VI");
        //fopd.setExpiryDate("0113");
        //fopd.setApprovalCode(null);
        //fopd.setCurrencyCode("EUR");

        de.setFormOfPayment(fop);
        //FopExtension fope = new FopExtension();
        //fope.setFopSequenceNumber(BigDecimal.ONE);
        //de.getFopExtension().add(fope);
        de.setElementManagementData(emd);
        return de;
    }
    //info for ticketing agent
    public DataElementsMaster.DataElementsIndiv addReceivedFrom(int qualifierNumber) {
        DataElementsMaster.DataElementsIndiv de = new DataElementsMaster.DataElementsIndiv();
        ElementManagementSegmentType emd = new ElementManagementSegmentType();
        emd.setSegmentName("RF");

        ReferencingDetailsType rf = new ReferencingDetailsType();
        rf.setQualifier("OT");
        rf.setNumber((++qualifierNumber)+"");
        emd.setReference(rf);

        LongFreeTextType ftd = new LongFreeTextType();
        de.setFreetextData(ftd);

        FreeTextQualificationType ftdt = new FreeTextQualificationType();
        ftdt.setSubjectQualifier("3");
        ftdt.setType("P22");
        ftd.setFreetextDetail(ftdt);
        ftd.setLongFreetext("internet/BOMVS34C3");
        de.setElementManagementData(emd);
        return de;
    }

    //ticketing arrangement info
    public DataElementsIndiv addTckArr(int qualifierNumber) {
        DataElementsIndiv de = new DataElementsIndiv();
        ElementManagementSegmentType emd = new ElementManagementSegmentType();
        emd.setSegmentName("TK");

        ReferencingDetailsType rf = new ReferencingDetailsType();
        rf.setQualifier("OT");
        rf.setNumber((++qualifierNumber)+"");
        emd.setReference(rf);

        TicketElementType te = new TicketElementType();
        TicketInformationType tck = new TicketInformationType();
        tck.setIndicator("OK");
        te.setTicket(tck);
        te.setPassengerType("PAX");
        de.setTicketElement(te);
        de.setElementManagementData(emd);
        return de;
    }
    //end of transaction info
    public DataElementsIndiv addEOTInfo() {
        DataElementsIndiv de = new DataElementsIndiv();
        ElementManagementSegmentType emd = new ElementManagementSegmentType();
        emd.setSegmentName("RF");

        LongFreeTextType ftd = new LongFreeTextType();
        de.setFreetextData(ftd);

        FreeTextQualificationType ftdt = new FreeTextQualificationType();
        ftdt.setSubjectQualifier("3");
        ftdt.setType("P22");
        ftd.setFreetextDetail(ftdt);
        ftd.setLongFreetext("RF ADDED VIA PNRADD");
        de.setElementManagementData(emd);
        return de;
    }
    public List<DataElementsIndiv> addAdditionalPassengerDetails(TravellerMasterInfo travellerMasterInfo, int qualifierNumber){
        int passengerReference = 1;
        List<DataElementsIndiv> dataElementsDivList = new ArrayList<>();
        for(com.compassites.model.traveller.Traveller traveller : travellerMasterInfo.getTravellersList()){
            if(StringUtils.hasText(traveller.getPassportDetails().getPassportNumber())) {
                dataElementsDivList.add(addPassportDetails(traveller, qualifierNumber, passengerReference));
            }
            Preferences preferences = traveller.getPreferences();
            if(StringUtils.hasText(preferences.getMeal())){
                dataElementsDivList.add(addMealPreference(traveller, qualifierNumber, passengerReference));
            }
            if(StringUtils.hasText(preferences.getFrequentFlyerAirlines()) &&  StringUtils.hasText(preferences.getFrequentFlyerNumber())){
                dataElementsDivList.add(addFrequentFlyerNumber(traveller, qualifierNumber, passengerReference));
            }
            passengerReference++;
        }
        return dataElementsDivList;
    }

    public DataElementsIndiv addMealPreference(com.compassites.model.traveller.Traveller traveller, int qualifierNumber, int passengerRefNumber){
        DataElementsIndiv de = new DataElementsIndiv();
        ElementManagementSegmentType elementManagementData = new ElementManagementSegmentType();
        elementManagementData.setSegmentName("SSR");
        de.setElementManagementData(elementManagementData);

        SpecialRequirementsDetailsTypeI serviceRequest = new SpecialRequirementsDetailsTypeI();
        SpecialRequirementsTypeDetailsTypeI ssr = new SpecialRequirementsTypeDetailsTypeI();
        ssr.setType(traveller.getPreferences().getMeal());
        ssr.setStatus("NN");
        ssr.setQuantity(BigInteger.valueOf(1));

        serviceRequest.setSsr(ssr);
        de.setServiceRequest(serviceRequest);

        ReferenceInfoType referenceForDataElement = new ReferenceInfoType();
        List<ReferencingDetailsType> referenceList = referenceForDataElement.getReference();
        ReferencingDetailsType rf = new ReferencingDetailsType();
        rf.setQualifier("PR");
        rf.setNumber("" + (passengerRefNumber));
        referenceList.add(rf);
        de.setReferenceForDataElement(referenceForDataElement);

        return de;
    }

    public DataElementsIndiv addFrequentFlyerNumber(com.compassites.model.traveller.Traveller traveller, int qualifierNumber, int passengerRefNumber){
        DataElementsIndiv de = new DataElementsIndiv();
        ElementManagementSegmentType elementManagementData = new ElementManagementSegmentType();
        elementManagementData.setSegmentName("SSR");
        de.setElementManagementData(elementManagementData);

        SpecialRequirementsDetailsTypeI serviceRequest = new SpecialRequirementsDetailsTypeI();
        SpecialRequirementsTypeDetailsTypeI ssr = new SpecialRequirementsTypeDetailsTypeI();
        ssr.setType("FQTV");
        ssr.setCompanyId(traveller.getPreferences().getFrequentFlyerAirlines());
        serviceRequest.setSsr(ssr);
        de.setServiceRequest(serviceRequest);

        FrequentTravellerInformationTypeU frequentTravellerData = new FrequentTravellerInformationTypeU();
        FrequentTravellerIdentificationTypeU frequentTraveller = new FrequentTravellerIdentificationTypeU();
        frequentTraveller.setCompanyId(traveller.getPreferences().getFrequentFlyerAirlines());
        frequentTraveller.setMembershipNumber(traveller.getPreferences().getFrequentFlyerNumber());
        frequentTravellerData.setFrequentTraveller(frequentTraveller);
        de.setFrequentTravellerData(frequentTravellerData);

        ReferenceInfoType referenceForDataElement = new ReferenceInfoType();
        List<ReferencingDetailsType> referenceList = referenceForDataElement.getReference();
        ReferencingDetailsType rf = new ReferencingDetailsType();
        rf.setQualifier("PR");
        rf.setNumber("" + (passengerRefNumber));
        referenceList.add(rf);
        de.setReferenceForDataElement(referenceForDataElement);

        return de;
    }
    public DataElementsIndiv addPassportDetails(com.compassites.model.traveller.Traveller traveller, int qualifierNumber, int passengerRefNumber){
        DataElementsIndiv de = new DataElementsIndiv();
        PassportDetails passportDetails = traveller.getPassportDetails();

        ElementManagementSegmentType elementManagementData = new ElementManagementSegmentType();
        /*Reference refrence = new Reference();
        refrence.setQualifier("OT");
        refrence.setNumber((++qualifierNumber)+"");
        elementManagementData.setReference(refrence); */
        elementManagementData.setSegmentName("SSR");
        de.setElementManagementData(elementManagementData);

        SpecialRequirementsDetailsTypeI serviceRequest = new SpecialRequirementsDetailsTypeI();
        SpecialRequirementsTypeDetailsTypeI ssr = new SpecialRequirementsTypeDetailsTypeI();
        ssr.setType("DOCS");
        ssr.setStatus("HK");
        ssr.setQuantity(BigInteger.valueOf(1));
        //Todo -- remove the hard coded company value
        ssr.setCompanyId("YY");
        List<String> freeTextList = ssr.getFreetext();
        SimpleDateFormat ddMMMyyFormat = new SimpleDateFormat("ddMMMyy");
        // Sample text P-IND-H12232323-IND-30JUN73-M-14APR09-JOHNSON-SIMON

        String freeText = "P-IND-" +passportDetails.getPassportNumber()+"-IND-"+ ddMMMyyFormat.format(passportDetails.getDateOfBirth())
                +"-"+ StringUtility.getGenderCode(traveller.getPersonalDetails().getGender())+"-"+ddMMMyyFormat.format(passportDetails.getDateOfExpiry())+"-"+
                traveller.getPersonalDetails().getFirstName()+"-"+traveller.getPersonalDetails().getLastName();
        //String freeText = "P-IND-H12232323-IND-30JUN73-M-14APR09-JOHNSON-SIMON";
        freeTextList.add(freeText);

        serviceRequest.setSsr(ssr);
        de.setServiceRequest(serviceRequest);

        ReferenceInfoType referenceForDataElement = new ReferenceInfoType();
        List<ReferencingDetailsType> referenceList = referenceForDataElement.getReference();
        ReferencingDetailsType rf = new ReferencingDetailsType();
        rf.setQualifier("PR");
        rf.setNumber("" + (passengerRefNumber));
        referenceList.add(rf);
        de.setReferenceForDataElement(referenceForDataElement);

        return de;
    }
}
