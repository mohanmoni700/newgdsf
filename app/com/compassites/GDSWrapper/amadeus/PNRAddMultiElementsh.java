package com.compassites.GDSWrapper.amadeus;


import com.amadeus.xml.pnradd_10_1_1a.*;
import com.amadeus.xml.pnradd_11_3_1a.*;
import com.amadeus.xml.pnradd_11_3_1a.PNRAddMultiElements;
import com.amadeus.xml.pnradd_11_3_1a.PNRAddMultiElements.DataElementsMaster;
import com.amadeus.xml.pnradd_11_3_1a.PNRAddMultiElements.DataElementsMaster.DataElementsIndiv;
import com.amadeus.xml.pnradd_11_3_1a.PNRAddMultiElements.TravellerInfo;
import com.compassites.constants.AmadeusConstants;
import com.compassites.model.AirSegmentInformation;
import com.compassites.model.Journey;
import com.compassites.model.PassengerTypeCode;
import com.compassites.model.traveller.*;
import models.NationalityDao;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.util.StringUtils;

import utils.DateUtility;
import utils.StringUtility;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.amadeus.xml.pnradd_11_3_1a.PNRAddMultiElements.DataElementsMaster.*;

public class PNRAddMultiElementsh {

    public PNRAddMultiElements getMultiElements(TravellerMasterInfo travellerMasterInfo) {
        PNRAddMultiElements element = new PNRAddMultiElements();
        OptionalPNRActionsType pnrActions = new OptionalPNRActionsType();
        pnrActions.getOptionCode().add(new BigInteger("0"));
        element.setPnrActions(pnrActions);

        addLiveEntry(element,travellerMasterInfo);
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

    public void addLiveEntry(PNRAddMultiElements element,TravellerMasterInfo travellerMasterInfo){
        List<Journey> journeyList = travellerMasterInfo.getItinerary().getJourneys(travellerMasterInfo.isSeamen());
        List<AirSegmentInformation> airSegmentInformations = journeyList.get(journeyList.size()-1).getAirSegmentList();
        BigInteger paxCount = BigInteger.valueOf(travellerMasterInfo.getTravellersList().size());
        String fromLocation = journeyList.get(0).getAirSegmentList().get(0).getFromLocation();
        String toLocation = journeyList.get(journeyList.size()-1).getAirSegmentList().get(airSegmentInformations.size()-1).getToLocation();

        Date bookingDate = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(bookingDate);
        cal.add(Calendar.MONTH, 11);
        Integer month = cal.get(Calendar.MONTH)+1;
        Integer year = cal.get(Calendar.YEAR);
        String bookingMonth = String.format("%02d",month);
        String depDate = "01"+bookingMonth+year.toString().substring(2,4);

        List<PNRAddMultiElements.OriginDestinationDetails> originDestinationDetailsList = new ArrayList<>();
        PNRAddMultiElements.OriginDestinationDetails originDestinationDetails = new PNRAddMultiElements.OriginDestinationDetails();
        OriginAndDestinationDetailsTypeI originAndDestinationDetailsTypeI = new OriginAndDestinationDetailsTypeI();
        originAndDestinationDetailsTypeI.setOrigin(fromLocation);
        originAndDestinationDetailsTypeI.setDestination(toLocation);
        originDestinationDetails.setOriginDestination(originAndDestinationDetailsTypeI);

        List<PNRAddMultiElements.OriginDestinationDetails.ItineraryInfo> itineraryInfos = new ArrayList<>();
        PNRAddMultiElements.OriginDestinationDetails.ItineraryInfo itineraryInfo = new PNRAddMultiElements.OriginDestinationDetails.ItineraryInfo();
        ElementManagementSegmentType elementManagementItinerary = new ElementManagementSegmentType();
        ReferencingDetailsType referencingDetailsType = new ReferencingDetailsType();
        referencingDetailsType.setNumber("1");
        referencingDetailsType.setQualifier("OT");
        elementManagementItinerary.setSegmentName("RU");
        elementManagementItinerary.setReference(referencingDetailsType);
        itineraryInfo.setElementManagementItinerary(elementManagementItinerary);

        PNRAddMultiElements.OriginDestinationDetails.ItineraryInfo.AirAuxItinerary airAuxItinerary = new PNRAddMultiElements.OriginDestinationDetails.ItineraryInfo.AirAuxItinerary();
        TravelProductInformationType travelProduct = new TravelProductInformationType();
        ProductDateTimeTypeI product = new ProductDateTimeTypeI();
        product.setDepDate(depDate);
        travelProduct.setProduct(product);

        LocationTypeI boardpointDetail = new LocationTypeI();
        boardpointDetail.setCityCode(fromLocation);
        travelProduct.setBoardpointDetail(boardpointDetail);

        CompanyIdentificationTypeI company = new CompanyIdentificationTypeI();
        company.setIdentification("1A");
        travelProduct.setCompany(company);

        airAuxItinerary.setTravelProduct(travelProduct);

        MessageActionDetailsTypeI messageActionDetailsTypeI = new MessageActionDetailsTypeI();
        MessageFunctionBusinessDetailsTypeI business = new MessageFunctionBusinessDetailsTypeI();
        business.setFunction("32");
        messageActionDetailsTypeI.setBusiness(business);
        airAuxItinerary.setMessageAction(messageActionDetailsTypeI);

        RelatedProductInformationTypeI relatedProductInformationTypeI = new RelatedProductInformationTypeI();
        relatedProductInformationTypeI.setQuantity(paxCount);
        relatedProductInformationTypeI.setStatus("HK");
        airAuxItinerary.setRelatedProduct(relatedProductInformationTypeI);

        LongFreeTextType freetextItinerary = new LongFreeTextType();
        FreeTextQualificationType freetextDetail1 = new FreeTextQualificationType();
        freetextDetail1.setSubjectQualifier("3");
        freetextDetail1.setCompanyId("TK");
        freetextItinerary.setFreetextDetail(freetextDetail1);
        //freetextItinerary.setLongFreetext("THANK YOU FOR CHOOSING Flyhi TRAVELS");
        airAuxItinerary.setFreetextItinerary(freetextItinerary);
        itineraryInfo.setAirAuxItinerary(airAuxItinerary);
        itineraryInfos.add(itineraryInfo);
        originDestinationDetails.getItineraryInfo().addAll(itineraryInfos);
        originDestinationDetailsList.add(originDestinationDetails);

        element.getOriginDestinationDetails().addAll(originDestinationDetailsList);
    }

    public PNRAddMultiElements addSSRDetails(TravellerMasterInfo travellerMasterInfo, List<String> segmentNumbers, Map<String,String> travellerMap) {
        PNRAddMultiElements element = new PNRAddMultiElements();
        OptionalPNRActionsType pnrActions = new OptionalPNRActionsType();
        pnrActions.getOptionCode().add(new BigInteger("0"));
        element.setPnrActions(pnrActions);

//        element.getTravellerInfo().addAll(getPassengersList(travellerMasterInfo))  ;
        DataElementsMaster dem = new DataElementsMaster();
        dem.setMarker1(new DummySegmentTypeI());
        int qualifierNumber = 0;
        dem.getDataElementsIndiv().addAll(addAdditionalPassengerDetails(travellerMasterInfo, qualifierNumber, segmentNumbers, travellerMap));
        element.setDataElementsMaster(dem);
        return element;
    }

    private List<TravellerInfo> getPassengersList(TravellerMasterInfo travellerMasterInfo){

        int passengerCount = 1;
        List<TravellerInfo> travellerInfoList = new ArrayList<>();
        boolean isSeamen = travellerMasterInfo.isSeamen();

        List<Traveller> travellers = new ArrayList<>();
        travellers.addAll(travellerMasterInfo.getTravellersList()); //copying travellers as the list would be modified
        List<Traveller> infantTravellerList = new ArrayList<>();
        if(!isSeamen) {
            infantTravellerList = getInfantTravellerList(travellers);  //also removes the infant from traveller
        }
        int infantIndex = 0;
        for (com.compassites.model.traveller.Traveller traveller : travellers){
            TravellerInfo travellerInfo = new TravellerInfo();
            ElementManagementSegmentType emp = new ElementManagementSegmentType();
            ReferencingDetailsType rf = new ReferencingDetailsType();
            rf.setNumber(String.valueOf(passengerCount++));
            rf.setQualifier("PR");
            emp.setReference(rf);
            emp.setSegmentName("NM");
            travellerInfo.setElementManagementPassenger(emp);

            TravellerInfo.PassengerData passengerData = new TravellerInfo.PassengerData();
            TravellerInformationTypeI travellerInformation = new TravellerInformationTypeI();
            TravellerSurnameInformationTypeI gdsTraveller = new TravellerSurnameInformationTypeI();
            gdsTraveller.setSurname(traveller.getPersonalDetails().getLastName());
            gdsTraveller.setQuantity(new BigInteger("1"));

            TravellerDetailsTypeI passenger = new TravellerDetailsTypeI();
            String salutation = "";

            if(traveller.getPersonalDetails().getSalutation() != null){
                salutation = traveller.getPersonalDetails().getSalutation().replace(".","");
            }

            String name = "";
            if(traveller.getPersonalDetails().getMiddleName() != null){
                name = traveller.getPersonalDetails().getFirstName() + " " + traveller.getPersonalDetails().getMiddleName() + " " + salutation;
            }else {
                name = traveller.getPersonalDetails().getFirstName() + " " + salutation;
            }


            passenger.setFirstName(name);

            String passengerType = DateUtility.getPassengerTypeFromDOB(traveller.getPassportDetails().getDateOfBirth()).toString();
            if(travellerMasterInfo.isSeamen()){
                passenger.setType(PassengerTypeCode.SEA.toString());

            }else {
                passenger.setType(passengerType);
            }

            TravellerInfo.PassengerData infantPassengerData = null;
            if(!isSeamen && "ADT".equalsIgnoreCase(passengerType) && infantTravellerList.size() >= 1){
                passenger.setInfantIndicator("3");
                infantPassengerData = addInfantAssociation(infantTravellerList.get(infantIndex));
                infantTravellerList.remove(infantIndex);
//                infantIndex++;
            }

            travellerInformation.getPassenger().add(passenger);
            travellerInformation.setTraveller(gdsTraveller);
            passengerData.setTravellerInformation(travellerInformation);
            travellerInfo.getPassengerData().add(passengerData);
            if(infantPassengerData != null){
                travellerInfo.getPassengerData().add(infantPassengerData);
            }
            travellerInfoList.add(travellerInfo);
        }

        return travellerInfoList;
    }

    public TravellerInfo.PassengerData addInfantAssociation(Traveller traveller){
        TravellerInfo.PassengerData passengerData = new TravellerInfo.PassengerData();
        TravellerInformationTypeI travellerInformation = new TravellerInformationTypeI();
        TravellerSurnameInformationTypeI gdsTraveller = new TravellerSurnameInformationTypeI();
        gdsTraveller.setSurname(traveller.getPersonalDetails().getLastName());
        gdsTraveller.setQuantity(new BigInteger("1"));


        TravellerDetailsTypeI passenger = new TravellerDetailsTypeI();
        String salutation = "";
        if(traveller.getPersonalDetails().getSalutation() != null){
            salutation = traveller.getPersonalDetails().getSalutation().replace(".","");
        }
        String name = "";
        if(traveller.getPersonalDetails().getMiddleName() != null){
            name = traveller.getPersonalDetails().getFirstName() + " " + traveller.getPersonalDetails().getMiddleName() + " " + salutation;
        }else {
            name = traveller.getPersonalDetails().getFirstName() + " " + salutation;
        }
        passenger.setFirstName(name);
        passenger.setType(getPassengerType(traveller.getPassportDetails().getDateOfBirth()));

        DateAndTimeInformationType dateAndTimeInformationType = new DateAndTimeInformationType();
        DateAndTimeDetailsTypeI56946C dateAndTimeDetails = new DateAndTimeDetailsTypeI56946C();
        dateAndTimeDetails.setQualifier(AmadeusConstants.DOB_QUALIFIER);
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMMyy");
        dateAndTimeDetails.setDate(sdf.format(traveller.getPassportDetails().getDateOfBirth()));
        dateAndTimeInformationType.setDateAndTimeDetails(dateAndTimeDetails);
        passengerData.setDateOfBirth(dateAndTimeInformationType);

        travellerInformation.getPassenger().add(passenger);
        travellerInformation.setTraveller(gdsTraveller);
        passengerData.setTravellerInformation(travellerInformation);

        return passengerData;
    }

    public List<Traveller>  getInfantTravellerList(List<Traveller> travellerList){
        List<Traveller> infantList = new ArrayList<>();
        Iterator<Traveller> travellerIterator = travellerList.iterator();
        while(travellerIterator.hasNext()){
            Traveller traveller = travellerIterator.next();
            String passengerType = DateUtility.getPassengerTypeFromDOB(traveller.getPassportDetails().getDateOfBirth()).toString();
            if("INF".equals(passengerType)){
                infantList.add(traveller);
                travellerIterator.remove();
            }

        }
        return infantList;
    }

    //TODO-- Add Seaman type to the adult passenger
    private String getPassengerType(Date passengerDOB){
        return DateUtility.getPassengerTypeFromDOB(passengerDOB).toString();
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


    public PNRAddMultiElements ignoreAndRetrievePNR(){

        PNRAddMultiElements element = new PNRAddMultiElements();
        OptionalPNRActionsType pnrActions = new OptionalPNRActionsType();
        pnrActions.getOptionCode().add(new BigInteger("21"));
        element.setPnrActions(pnrActions);

        return element;
    }

    public PNRAddMultiElements ignorePNRAddMultiElement(){

        PNRAddMultiElements element = new PNRAddMultiElements();
        OptionalPNRActionsType pnrActions = new OptionalPNRActionsType();
        pnrActions.getOptionCode().add(new BigInteger("20"));
        element.setPnrActions(pnrActions);

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

public DataElementsIndiv addTravelAgentInfo(int qualifierNumber){
    DataElementsIndiv de1 = new DataElementsIndiv();
    LongFreeTextType ftd1 = new LongFreeTextType();
    ftd1.setLongFreetext(play.Play.application().configuration().getString("travelagent.info"));
    FreeTextQualificationType ftdt1 = new FreeTextQualificationType();
    ftdt1.setSubjectQualifier("3");
    ftdt1.setType("5");
    ftd1.setFreetextDetail(ftdt1);
    de1.setFreetextData(ftd1);
    ElementManagementSegmentType elementManagementData1 = new ElementManagementSegmentType();
    elementManagementData1.setSegmentName("AP");
    ReferencingDetailsType rf1 = new ReferencingDetailsType();
    rf1.setQualifier("OT");
    rf1.setNumber((++qualifierNumber)+"");
    elementManagementData1.setReference(rf1);
    de1.setElementManagementData(elementManagementData1);

    return de1;
}
    //contact information
    public List<DataElementsIndiv> addContactInfo(TravellerMasterInfo travellerMasterInfo, int qualifierNumber) {
        //email info
    	int passengerRefnumber = 0;
    	List<DataElementsIndiv> dataElementsDivList = new ArrayList<>();
    	try {
    		for(Traveller traveller :travellerMasterInfo.getTravellersList()){
        		
       		 PersonalDetails personalDetails = traveller.getPersonalDetails();
       		
       		 if (travellerMasterInfo.isSeamen() || (!travellerMasterInfo.isSeamen() && !PassengerTypeCode.INF
 					.equals(DateUtility.getPassengerTypeFromDOB(traveller.getPassportDetails().getDateOfBirth())))) {
       			
       			passengerRefnumber = passengerRefnumber+1;
       			 System.out.println("************passengerRefnumber"+passengerRefnumber);
           		 DataElementsIndiv de1 = new DataElementsIndiv();
       	    		 LongFreeTextType ftd1 = new LongFreeTextType();
       	    		 ftd1.setLongFreetext(personalDetails.getEmail());
       		    		 FreeTextQualificationType ftdt1 = new FreeTextQualificationType();
       		    	     ftdt1.setSubjectQualifier("3");
       		    	     ftdt1.setType("P02");
       		    	 ftd1.setFreetextDetail(ftdt1);
       		     de1.setFreetextData(ftd1);
       	    		 ElementManagementSegmentType elementManagementData1 = new ElementManagementSegmentType();
       	    		 elementManagementData1.setSegmentName("AP");    		 
       		    		 ReferencingDetailsType rf1 = new ReferencingDetailsType();
       		    		 rf1.setQualifier("OT");
       		    	     rf1.setNumber((++qualifierNumber)+"");
       		    	 elementManagementData1.setReference(rf1);
           	     de1.setElementManagementData(elementManagementData1);
           	     
           	    ReferenceInfoType referenceForDataElement = new ReferenceInfoType();
         	        List<ReferencingDetailsType> referenceList = referenceForDataElement.getReference();
         	        ReferencingDetailsType rf = new ReferencingDetailsType();
         	        rf.setQualifier("PR");
         	        rf.setNumber("" + (passengerRefnumber));
         	        referenceList.add(rf);
         	        de1.setReferenceForDataElement(referenceForDataElement);
           	     
           	     dataElementsDivList.add(de1);
           	     
           	     //home contact number
           	    DataElementsIndiv de2 = new DataElementsIndiv();
                ElementManagementSegmentType elementManagementData2 = new ElementManagementSegmentType();
                elementManagementData2.setSegmentName("AP");
                ReferencingDetailsType rf2 = new ReferencingDetailsType();
                rf2.setQualifier("OT");
                rf2.setNumber(++qualifierNumber+"");
                elementManagementData2.setReference(rf2);
                de2.setElementManagementData(elementManagementData2);
                LongFreeTextType ftd2 = new LongFreeTextType();
                FreeTextQualificationType ftdt2 = new FreeTextQualificationType();
                ftdt2.setSubjectQualifier("3");
                ftdt2.setType("7");
                ftd2.setFreetextDetail(ftdt2);
       	    	ftd2.setLongFreetext(personalDetails.getMobileNumber());
           	    de2.setFreetextData(ftd2);
           	    de2.setReferenceForDataElement(referenceForDataElement);
           	    dataElementsDivList.add(de2);
           	     //emergency contact number
                 if(StringUtils.hasText(personalDetails.getEmergencyContactNumber())) {
                     DataElementsIndiv de3 = new DataElementsIndiv();
                     ElementManagementSegmentType elementManagementData3 = new ElementManagementSegmentType();
                     elementManagementData3.setSegmentName("OS");
                    /* ReferencingDetailsType rf3 = new ReferencingDetailsType();
                     rf3.setQualifier("OT");
                     rf3.setNumber((++qualifierNumber) + "");
                     elementManagementData3.setReference(rf3);*/
                     de3.setElementManagementData(elementManagementData3);
                     LongFreeTextType ftd3 = new LongFreeTextType();
                     FreeTextQualificationType ftdt3 = new FreeTextQualificationType();
                     ftdt3.setSubjectQualifier("3");
                     ftdt3.setType("3");
                     ftd3.setFreetextDetail(ftdt3);
                     String emergencyContactNo = personalDetails.getEmergencyContactCode() + personalDetails.getEmergencyContactNumber();
                     emergencyContactNo = emergencyContactNo.replaceAll("\\+", "");
                     ftd3.setLongFreetext("Emergency Contact Number " + emergencyContactNo);
                     de3.setFreetextData(ftd3);
                     de3.setReferenceForDataElement(referenceForDataElement);

                     dataElementsDivList.add(de3);
                 }

                 if(StringUtils.hasText(personalDetails.getOfficeNumber())) {
                     DataElementsIndiv businessNoDiv = new DataElementsIndiv();
                     ElementManagementSegmentType businessNoelementManagement = new ElementManagementSegmentType();
                     businessNoelementManagement.setSegmentName("AP");
                     ReferencingDetailsType rf4 = new ReferencingDetailsType();
                     rf4.setQualifier("OT");
                     rf4.setNumber((++qualifierNumber) + "");
                     businessNoelementManagement.setReference(rf4);
                     businessNoDiv.setElementManagementData(businessNoelementManagement);
                     LongFreeTextType businessNoLongFreeText = new LongFreeTextType();
                     FreeTextQualificationType freeTextQualificationType = new FreeTextQualificationType();
                     freeTextQualificationType.setSubjectQualifier("3");
                     freeTextQualificationType.setType("3");
                     businessNoLongFreeText.setFreetextDetail(freeTextQualificationType);
                     businessNoLongFreeText.setLongFreetext(personalDetails.getOfficeNoCode() + personalDetails.getOfficeNumber());
                     businessNoDiv.setFreetextData(businessNoLongFreeText);
                     businessNoDiv.setReferenceForDataElement(referenceForDataElement);
                     dataElementsDivList.add(businessNoDiv);
                 }

             }
       			        
       	}
		} catch (Exception e) {
			e.printStackTrace();
		}
        return dataElementsDivList;
    }
    //credit card info
    public DataElementsIndiv addCreditCardData(int qualifierNumber) {
        DataElementsIndiv de = new DataElementsIndiv();
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
    public DataElementsIndiv addReceivedFrom(int qualifierNumber) {
        DataElementsIndiv de = new DataElementsIndiv();
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
    public List<DataElementsIndiv> addAdditionalPassengerDetails(TravellerMasterInfo travellerMasterInfo,
                                     int qualifierNumber, List<String> segmentNumbers, Map<String,String> travellerMap){
        int passengerReference = 1;
        String contactName = "";
        List<DataElementsIndiv> dataElementsDivList = new ArrayList<>();
        for(com.compassites.model.traveller.Traveller traveller : travellerMasterInfo.getTravellersList()){

            if (travellerMasterInfo.isSeamen() || (!travellerMasterInfo.isSeamen() && !PassengerTypeCode.INF
                    .equals(DateUtility.getPassengerTypeFromDOB(traveller.getPassportDetails().getDateOfBirth())))) {
                if (traveller.getPersonalDetails().getMiddleName() != null) {
                    contactName = traveller.getPersonalDetails().getFirstName() + traveller.getPersonalDetails().getMiddleName();

                } else {
                    contactName = traveller.getPersonalDetails().getFirstName();
                }
                contactName = contactName + traveller.getPersonalDetails().getSalutation();
                contactName = contactName.replaceAll("\\s+", "").replaceAll("\\.", "");
                String contactLastName = traveller.getPersonalDetails().getLastName();
                contactLastName = contactLastName.replaceAll("\\s+", "");
                String contactFullName = contactName + contactLastName;
                contactFullName = contactFullName.toLowerCase();
                passengerReference = Integer.parseInt(travellerMap.get(contactFullName));

                if (StringUtils.hasText(traveller.getPassportDetails().getPassportNumber())) {
                    dataElementsDivList.add(addPassportDetails(traveller, qualifierNumber, passengerReference, travellerMasterInfo.getUserTimezone()));
                }
                Preferences preferences = traveller.getPreferences();
                if (StringUtils.hasText(preferences.getMeal())) {
                    dataElementsDivList.add(addMealPreference(traveller, qualifierNumber, passengerReference, segmentNumbers));
                }
                if (StringUtils.hasText(preferences.getFrequentFlyerAirlines()) && StringUtils.hasText(preferences.getFrequentFlyerNumber())) {
                    dataElementsDivList.add(addFrequentFlyerNumber(traveller, qualifierNumber, passengerReference));
                }
                if (StringUtils.hasText(preferences.getSeatPreference()) && !"any".equalsIgnoreCase(preferences.getSeatPreference())) {
                    dataElementsDivList.add(addSeatPreference(traveller, passengerReference));
                }
            }
//            passengerReference++;
        }
        return dataElementsDivList;
    }

    public DataElementsIndiv addMealPreference(com.compassites.model.traveller.Traveller traveller, int qualifierNumber,
                                               int passengerRefNumber, List<String> segmentNumbers){
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
        rf.setQualifier("PT");
//        rf.setNumber("" + (passengerRefNumber));
        //// TODO: 28-03-2016  removed added for testing
        rf.setNumber("" + (2));
        referenceList.add(rf);

        for(String segment : segmentNumbers){
            ReferencingDetailsType segmentRef = new ReferencingDetailsType();
            segmentRef.setQualifier(AmadeusConstants.SEGMENT_REFERENCE_STRING);
            segmentRef.setNumber("" + (segment));
            referenceList.add(segmentRef);
        }

        de.setReferenceForDataElement(referenceForDataElement);

        return de;
    }

    public DataElementsIndiv addFrequentFlyerNumber(com.compassites.model.traveller.Traveller traveller, int qualifierNumber, int passengerRefNumber){
        DataElementsIndiv de = new DataElementsIndiv();


        ReferencingDetailsType rfSSR = new ReferencingDetailsType();
        rfSSR.setQualifier(AmadeusConstants.PASSENGER_REFERENCE_STRING);
        rfSSR.setNumber("" + (passengerRefNumber));

        ElementManagementSegmentType elementManagementData = new ElementManagementSegmentType();
        elementManagementData.setReference(rfSSR);
        elementManagementData.setSegmentName("SSR");
        de.setElementManagementData(elementManagementData);

        SpecialRequirementsDetailsTypeI serviceRequest = new SpecialRequirementsDetailsTypeI();
        SpecialRequirementsTypeDetailsTypeI ssr = new SpecialRequirementsTypeDetailsTypeI();
        ssr.setType("FQTV");
        ssr.setCompanyId(traveller.getPreferences().getFrequentFlyerAirlines());
        ssr.setIndicator("P01");
        serviceRequest.setSsr(ssr);
        de.setServiceRequest(serviceRequest);

        FrequentTravellerInformationTypeU frequentTravellerData = new FrequentTravellerInformationTypeU();
        FrequentTravellerIdentificationTypeU frequentTraveller = new FrequentTravellerIdentificationTypeU();
        frequentTraveller.setCompanyId(traveller.getPreferences().getFrequentFlyerAirlines());
        frequentTraveller.setMembershipNumber(traveller.getPreferences().getFrequentFlyerAirlines() + traveller.getPreferences().getFrequentFlyerNumber());
        frequentTravellerData.setFrequentTraveller(frequentTraveller);
        de.setFrequentTravellerData(frequentTravellerData);


        ReferenceInfoType referenceForDataElement = new ReferenceInfoType();
        List<ReferencingDetailsType> referenceList = referenceForDataElement.getReference();
        ReferencingDetailsType rf = new ReferencingDetailsType();
        rf.setQualifier(AmadeusConstants.PASSENGER_REFERENCE_STRING);
        rf.setNumber("" + (passengerRefNumber));
        referenceList.add(rf);
        de.setReferenceForDataElement(referenceForDataElement);

        return de;
    }

    public DataElementsIndiv addPassportDetails(com.compassites.model.traveller.Traveller traveller, int qualifierNumber,
                                                int passengerRefNumber, String userTimezone){
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
        // Sample text P-IND-H12232323-IND-3    0JUN73-M-14APR09-JOHNSON-SIMON

        DateTimeFormatter fmt = DateTimeFormat.forPattern("ddMMMyy");
        DateTimeZone dateTimeZone  = DateTimeZone.forID(userTimezone);

        DateTime dob = new DateTime(passportDetails.getDateOfBirth()).withZone(dateTimeZone);
        DateTime dateOfExpiry = new DateTime(passportDetails.getDateOfExpiry()).withZone(dateTimeZone);

        String issuanceCountryCode = NationalityDao.getCodeForCountry(traveller.getPassportDetails().getPlaceOfIssue());
        String freeText = "P-"+ issuanceCountryCode + "-" + passportDetails.getPassportNumber();
        if(traveller.getPassportDetails().getNationality() != null){
            freeText=freeText + "-" + traveller.getPassportDetails().getNationality().getThreeLetterCode();
        }
        freeText = freeText + "-" + fmt.print(dob)
                + "-" + StringUtility.getGenderCode(traveller.getPersonalDetails().getGender()) + "-" + fmt.print(dateOfExpiry)+"-";
        String name = traveller.getPersonalDetails().getLastName()+"-"+traveller.getPersonalDetails().getFirstName();

        if(freeText.length() + name.length() > 70){
            name = traveller.getPersonalDetails().getLastName()+"-"+traveller.getPersonalDetails().getFirstName().charAt(0);
        }
        freeText = freeText + name;
        freeTextList.add(freeText);

        serviceRequest.setSsr(ssr);
        de.setServiceRequest(serviceRequest);

        ReferenceInfoType referenceForDataElement = new ReferenceInfoType();
        List<ReferencingDetailsType> referenceList = referenceForDataElement.getReference();
        ReferencingDetailsType rf = new ReferencingDetailsType();
        rf.setQualifier("PT");
        rf.setNumber("" + (passengerRefNumber));
        referenceList.add(rf);
        de.setReferenceForDataElement(referenceForDataElement);

        return de;
    }

    public DataElementsIndiv addSeatPreference(Traveller traveller, int passengerRefNumber){
        DataElementsIndiv de = new DataElementsIndiv();
        ElementManagementSegmentType elementManagementData = new ElementManagementSegmentType();
        elementManagementData.setSegmentName("STR");
        de.setElementManagementData(elementManagementData);

        SeatEntityType seatGroup = new SeatEntityType();
        SeatRequestType seatRequestType =  new SeatRequestType();
        seatGroup.setSeatRequest(seatRequestType);
        SeatRequierementsDataType seatRequierementsDataType = new SeatRequierementsDataType();
        seatRequestType.getSpecial().add(seatRequierementsDataType);
        String seatType = null;
        if("aisle".equalsIgnoreCase(traveller.getPreferences().getSeatPreference())){
           seatType = AmadeusConstants.SEAT_TYPE.AISLE.getSeatType();
        }else if("window".equalsIgnoreCase(traveller.getPreferences().getSeatPreference())){
            seatType = AmadeusConstants.SEAT_TYPE.WIDOW.getSeatType();
        }
        seatRequierementsDataType.getSeatType().add(seatType);

        de.setSeatGroup(seatGroup);

        ReferenceInfoType referenceForDataElement = new ReferenceInfoType();
        List<ReferencingDetailsType> referenceList = referenceForDataElement.getReference();
        ReferencingDetailsType rf = new ReferencingDetailsType();
        rf.setQualifier(AmadeusConstants.PASSENGER_REFERENCE_STRING);
        rf.setNumber("" + (passengerRefNumber));
        referenceList.add(rf);


        de.setReferenceForDataElement(referenceForDataElement);

        return de;
    }
}
