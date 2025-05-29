package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.pnradd_11_3_1a.*;
import com.amadeus.xml.pnradd_11_3_1a.PNRAddMultiElements;
import com.amadeus.xml.pnradd_11_3_1a.PNRAddMultiElements.DataElementsMaster;
import com.amadeus.xml.pnradd_11_3_1a.PNRAddMultiElements.DataElementsMaster.DataElementsIndiv;
import com.amadeus.xml.pnradd_11_3_1a.PNRAddMultiElements.TravellerInfo;
import com.amadeus.xml.pnrxcl_11_3_1a.CancelPNRElementType;
import com.amadeus.xml.pnrxcl_11_3_1a.PNRCancel;
import com.amadeus.xml.pnrxcl_11_3_1a.ReservationControlInformationType;
import com.compassites.constants.AmadeusConstants;
import com.compassites.model.AirSegmentInformation;
import com.compassites.model.Journey;
import com.compassites.model.PassengerTypeCode;
import com.compassites.model.traveller.*;
import models.NationalityDao;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import utils.DateUtility;
import utils.StringUtility;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;


public class PNRAddMultiElementsh {

    private static final Logger logger = LoggerFactory.getLogger("gds");

    public PNRAddMultiElements getMultiElements(TravellerMasterInfo travellerMasterInfo) {
        PNRAddMultiElements element = new PNRAddMultiElements();
        OptionalPNRActionsType pnrActions = new OptionalPNRActionsType();
        pnrActions.getOptionCode().add(new BigInteger("0"));
        element.setPnrActions(pnrActions);

        addLiveEntry(element, travellerMasterInfo);
        //element.getTravellerInfo().add(addPassenger());
        //element.getTravellerInfo().add(addChildPassenger());
        //element.getTravellerInfo().add(addInfantPassenger());
        if (travellerMasterInfo.getAdditionalInfo() != null && travellerMasterInfo.getAdditionalInfo().getAddBooking() == null) {
            element.getTravellerInfo().addAll(getPassengersList(travellerMasterInfo));
        }
        PNRAddMultiElements.DataElementsMaster dem = new PNRAddMultiElements.DataElementsMaster();
        dem.setMarker1(new DummySegmentTypeI());
        int qualifierNumber = 0;
        if (travellerMasterInfo.getAdditionalInfo() != null && travellerMasterInfo.getAdditionalInfo().getAddBooking() == null) {
            dem.getDataElementsIndiv().add(addCreditCardData(qualifierNumber));
            dem.getDataElementsIndiv().add(addReceivedFrom(qualifierNumber));
            dem.getDataElementsIndiv().add(addTckArr(qualifierNumber));
            dem.getDataElementsIndiv().addAll(addContactInfo(travellerMasterInfo, qualifierNumber));
            dem.getDataElementsIndiv().addAll(addRCEntry(travellerMasterInfo,qualifierNumber));
        }


//        dem.getDataElementsIndiv().addAll(addAdditionalPassengerDetails(travellerMasterInfo, qualifierNumber));
        //dem.getDataElementsIndiv().add(addEOTInfo());

        element.setDataElementsMaster(dem);
        return element;
    }

    public List<DataElementsIndiv> addRCEntry(TravellerMasterInfo travellerMasterInfo,int qualifierNumber) {
        List<DataElementsIndiv> dataElementsDivList = new ArrayList<>();
        try {
            DataElementsIndiv de1 = new DataElementsIndiv();

            ElementManagementSegmentType elementManagementData1 = new ElementManagementSegmentType();
            elementManagementData1.setSegmentName("RC");
            ReferencingDetailsType rf1 = new ReferencingDetailsType();
            rf1.setQualifier("OT");
            //rf1.setNumber((++qualifierNumber) + "");
            rf1.setNumber("13");
            elementManagementData1.setReference(rf1);
            de1.setElementManagementData(elementManagementData1);

            MiscellaneousRemarksType miscellaneousRemarksType = new MiscellaneousRemarksType();
            MiscellaneousRemarkType remarks = new MiscellaneousRemarkType();
            remarks.setType("RC");
            remarks.setFreetext(travellerMasterInfo.getAccountName().toUpperCase());
            miscellaneousRemarksType.setRemarks(remarks);
            de1.setMiscellaneousRemark(miscellaneousRemarksType);
            dataElementsDivList.add(de1);

            DataElementsIndiv de2 = new DataElementsIndiv();

            ElementManagementSegmentType elementManagementData2 = new ElementManagementSegmentType();
            elementManagementData2.setSegmentName("RC");
            ReferencingDetailsType rf2 = new ReferencingDetailsType();
            rf2.setQualifier("OT");
            rf2.setNumber("13");
            elementManagementData2.setReference(rf2);
            de2.setElementManagementData(elementManagementData2);

            MiscellaneousRemarksType miscellaneousRemarksType1 = new MiscellaneousRemarksType();
            MiscellaneousRemarkType remarks1 = new MiscellaneousRemarkType();
            remarks1.setType("RC");
            remarks1.setFreetext(travellerMasterInfo.getBookerDetails().toUpperCase());
            miscellaneousRemarksType1.setRemarks(remarks1);
            de2.setMiscellaneousRemark(miscellaneousRemarksType1);

            dataElementsDivList.add(de2);

            if (travellerMasterInfo.getApprovers() != null && travellerMasterInfo.getApprovers().length() > 0) {
                DataElementsIndiv de3 = new DataElementsIndiv();

                ElementManagementSegmentType elementManagementData3 = new ElementManagementSegmentType();
                elementManagementData3.setSegmentName("RC");
                ReferencingDetailsType rf3 = new ReferencingDetailsType();
                rf3.setQualifier("OT");
                rf3.setNumber("13");
                elementManagementData3.setReference(rf3);
                de3.setElementManagementData(elementManagementData3);

                MiscellaneousRemarksType miscellaneousRemarksType2 = new MiscellaneousRemarksType();
                MiscellaneousRemarkType remarks2 = new MiscellaneousRemarkType();
                remarks2.setType("RC");
                remarks2.setFreetext(travellerMasterInfo.getApproverName().toUpperCase());
                miscellaneousRemarksType2.setRemarks(remarks2);
                de3.setMiscellaneousRemark(miscellaneousRemarksType2);

                dataElementsDivList.add(de3);

                DataElementsIndiv de4 = new DataElementsIndiv();
                ElementManagementSegmentType elementManagementData4 = new ElementManagementSegmentType();
                elementManagementData4.setSegmentName("RC");
                ReferencingDetailsType rf4 = new ReferencingDetailsType();
                rf4.setQualifier("OT");
                rf4.setNumber("13");
                elementManagementData4.setReference(rf4);
                de4.setElementManagementData(elementManagementData4);

                MiscellaneousRemarksType miscellaneousRemarksType3 = new MiscellaneousRemarksType();
                MiscellaneousRemarkType remarks3 = new MiscellaneousRemarkType();
                remarks3.setType("RC");
                remarks3.setFreetext(travellerMasterInfo.getReasonForApproval());
                miscellaneousRemarksType3.setRemarks(remarks3);
                de4.setMiscellaneousRemark(miscellaneousRemarksType3);
                dataElementsDivList.add(de4);
            }
            DataElementsIndiv de5 = new DataElementsIndiv();
            ElementManagementSegmentType elementManagementData5 = new ElementManagementSegmentType();
            elementManagementData5.setSegmentName("RC");
            ReferencingDetailsType rf5 = new ReferencingDetailsType();
            rf5.setQualifier("OT");
            rf5.setNumber("13");
            elementManagementData5.setReference(rf5);
            de5.setElementManagementData(elementManagementData5);

            MiscellaneousRemarksType miscellaneousRemarksType4 = new MiscellaneousRemarksType();
            MiscellaneousRemarkType remarks4 = new MiscellaneousRemarkType();
            remarks4.setType("RC");
            remarks4.setFreetext(String.valueOf(travellerMasterInfo.getItinerary().getPricingInformation(travellerMasterInfo.isSeamen()).getTotalPrice()));
            miscellaneousRemarksType4.setRemarks(remarks4);
            de5.setMiscellaneousRemark(miscellaneousRemarksType4);
            dataElementsDivList.add(de5);

        } catch (Exception e ) {
            e.printStackTrace();
        }
        return dataElementsDivList;
    }

    public void addLiveEntry(PNRAddMultiElements element, TravellerMasterInfo travellerMasterInfo) {
        List<Journey> journeyList = travellerMasterInfo.getItinerary().getJourneys(travellerMasterInfo.isSeamen());
        List<AirSegmentInformation> airSegmentInformations = journeyList.get(journeyList.size() - 1).getAirSegmentList();
        BigInteger paxCount = BigInteger.valueOf(travellerMasterInfo.getTravellersList().size());
        String fromLocation = journeyList.get(0).getAirSegmentList().get(0).getFromLocation();
        String toLocation = journeyList.get(journeyList.size() - 1).getAirSegmentList().get(airSegmentInformations.size() - 1).getToLocation();

        Date bookingDate = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(bookingDate);
        cal.add(Calendar.MONTH, 11);
        Integer month = cal.get(Calendar.MONTH) + 1;
        Integer year = cal.get(Calendar.YEAR);
        String bookingMonth = String.format("%02d", month);
        String depDate = "01" + bookingMonth + year.toString().substring(2, 4);

        List<PNRAddMultiElements.OriginDestinationDetails> originDestinationDetailsList = new ArrayList<>();
        PNRAddMultiElements.OriginDestinationDetails originDestinationDetails = new PNRAddMultiElements.OriginDestinationDetails();
        OriginAndDestinationDetailsTypeI originAndDestinationDetailsTypeI = new OriginAndDestinationDetailsTypeI();
        originAndDestinationDetailsTypeI.setOrigin(fromLocation);
        if (!travellerMasterInfo.isCreateTmpPNR()) {
            originAndDestinationDetailsTypeI.setDestination(toLocation);
        }
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
        boardpointDetail.setCityCode("BOM");
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


    public PNRAddMultiElements addSSRDetails(TravellerMasterInfo travellerMasterInfo, List<String> segmentNumbers, Map<String, String> travellerMap) {

        try {

            PNRAddMultiElements element = new PNRAddMultiElements();
            OptionalPNRActionsType pnrActions = new OptionalPNRActionsType();
            pnrActions.getOptionCode().add(new BigInteger("0"));
            element.setPnrActions(pnrActions);

            DataElementsMaster dem = new DataElementsMaster();
            dem.setMarker1(new DummySegmentTypeI());
            int qualifierNumber = 0;
            dem.getDataElementsIndiv().addAll(addAdditionalPassengerDetails(travellerMasterInfo, qualifierNumber, segmentNumbers, travellerMap));
            element.setDataElementsMaster(dem);

            return element;
        } catch (Exception e) {
            logger.debug("Error Adding SSR Details to PNR {}", e.getMessage(), e);
            return null;
        }
    }

    private List<TravellerInfo> getPassengersList(TravellerMasterInfo travellerMasterInfo) {

        int passengerCount = 1;
        List<TravellerInfo> travellerInfoList = new ArrayList<>();
        boolean isSeamen = travellerMasterInfo.isSeamen();

        List<Traveller> travellers = new ArrayList<>();
        travellers.addAll(travellerMasterInfo.getTravellersList()); //copying travellers as the list would be modified
        List<Traveller> infantTravellerList = new ArrayList<>();
        if (!isSeamen) {
            infantTravellerList = getInfantTravellerList(travellers);  //also removes the infant from traveller
        }
        int infantIndex = 0;
        for (com.compassites.model.traveller.Traveller traveller : travellers) {
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

            if (traveller.getPersonalDetails().getSalutation() != null) {
                salutation = traveller.getPersonalDetails().getSalutation().replace(".", "");
            }

            String name = "";
            if (traveller.getPersonalDetails().getMiddleName() != null) {
                name = traveller.getPersonalDetails().getFirstName() + " " + traveller.getPersonalDetails().getMiddleName() + " " + salutation;
            } else {
                name = traveller.getPersonalDetails().getFirstName() + " " + salutation;
            }


            passenger.setFirstName(name);

            String passengerType = DateUtility.getPassengerTypeFromDOB(traveller.getPassportDetails().getDateOfBirth()).toString();
            if (travellerMasterInfo.isSeamen()) {
                passenger.setType(PassengerTypeCode.SEA.toString());

            } else {
                passenger.setType(passengerType);
            }

            TravellerInfo.PassengerData infantPassengerData = null;
            if (!isSeamen && "ADT".equalsIgnoreCase(passengerType) && infantTravellerList.size() >= 1) {
                passenger.setInfantIndicator("3");
                infantPassengerData = addInfantAssociation(infantTravellerList.get(infantIndex));
                infantTravellerList.remove(infantIndex);
//                infantIndex++;
            }

            travellerInformation.getPassenger().add(passenger);
            travellerInformation.setTraveller(gdsTraveller);
            passengerData.setTravellerInformation(travellerInformation);
            travellerInfo.getPassengerData().add(passengerData);
            if (infantPassengerData != null) {
                travellerInfo.getPassengerData().add(infantPassengerData);
            }
            travellerInfoList.add(travellerInfo);
        }

        return travellerInfoList;
    }

    public TravellerInfo.PassengerData addInfantAssociation(Traveller traveller) {
        TravellerInfo.PassengerData passengerData = new TravellerInfo.PassengerData();
        TravellerInformationTypeI travellerInformation = new TravellerInformationTypeI();
        TravellerSurnameInformationTypeI gdsTraveller = new TravellerSurnameInformationTypeI();
        gdsTraveller.setSurname(traveller.getPersonalDetails().getLastName());
        gdsTraveller.setQuantity(new BigInteger("1"));


        TravellerDetailsTypeI passenger = new TravellerDetailsTypeI();
        String salutation = "";
        if (traveller.getPersonalDetails().getSalutation() != null) {
            salutation = traveller.getPersonalDetails().getSalutation().replace(".", "");
        }
        String name = "";
        if (traveller.getPersonalDetails().getMiddleName() != null) {
            name = traveller.getPersonalDetails().getFirstName() + " " + traveller.getPersonalDetails().getMiddleName() + " " + salutation;
        } else {
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

    public List<Traveller> getInfantTravellerList(List<Traveller> travellerList) {
        List<Traveller> infantList = new ArrayList<>();
        Iterator<Traveller> travellerIterator = travellerList.iterator();
        while (travellerIterator.hasNext()) {
            Traveller traveller = travellerIterator.next();
            String passengerType = DateUtility.getPassengerTypeFromDOB(traveller.getPassportDetails().getDateOfBirth()).toString();
            if ("INF".equals(passengerType)) {
                infantList.add(traveller);
                travellerIterator.remove();
            }

        }
        return infantList;
    }

    //TODO-- Add Seaman type to the adult passenger
    private String getPassengerType(Date passengerDOB) {
        return DateUtility.getPassengerTypeFromDOB(passengerDOB).toString();
    }

    public TravellerInfo addPassenger() {
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

    public TravellerInfo addChildPassenger() {
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

    public TravellerInfo addInfantPassenger() {
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

        DataElementsMaster dataElementsMaster = new DataElementsMaster();
        DataElementsIndiv dataElementsIndiv = new DataElementsIndiv();

        ElementManagementSegmentType elementManagementData = new ElementManagementSegmentType();
        ReferencingDetailsType reference = new ReferencingDetailsType();
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

    public PNRAddMultiElements savePnr(String ESOfficeId) {
        return esxEntry(ESOfficeId);
//        PNRAddMultiElements element = savePnr();
//        if(ESOfficeId != null){
//            DataElementsIndiv dataElementsIndiv = new DataElementsIndiv();
//            ElementManagementSegmentType elementManagementData = new ElementManagementSegmentType();
//            ReferencingDetailsType reference =  new ReferencingDetailsType();
//            reference.setNumber("1");
//            reference.setQualifier("OT");
//            elementManagementData.setSegmentName("ES");
//            elementManagementData.setReference(reference);
//            dataElementsIndiv.setElementManagementData(elementManagementData);
//
//            //IndividualPnrSecurityInformationType
//            //com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements.DataElementsMaster.DataElementsIndiv.PnrSecurity pnrSecurity = new com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements.DataElementsMaster.DataElementsIndiv.PnrSecurity()
//            IndividualPnrSecurityInformationType pnrSecurity = new IndividualPnrSecurityInformationType();
//            IndividualSecurityType security = new IndividualSecurityType();
//            security.setIdentification(ESOfficeId);
//            security.setAccessMode("B");
//            pnrSecurity.getSecurity().add(security);
//            pnrSecurity.setIndicator("G");
//            dataElementsIndiv.setPnrSecurity(pnrSecurity);
//            element.getDataElementsMaster().getDataElementsIndiv().add(dataElementsIndiv);
//        }
//        return element;
    }


    public PNRAddMultiElements ignoreAndRetrievePNR() {

        PNRAddMultiElements element = new PNRAddMultiElements();
        OptionalPNRActionsType pnrActions = new OptionalPNRActionsType();
        pnrActions.getOptionCode().add(new BigInteger("21"));
        element.setPnrActions(pnrActions);

        return element;
    }

    public PNRAddMultiElements ignorePNRAddMultiElement() {

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

    public DataElementsIndiv addTravelAgentInfo(int qualifierNumber) {
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
        rf1.setNumber((++qualifierNumber) + "");
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
            for (Traveller traveller : travellerMasterInfo.getTravellersList()) {

                PersonalDetails personalDetails = traveller.getPersonalDetails();

                if (travellerMasterInfo.isSeamen() || (!travellerMasterInfo.isSeamen() && !PassengerTypeCode.INF
                        .equals(DateUtility.getPassengerTypeFromDOB(traveller.getPassportDetails().getDateOfBirth())))) {

                    passengerRefnumber = passengerRefnumber + 1;
                    System.out.println("************passengerRefnumber" + passengerRefnumber);
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
                    rf1.setNumber((++qualifierNumber) + "");
                    elementManagementData1.setReference(rf1);
                    de1.setElementManagementData(elementManagementData1);

                    ReferenceInfoType referenceForDataElement = new ReferenceInfoType();
                    List<ReferencingDetailsType> referenceList = referenceForDataElement.getReference();
                    ReferencingDetailsType rf = new ReferencingDetailsType();
                    rf.setQualifier("PR");
                    rf.setNumber("" + (passengerRefnumber));
                    referenceList.add(rf);

                    //de1.setReferenceForDataElement(referenceForDataElement);

                    dataElementsDivList.add(de1);

                    //home contact number
                    DataElementsIndiv de2 = new DataElementsIndiv();
                    ElementManagementSegmentType elementManagementData2 = new ElementManagementSegmentType();
                    elementManagementData2.setSegmentName("AP");
                    ReferencingDetailsType rf2 = new ReferencingDetailsType();
                    rf2.setQualifier("OT");
                    rf2.setNumber(++qualifierNumber + "");
                    elementManagementData2.setReference(rf2);
                    de2.setElementManagementData(elementManagementData2);
                    LongFreeTextType ftd2 = new LongFreeTextType();
                    FreeTextQualificationType ftdt2 = new FreeTextQualificationType();
                    ftdt2.setSubjectQualifier("3");
                    ftdt2.setType("7");
                    ftd2.setFreetextDetail(ftdt2);
                    ftd2.setLongFreetext(personalDetails.getMobileNumber());
                    de2.setFreetextData(ftd2);
                    //de2.setReferenceForDataElement(referenceForDataElement);
                    dataElementsDivList.add(de2);
                    //emergency contact number
                    if (StringUtils.hasText(personalDetails.getEmergencyContactNumber())) {
                        DataElementsIndiv de3 = new DataElementsIndiv();
                        ElementManagementSegmentType elementManagementData3 = new ElementManagementSegmentType();
                        elementManagementData3.setSegmentName("OS");
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
                        //de3.setReferenceForDataElement(referenceForDataElement);

                        dataElementsDivList.add(de3);
                    }

                    DataElementsIndiv de4 = new DataElementsIndiv();
                    ElementManagementSegmentType elementManagementData4 = new ElementManagementSegmentType();
                    elementManagementData4.setSegmentName("OS");
                    de4.setElementManagementData(elementManagementData4);
                    LongFreeTextType ftd4 = new LongFreeTextType();
                    FreeTextQualificationType ftdt4 = new FreeTextQualificationType();
                    ftdt4.setSubjectQualifier("3");
                    ftdt4.setType("P27");
                    ftdt4.setCompanyId("YY");
                    ftd4.setFreetextDetail(ftdt4);
                    ftd4.setLongFreetext("FLY HI TRAVEL MUMBAI TEL 00 91 9619004000");
                    de4.setFreetextData(ftd4);
                    //de4.setReferenceForDataElement(referenceForDataElement);
                    dataElementsDivList.add(de4);

                    DataElementsIndiv de5 = new DataElementsIndiv();
                    ElementManagementSegmentType elementManagementData5 = new ElementManagementSegmentType();
                    elementManagementData5.setSegmentName("OS");
                    de5.setElementManagementData(elementManagementData5);
                    LongFreeTextType ftd5 = new LongFreeTextType();
                    FreeTextQualificationType ftdt5 = new FreeTextQualificationType();
                    ftdt5.setSubjectQualifier("3");
                    ftdt5.setType("P27");
                    ftdt5.setCompanyId("YY");
                    ftd5.setFreetextDetail(ftdt5);
                    ftd5.setLongFreetext("FLY HI TRAVEL IATA CODE AGT 14308534");
                    de5.setFreetextData(ftd5);
                    //de5.setReferenceForDataElement(referenceForDataElement);
                    dataElementsDivList.add(de5);

                    if (StringUtils.hasText(personalDetails.getOfficeNumber())) {
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
                        //businessNoDiv.setReferenceForDataElement(referenceForDataElement);
                        dataElementsDivList.add(businessNoDiv);
                    }

                    if (isLH_UA_LX_SK_OS_AC(travellerMasterInfo)) {
                        DataElementsIndiv de6 = new DataElementsIndiv();
                        ElementManagementSegmentType elementManagementData6 = new ElementManagementSegmentType();
                        elementManagementData6.setSegmentName("OS");
                        de6.setElementManagementData(elementManagementData6);
                        LongFreeTextType ftd6 = new LongFreeTextType();
                        FreeTextQualificationType ftdt6 = new FreeTextQualificationType();
                        ftdt6.setSubjectQualifier("3");
                        ftdt6.setType("P27");
                        ftdt6.setCompanyId("YY");
                        ftd6.setFreetextDetail(ftdt6);
                        ftd6.setLongFreetext("DS/SIN/IN366409");
                        de6.setFreetextData(ftd6);
                        //de6.setReferenceForDataElement(referenceForDataElement);
                        dataElementsDivList.add(de6);
                    }

                    if (isAF_KL_DL(travellerMasterInfo)) {
                        DataElementsIndiv de7 = new DataElementsIndiv();
                        ElementManagementSegmentType elementManagementData7 = new ElementManagementSegmentType();
                        elementManagementData7.setSegmentName("OS");
                        de7.setElementManagementData(elementManagementData7);
                        LongFreeTextType ftd7 = new LongFreeTextType();
                        FreeTextQualificationType ftdt7 = new FreeTextQualificationType();
                        ftdt7.setSubjectQualifier("3");
                        ftdt7.setType("P27");
                        ftdt7.setCompanyId("YY");
                        ftd7.setFreetextDetail(ftdt7);
                        ftd7.setLongFreetext("OIN IN05073");
                        de7.setFreetextData(ftd7);
                        //de7.setReferenceForDataElement(referenceForDataElement);
                        dataElementsDivList.add(de7);
                    }

                }

//                else {
//
//                    passengerRefnumber = passengerRefnumber + 1;
//
//                    ReferenceInfoType referenceForDataElement = new ReferenceInfoType();
//                    List<ReferencingDetailsType> referenceList = referenceForDataElement.getReference();
//                    ReferencingDetailsType rf = new ReferencingDetailsType();
//                    rf.setQualifier("PR");
//                    rf.setNumber("" + (passengerRefnumber));
//                    referenceList.add(rf);
//
//
//                    DataElementsIndiv de1 = new DataElementsIndiv();
//                    ElementManagementSegmentType elementManagementData1 = new ElementManagementSegmentType();
//                    elementManagementData1.setSegmentName("OS");
//                    de1.setElementManagementData(elementManagementData1);
//                    LongFreeTextType ftd1 = new LongFreeTextType();
//                    FreeTextQualificationType ftdt1 = new FreeTextQualificationType();
//                    ftdt1.setSubjectQualifier("3");
//                    ftdt1.setType("P27");
//                    ftdt1.setCompanyId("YY");
//                    ftd1.setFreetextDetail(ftdt1);
//                    ftd1.setLongFreetext("FLY HI TRAVEL MUMBAI TEL 00 91 9619004000");
//                    de1.setFreetextData(ftd1);
//                    de1.setReferenceForDataElement(referenceForDataElement);
//                    dataElementsDivList.add(de1);
//
//                    DataElementsIndiv de2 = new DataElementsIndiv();
//                    ElementManagementSegmentType elementManagementData2 = new ElementManagementSegmentType();
//                    elementManagementData2.setSegmentName("OS");
//                    de2.setElementManagementData(elementManagementData2);
//                    LongFreeTextType ftd2 = new LongFreeTextType();
//                    FreeTextQualificationType ftdt2 = new FreeTextQualificationType();
//                    ftdt2.setSubjectQualifier("3");
//                    ftdt2.setType("P27");
//                    ftdt2.setCompanyId("YY");
//                    ftd2.setFreetextDetail(ftdt2);
//                    ftd2.setLongFreetext("FLY HI TRAVEL IATA CODE AGT 14308534");
//                    de2.setFreetextData(ftd2);
//                    de2.setReferenceForDataElement(referenceForDataElement);
//                    dataElementsDivList.add(de2);
//
//                    if (isAF_KL_DL(travellerMasterInfo)) {
//                        DataElementsIndiv de3 = new DataElementsIndiv();
//                        ElementManagementSegmentType elementManagementData3 = new ElementManagementSegmentType();
//                        elementManagementData3.setSegmentName("OS");
//                        de3.setElementManagementData(elementManagementData3);
//                        LongFreeTextType ftd3 = new LongFreeTextType();
//                        FreeTextQualificationType ftdt3 = new FreeTextQualificationType();
//                        ftdt3.setSubjectQualifier("3");
//                        ftdt3.setType("P27");
//                        ftdt3.setCompanyId("YY");
//                        ftd3.setFreetextDetail(ftdt3);
//                        ftd3.setLongFreetext("OIN IN05073");
//                        de3.setFreetextData(ftd3);
//                        de3.setReferenceForDataElement(referenceForDataElement);
//                        dataElementsDivList.add(de3);
//                    }
//                }
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
        rf.setNumber((++qualifierNumber) + "");
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
        rf.setNumber((++qualifierNumber) + "");
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
        rf.setNumber((++qualifierNumber) + "");
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

    //Adding SSR details here
    public List<DataElementsIndiv> addAdditionalPassengerDetails(TravellerMasterInfo travellerMasterInfo, int qualifierNumber, List<String> segmentNumbers, Map<String, String> travellerMap) {
        try {

            int passengerReference = 1;
            String contactName = "";
            List<DataElementsIndiv> dataElementsDivList = new ArrayList<>();
            for (com.compassites.model.traveller.Traveller traveller : travellerMasterInfo.getTravellersList()) {

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

                if (travellerMasterInfo.isSeamen() || (!travellerMasterInfo.isSeamen() && !PassengerTypeCode.INF.equals(DateUtility.getPassengerTypeFromDOB(traveller.getPassportDetails().getDateOfBirth())))) {

                    //Adding Is Seaman entry here
                    if (travellerMasterInfo.isSeamen()) {
                        dataElementsDivList.add(addIsSeaman(passengerReference));
                    }
                    //Adding Mobile number here
                    dataElementsDivList.add(addCTCM(passengerReference));

                    //Adding Email Address here
                    dataElementsDivList.add(addCTCE(passengerReference));

                    //Adding Vessel Name here if it exists
                    if (travellerMasterInfo.getAdditionalInfo() != null && StringUtils.hasText(travellerMasterInfo.getAdditionalInfo().getVesselId())) {
                        dataElementsDivList.add(addVesselName(passengerReference, travellerMasterInfo.getVesselName()));
                    }

                    //Adding passport number here if it exists
                    if (StringUtils.hasText(traveller.getPassportDetails().getPassportNumber()) || traveller.getPassportDetails() != null) {
                        dataElementsDivList.add(addPassportNumber(traveller, passengerReference));
                    }

                    //Adding SQ data
                    if (isSQ(travellerMasterInfo)) {
                        dataElementsDivList.add(addSQSSR(passengerReference));
                    }

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
            }

            return dataElementsDivList;
        } catch (Exception e) {
            logger.debug("Error While adding additional passenger details in SSR {} ", e.getMessage(), e);
            return null;
        }
    }

    public DataElementsIndiv addMealPreference(com.compassites.model.traveller.Traveller traveller, int qualifierNumber,
                                               int passengerRefNumber, List<String> segmentNumbers) {
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
        rf.setNumber("" + (passengerRefNumber));
        //// TODO: 28-03-2016  removed added for testing
//        rf.setNumber("" + (2));
        referenceList.add(rf);

        for (String segment : segmentNumbers) {
            ReferencingDetailsType segmentRef = new ReferencingDetailsType();
            segmentRef.setQualifier(AmadeusConstants.SEGMENT_REFERENCE_STRING);
            segmentRef.setNumber("" + (segment));
            referenceList.add(segmentRef);
        }

        de.setReferenceForDataElement(referenceForDataElement);

        return de;
    }

    public DataElementsIndiv addFrequentFlyerNumber(com.compassites.model.traveller.Traveller traveller, int qualifierNumber, int passengerRefNumber) {
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
                                                int passengerRefNumber, String userTimezone) {
        DataElementsIndiv de = new DataElementsIndiv();
        PassportDetails passportDetails = traveller.getPassportDetails();

        ElementManagementSegmentType elementManagementData = new ElementManagementSegmentType();
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
        DateTimeZone dateTimeZone = DateTimeZone.forID(userTimezone);

        DateTime dob = new DateTime(passportDetails.getDateOfBirth()).withZone(dateTimeZone);
        DateTime dateOfExpiry = new DateTime(passportDetails.getDateOfExpiry()).withZone(dateTimeZone);

        String issuanceCountryCode = NationalityDao.getCodeForCountry(traveller.getPassportDetails().getPlaceOfIssue());
        String freeText = "P//";
        if(!issuanceCountryCode.equalsIgnoreCase("") && issuanceCountryCode !=null) {
            freeText = freeText + issuanceCountryCode + "/" + passportDetails.getPassportNumber();
        } else {
            freeText = freeText + passportDetails.getPassportNumber();
        }
        if (traveller.getPassportDetails().getNationality() != null) {
            if(traveller.getPassportDetails().getNationality().getThreeLetterCode()!=null) {
                freeText = freeText + "/" + traveller.getPassportDetails().getNationality().getThreeLetterCode();
            } else {
                freeText = freeText + "/" + traveller.getPassportDetails().getNationality().getNationality().substring(0, 3);
            }
        } else {
            freeText = freeText + "/" + traveller.getPassportDetails().getPlaceOfIssue().substring(0, 3);
        }
        freeText = freeText + "/" + fmt.print(dob) + "/" + StringUtility.getGenderCode(traveller.getPersonalDetails().getGender()) + "/" + fmt.print(dateOfExpiry) + "/";
        String name = traveller.getPersonalDetails().getLastName() + "/" + traveller.getPersonalDetails().getFirstName();

        if (freeText.length() + name.length() > 70) {
            name = traveller.getPersonalDetails().getLastName() + "/" + traveller.getPersonalDetails().getFirstName().charAt(0);
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

    public DataElementsIndiv addSeatPreference(Traveller traveller, int passengerRefNumber) {
        DataElementsIndiv de = new DataElementsIndiv();
        ElementManagementSegmentType elementManagementData = new ElementManagementSegmentType();
        elementManagementData.setSegmentName("STR");
        de.setElementManagementData(elementManagementData);

        SeatEntityType seatGroup = new SeatEntityType();
        SeatRequestType seatRequestType = new SeatRequestType();
        seatGroup.setSeatRequest(seatRequestType);
        SeatRequierementsDataType seatRequierementsDataType = new SeatRequierementsDataType();
        seatRequestType.getSpecial().add(seatRequierementsDataType);
        String seatType = null;
        if ("aisle".equalsIgnoreCase(traveller.getPreferences().getSeatPreference())) {
            seatType = AmadeusConstants.SEAT_TYPE.AISLE.getSeatType();
        } else if ("window".equalsIgnoreCase(traveller.getPreferences().getSeatPreference())) {
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

    public PNRAddMultiElements esxEntry(String officeId) {
        PNRAddMultiElements element = new PNRAddMultiElements();
        OptionalPNRActionsType pnrActions = new OptionalPNRActionsType();
        pnrActions.getOptionCode().add(new BigInteger("11"));
        element.setPnrActions(pnrActions);

        DataElementsMaster dataElementsMaster = new DataElementsMaster();
        DataElementsIndiv dataElementsIndiv = new DataElementsIndiv();

        ElementManagementSegmentType elementManagementData = new ElementManagementSegmentType();
        elementManagementData.setSegmentName("ES");

        IndividualPnrSecurityInformationType individualPnrSecurityInformationType = new IndividualPnrSecurityInformationType();
        IndividualSecurityType individualSecurityType = new IndividualSecurityType();
        individualSecurityType.setIdentification(officeId);
        individualSecurityType.setAccessMode("B");

        individualPnrSecurityInformationType.setIndicator("G");
        individualPnrSecurityInformationType.getSecurity().add(individualSecurityType);
        dataElementsIndiv.setPnrSecurity(individualPnrSecurityInformationType);
        dataElementsIndiv.setElementManagementData(elementManagementData);


        DataElementsIndiv dataElementsIndiv1 = new DataElementsIndiv();
        ElementManagementSegmentType elementManagementData1 = new ElementManagementSegmentType();
        elementManagementData1.setSegmentName("RF");
        dataElementsIndiv1.setElementManagementData(elementManagementData1);

        LongFreeTextType freetextData = new LongFreeTextType();
        FreeTextQualificationType freetextDetail = new FreeTextQualificationType();
        freetextDetail.setSubjectQualifier("3");
        freetextDetail.setType("P22");
        freetextData.setFreetextDetail(freetextDetail);
        freetextData.setLongFreetext("RF ADDED VIA PNRADD");
        dataElementsIndiv1.setFreetextData(freetextData);
        dataElementsMaster.getDataElementsIndiv().add(dataElementsIndiv);
        dataElementsMaster.getDataElementsIndiv().add(dataElementsIndiv1);
        dataElementsMaster.setMarker1(new DummySegmentTypeI());
        element.setDataElementsMaster(dataElementsMaster);
        return element;
    }

    public PNRCancel exitEsx(String pnr) {
        PNRCancel pnrCancel = new PNRCancel();

        ReservationControlInformationType reservationControlInformationType = new ReservationControlInformationType();
        com.amadeus.xml.pnrxcl_11_3_1a.ReservationControlInformationDetailsTypeI reservationControlInformationDetailsTypeI = new com.amadeus.xml.pnrxcl_11_3_1a.ReservationControlInformationDetailsTypeI();
        reservationControlInformationDetailsTypeI.setControlNumber(pnr);
        reservationControlInformationType.setReservation(reservationControlInformationDetailsTypeI);
        pnrCancel.setReservationInfo(reservationControlInformationType);

        com.amadeus.xml.pnrxcl_11_3_1a.OptionalPNRActionsType pnrActionsType = new com.amadeus.xml.pnrxcl_11_3_1a.OptionalPNRActionsType();
        pnrActionsType.getOptionCode().add(new BigInteger(String.valueOf(0)));
        pnrCancel.setPnrActions(pnrActionsType);

        CancelPNRElementType cancelPNRElementType = new CancelPNRElementType();
        cancelPNRElementType.setEntryType("S");
        pnrCancel.getCancelElements().add(cancelPNRElementType);
        return pnrCancel;
    }

    public DataElementsIndiv addPassportNumber(com.compassites.model.traveller.Traveller traveller, int passengerRefNumber) {
        DataElementsIndiv de = new DataElementsIndiv();
        PassportDetails passportDetails = traveller.getPassportDetails();

        ElementManagementSegmentType elementManagementData = new ElementManagementSegmentType();

        elementManagementData.setSegmentName("SSR");
        de.setElementManagementData(elementManagementData);

        SpecialRequirementsDetailsTypeI serviceRequest = new SpecialRequirementsDetailsTypeI();
        SpecialRequirementsTypeDetailsTypeI ssr = new SpecialRequirementsTypeDetailsTypeI();

        ssr.setType("FOID");
        ssr.setCompanyId("YY");

        List<String> freeTextList = ssr.getFreetext();

        String passportNumber = passportDetails.getPassportNumber();
        String freeText = "PP" + passportNumber;

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

    public DataElementsIndiv addIsSeaman(int passengerRefNumber) {

        DataElementsIndiv de = new DataElementsIndiv();
        ElementManagementSegmentType elementManagementData = new ElementManagementSegmentType();

        elementManagementData.setSegmentName("SSR");
        de.setElementManagementData(elementManagementData);

        SpecialRequirementsDetailsTypeI serviceRequest = new SpecialRequirementsDetailsTypeI();
        SpecialRequirementsTypeDetailsTypeI ssr = new SpecialRequirementsTypeDetailsTypeI();

        ssr.setType("CKIN");
        ssr.setCompanyId("YY");

        List<String> freeTextList = ssr.getFreetext();

        String freeText = "SEAMAN";

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

    public DataElementsIndiv addVesselName(int passengerRefNumber, String vesselName) {

        DataElementsIndiv de = new DataElementsIndiv();
        ElementManagementSegmentType elementManagementData = new ElementManagementSegmentType();

        elementManagementData.setSegmentName("SSR");
        de.setElementManagementData(elementManagementData);

        SpecialRequirementsDetailsTypeI serviceRequest = new SpecialRequirementsDetailsTypeI();
        SpecialRequirementsTypeDetailsTypeI ssr = new SpecialRequirementsTypeDetailsTypeI();

        ssr.setType("OTHS");
        ssr.setCompanyId("YY");

        List<String> freeTextList = ssr.getFreetext();

        String freeText = "PAX SEMN JNG/OFF " + vesselName;


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

    public DataElementsIndiv addCTCM(int passengerRefNumber) {

        DataElementsIndiv de = new DataElementsIndiv();
        ElementManagementSegmentType elementManagementData = new ElementManagementSegmentType();

        elementManagementData.setSegmentName("SSR");
        de.setElementManagementData(elementManagementData);

        SpecialRequirementsDetailsTypeI serviceRequest = new SpecialRequirementsDetailsTypeI();
        SpecialRequirementsTypeDetailsTypeI ssr = new SpecialRequirementsTypeDetailsTypeI();

        ssr.setType("CTCM");
        ssr.setCompanyId("YY");

        List<String> freeTextList = ssr.getFreetext();

        String freeText = "919619004000";

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

    public DataElementsIndiv addCTCE(int passengerRefNumber) {

        DataElementsIndiv de = new DataElementsIndiv();
        ElementManagementSegmentType elementManagementData = new ElementManagementSegmentType();

        elementManagementData.setSegmentName("SSR");
        de.setElementManagementData(elementManagementData);

        SpecialRequirementsDetailsTypeI serviceRequest = new SpecialRequirementsDetailsTypeI();
        SpecialRequirementsTypeDetailsTypeI ssr = new SpecialRequirementsTypeDetailsTypeI();

        ssr.setType("CTCE");
        ssr.setCompanyId("YY");

        List<String> freeTextList = ssr.getFreetext();

        String freeText = "TRAVEL//JOCO.AI";

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

    private boolean isSQ(TravellerMasterInfo travellerMasterInfo) {
        String airlineStr = "SQ";
        List<String> specialAirline = new ArrayList<String>(Arrays.asList(airlineStr.split(",")));
        for (Journey journey : travellerMasterInfo.getItinerary().getJourneyList()) {
            for (AirSegmentInformation airSegmentInformation : journey.getAirSegmentList()) {
                if (specialAirline.contains(airSegmentInformation.getCarrierCode())) {
                    return true;
                }
            }
        }
        return false;
    }

    public DataElementsIndiv addSQSSR(int passengerRefNumber) {

        DataElementsIndiv de = new DataElementsIndiv();
        ElementManagementSegmentType elementManagementData = new ElementManagementSegmentType();

        elementManagementData.setSegmentName("SSR");
        de.setElementManagementData(elementManagementData);

        SpecialRequirementsDetailsTypeI serviceRequest = new SpecialRequirementsDetailsTypeI();
        SpecialRequirementsTypeDetailsTypeI ssr = new SpecialRequirementsTypeDetailsTypeI();

        ssr.setType("OTHS");
        ssr.setCompanyId("SQ");

        List<String> freeTextList = ssr.getFreetext();

        String freeText = "SEMN SQ NN1 VIEW SEAMEN TRVL DOC";

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

    private boolean isLH_UA_LX_SK_OS_AC(TravellerMasterInfo travellerMasterInfo) {
        String airlineStr = "LH,UA,LX,SK,OS,AC";
        List<String> specialAirline = new ArrayList<String>(Arrays.asList(airlineStr.split(",")));
        for (Journey journey : travellerMasterInfo.getItinerary().getJourneyList()) {
            for (AirSegmentInformation airSegmentInformation : journey.getAirSegmentList()) {
                if (specialAirline.contains(airSegmentInformation.getCarrierCode())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isAF_KL_DL(TravellerMasterInfo travellerMasterInfo) {
        String airlineStr = "AF,KL,DL";
        List<String> specialAirline = new ArrayList<String>(Arrays.asList(airlineStr.split(",")));
        for (Journey journey : travellerMasterInfo.getItinerary().getJourneyList()) {
            for (AirSegmentInformation airSegmentInformation : journey.getAirSegmentList()) {
                if (specialAirline.contains(airSegmentInformation.getCarrierCode())) {
                    return true;
                }
            }
        }
        return false;
    }

}
