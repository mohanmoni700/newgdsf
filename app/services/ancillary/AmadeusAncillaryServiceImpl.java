package services.ancillary;

import com.amadeus.xml.tpscgr_17_1_1a.ReferencingDetailsType;
import com.amadeus.xml.tpscgr_17_1_1a.ServiceStandaloneCatalogueReply;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.AncillaryServicesResponse;
import com.compassites.model.BaggageDetails;
import com.compassites.model.MealDetails;
import models.AmadeusSessionWrapper;
import models.AncillaryServiceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static com.compassites.model.PROVIDERS.AMADEUS;

@Component
public class AmadeusAncillaryServiceImpl implements AmadeusAncillaryService {

    private static final Logger logger = LoggerFactory.getLogger("gds");

    @Override
    public AncillaryServicesResponse additionalBaggageInformationStandalone(AncillaryServiceRequest ancillaryServiceRequest) {

        ServiceHandler serviceHandler = null;
        AmadeusSessionWrapper amadeusSessionWrapper = null;
        AncillaryServicesResponse excessBaggageInfoStandalone = new AncillaryServicesResponse();
        excessBaggageInfoStandalone.setProvider(AMADEUS.toString());

        try {
            serviceHandler = new ServiceHandler();
            amadeusSessionWrapper = serviceHandler.logIn();

            //1. Getting the BaggageDetails here
            ServiceStandaloneCatalogueReply serviceStandaloneCatalogueReply = serviceHandler.getAdditionalBaggageInfoStandalone(amadeusSessionWrapper, ancillaryServiceRequest);

            getAdditionalBaggageInformationStandalone(serviceStandaloneCatalogueReply, excessBaggageInfoStandalone);

        } catch (Exception e) {
            logger.debug("Error getting additional baggage ancillary information Standalone{} ", e.getMessage(), e);
        } finally {
            serviceHandler.logOut(amadeusSessionWrapper);
        }

        return excessBaggageInfoStandalone;
    }

    private static void getAdditionalBaggageInformationStandalone(ServiceStandaloneCatalogueReply serviceStandaloneCatalogueReply, AncillaryServicesResponse excessBaggageInfoStandalone) {

        Map<String, List<BaggageDetails>> baggageMap = new LinkedHashMap<>();

        try {
            List<ServiceStandaloneCatalogueReply.SsrInformation> ssrInformation = serviceStandaloneCatalogueReply.getSsrInformation();
            List<ServiceStandaloneCatalogueReply.ServiceGroup> serviceGroupList = serviceStandaloneCatalogueReply.getServiceGroup();
            List<ServiceStandaloneCatalogueReply.FlightInfo> flightInfos = serviceStandaloneCatalogueReply.getFlightInfo();

            List<BaggageDetails> baggageList = new ArrayList<>();
            Map<String, String> segmentMap = new HashMap<>();

            try {
                getSegmentWiseFlightMap(flightInfos, segmentMap);
            } catch (Exception e) {
                logger.debug("Error Getting segment wise flight map : {} ", e.getMessage(), e);
            }

            for (ServiceStandaloneCatalogueReply.ServiceGroup serviceGroup : serviceGroupList) {

                com.amadeus.xml.tpscgr_17_1_1a.ItemNumberType serviceId = serviceGroup.getServiceId();

                String serviceType = serviceId.getItemNumberDetails().get(0).getType();
                String serviceNumber = serviceId.getItemNumberDetails().get(0).getNumber();

                //Type F Baggage Types handled here
                if (serviceType.equalsIgnoreCase("SR")) {

                    BaggageDetails baggageDetails = new BaggageDetails();

                    Map<String, String> segmentStatusMap = new HashMap<>();
                    try {
                        getFlightStatusMap(serviceGroup, segmentStatusMap);
                    } catch (Exception e) {
                        logger.debug("Error With Standalone baggage - Is can Issue - : {}", e.getMessage(), e);
                    }
                    baggageDetails.setServiceId(serviceNumber);

                    //Setting Service Codes here
                    com.amadeus.xml.tpscgr_17_1_1a.PricingOrTicketingSubsequentType serviceCodes = serviceGroup.getServiceCodes();
                    baggageDetails.setRfic(serviceCodes.getSpecialCondition());
                    baggageDetails.setRfisc(serviceCodes.getOtherSpecialCondition());

                    //Setting Booking Method, MIF and Refundable here
                    List<com.amadeus.xml.tpscgr_17_1_1a.AttributeType> serviceAttributes = serviceGroup.getServiceAttributes();
                    try {
                        getBaggageServiceAttributes(serviceAttributes, baggageDetails);
                    } catch (Exception e) {
                        logger.debug("Error With Standalone baggage - Get Service Attributes - : {}", e.getMessage(), e);
                    }

                    //Service Details here
                    List<ServiceStandaloneCatalogueReply.ServiceGroup.ServiceDetailsGroup> serviceDetailsGroupList = serviceGroup.getServiceDetailsGroup();
                    try {
                        getBaggageValues(serviceGroup, serviceDetailsGroupList, baggageDetails);
                    } catch (Exception e) {
                        logger.debug("Error With Standalone baggage - Get Baggage Values - : {}", e.getMessage(), e);
                    }

                    //Setting mandatory manual inputs here
                    if (baggageDetails.isMIF()) {
                        try {
                            getMandatoryInputs(ssrInformation, baggageDetails);
                        } catch (Exception e) {
                            logger.debug("Error With Standalone baggage - Get Baggage Values - : {}", e.getMessage(), e);
                        }
                    }

                    //Setting price here
                    ServiceStandaloneCatalogueReply.ServiceGroup.PricingGroup pricingGroup = serviceGroup.getPricingGroup().get(0);

                    for (Map.Entry<String, String> entry : segmentStatusMap.entrySet()) {

                        String flightRef = entry.getKey();
                        String flightStatus = entry.getValue();

                        if (flightStatus != null && flightStatus.equalsIgnoreCase("OK")) {

                            BaggageDetails segmentBaggage = new BaggageDetails();
                            BeanUtils.copyProperties(baggageDetails, segmentBaggage);

                            //Pricing info per segment is set here
                            try {
                                getPricingInfo(pricingGroup, segmentBaggage, flightRef);
                            } catch (Exception e) {
                                logger.debug("Error With Standalone baggage - Get Baggage Pricing - : {}", e.getMessage(), e);
                            }

                            String segmentName = segmentMap.get(flightRef);
                            segmentBaggage.setSegmentNumber(flightRef);

                            //Converting the origin-destination string
                            List<String> origin0Destination1 = getOrigin0Destination1(segmentName);
                            segmentBaggage.setOrigin(origin0Destination1.get(0));
                            segmentBaggage.setDestination(origin0Destination1.get(1));

                            baggageList.add(segmentBaggage);

                        }

                    }
                }

            }

            for (Map.Entry<String, String> entry : segmentMap.entrySet()) {
                String segmentKey = entry.getValue();
                String refNo = entry.getKey();
                List<BaggageDetails> segmentWiseBaggageList = new ArrayList<>();
                for (BaggageDetails baggageDetails : baggageList) {
                    if (refNo.equalsIgnoreCase(baggageDetails.getSegmentNumber())) {
                        segmentWiseBaggageList.add(baggageDetails);
                    }
                }
                baggageMap.put(segmentKey, segmentWiseBaggageList);
            }

            excessBaggageInfoStandalone.setBaggageMap(baggageMap);
            excessBaggageInfoStandalone.setSuccess(true);


        } catch (Exception e) {
            logger.debug("Error with add Baggage information : {} ", e.getMessage(), e);
            excessBaggageInfoStandalone.setSuccess(false);
        }
    }

    private static void getSegmentWiseFlightMap(List<ServiceStandaloneCatalogueReply.FlightInfo> flightInfos, Map<String, String> segmentMap) {

        for (ServiceStandaloneCatalogueReply.FlightInfo flightInfo : flightInfos) {
            com.amadeus.xml.tpscgr_17_1_1a.TravelProductInformationType flightDetails = flightInfo.getFlightDetails();

            String segmentNumber = flightDetails.getItemNumber().toString();
            String originDestination = flightDetails.getBoardPointDetails().getTrueLocationId() + "-" + flightDetails.getOffpointDetails().getTrueLocationId();

            segmentMap.put(segmentNumber, originDestination);
        }
    }

    private static void getFlightStatusMap(ServiceStandaloneCatalogueReply.ServiceGroup serviceGroup, Map<String, String> segmentStatusMap) {

        List<ServiceStandaloneCatalogueReply.ServiceGroup.QuotaGroup> quotaGroups = serviceGroup.getQuotaGroup();

        for (ServiceStandaloneCatalogueReply.ServiceGroup.QuotaGroup quotaGroup : quotaGroups) {

            List<com.amadeus.xml.tpscgr_17_1_1a.ReferencingDetailsType> referenceDetails = quotaGroup.getSegmentReference().getReferenceDetails();
            String segmentStatus = quotaGroup.getServiceQuota().getQuotaInfo().getQuotaReachedReplyStatus();
            segmentStatusMap.put(referenceDetails.get(0).getValue(), segmentStatus);
        }
    }

    private static void getBaggageServiceAttributes(List<com.amadeus.xml.tpscgr_17_1_1a.AttributeType> serviceAttributes, BaggageDetails baggageDetails) {

        for (com.amadeus.xml.tpscgr_17_1_1a.AttributeType serviceAttribute : serviceAttributes) {
            List<com.amadeus.xml.tpscgr_17_1_1a.AttributeInformationType> criteriaDetails = serviceAttribute.getCriteriaDetails();

            for (com.amadeus.xml.tpscgr_17_1_1a.AttributeInformationType criteriaDetail : criteriaDetails) {
                String attributeType = criteriaDetail.getAttributeType();
                String attributeDescription = criteriaDetail.getAttributeDescription();

                switch (attributeType.toUpperCase()) {

                    case "BKM":
                        baggageDetails.setBkm(attributeDescription);
                        break;

                    case "MIF":
                        if (!attributeDescription.equalsIgnoreCase("N")) {
                            baggageDetails.setMIF(true);
                        }
                        break;

                    case "ROR":
                        if (attributeDescription.equalsIgnoreCase("Y")) {
                            baggageDetails.setRefundable(true);
                        }
                        break;

                    case "CNM":
                        baggageDetails.setBaggageDescription(attributeDescription);
                        break;

                }
            }
        }
    }

    private static void getBaggageValues(ServiceStandaloneCatalogueReply.ServiceGroup serviceGroup, List<ServiceStandaloneCatalogueReply.ServiceGroup.ServiceDetailsGroup> serviceDetailsGroupList, BaggageDetails baggageDetails) {

        String ssrCode;
        for (ServiceStandaloneCatalogueReply.ServiceGroup.ServiceDetailsGroup serviceDetailsGroup : serviceDetailsGroupList) {

            //Setting Airline Code and SSR code here
            com.amadeus.xml.tpscgr_17_1_1a.SpecialRequirementsDetailsType serviceDetails = serviceDetailsGroup.getServiceDetails();
            com.amadeus.xml.tpscgr_17_1_1a.SpecialRequirementsTypeDetailsType245333C specialRequirementsInfo = serviceDetails.getSpecialRequirementsInfo();

            ssrCode = specialRequirementsInfo.getSsrCode();

            baggageDetails.setCode(ssrCode);
            baggageDetails.setCarrierCode(specialRequirementsInfo.getAirlineCode());

            //Setting excess baggage value here with respect to airline filing
            ServiceStandaloneCatalogueReply.ServiceGroup.BaggageDescriptionGroup baggageDescriptionGroup;
            switch (ssrCode) {

                case "PDBG":
                    baggageDescriptionGroup = serviceGroup.getBaggageDescriptionGroup();
                    List<com.amadeus.xml.tpscgr_17_1_1a.RangeDetailsType191709S> ranges = baggageDescriptionGroup.getRange();

                    outerForLoopRange:
                    for (com.amadeus.xml.tpscgr_17_1_1a.RangeDetailsType191709S range : ranges) {
                        List<com.amadeus.xml.tpscgr_17_1_1a.RangeType> rangeDetails = range.getRangeDetails();
                        for (com.amadeus.xml.tpscgr_17_1_1a.RangeType rangeDetail : rangeDetails) {
                            String rangeType = rangeDetail.getDataType();
                            if (rangeType.equals("K")) {
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

                                baggageDetails.setWeight(value + " KG");
                                break outerForLoopRange;
                            }
                        }
                    }

                    break;


                case "ABAG":
                case "BBAG":
                case "CBAG":
                case "DBAG":
                case "EBAG":
                case "PBAG":
                case "SBAG":

                case "OVBG":
                case "HPBG":
                case "XWBG":
                case "CHDS":

                    baggageDetails.setPiece("1 PC");

                    break;

                default:
                    List<ServiceStandaloneCatalogueReply.ServiceGroup.ServiceDetailsGroup.FsfkwDataGroup> fsfkwDataGroup = serviceDetailsGroup.getFsfkwDataGroup();
                    baggageDescriptionGroup = serviceGroup.getBaggageDescriptionGroup();

                    if (fsfkwDataGroup != null && !fsfkwDataGroup.isEmpty()) {
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
                    } else if (baggageDescriptionGroup != null) {
                        List<com.amadeus.xml.tpscgr_17_1_1a.RangeDetailsType191709S> rangeList = baggageDescriptionGroup.getRange();

                        for (com.amadeus.xml.tpscgr_17_1_1a.RangeDetailsType191709S range : rangeList) {
                            List<com.amadeus.xml.tpscgr_17_1_1a.RangeType> rangeDetails = range.getRangeDetails();
                            for (com.amadeus.xml.tpscgr_17_1_1a.RangeType rangeDetail : rangeDetails) {
                                String rangeType = rangeDetail.getDataType();
                                if (rangeType.equals("K")) {
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

                                    baggageDetails.setWeight(value + " KG");
                                }

                                break;
                            }

                        }
                    }
            }
        }
    }

    private static void getMandatoryInputs(List<ServiceStandaloneCatalogueReply.SsrInformation> ssrInformation, BaggageDetails baggageDetails) {

        for (ServiceStandaloneCatalogueReply.SsrInformation ssrInfo : ssrInformation) {

            com.amadeus.xml.tpscgr_17_1_1a.SpecialRequirementsDetailsType174527S serviceRequest = ssrInfo.getServiceRequest();
            List<ServiceStandaloneCatalogueReply.SsrInformation.SsrInformationDetails> ssrInformationDetails = ssrInfo.getSsrInformationDetails();

            if (serviceRequest.getSpecialRequirementsInfo().getSsrCode().equalsIgnoreCase(baggageDetails.getCode())) {
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

    private static void getPricingInfo(ServiceStandaloneCatalogueReply.ServiceGroup.PricingGroup pricingGroup, BaggageDetails segmentBaggage, String flightRef) {

        List<ServiceStandaloneCatalogueReply.ServiceGroup.PricingGroup.CouponInfoGroup> couponInfoGroupList = pricingGroup.getCouponInfoGroup();
        for (ServiceStandaloneCatalogueReply.ServiceGroup.PricingGroup.CouponInfoGroup couponInfoGroup : couponInfoGroupList) {

            List<ReferencingDetailsType> referenceDetails = couponInfoGroup.getSegmentCouponReference().getReferenceDetails();
            for (ReferencingDetailsType referencingDetailsType : referenceDetails) {
                if (flightRef.equalsIgnoreCase(referencingDetailsType.getValue())) {
                    BigDecimal segmentWisePrice = couponInfoGroup.getMonetaryInfo().getMonetaryDetails().getAmount();
                    segmentBaggage.setPrice(segmentWisePrice.longValue());
                    break;
                }
            }
        }
    }

    private static List<String> getOrigin0Destination1(String segmentName) {
        return Arrays.asList(segmentName.split("-"));
    }


    @Override
    public AncillaryServicesResponse additionalMealsInformationStandalone(AncillaryServiceRequest ancillaryServiceRequest) {

        ServiceHandler serviceHandler = null;
        AmadeusSessionWrapper amadeusSessionWrapper = null;
        AncillaryServicesResponse mealsInfoStandalone = new AncillaryServicesResponse();
        mealsInfoStandalone.setProvider(AMADEUS.toString());

        try {
            serviceHandler = new ServiceHandler();
            amadeusSessionWrapper = serviceHandler.logIn();

            //1. Getting the Meals here
            ServiceStandaloneCatalogueReply serviceStandaloneCatalogueReply = serviceHandler.getMealsInfoStandalone(amadeusSessionWrapper, ancillaryServiceRequest);

            getMealsInformationStandalone(serviceStandaloneCatalogueReply, mealsInfoStandalone);

        } catch (Exception e) {
            logger.debug("Error getting meals information Standalone{} ", e.getMessage(), e);
        } finally {
            serviceHandler.logOut(amadeusSessionWrapper);
        }

        return mealsInfoStandalone;
    }

    private static void getMealsInformationStandalone(ServiceStandaloneCatalogueReply serviceStandaloneCatalogueReply, AncillaryServicesResponse mealsInfoStandalone) {

        List<MealDetails> mealsList = new ArrayList<>();

        try {

            List<ServiceStandaloneCatalogueReply.SsrInformation> ssrInformation = serviceStandaloneCatalogueReply.getSsrInformation();
            List<ServiceStandaloneCatalogueReply.ServiceGroup> serviceGroupList = serviceStandaloneCatalogueReply.getServiceGroup();
            List<ServiceStandaloneCatalogueReply.Portions> portions = serviceStandaloneCatalogueReply.getPortions();


            outerForLoop:
            for (ServiceStandaloneCatalogueReply.ServiceGroup serviceGroup : serviceGroupList) {

                com.amadeus.xml.tpscgr_17_1_1a.ItemNumberType serviceId = serviceGroup.getServiceId();

                String serviceType = serviceId.getItemNumberDetails().get(0).getType();
                String serviceNumber = serviceId.getItemNumberDetails().get(0).getNumber();

                //Type F
                if (serviceType.equalsIgnoreCase("SR")) {

                    MealDetails mealDetails = new MealDetails();

                    mealDetails.setServiceId(serviceNumber);

                    boolean canIssue = true;

                    //Checking if the baggage can be issued here

                    List<ServiceStandaloneCatalogueReply.ServiceGroup.QuotaGroup> quotaGroups = serviceGroup.getQuotaGroup();
                    for (ServiceStandaloneCatalogueReply.ServiceGroup.QuotaGroup quotaGroup : quotaGroups) {
                        if (quotaGroup.getServiceQuota().getQuotaInfo().getQuotaReachedReplyStatus()!=null) {
                            if (!"OK".equalsIgnoreCase(quotaGroup.getServiceQuota().getQuotaInfo().getQuotaReachedReplyStatus())) {
                                canIssue = false;
                                break;
                            }
                        } if (quotaGroup.getServiceQuota().getQuotaInfo().getAvailability()!=null) {
                            int isGreaterThanZero = quotaGroup.getServiceQuota().getQuotaInfo().getAvailability().compareTo(BigInteger.valueOf(0));
                            if(isGreaterThanZero > 0){
                                mealDetails.setAvailability(quotaGroup.getServiceQuota().getQuotaInfo().getAvailability());
                            } else if (isGreaterThanZero == 0) {
                                canIssue = false;
                                break;
                            }
                        }
                    }

                    if (canIssue) {

                        //Setting Service Codes here
                        com.amadeus.xml.tpscgr_17_1_1a.PricingOrTicketingSubsequentType serviceCodes = serviceGroup.getServiceCodes();
                        mealDetails.setRfic(serviceCodes.getSpecialCondition());
                        mealDetails.setRfisc(serviceCodes.getOtherSpecialCondition());


                        //Setting Booking Method, MIF and Refundable here
                        List<com.amadeus.xml.tpscgr_17_1_1a.AttributeType> serviceAttributes = serviceGroup.getServiceAttributes();
                        for (com.amadeus.xml.tpscgr_17_1_1a.AttributeType serviceAttribute : serviceAttributes) {
                            List<com.amadeus.xml.tpscgr_17_1_1a.AttributeInformationType> criteriaDetails = serviceAttribute.getCriteriaDetails();

                            for (com.amadeus.xml.tpscgr_17_1_1a.AttributeInformationType criteriaDetail : criteriaDetails) {
                                String attributeType = criteriaDetail.getAttributeType();
                                String attributeDescription = criteriaDetail.getAttributeDescription();

                                switch (attributeType.toUpperCase()) {

                                    case "BKM":
                                        mealDetails.setBkm(attributeDescription);
                                        break ;

                                    case "MIF":
                                        if (!attributeDescription.equalsIgnoreCase("N")) {
                                            mealDetails.setMIF(true);
                                        }
                                        break ;

                                    case "ROR":
                                        if (attributeDescription.equalsIgnoreCase("Y")) {
                                            mealDetails.setRefundable(true);
                                        }
                                        break ;

                                    case "CNM":
                                        mealDetails.setMealDesc(attributeDescription);
                                        break ;

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

                            mealDetails.setMealCode(ssrCode);
                            mealDetails.setCarrierCode(specialRequirementsInfo.getAirlineCode());

                            //Setting excess baggage value here with respect to airline filing

                            ServiceStandaloneCatalogueReply.ServiceGroup.BaggageDescriptionGroup baggageDescriptionGroup;
                            switch (ssrCode) {


                            }
                        }

                        //Setting mandatory manual inputs here for meals
                        if (mealDetails.isMIF()) {

                            for (ServiceStandaloneCatalogueReply.SsrInformation ssrInfo : ssrInformation) {

                                com.amadeus.xml.tpscgr_17_1_1a.SpecialRequirementsDetailsType174527S serviceRequest = ssrInfo.getServiceRequest();
                                List<ServiceStandaloneCatalogueReply.SsrInformation.SsrInformationDetails> ssrInformationDetails = ssrInfo.getSsrInformationDetails();

                            }
                        }

                        //Setting price here
                        ServiceStandaloneCatalogueReply.ServiceGroup.PricingGroup pricingGroup = serviceGroup.getPricingGroup().get(0);

                        //Setting Total baggage price here
                        com.amadeus.xml.tpscgr_17_1_1a.MonetaryInformationType monetaryInformation = pricingGroup.getComputedTaxSubDetails();
                        mealDetails.setMealPrice(BigDecimal.valueOf(monetaryInformation.getMonetaryDetails().getAmount().longValue()));

                        //Setting base fare and taxes here
                        List<com.amadeus.xml.tpscgr_17_1_1a.MonetaryInformationDetailsType> otherMonetaryDetails = monetaryInformation.getOtherMonetaryDetails();
                        for (com.amadeus.xml.tpscgr_17_1_1a.MonetaryInformationDetailsType monetaryInformationDetailsType : otherMonetaryDetails) {
                            String type = monetaryInformationDetailsType.getTypeQualifier();
                            Long amount = monetaryInformationDetailsType.getAmount().longValue();

                            if (type.equalsIgnoreCase("B")) {
                                mealDetails.setBasePrice(amount);
                            }

                            if (type.equalsIgnoreCase("TX")) {
                                mealDetails.setTax(amount);
                            }

                        }

                        //TODO: Create Journey wise cost here


                        mealsList.add(mealDetails);
                    }
                }
            }

            mealsInfoStandalone.setSuccess(true);
            mealsInfoStandalone.setMealDetailsList(mealsList);

        } catch (Exception e) {
            logger.debug("Error with add Meals information : {} ", e.getMessage(), e);
            mealsInfoStandalone.setSuccess(false);
        }
    }

}
