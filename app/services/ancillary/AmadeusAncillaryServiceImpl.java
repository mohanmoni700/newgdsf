package services.ancillary;

import com.amadeus.xml.pnracc_11_3_1a.ElementManagementSegmentType;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.tpicgr_17_1_1a.*;
import com.amadeus.xml.tpscgr_17_1_1a.ServiceStandaloneCatalogueReply;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.AncillaryServicesResponse;
import com.compassites.model.BaggageDetails;
import models.AmadeusSessionWrapper;
import models.AncillaryServiceRequest;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.compassites.model.PROVIDERS.AMADEUS;

@Component
public class AmadeusAncillaryServiceImpl implements AmadeusAncillaryService {

    private static final Logger logger = LoggerFactory.getLogger("gds");

    @Override
    public AncillaryServicesResponse additionalBaggageInformation(String gdsPnr) {

        ServiceHandler serviceHandler = null;
        AmadeusSessionWrapper amadeusSessionWrapper = null;
        AncillaryServicesResponse excessBaggageInfo = new AncillaryServicesResponse();
        excessBaggageInfo.setProvider(AMADEUS.toString());

        try {
            serviceHandler = new ServiceHandler();
            amadeusSessionWrapper = serviceHandler.logIn();

            //1. Retrieving the PNR
            PNRReply pnrReply = serviceHandler.retrivePNR(gdsPnr, amadeusSessionWrapper);

            List<Map<String, String>> passengerRefMap = getPassengerRefAndNames(pnrReply);
            excessBaggageInfo.setPassengerMap(passengerRefMap);

            //2. Getting the BaggageDetails here
            ServiceIntegratedCatalogueReply serviceIntegratedCatalogueReply = serviceHandler.getAdditionalBaggageInformationAmadeus(amadeusSessionWrapper);

            getAdditionalBaggageInformation(serviceIntegratedCatalogueReply, excessBaggageInfo);

        } catch (Exception e) {
            logger.debug("Error getting additional baggage ancillary information {} ", e.getMessage(), e);
        } finally {
            serviceHandler.logOut(amadeusSessionWrapper);
        }

        return excessBaggageInfo;
    }

    private static List<Map<String, String>> getPassengerRefAndNames(PNRReply pnrReply) {

        List<Map<String, String>> passengerMapList = new ArrayList<>();

        try {
            List<PNRReply.TravellerInfo> travellerInfoList = pnrReply.getTravellerInfo();
            for (PNRReply.TravellerInfo travellerInfo : travellerInfoList) {
                Map<String, String> passengerMap = new HashMap<>();

                ElementManagementSegmentType passengerReference = travellerInfo.getElementManagementPassenger();
                String referenceNumber = String.valueOf(passengerReference.getReference().getNumber());


                PNRReply.TravellerInfo.PassengerData passengerData = travellerInfo.getPassengerData().get(0);

                String salutation = null;
                String firstName = null;
                String lastName = passengerData.getTravellerInformation().getTraveller().getSurname();

                String firstNameResponse = passengerData.getTravellerInformation().getPassenger().get(0).getFirstName();
                String[] names = firstNameResponse.split("\\s");

                if (names.length > 1) {
                    //personalDetails.setSalutation(names[names.length-1]);
                    for (String name : names) {
                        if (name.equalsIgnoreCase("Mr") || name.equalsIgnoreCase("Mrs") || name.equalsIgnoreCase("Ms") || name.equalsIgnoreCase("Miss") || name.equalsIgnoreCase("Master") || name.equalsIgnoreCase("Mstr") || name.equalsIgnoreCase("Capt")) {
                            salutation = WordUtils.capitalizeFully(name);
                        } else {
                            firstName = name;
                        }
                    }
                }

                String fullName = salutation + " " + firstName + " " + lastName;

                passengerMap.put(referenceNumber, fullName);
                passengerMapList.add(passengerMap);

            }

            return passengerMapList;
        } catch (Exception e) {
            logger.debug("Error setting passenger reference : {} ", e.getMessage(), e);
            return null;
        }
    }

    private static void getAdditionalBaggageInformation(ServiceIntegratedCatalogueReply serviceIntegratedCatalogueReply, AncillaryServicesResponse excessBaggageInfo) {

        List<BaggageDetails> baggageList = new ArrayList<>();

        boolean canIssue = true;

        try {
            List<ServiceIntegratedCatalogueReply.SsrInformation> ssrInformation = serviceIntegratedCatalogueReply.getSsrInformation();
            List<ServiceIntegratedCatalogueReply.ServiceGroup> serviceGroupList = serviceIntegratedCatalogueReply.getServiceGroup();
            List<ServiceIntegratedCatalogueReply.Portions> portions = serviceIntegratedCatalogueReply.getPortions();

            outerForLoop:
            for (ServiceIntegratedCatalogueReply.ServiceGroup serviceGroup : serviceGroupList) {

                ItemNumberType serviceId = serviceGroup.getServiceId();

                String serviceType = serviceId.getItemNumberDetails().get(0).getType();
                String serviceNumber = serviceId.getItemNumberDetails().get(0).getNumber();

                //Type F
                if (serviceType.equalsIgnoreCase("SR")) {

                    BaggageDetails baggageDetails = new BaggageDetails();

                    baggageDetails.setServiceId(serviceNumber);

                    //Checking if the baggage can be issued here
                    List<ServiceIntegratedCatalogueReply.ServiceGroup.QuotaGroup> quotaGroups = serviceGroup.getQuotaGroup();
                    for (ServiceIntegratedCatalogueReply.ServiceGroup.QuotaGroup quotaGroup : quotaGroups) {
                        if (!"OK".equalsIgnoreCase(quotaGroup.getServiceQuota().getQuotaInfo().getQuotaReachedReplyStatus())) {
                            canIssue = false;
                            break;
                        }
                    }

                    if (canIssue) {

                        //Setting Service Codes here
                        PricingOrTicketingSubsequentType serviceCodes = serviceGroup.getServiceCodes();
                        baggageDetails.setRfic(serviceCodes.getSpecialCondition());
                        baggageDetails.setRfisc(serviceCodes.getOtherSpecialCondition());


                        //Setting Booking Method, MIF and Refundable here
                        List<AttributeType> serviceAttributes = serviceGroup.getServiceAttributes();
                        for (AttributeType serviceAttribute : serviceAttributes) {
                            List<AttributeInformationType> criteriaDetails = serviceAttribute.getCriteriaDetails();

                            for (AttributeInformationType criteriaDetail : criteriaDetails) {
                                String attributeType = criteriaDetail.getAttributeType();
                                String attributeDescription = criteriaDetail.getAttributeDescription();

                                if (attributeType.equalsIgnoreCase("BKM")) {
                                    baggageDetails.setBkm(attributeDescription);
                                }

                                if (attributeType.equalsIgnoreCase("MIF") && !attributeDescription.equalsIgnoreCase("N")) {
                                    baggageDetails.setMIF(true);
                                }

                                if (attributeType.equalsIgnoreCase("ROR") && attributeDescription.equalsIgnoreCase("Y")) {
                                    baggageDetails.setRefundable(true);
                                }

                                if (attributeType.equalsIgnoreCase("CNM")) {
                                    baggageDetails.setBaggageDescription(attributeDescription);
                                }

                            }
                        }

                        //Service Details here
                        String ssrCode = null;
                        List<ServiceIntegratedCatalogueReply.ServiceGroup.ServiceDetailsGroup> serviceDetailsGroupList = serviceGroup.getServiceDetailsGroup();
                        for (ServiceIntegratedCatalogueReply.ServiceGroup.ServiceDetailsGroup serviceDetailsGroup : serviceDetailsGroupList) {

                            //Setting Airline Code and SSR code here
                            SpecialRequirementsDetailsType serviceDetails = serviceDetailsGroup.getServiceDetails();
                            SpecialRequirementsTypeDetailsType245333C specialRequirementsInfo = serviceDetails.getSpecialRequirementsInfo();

                            ssrCode = specialRequirementsInfo.getSsrCode();

                            baggageDetails.setCode(ssrCode);
                            baggageDetails.setCarrierCode(specialRequirementsInfo.getAirlineCode());

                            //Setting excess baggage value here with respect to airline filing
                            if (!ssrCode.equalsIgnoreCase("PDBG")) {

                                List<ServiceIntegratedCatalogueReply.ServiceGroup.ServiceDetailsGroup.FsfkwDataGroup> fsfkwDataGroup = serviceDetailsGroup.getFsfkwDataGroup();
                                for (ServiceIntegratedCatalogueReply.ServiceGroup.ServiceDetailsGroup.FsfkwDataGroup fsfkwData : fsfkwDataGroup) {
                                    AttributeType208309S fsfkwValues = fsfkwData.getFsfkwValues();
                                    RangeDetailsType208311S fsfkwRanges = fsfkwData.getFsfkwRanges();

                                    if (fsfkwValues.getCriteriaDetails().getAttributeType().equalsIgnoreCase("WVAL")) {
                                        baggageDetails.setWeight(fsfkwRanges.getRangeDetails().getMax() + " KG");
                                    }
                                    if (fsfkwValues.getCriteriaDetails().getAttributeType().equalsIgnoreCase("PVAL")) {
                                        baggageDetails.setPiece(fsfkwRanges.getRangeDetails().getMax() + " PC");
                                    }
                                }
                            }

                            if (ssrCode.equalsIgnoreCase("PDBG")) {

                                ServiceIntegratedCatalogueReply.ServiceGroup.BaggageDescriptionGroup baggageDescriptionGroup = serviceGroup.getBaggageDescriptionGroup();
                                List<AttributeType> baggageAttributes = baggageDescriptionGroup.getBaggageAttributes();
                                List<RangeDetailsType191709S> ranges = baggageDescriptionGroup.getRange();
                                ExcessBaggageType baggageData = baggageDescriptionGroup.getBaggageData();

                                for (RangeDetailsType191709S range : ranges) {
                                    String quantity = range.getRangeQualifier();
                                    List<RangeType> rangeDetails = range.getRangeDetails();
                                    for (RangeType rangeDetail : rangeDetails) {
                                        String rangeType = rangeDetail.getDataType();
                                        if (rangeType.equalsIgnoreCase("K") || rangeType.equalsIgnoreCase("P")) {
                                            BigDecimal max = rangeDetail.getMax();
                                            BigDecimal min = rangeDetail.getMin();
                                            BigDecimal value = null;

                                            if (min != null && max != null) {
                                                value = max;
                                            } else if (max != null) {
                                                value = max;
                                            } else if (min != null) {
                                                value = min;
                                            }

                                            switch (rangeType) {

                                                case "K":
                                                    baggageDetails.setWeight(value + " KG");
                                                    break;
                                                case "P":
                                                    baggageDetails.setPiece(value + " PC");
                                                    break;
                                                default:
                                                    baggageDetails.setWeight(null);
                                                    baggageDetails.setPiece(null);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        //Setting mandatory manual inputs here
                        if (baggageDetails.isMIF()) {

                            for (ServiceIntegratedCatalogueReply.SsrInformation ssrInfo : ssrInformation) {

                                SpecialRequirementsDetailsType174527S serviceRequest = ssrInfo.getServiceRequest();
                                List<ServiceIntegratedCatalogueReply.SsrInformation.SsrInformationDetails> ssrInformationDetails = ssrInfo.getSsrInformationDetails();

                                if (serviceRequest.getSpecialRequirementsInfo().getSsrCode().equalsIgnoreCase(ssrCode)) {
                                    for (ServiceIntegratedCatalogueReply.SsrInformation.SsrInformationDetails ssrInformationDetail : ssrInformationDetails) {
                                        List<StructureComponentDefinitionType> ssrFormattedFreeTexts = ssrInformationDetail.getSsrFormattedFreetext();
                                        for (StructureComponentDefinitionType ssrFormattedFreetext : ssrFormattedFreeTexts) {

                                            String identifier = ssrFormattedFreetext.getComponentId().getIdentifier();
                                            boolean isMandatory = ssrFormattedFreetext.getStatus().equals("M");

                                            if (identifier.equalsIgnoreCase("FMT") && isMandatory) {
                                                baggageDetails.setFMT(true);
                                            } else if (identifier.equalsIgnoreCase("WVAL") && isMandatory) {
                                                baggageDetails.setWVAL(true);
                                            } else if (identifier.equalsIgnoreCase("PVAL") && isMandatory) {
                                                baggageDetails.setPVAL(true);
                                            } else if (identifier.equalsIgnoreCase("FTXT") && isMandatory) {
                                                baggageDetails.setFTXT(true);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        //Setting price here
                        ServiceIntegratedCatalogueReply.ServiceGroup.PricingGroup pricingGroup = serviceGroup.getPricingGroup().get(0);

                        //Setting Total baggage price here
                        MonetaryInformationType monetaryInformation = pricingGroup.getComputedTaxSubDetails();
                        baggageDetails.setPrice(monetaryInformation.getMonetaryDetails().getAmount().longValue());

                        //Setting base fare and taxes here
                        List<MonetaryInformationDetailsType> otherMonetaryDetails = monetaryInformation.getOtherMonetaryDetails();
                        for (MonetaryInformationDetailsType monetaryInformationDetailsType : otherMonetaryDetails) {
                            String type = monetaryInformationDetailsType.getTypeQualifier();
                            Long amount = monetaryInformationDetailsType.getAmount().longValue();

                            if (type.equalsIgnoreCase("B")) {
                                baggageDetails.setBasePrice(amount);
                            }

                            if (type.equalsIgnoreCase("TX")) {
                                baggageDetails.setTax(amount);
                            }

                        }

                        //TODO: Create Journey wise cost here


                        baggageList.add(baggageDetails);
                    }
                }
            }

            excessBaggageInfo.setSuccess(true);
            excessBaggageInfo.setBaggageList(baggageList);

        } catch (Exception e) {
            logger.debug("Error with add Baggage information : {} ", e.getMessage(), e);
            excessBaggageInfo.setSuccess(false);
        }
    }

    @Override
    public AncillaryServicesResponse additionalBaggageInformationStandalone( AncillaryServiceRequest ancillaryServiceRequest) {

        ServiceHandler serviceHandler = null;
        AmadeusSessionWrapper amadeusSessionWrapper = null;
        AncillaryServicesResponse excessBaggageInfoStandalone = new AncillaryServicesResponse();
        excessBaggageInfoStandalone.setProvider(AMADEUS.toString());

        try {
            serviceHandler = new ServiceHandler();
            amadeusSessionWrapper = serviceHandler.logIn();

            //1. Retrieving the PNR
            PNRReply pnrReply = serviceHandler.retrivePNR(ancillaryServiceRequest.getGdsPNR(), amadeusSessionWrapper);

            List<Map<String, String>> passengerRefMap = getPassengerRefAndNames(pnrReply);
            excessBaggageInfoStandalone.setPassengerMap(passengerRefMap);

            //2. Getting the BaggageDetails here
            ServiceStandaloneCatalogueReply serviceStandaloneCatalogueReply = serviceHandler.getAdditionalBaggageInfoStandalone(amadeusSessionWrapper, passengerRefMap, ancillaryServiceRequest);

            getAdditionalBaggageInformationStandalone(serviceStandaloneCatalogueReply, excessBaggageInfoStandalone);

        } catch (Exception e) {
            logger.debug("Error getting additional baggage ancillary information Standalone{} ", e.getMessage(), e);
        } finally {
            serviceHandler.logOut(amadeusSessionWrapper);
        }

        return excessBaggageInfoStandalone;
    }

    private static void getAdditionalBaggageInformationStandalone(ServiceStandaloneCatalogueReply serviceStandaloneCatalogueReply, AncillaryServicesResponse excessBaggageInfoStandalone) {

        List<BaggageDetails> baggageList = new ArrayList<>();

        boolean canIssue = true;

        try {
            List<ServiceStandaloneCatalogueReply.SsrInformation> ssrInformation = serviceStandaloneCatalogueReply.getSsrInformation();
            List<ServiceStandaloneCatalogueReply.ServiceGroup> serviceGroupList = serviceStandaloneCatalogueReply.getServiceGroup();
            List<ServiceStandaloneCatalogueReply.Portions> portions = serviceStandaloneCatalogueReply.getPortions();

            String freeBaggageValue;
            String freeBaggageUnit;

            //Free Baggage Allowance here
            for (ServiceStandaloneCatalogueReply.ServiceGroup serviceGroup : serviceGroupList) {
                com.amadeus.xml.tpscgr_17_1_1a.ItemNumberType serviceId = serviceGroup.getServiceId();
                String serviceType = serviceId.getItemNumberDetails().get(0).getType();

                if (serviceType.equalsIgnoreCase("FBA")) {

                    ServiceStandaloneCatalogueReply.ServiceGroup.BaggageDescriptionGroup baggageDescriptionGroup = serviceGroup.getBaggageDescriptionGroup();
                    com.amadeus.xml.tpscgr_17_1_1a.BaggageDetailsType baggageDetails = baggageDescriptionGroup.getBaggageData().getBaggageDetails();

                    String unitQualifier = baggageDetails.getUnitQualifier();
                    switch (unitQualifier) {

                        case "K":
                            freeBaggageUnit = "KG";
                            break;
                        case "P":
                            freeBaggageUnit = "PC";
                            break;

                    }

                    freeBaggageValue = String.valueOf(baggageDetails.getMeasurement());
                }
            }

            outerForLoop:
            for (ServiceStandaloneCatalogueReply.ServiceGroup serviceGroup : serviceGroupList) {

                com.amadeus.xml.tpscgr_17_1_1a.ItemNumberType serviceId = serviceGroup.getServiceId();

                String serviceType = serviceId.getItemNumberDetails().get(0).getType();
                String serviceNumber = serviceId.getItemNumberDetails().get(0).getNumber();

                //Type F
                if (serviceType.equalsIgnoreCase("SR")) {

                    BaggageDetails baggageDetails = new BaggageDetails();

                    baggageDetails.setServiceId(serviceNumber);

                    //Checking if the baggage can be issued here
                    List<ServiceStandaloneCatalogueReply.ServiceGroup.QuotaGroup> quotaGroups = serviceGroup.getQuotaGroup();
                    for (ServiceStandaloneCatalogueReply.ServiceGroup.QuotaGroup quotaGroup : quotaGroups) {
                        if (!"OK".equalsIgnoreCase(quotaGroup.getServiceQuota().getQuotaInfo().getQuotaReachedReplyStatus())) {
                            canIssue = false;
                            break;
                        }
                    }

                    if (canIssue) {

                        //Setting Service Codes here
                        com.amadeus.xml.tpscgr_17_1_1a.PricingOrTicketingSubsequentType serviceCodes = serviceGroup.getServiceCodes();
                        baggageDetails.setRfic(serviceCodes.getSpecialCondition());
                        baggageDetails.setRfisc(serviceCodes.getOtherSpecialCondition());


                        //Setting Booking Method, MIF and Refundable here
                        List<com.amadeus.xml.tpscgr_17_1_1a.AttributeType> serviceAttributes = serviceGroup.getServiceAttributes();
                        for (com.amadeus.xml.tpscgr_17_1_1a.AttributeType serviceAttribute : serviceAttributes) {
                            List<com.amadeus.xml.tpscgr_17_1_1a.AttributeInformationType> criteriaDetails = serviceAttribute.getCriteriaDetails();

                            for (com.amadeus.xml.tpscgr_17_1_1a.AttributeInformationType criteriaDetail : criteriaDetails) {
                                String attributeType = criteriaDetail.getAttributeType();
                                String attributeDescription = criteriaDetail.getAttributeDescription();

                                if (attributeType.equalsIgnoreCase("BKM")) {
                                    baggageDetails.setBkm(attributeDescription);
                                }

                                if (attributeType.equalsIgnoreCase("MIF") && !attributeDescription.equalsIgnoreCase("N")) {
                                    baggageDetails.setMIF(true);
                                }

                                if (attributeType.equalsIgnoreCase("ROR") && attributeDescription.equalsIgnoreCase("Y")) {
                                    baggageDetails.setRefundable(true);
                                }

                                if (attributeType.equalsIgnoreCase("CNM")) {
                                    baggageDetails.setBaggageDescription(attributeDescription);
                                }

                            }
                        }

                        //Service Details here
                        String ssrCode = null;
                        List<ServiceStandaloneCatalogueReply.ServiceGroup.ServiceDetailsGroup> serviceDetailsGroupList = serviceGroup.getServiceDetailsGroup();
                        for (ServiceStandaloneCatalogueReply.ServiceGroup.ServiceDetailsGroup serviceDetailsGroup : serviceDetailsGroupList) {

                            //Setting Airline Code and SSR code here
                            com.amadeus.xml.tpscgr_17_1_1a.SpecialRequirementsDetailsType serviceDetails = serviceDetailsGroup.getServiceDetails();
                            com.amadeus.xml.tpscgr_17_1_1a.SpecialRequirementsTypeDetailsType245333C specialRequirementsInfo = serviceDetails.getSpecialRequirementsInfo();

                            ssrCode = specialRequirementsInfo.getSsrCode();

                            baggageDetails.setCode(ssrCode);
                            baggageDetails.setCarrierCode(specialRequirementsInfo.getAirlineCode());

                            //Setting excess baggage value here with respect to airline filing

                            switch (ssrCode) {

                                case "PDBG":

                                    ServiceStandaloneCatalogueReply.ServiceGroup.BaggageDescriptionGroup baggageDescriptionGroup = serviceGroup.getBaggageDescriptionGroup();
                                    List<com.amadeus.xml.tpscgr_17_1_1a.AttributeType> baggageAttributes = baggageDescriptionGroup.getBaggageAttributes();
                                    List<com.amadeus.xml.tpscgr_17_1_1a.RangeDetailsType191709S> ranges = baggageDescriptionGroup.getRange();
                                    com.amadeus.xml.tpscgr_17_1_1a.ExcessBaggageType baggageData = baggageDescriptionGroup.getBaggageData();

                                    for (com.amadeus.xml.tpscgr_17_1_1a.RangeDetailsType191709S range : ranges) {
                                        String quantity = range.getRangeQualifier();
                                        List<com.amadeus.xml.tpscgr_17_1_1a.RangeType> rangeDetails = range.getRangeDetails();
                                        for (com.amadeus.xml.tpscgr_17_1_1a.RangeType rangeDetail : rangeDetails) {
                                            String rangeType = rangeDetail.getDataType();
                                            if (rangeType.equalsIgnoreCase("K") || rangeType.equalsIgnoreCase("P")) {
                                                BigDecimal max = rangeDetail.getMax();
                                                BigDecimal min = rangeDetail.getMin();
                                                BigDecimal value = null;

                                                if (min != null && max != null) {
                                                    value = max;
                                                } else if (max != null) {
                                                    value = max;
                                                } else if (min != null) {
                                                    value = min;
                                                }

                                                switch (rangeType) {

                                                    case "K":
                                                        baggageDetails.setWeight(value + " KG");
                                                        break;
                                                    case "P":
                                                        baggageDetails.setPiece(value + " PC");
                                                        break;
                                                    default:
                                                        baggageDetails.setWeight(null);
                                                        baggageDetails.setPiece(null);
                                                }
                                            }
                                        }
                                    }
                                    break;

                                case "ABAG":
                                case "BBAG":
                                case "CBAG":
                                case "DBAG":
                                case "EBAG":

                                    baggageDetails.setPiece("1 PC");

                                    break;

                                default:
                                    List<ServiceStandaloneCatalogueReply.ServiceGroup.ServiceDetailsGroup.FsfkwDataGroup> fsfkwDataGroup = serviceDetailsGroup.getFsfkwDataGroup();
                                    for (ServiceStandaloneCatalogueReply.ServiceGroup.ServiceDetailsGroup.FsfkwDataGroup fsfkwData : fsfkwDataGroup) {
                                        com.amadeus.xml.tpscgr_17_1_1a.AttributeType208309S fsfkwValues = fsfkwData.getFsfkwValues();
                                        com.amadeus.xml.tpscgr_17_1_1a.RangeDetailsType208311S fsfkwRanges = fsfkwData.getFsfkwRanges();

                                        if (fsfkwValues.getCriteriaDetails().getAttributeType().equalsIgnoreCase("WVAL")) {
                                            baggageDetails.setWeight(fsfkwRanges.getRangeDetails().getMax() + " KG");
                                        }
                                        if (fsfkwValues.getCriteriaDetails().getAttributeType().equalsIgnoreCase("PVAL")) {
                                            baggageDetails.setPiece(fsfkwRanges.getRangeDetails().getMax() + " PC");
                                        }
                                    }
                            }



//                            if (!ssrCode.equalsIgnoreCase("PDBG")) {
//
//                                List<ServiceStandaloneCatalogueReply.ServiceGroup.ServiceDetailsGroup.FsfkwDataGroup> fsfkwDataGroup = serviceDetailsGroup.getFsfkwDataGroup();
//                                for (ServiceStandaloneCatalogueReply.ServiceGroup.ServiceDetailsGroup.FsfkwDataGroup fsfkwData : fsfkwDataGroup) {
//                                    com.amadeus.xml.tpscgr_17_1_1a.AttributeType208309S fsfkwValues = fsfkwData.getFsfkwValues();
//                                    com.amadeus.xml.tpscgr_17_1_1a.RangeDetailsType208311S fsfkwRanges = fsfkwData.getFsfkwRanges();
//
//                                    if (fsfkwValues.getCriteriaDetails().getAttributeType().equalsIgnoreCase("WVAL")) {
//                                        baggageDetails.setWeight(fsfkwRanges.getRangeDetails().getMax() + " KG");
//                                    }
//                                    if (fsfkwValues.getCriteriaDetails().getAttributeType().equalsIgnoreCase("PVAL")) {
//                                        baggageDetails.setPiece(fsfkwRanges.getRangeDetails().getMax() + " PC");
//                                    }
//                                }
//                            }
//
//                            if (ssrCode.equalsIgnoreCase("PDBG")) {
//
//                                ServiceStandaloneCatalogueReply.ServiceGroup.BaggageDescriptionGroup baggageDescriptionGroup = serviceGroup.getBaggageDescriptionGroup();
//                                List<com.amadeus.xml.tpscgr_17_1_1a.AttributeType> baggageAttributes = baggageDescriptionGroup.getBaggageAttributes();
//                                List<com.amadeus.xml.tpscgr_17_1_1a.RangeDetailsType191709S> ranges = baggageDescriptionGroup.getRange();
//                                com.amadeus.xml.tpscgr_17_1_1a.ExcessBaggageType baggageData = baggageDescriptionGroup.getBaggageData();
//
//                                for (com.amadeus.xml.tpscgr_17_1_1a.RangeDetailsType191709S range : ranges) {
//                                    String quantity = range.getRangeQualifier();
//                                    List<com.amadeus.xml.tpscgr_17_1_1a.RangeType> rangeDetails = range.getRangeDetails();
//                                    for (com.amadeus.xml.tpscgr_17_1_1a.RangeType rangeDetail : rangeDetails) {
//                                        String rangeType = rangeDetail.getDataType();
//                                        if (rangeType.equalsIgnoreCase("K") || rangeType.equalsIgnoreCase("P")) {
//                                            BigDecimal max = rangeDetail.getMax();
//                                            BigDecimal min = rangeDetail.getMin();
//                                            BigDecimal value = null;
//
//                                            if (min != null && max != null) {
//                                                value = max;
//                                            } else if (max != null) {
//                                                value = max;
//                                            } else if (min != null) {
//                                                value = min;
//                                            }
//
//                                            switch (rangeType) {
//
//                                                case "K":
//                                                    baggageDetails.setWeight(value + " KG");
//                                                    break;
//                                                case "P":
//                                                    baggageDetails.setPiece(value + " PC");
//                                                    break;
//                                                default:
//                                                    baggageDetails.setWeight(null);
//                                                    baggageDetails.setPiece(null);
//                                            }
//                                        }
//                                    }
//                                }
//                            }
                       }

                        //Setting mandatory manual inputs here
                        if (baggageDetails.isMIF()) {

                            for (ServiceStandaloneCatalogueReply.SsrInformation ssrInfo : ssrInformation) {

                                com.amadeus.xml.tpscgr_17_1_1a.SpecialRequirementsDetailsType174527S serviceRequest = ssrInfo.getServiceRequest();
                                List<ServiceStandaloneCatalogueReply.SsrInformation.SsrInformationDetails> ssrInformationDetails = ssrInfo.getSsrInformationDetails();

                                if (serviceRequest.getSpecialRequirementsInfo().getSsrCode().equalsIgnoreCase(ssrCode)) {
                                    for (ServiceStandaloneCatalogueReply.SsrInformation.SsrInformationDetails ssrInformationDetail : ssrInformationDetails) {
                                        List<com.amadeus.xml.tpscgr_17_1_1a.StructureComponentDefinitionType> ssrFormattedFreeTexts = ssrInformationDetail.getSsrFormattedFreetext();
                                        for (com.amadeus.xml.tpscgr_17_1_1a.StructureComponentDefinitionType ssrFormattedFreetext : ssrFormattedFreeTexts) {

                                            String identifier = ssrFormattedFreetext.getComponentId().getIdentifier();
                                            boolean isMandatory = ssrFormattedFreetext.getStatus().equals("M");

                                            if (identifier.equalsIgnoreCase("FMT") && isMandatory) {
                                                baggageDetails.setFMT(true);
                                            } else if (identifier.equalsIgnoreCase("WVAL") && isMandatory) {
                                                baggageDetails.setWVAL(true);
                                            } else if (identifier.equalsIgnoreCase("PVAL") && isMandatory) {
                                                baggageDetails.setPVAL(true);
                                            } else if (identifier.equalsIgnoreCase("FTXT") && isMandatory) {
                                                baggageDetails.setFTXT(true);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        //Setting price here
                        ServiceStandaloneCatalogueReply.ServiceGroup.PricingGroup pricingGroup = serviceGroup.getPricingGroup().get(0);

                        //Setting Total baggage price here
                        com.amadeus.xml.tpscgr_17_1_1a.MonetaryInformationType monetaryInformation = pricingGroup.getComputedTaxSubDetails();
                        baggageDetails.setPrice(monetaryInformation.getMonetaryDetails().getAmount().longValue());

                        //Setting base fare and taxes here
                        List<com.amadeus.xml.tpscgr_17_1_1a.MonetaryInformationDetailsType> otherMonetaryDetails = monetaryInformation.getOtherMonetaryDetails();
                        for (com.amadeus.xml.tpscgr_17_1_1a.MonetaryInformationDetailsType monetaryInformationDetailsType : otherMonetaryDetails) {
                            String type = monetaryInformationDetailsType.getTypeQualifier();
                            Long amount = monetaryInformationDetailsType.getAmount().longValue();

                            if (type.equalsIgnoreCase("B")) {
                                baggageDetails.setBasePrice(amount);
                            }

                            if (type.equalsIgnoreCase("TX")) {
                                baggageDetails.setTax(amount);
                            }

                        }

                        //TODO: Create Journey wise cost here


                        baggageList.add(baggageDetails);
                    }
                }
            }

            excessBaggageInfoStandalone.setSuccess(true);
            excessBaggageInfoStandalone.setBaggageList(baggageList);

        } catch (Exception e) {
            logger.debug("Error with add Baggage information : {} ", e.getMessage(), e);
            excessBaggageInfoStandalone.setSuccess(false);
        }
    }

}
