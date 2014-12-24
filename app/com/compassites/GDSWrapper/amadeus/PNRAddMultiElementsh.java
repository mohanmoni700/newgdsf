package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements;
import com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements.DataElementsMaster;
import com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements.DataElementsMaster.DataElementsIndiv;
import com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements.DataElementsMaster.DataElementsIndiv.ElementManagementData;
import com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements.DataElementsMaster.DataElementsIndiv.ElementManagementData.Reference;
import com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements.DataElementsMaster.DataElementsIndiv.FormOfPayment;
import com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements.DataElementsMaster.DataElementsIndiv.FormOfPayment.Fop;
import com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements.DataElementsMaster.DataElementsIndiv.FreetextData;
import com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements.DataElementsMaster.DataElementsIndiv.FreetextData.FreetextDetail;
import com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements.DataElementsMaster.DataElementsIndiv.TicketElement;
import com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements.DataElementsMaster.DataElementsIndiv.TicketElement.Ticket;
import com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements.DataElementsMaster.Marker1;
import com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements.PnrActions;
import com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements.TravellerInfo;
import com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements.TravellerInfo.ElementManagementPassenger;
import com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements.TravellerInfo.PassengerData;
import com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements.TravellerInfo.PassengerData.TravellerInformation;
import com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements.TravellerInfo.PassengerData.TravellerInformation.Passenger;
import com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements.TravellerInfo.PassengerData.TravellerInformation.Traveller;
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

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PNRAddMultiElementsh {

    public PNRAddMultiElements getMultiElements(TravellerMasterInfo travellerMasterInfo) {
        PNRAddMultiElements element = new PNRAddMultiElements();
        PnrActions pnrActions = new PnrActions();
        pnrActions.getOptionCode().add(new BigDecimal("0"));
        element.setPnrActions(pnrActions);
        
        //element.getTravellerInfo().add(addPassenger());
        //element.getTravellerInfo().add(addChildPassenger());
        //element.getTravellerInfo().add(addInfantPassenger());
        element.getTravellerInfo().addAll(getPassengersList(travellerMasterInfo))  ;
        DataElementsMaster dem = new DataElementsMaster();
        dem.setMarker1(new Marker1());
        int qualifierNumber = 0;
        dem.getDataElementsIndiv().add(addCreditCardData(qualifierNumber));
        dem.getDataElementsIndiv().add(addReceivedFrom(qualifierNumber));
        dem.getDataElementsIndiv().add(addTckArr(qualifierNumber));
        dem.getDataElementsIndiv().addAll(addContactInfo(travellerMasterInfo, qualifierNumber));
        dem.getDataElementsIndiv().addAll(addAdditionalPassengerDetails(travellerMasterInfo, qualifierNumber));
        //dem.getDataElementsIndiv().add(addEOTInfo());

        element.setDataElementsMaster(dem);
        return element;
    }


    private List<TravellerInfo> getPassengersList(TravellerMasterInfo travellerMasterInfo){

        int passengerCount = 1;
        List<TravellerInfo> travellerInfoList = new ArrayList<>();

        for (com.compassites.model.traveller.Traveller traveller : travellerMasterInfo.getTravellersList()){
            TravellerInfo travellerInfo = new TravellerInfo();
            ElementManagementPassenger emp = new ElementManagementPassenger();
            ElementManagementPassenger.Reference rf = new ElementManagementPassenger.Reference();
            rf.setNumber(String.valueOf(passengerCount++));
            rf.setQualifier("PR");
            emp.setReference(rf);
            emp.setSegmentName("NM");
            travellerInfo.setElementManagementPassenger(emp);

            PassengerData passengerData = new PassengerData();
            TravellerInformation ti = new TravellerInformation();
            Traveller tr = new Traveller();
            tr.setSurname(traveller.getPersonalDetails().getLastName());
            tr.setQuantity(new BigDecimal("1"));

            Passenger p = new Passenger();
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
        ElementManagementPassenger emp = new ElementManagementPassenger();
        ElementManagementPassenger.Reference rf = new ElementManagementPassenger.Reference();
        rf.setNumber("1");
        rf.setQualifier("PR");
        emp.setReference(rf);
        emp.setSegmentName("NM");
        travellerInfo.setElementManagementPassenger(emp);

        PassengerData passengerData = new PassengerData();
        TravellerInformation ti = new TravellerInformation();
        Traveller tr = new Traveller();
        tr.setSurname("DUPONT");
        tr.setQuantity(new BigDecimal("1"));

        Passenger p = new Passenger();
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
        ElementManagementPassenger emp = new ElementManagementPassenger();
        ElementManagementPassenger.Reference rf = new ElementManagementPassenger.Reference();
        rf.setNumber("2");
        rf.setQualifier("PR");
        emp.setReference(rf);
        emp.setSegmentName("NM");
        travellerInfo.setElementManagementPassenger(emp);

        PassengerData passengerData = new PassengerData();
        TravellerInformation ti = new TravellerInformation();
        Traveller tr = new Traveller();
        tr.setSurname("DUPONTER");
        tr.setQuantity(new BigDecimal("1"));

        Passenger p = new Passenger();
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
        ElementManagementPassenger emp = new ElementManagementPassenger();
        ElementManagementPassenger.Reference rf = new ElementManagementPassenger.Reference();
        rf.setNumber("3");
        rf.setQualifier("PR");
        emp.setReference(rf);
        emp.setSegmentName("NM");
        travellerInfo.setElementManagementPassenger(emp);

        PassengerData passengerData = new PassengerData();
        TravellerInformation ti = new TravellerInformation();
        Traveller tr = new Traveller();
        tr.setSurname("DUPONTERINF");
        tr.setQuantity(new BigDecimal("1"));

        Passenger p = new Passenger();
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
        PnrActions pnrActions = new PnrActions();
        pnrActions.getOptionCode().add(new BigDecimal("10"));
        element.setPnrActions(pnrActions);

        DataElementsMaster dataElementsMaster =  new DataElementsMaster();
        DataElementsIndiv dataElementsIndiv = new DataElementsIndiv();

        ElementManagementData elementManagementData = new ElementManagementData();
        Reference reference =  new Reference();
        reference.setNumber("15");
        reference.setQualifier("OT");
        elementManagementData.setSegmentName("RF");
        elementManagementData.setReference(reference);
        dataElementsIndiv.setElementManagementData(elementManagementData);

        FreetextData freetextData = new FreetextData();
        FreetextDetail freetextDetail = new FreetextDetail();
        freetextDetail.setSubjectQualifier("3");
        freetextDetail.setType("P22");
        freetextData.setFreetextDetail(freetextDetail);
        freetextData.setLongFreetext("Internet");
        dataElementsIndiv.setFreetextData(freetextData);
        dataElementsMaster.getDataElementsIndiv().add(dataElementsIndiv);
        dataElementsMaster.setMarker1(new Marker1());
        element.setDataElementsMaster(dataElementsMaster);

        return element;
    }
   
    public PNRAddMultiElements addEotTimeElement() {
        PNRAddMultiElements element = new PNRAddMultiElements();
        PnrActions pnrActions = new PnrActions();
        pnrActions.getOptionCode().add(new BigDecimal("0"));
        element.setPnrActions(pnrActions);
        DataElementsMaster dem = new DataElementsMaster();
        dem.setMarker1(new Marker1());
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
        ElementManagementData elementManagementData = new ElementManagementData();

        elementManagementData.setSegmentName("AP");
        Reference rf = new Reference();
        rf.setQualifier("OT");
        rf.setNumber((++qualifierNumber)+"");
        elementManagementData.setReference(rf);
        de.setElementManagementData(elementManagementData);

        FreetextData ftd = new FreetextData();
        de.setFreetextData(ftd);
        FreetextDetail ftdt = new FreetextDetail();
        ftdt.setSubjectQualifier("3");
        ftdt.setType("P02");
        ftd.setFreetextDetail(ftdt);
        ftd.setLongFreetext(personalDetails.getEmail());
        dataElementsDivList.add(de);

        //home contact number
        de = new DataElementsIndiv();
        elementManagementData = new ElementManagementData();

        elementManagementData.setSegmentName("AP");
        rf = new Reference();
        rf.setQualifier("OT");
        rf.setNumber(++qualifierNumber+"");
        elementManagementData.setReference(rf);
        de.setElementManagementData(elementManagementData);

        ftd = new FreetextData();
        de.setFreetextData(ftd);
        ftdt = new FreetextDetail();
        ftdt.setSubjectQualifier("3");
        ftdt.setType("3");
        ftd.setFreetextDetail(ftdt);
        ftd.setLongFreetext(personalDetails.getMobileNumber());
        dataElementsDivList.add(de);

        //emergency contact number
        de = new DataElementsIndiv();
        elementManagementData = new ElementManagementData();

        elementManagementData.setSegmentName("OS");
        rf = new Reference();
        rf.setQualifier("OT");
        rf.setNumber((++qualifierNumber)+"");
        elementManagementData.setReference(rf);
        de.setElementManagementData(elementManagementData);

        ftd = new FreetextData();
        de.setFreetextData(ftd);
        ftdt = new FreetextDetail();
        ftdt.setSubjectQualifier("3");
        ftdt.setType("3");
        ftd.setFreetextDetail(ftdt);
        ftd.setLongFreetext("Emergency Contact Number "+personalDetails.getEmergencyContactNumber());
        dataElementsDivList.add(de);
        return dataElementsDivList;
    }
    //credit card info
    public DataElementsIndiv addCreditCardData(int qualifierNumber) {
        DataElementsIndiv de = new DataElementsIndiv();
        ElementManagementData emd = new ElementManagementData();
        emd.setSegmentName("FP");
        Reference rf = new Reference();
        rf.setQualifier("OT");
        rf.setNumber((++qualifierNumber)+"");
        emd.setReference(rf);

        FormOfPayment fop = new FormOfPayment();
        Fop fopd = new Fop();
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
    public DataElementsIndiv addReceivedFrom(int qualifierNumber) {
        DataElementsIndiv de = new DataElementsIndiv();
        ElementManagementData emd = new ElementManagementData();
        emd.setSegmentName("RF");
        
        Reference rf = new Reference();
        rf.setQualifier("OT");
        rf.setNumber((++qualifierNumber)+"");
        emd.setReference(rf);
        
        FreetextData ftd = new FreetextData();
        de.setFreetextData(ftd);

        FreetextDetail ftdt = new FreetextDetail();
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
        ElementManagementData emd = new ElementManagementData();
        emd.setSegmentName("TK");

        Reference rf = new Reference();
        rf.setQualifier("OT");
        rf.setNumber((++qualifierNumber)+"");
        emd.setReference(rf);

        TicketElement te = new TicketElement();
        Ticket tck = new Ticket();
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
        ElementManagementData emd = new ElementManagementData();
        emd.setSegmentName("RF");

        FreetextData ftd = new FreetextData();
        de.setFreetextData(ftd);

        FreetextDetail ftdt = new FreetextDetail();
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
        ElementManagementData elementManagementData = new ElementManagementData();
        elementManagementData.setSegmentName("SSR");
        de.setElementManagementData(elementManagementData);

        DataElementsIndiv.ServiceRequest serviceRequest = new DataElementsIndiv.ServiceRequest();
        DataElementsIndiv.ServiceRequest.Ssr ssr = new DataElementsIndiv.ServiceRequest.Ssr();
        ssr.setType(traveller.getPreferences().getMeal());
        ssr.setStatus("NN");
        ssr.setQuantity(new BigDecimal(1));

        serviceRequest.setSsr(ssr);
        de.setServiceRequest(serviceRequest);

        DataElementsIndiv.ReferenceForDataElement referenceForDataElement = new DataElementsIndiv.ReferenceForDataElement();
        List<DataElementsIndiv.ReferenceForDataElement.Reference> referenceList = referenceForDataElement.getReference();
        DataElementsIndiv.ReferenceForDataElement.Reference rf = new DataElementsIndiv.ReferenceForDataElement.Reference();
        rf.setQualifier("PR");
        rf.setNumber("" + (passengerRefNumber));
        referenceList.add(rf);
        de.setReferenceForDataElement(referenceForDataElement);

        return de;
    }

    public DataElementsIndiv addFrequentFlyerNumber(com.compassites.model.traveller.Traveller traveller, int qualifierNumber, int passengerRefNumber){
        DataElementsIndiv de = new DataElementsIndiv();
        ElementManagementData elementManagementData = new ElementManagementData();
        elementManagementData.setSegmentName("SSR");
        de.setElementManagementData(elementManagementData);

        DataElementsIndiv.ServiceRequest serviceRequest = new DataElementsIndiv.ServiceRequest();
        DataElementsIndiv.ServiceRequest.Ssr ssr = new DataElementsIndiv.ServiceRequest.Ssr();
        ssr.setType("FQTV");
        ssr.setCompanyId(traveller.getPreferences().getFrequentFlyerAirlines());
        serviceRequest.setSsr(ssr);
        de.setServiceRequest(serviceRequest);

        DataElementsIndiv.FrequentTravellerData frequentTravellerData = new DataElementsIndiv.FrequentTravellerData();
        DataElementsIndiv.FrequentTravellerData.FrequentTraveller frequentTraveller = new DataElementsIndiv.FrequentTravellerData.FrequentTraveller();
        frequentTraveller.setCompanyId(traveller.getPreferences().getFrequentFlyerAirlines());
        frequentTraveller.setMembershipNumber(traveller.getPreferences().getFrequentFlyerNumber());
        frequentTravellerData.setFrequentTraveller(frequentTraveller);
        de.setFrequentTravellerData(frequentTravellerData);

        DataElementsIndiv.ReferenceForDataElement referenceForDataElement = new DataElementsIndiv.ReferenceForDataElement();
        List<DataElementsIndiv.ReferenceForDataElement.Reference> referenceList = referenceForDataElement.getReference();
        DataElementsIndiv.ReferenceForDataElement.Reference rf = new DataElementsIndiv.ReferenceForDataElement.Reference();
        rf.setQualifier("PR");
        rf.setNumber("" + (passengerRefNumber));
        referenceList.add(rf);
        de.setReferenceForDataElement(referenceForDataElement);

        return de;
    }
    public DataElementsIndiv addPassportDetails(com.compassites.model.traveller.Traveller traveller, int qualifierNumber, int passengerRefNumber){
        DataElementsIndiv de = new DataElementsIndiv();
        PassportDetails passportDetails = traveller.getPassportDetails();

        ElementManagementData elementManagementData = new ElementManagementData();
        /*Reference refrence = new Reference();
        refrence.setQualifier("OT");
        refrence.setNumber((++qualifierNumber)+"");
        elementManagementData.setReference(refrence); */
        elementManagementData.setSegmentName("SSR");
        de.setElementManagementData(elementManagementData);

        DataElementsIndiv.ServiceRequest serviceRequest = new DataElementsIndiv.ServiceRequest();
        DataElementsIndiv.ServiceRequest.Ssr ssr = new DataElementsIndiv.ServiceRequest.Ssr();
        ssr.setType("DOCS");
        ssr.setStatus("HK");
        ssr.setQuantity(new BigDecimal(1));
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

        DataElementsIndiv.ReferenceForDataElement referenceForDataElement = new DataElementsIndiv.ReferenceForDataElement();
        List<DataElementsIndiv.ReferenceForDataElement.Reference> referenceList = referenceForDataElement.getReference();
        DataElementsIndiv.ReferenceForDataElement.Reference rf = new DataElementsIndiv.ReferenceForDataElement.Reference();
        rf.setQualifier("PR");
        rf.setNumber("" + (passengerRefNumber));
        referenceList.add(rf);
        de.setReferenceForDataElement(referenceForDataElement);

        return de;
    }
}
