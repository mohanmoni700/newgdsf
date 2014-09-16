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
import com.compassites.model.traveller.TravellerMasterInfo;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import java.math.BigDecimal;
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
        
        dem.getDataElementsIndiv().add(addCreditCardData());
        dem.getDataElementsIndiv().add(addReceivedFrom());
        dem.getDataElementsIndiv().add(addTckArr());
        dem.getDataElementsIndiv().add(addContactInfo());
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
            rf.setNumber(String.valueOf(passengerCount));
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
            passengerType = "CH";
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
    public DataElementsIndiv addContactInfo() {
        DataElementsIndiv de = new DataElementsIndiv();
        ElementManagementData emd = new ElementManagementData();
        emd.setSegmentName("AP");

        Reference rf = new Reference();
        rf.setQualifier("OT");
        rf.setNumber("1");
        emd.setReference(rf);

        FreetextData ftd = new FreetextData();
        de.setFreetextData(ftd);

        FreetextDetail ftdt = new FreetextDetail();
        ftdt.setSubjectQualifier("3");
        ftdt.setType("P02");
        ftd.setFreetextDetail(ftdt);
        ftd.setLongFreetext("LCHILDRENS@AMADEUS.NET");
        de.setElementManagementData(emd);
        return de;
    }
    //credit card info
    public DataElementsIndiv addCreditCardData() {
        DataElementsIndiv de = new DataElementsIndiv();
        ElementManagementData emd = new ElementManagementData();
        emd.setSegmentName("FP");
        Reference rf = new Reference();
        rf.setQualifier("OT");
        rf.setNumber("2");
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
    public DataElementsIndiv addReceivedFrom() {
        DataElementsIndiv de = new DataElementsIndiv();
        ElementManagementData emd = new ElementManagementData();
        emd.setSegmentName("RF");
        
        Reference rf = new Reference();
        rf.setQualifier("OT");
        rf.setNumber("3");
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
    public DataElementsIndiv addTckArr() {
        DataElementsIndiv de = new DataElementsIndiv();
        ElementManagementData emd = new ElementManagementData();
        emd.setSegmentName("TK");

        Reference rf = new Reference();
        rf.setQualifier("OT");
        rf.setNumber("4");
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
}
