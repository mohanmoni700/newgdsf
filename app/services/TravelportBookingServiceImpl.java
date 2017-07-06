package services;

import com.compassites.GDSWrapper.travelport.*;
import com.compassites.model.*;
import com.compassites.model.Journey;
import com.compassites.model.traveller.PersonalDetails;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.travelport.schema.air_v26_0.*;
import com.travelport.schema.common_v26_0.*;
import com.travelport.schema.universal_v26_0.*;
import com.travelport.service.air_v26_0.AirFaultMessage;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import play.libs.Json;
import utils.*;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/*
 *
 * Created by user on 02-09-2014.
 */

@Service
public class TravelportBookingServiceImpl implements BookingService {

    static Logger logger = LoggerFactory.getLogger("gds");

	private TravelportCancelServiceImpl cancelService;

	public TravelportCancelServiceImpl getCancelService() {
		return cancelService;
	}

	@Autowired
	private RedisTemplate redisTemplate;

	public RedisTemplate getRedisTemplate() {
		return redisTemplate;
	}

	public void setRedisTemplate(RedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Autowired
	public void setCancelService(TravelportCancelServiceImpl cancelService) {
		this.cancelService = cancelService;
	}

	@Override
	public PNRResponse generatePNR(TravellerMasterInfo travellerMasterInfo) {
		PNRResponse pnrResponse = new PNRResponse();
		try {
			// AirItinerary airItinerary =
			// AirRequestClient.getItinerary(responseTwo,
			// responseTwo.getAirPricingSolution().get(0));
			AirItinerary airItinerary = AirRequestClient
					.buildAirItinerary(travellerMasterInfo.getItinerary(), travellerMasterInfo.isSeamen());
			TypeCabinClass typeCabinClass = TypeCabinClass
					.valueOf(travellerMasterInfo.getCabinClass().upperValue());

			AirPriceRsp priceRsp = AirRequestClient.priceItinerary(
					airItinerary, "INR", typeCabinClass, travellerMasterInfo.getTravellersList(), travellerMasterInfo.isSeamen());
			pnrResponse = checkFare(priceRsp, travellerMasterInfo);
			if (!pnrResponse.isPriceChanged()) {
				// AirPricingSolution airPriceSolution =
				// AirReservationClient.stripNonXmitSections(AirRequestClient.getPriceSolution(priceRsp));
				AirCreateReservationRsp reservationRsp = AirReservationClient
						.reserve(AirRequestClient.getPriceSolution(priceRsp),
								travellerMasterInfo);
				//todo check status of each segment
				boolean anyUnConfirmedSegments = TravelportBookingHelper.checkSegmentStatus(reservationRsp.getUniversalRecord().getAirReservation().get(0).getAirSegment());
				if(anyUnConfirmedSegments){
					logger.debug("Unconfirmed segments received cancelling the booking");
					pnrResponse.setFlightAvailable(false);
					cancelService.cancelPNR(reservationRsp.getUniversalRecord().getAirReservation().get(0).getLocatorCode());
					return pnrResponse;
				}
				UniversalRecordRetrieveRsp universalRecordRetrieveRsp = UniversalRecordClient
						.retrievePNR(reservationRsp);
				pnrResponse = retrievePNR(universalRecordRetrieveRsp.getUniversalRecord(),
						pnrResponse,travellerMasterInfo);
			}
            setTaxBreakup(pnrResponse, travellerMasterInfo, priceRsp);

		} catch (AirFaultMessage airFaultMessage) {
			airFaultMessage.printStackTrace(); // To change body of catch
												// statement use File | Settings
												// | File Templates.
			logger.error("Travelport generatePNR error ", airFaultMessage);
			ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(
					"error", ErrorMessage.ErrorType.ERROR, "TravelPort");
			pnrResponse.setErrorMessage(errorMessage);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Travelport generatePNR error ", e);
			ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(
					"error", ErrorMessage.ErrorType.ERROR, "TravelPort");
			pnrResponse.setErrorMessage(errorMessage);
		}

		return pnrResponse;
	}

	@Override
	public PNRResponse priceChangePNR(TravellerMasterInfo travellerMasterInfo) {
		return generatePNR(travellerMasterInfo);
	}

	public PNRResponse checkFare(AirPriceRsp priceRsp,
			TravellerMasterInfo travellerMasterInfo) {
		PNRResponse pnrResponse = new PNRResponse();
		BigDecimal searchPrice = new BigDecimal(0);
		if (travellerMasterInfo.isSeamen()) {
			searchPrice = travellerMasterInfo.getItinerary()
					.getSeamanPricingInformation().getTotalPriceValue();
		} else {
			searchPrice = travellerMasterInfo.getItinerary()
					.getPricingInformation().getTotalPriceValue();
		}
		BigDecimal totalPrice = new BigDecimal(
				StringUtility.getPriceFromString(priceRsp.getAirPriceResult()
						.get(0).getAirPricingSolution().get(0).getTotalPrice()));
		BigDecimal changedBasePrice = new BigDecimal(
				StringUtility.getPriceFromString(priceRsp.getAirPriceResult()
						.get(0).getAirPricingSolution().get(0).getBasePrice()));

		PricingInformation pricingInformation = new PricingInformation();
		pricingInformation = TravelportHelper.getPriceDetails(pricingInformation,priceRsp.getAirPriceResult()
				.get(0).getAirPricingSolution().get(0).getAirPricingInfo());
		pnrResponse.setPricingInfo(pricingInformation);

		travellerMasterInfo.getItinerary().setPricingInformation(travellerMasterInfo.isSeamen(), pricingInformation);

		pnrResponse.setChangedPrice(totalPrice);
		pnrResponse.setChangedBasePrice(changedBasePrice);
		pnrResponse.setOriginalPrice(searchPrice);
		pnrResponse.setFlightAvailable(true);
		if(totalPrice.toBigIntegerExact().equals(searchPrice.toBigIntegerExact())) {
			pnrResponse.setPriceChanged(false);
		} else {
			pnrResponse.setPriceChanged(true);
		}

		return pnrResponse;
	}

    public static void setTaxBreakup(PNRResponse pnrResponse, TravellerMasterInfo travellerMasterInfo,AirPriceRsp  priceRsp){
        PricingInformation pricingInfo = travellerMasterInfo.isSeamen() ? travellerMasterInfo
                .getItinerary().getSeamanPricingInformation() : travellerMasterInfo
                .getItinerary().getPricingInformation();
        TravelportHelper.getPassengerTaxes(pricingInfo, priceRsp.getAirPriceResult().get(0).getAirPricingSolution().get(0).getAirPricingInfo());
        pnrResponse.setPricingInfo(pricingInfo);
    }

	public PNRResponse retrievePNR(UniversalRecord universalRecord,PNRResponse pnrResponse,TravellerMasterInfo travellerMasterInfo) {
		
		Helper.ReservationInfoMap reservationInfoMap = Helper
				.createReservationInfoMap(universalRecord.getProviderReservationInfo());
		Date lastDate = null;
		Calendar calendar = Calendar.getInstance();
		for (AirReservation airReservation : universalRecord.getAirReservation()) {
			for (ProviderReservationInfoRef reservationInfoRef : airReservation
					.getProviderReservationInfoRef()) {
				ProviderReservationInfo reservationInfo = reservationInfoMap
						.getByRef(reservationInfoRef);
				if(!StringUtils.hasText(reservationInfo.getLocatorCode())) {
					ErrorMessage error = ErrorMessageHelper.createErrorMessage(
							"Booking failed", ErrorMessage.ErrorType.ERROR, "Travelport");
					pnrResponse.setErrorMessage(error);
					return pnrResponse;
				}
				pnrResponse.setPnrNumber(reservationInfo.getLocatorCode());
			}
		}
		List<AirReservation> airResList = universalRecord.getAirReservation();
		if(airResList.size() > 0) {
			AirReservation airResInfo = airResList.get(0);
			List<SupplierLocator> supplierLocators = airResInfo.getSupplierLocator();
			if(supplierLocators.size() > 0) {		
				String airlinePNR = supplierLocators.get(0).getSupplierLocatorCode();
				pnrResponse.setAirlinePNR(airlinePNR);
			}
		}
        Date holdDate = HoldTimeUtility.getHoldTime(travellerMasterInfo);
		try {
			List<GeneralRemark> generalRemarks = universalRecord.getGeneralRemark();
			if(generalRemarks != null && generalRemarks.size() > 0) {
				String remarkData = generalRemarks.get(0).getRemarkData();
				int i = remarkData.lastIndexOf("BY");
				String subString = remarkData.substring(i + 2);

				subString = subString.trim();
				String[] args1 = subString.split("/");
				String dateString = args1[0] + "/" + args1[1];
				dateString = dateString + calendar.get(Calendar.YEAR);
				SimpleDateFormat sdf = new SimpleDateFormat("HHmm/ddMMMyyyy");

				lastDate = sdf.parse(dateString);
                pnrResponse.setHoldTime(false);
			} else {
				calendar.setTime(holdDate);
				lastDate = calendar.getTime();
                pnrResponse.setHoldTime(true);
			}
		} catch (Exception e) {
			e.printStackTrace();
			calendar.setTime(holdDate);
			lastDate = calendar.getTime();
		}
		pnrResponse.setValidTillDate(lastDate);
		return pnrResponse;
	}

	public PNRResponse checkFareChangeAndAvailability(
			TravellerMasterInfo travellerMasterInfo) {
		PNRResponse pnrResponse = new PNRResponse();
		try {
			// AirItinerary airItinerary =
			// AirRequestClient.getItinerary(responseTwo,
			// responseTwo.getAirPricingSolution().get(0));
			AirItinerary airItinerary = AirRequestClient.buildAirItinerary(travellerMasterInfo.getItinerary(), travellerMasterInfo.isSeamen());
			TypeCabinClass typeCabinClass = TypeCabinClass.valueOf(travellerMasterInfo.getCabinClass().upperValue());

			AirPriceRsp priceRsp = AirRequestClient.priceItinerary(airItinerary, "INR",typeCabinClass, travellerMasterInfo.getTravellersList(), travellerMasterInfo.isSeamen());
			pnrResponse = checkFare(priceRsp, travellerMasterInfo);

		} catch (AirFaultMessage airFaultMessage) {
			airFaultMessage.printStackTrace(); // To change body of catch
												// statement use File | Settings
												// | File Templates.
			ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(
					"error", ErrorMessage.ErrorType.ERROR, "TravelPort");
			pnrResponse.setErrorMessage(errorMessage);
		} catch (Exception e) {
			e.printStackTrace();
			ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(
					"error", ErrorMessage.ErrorType.ERROR, "TravelPort");

			pnrResponse.setErrorMessage(errorMessage);
		}

		return pnrResponse;
	}

	public TravellerMasterInfo allPNRDetails(IssuanceRequest issuanceRequest,
			String gdsPNR) {
		TravellerMasterInfo masterInfo = new TravellerMasterInfo();
		boolean isSeamen = issuanceRequest.isSeamen();
		masterInfo.setSeamen(isSeamen);

		try {
			UniversalRecordRetrieveRsp universalRecordRetrieveRsp = UniversalRecordClient.retrievePNR(gdsPNR);
			//logger.debug("Response======>>>>>\n"+ Json.toJson(universalRecordRetrieveRsp));
			// traveller deatials
			List<Traveller> travellerList = issuanceRequest.getTravellerList();
			
			
			for (Traveller traveller : travellerList) {
				Map<String, String> ticketMap = new HashMap<>();
				for (AirReservation airReservation : universalRecordRetrieveRsp.getUniversalRecord().getAirReservation()) {
					for (TypeBaseAirSegment airSegment : airReservation.getAirSegment()) {
						String key = airSegment.getOrigin()+airSegment.getDestination()+traveller.getContactId();
						ticketMap.put(key.toLowerCase(), airReservation.getDocumentInfo().getTicketInfo().get(0).getNumber());
					}
				}
				traveller.setTicketNumberMap(ticketMap);
			}

            masterInfo.setTravellersList(travellerList); // traveller is

            List<Journey> journeyList = TravelportHelper.getJourneyListFromPNR(universalRecordRetrieveRsp.getUniversalRecord(), redisTemplate);
			FlightItinerary flightItinerary = new FlightItinerary();
            PricingInformation pricingInformation = new PricingInformation();

            for(AirReservation airReservation : universalRecordRetrieveRsp.getUniversalRecord().getAirReservation()){
                for(AirPricingInfo airPricingInfo : airReservation.getAirPricingInfo()){
                    pricingInformation.setProvider("Travelport");
                    pricingInformation.setBasePrice(StringUtility.getDecimalFromString(airPricingInfo.getApproximateBasePrice()));
                    pricingInformation.setTax(StringUtility.getDecimalFromString(airPricingInfo.getTaxes()));
                    pricingInformation.setTotalPrice(StringUtility.getDecimalFromString(airPricingInfo.getTotalPrice()));
                    pricingInformation.setTotalPriceValue(StringUtility.getDecimalFromString(airPricingInfo.getTotalPrice()));


                }
                TravelportHelper.getPassengerTaxes(pricingInformation, airReservation.getAirPricingInfo());
            }

			if (isSeamen) {
				flightItinerary.setJourneyList(journeyList);
                flightItinerary.setSeamanPricingInformation(pricingInformation);
			} else {
				flightItinerary.setNonSeamenJourneyList(journeyList);
                flightItinerary.setPricingInformation(pricingInformation);
			}
			masterInfo.setItinerary(flightItinerary);
			logger.debug("\n<<<<<<<<===================masterInfo======>>>>>\n" + Json.toJson(masterInfo));

		} catch (Exception e) {
			e.printStackTrace();
		}

		return masterInfo;
	}
	
	public JsonNode getBookingDetails(String gdsPNR) {
		TravellerMasterInfo masterInfo = new TravellerMasterInfo();
		UniversalRecordImportRsp univRecRetrRsp = UniversalRecordImportClient.importPNR(gdsPNR);
		UniversalRecord univRec = univRecRetrRsp.getUniversalRecord();
		
		List<Traveller> travellersList = new ArrayList<>();
		List<BookingTraveler> bookingTravelerList = univRec.getBookingTraveler();
		for(BookingTraveler bookingTraveler : bookingTravelerList) {
			Traveller traveller = new Traveller();
			PersonalDetails personalDetails = new PersonalDetails();
			String[] names = bookingTraveler.getBookingTravelerName().getFirst().split("\\s");
			String fNmame = "";
			//personalDetails.setFirstName(names[0]);
			if(names.length > 1){
				//personalDetails.setSalutation(names[names.length-1]);
				for (String nam : names){
					if(nam.equalsIgnoreCase("Mr") || nam.equalsIgnoreCase("Mrs") || nam.equalsIgnoreCase("Ms")
							|| nam.equalsIgnoreCase("Miss") || nam.equalsIgnoreCase("Master") || nam.equalsIgnoreCase("Mstr")|| nam.equalsIgnoreCase("Capt")){
						personalDetails.setSalutation(WordUtils.capitalizeFully(nam));
						if(personalDetails.getSalutation().equalsIgnoreCase("Mstr"))
							personalDetails.setSalutation("Master");
					}else{
						fNmame = fNmame+" "+nam;
					}

				}

				personalDetails.setFirstName(fNmame.trim());
			}

			personalDetails.setLastName(bookingTraveler.getBookingTravelerName().getLast());
			traveller.setPersonalDetails(personalDetails);
			travellersList.add(traveller);
		}
		boolean isSeamen = false;
		if("SEA".equalsIgnoreCase(bookingTravelerList.get(0).getTravelerType()))
			isSeamen = true;
		masterInfo.setSeamen(isSeamen);
		masterInfo.setTravellersList(travellersList);
		
		TypeCabinClass cabinClass = univRec.getAirReservation().get(0).getAirSegment().get(0).getCabinClass();
		masterInfo.setCabinClass(com.compassites.model.CabinClass.fromValue(cabinClass.name()));
		
		List<Journey> journeyList = TravelportHelper.getJourneyListFromPNR(univRecRetrRsp.getUniversalRecord(), redisTemplate);
		Map<String,String> airSegmentRefMap = new HashMap<>();

		for (AirReservation airReservation : univRec.getAirReservation()) {
			for (TypeBaseAirSegment airSegment : airReservation.getAirSegment()) {
				airSegmentRefMap.put(airSegment.getKey(), (airSegment.getOrigin() + airSegment.getDestination()).toLowerCase());
			}
		}


		FlightItinerary flightItinerary = new FlightItinerary();
        PricingInformation pricingInfo = new PricingInformation();
        List<AirPricingInfo> airPricingInfoList = univRec.getAirReservation().get(0).getAirPricingInfo();

        BigDecimal totalFare = new BigDecimal(0);
        BigDecimal totalBaseFare = new BigDecimal(0);
        BigDecimal totalTax = new BigDecimal(0);

		BigDecimal adtBaseFare = new BigDecimal(0);
		BigDecimal chdBaseFare = new BigDecimal(0);
		BigDecimal infBaseFare = new BigDecimal(0);
		BigDecimal adtTotalFare = new BigDecimal(0);
		BigDecimal chdTotalFare = new BigDecimal(0);
		BigDecimal infTotalFare = new BigDecimal(0);
		boolean segmentWisePricing = false;

		List<SegmentPricing> segmentPricingList = new ArrayList<>();
		List<PassengerTax> passengerTaxList = new ArrayList<>();

        for(AirPricingInfo airPricingInfo : airPricingInfoList) {
        	BigDecimal total = StringUtility.getDecimalFromString(airPricingInfo.getApproximateTotalPrice());
        	BigDecimal base = StringUtility.getDecimalFromString(airPricingInfo.getApproximateBasePrice());
        	BigDecimal tax = StringUtility.getDecimalFromString(airPricingInfo.getTaxes());
        	pricingInfo.setGdsCurrency(StringUtility.getCurrencyFromString(airPricingInfo.getTotalPrice()));
        	List<PassengerType> passegerTypes = airPricingInfo.getPassengerType();
        	String paxType = passegerTypes.get(0).getCode();
        	
        	int paxCount = passegerTypes.size();
        	totalFare = totalFare.add(total.multiply(new BigDecimal(paxCount)));
        	totalBaseFare = totalBaseFare.add(base.multiply(new BigDecimal(paxCount)));
        	totalTax = totalTax.add(tax.multiply(new BigDecimal(paxCount)));
        	
        	if("CHD".equalsIgnoreCase(paxType) || "CNN".equalsIgnoreCase(paxType)) {
//				pricingInfo.setChdBasePrice(base);
				chdBaseFare = chdBaseFare.add(base);
				chdTotalFare = chdTotalFare.add(total);
			} else if("INF".equalsIgnoreCase(paxType)) {
//				pricingInfo.setInfBasePrice(base);
				infBaseFare = infBaseFare.add(base);
				infTotalFare = infTotalFare.add(total);
			} else {
//				pricingInfo.setAdtBasePrice(base);
				adtBaseFare = adtBaseFare.add(base);
				adtTotalFare = adtTotalFare.add(total);
			}

			if(airSegmentRefMap.size() != airPricingInfo.getBookingInfo().size()){
				segmentWisePricing = true;
			}

			List<String> segmentKeys = new ArrayList<>();
			SegmentPricing segmentPricing = new SegmentPricing();
			if(segmentWisePricing){
				for(BookingInfo bookingInfo : airPricingInfo.getBookingInfo()){
					String key = airSegmentRefMap.get(bookingInfo.getSegmentRef());
					segmentKeys.add(key);
				}
				PassengerTax passengerTax = TravelportHelper.getTaxDetailsList(airPricingInfo.getTaxInfo(), paxType, paxCount);
				segmentPricing.setSegmentKeysList(segmentKeys);
				segmentPricing.setTotalPrice(total.multiply(new BigDecimal(paxCount)));
				segmentPricing.setBasePrice(base.multiply(new BigDecimal(paxCount)));
				segmentPricing.setPassengerType(paxType);
				segmentPricing.setPassengerTax(passengerTax);
				segmentPricing.setPassengerCount(new Long(paxCount));
				segmentPricingList.add(segmentPricing);
			}

        }

        pricingInfo.setTax(totalTax);
		pricingInfo.setBasePrice(totalBaseFare);
		pricingInfo.setTotalPrice(totalFare);
        pricingInfo.setTotalPriceValue(totalFare);
		pricingInfo.setProvider("Travelport");

		pricingInfo.setSegmentWisePricing(segmentWisePricing);
		pricingInfo.setAdtBasePrice(adtBaseFare);
		pricingInfo.setAdtTotalPrice(adtTotalFare);
		pricingInfo.setChdBasePrice(chdBaseFare);
		pricingInfo.setChdTotalPrice(chdTotalFare);
		pricingInfo.setInfBasePrice(infBaseFare);
		pricingInfo.setInfTotalPrice(infTotalFare);
		pricingInfo.setSegmentPricingList(segmentPricingList);

		TravelportHelper.getPassengerTaxes(pricingInfo, airPricingInfoList);
		if (isSeamen) {
			flightItinerary.setJourneyList(journeyList);
            flightItinerary.setSeamanPricingInformation(pricingInfo);
		} else {
			flightItinerary.setNonSeamenJourneyList(journeyList);
            flightItinerary.setPricingInformation(pricingInfo);
		}
		masterInfo.setItinerary(flightItinerary);

		PNRResponse pnrResponse = new PNRResponse();
		pnrResponse.setPricingInfo(pricingInfo);
		retrievePNR(univRecRetrRsp.getUniversalRecord(), pnrResponse, masterInfo);
		
		Map<String, Object> json = new HashMap<>();
		json.put("travellerMasterInfo", masterInfo);
		json.put("pnrResponse", pnrResponse);
		
		return Json.toJson(json);
    }

    public LowFareResponse getLowestFare(String pnr, String provider, boolean isSeamen) {
        TerminalRequestClient terminalRequestClient = new TerminalRequestClient();
        LowFareResponse lowFareRS = new LowFareResponse();
        String token = terminalRequestClient.createTerminalSession();
        List<String> lowestFareTextList= terminalRequestClient.getLowestFare(token, isSeamen, pnr);
        //System.out.println("==========>> Lowest Fare "+ lowestFareTextList);
        String lowestPriceText = lowestFareTextList.get(5);
        BigDecimal lowestFarePrice = StringUtility.getLowestFareFromString(lowestPriceText);
        lowFareRS.setAmount(lowestFarePrice);
        lowFareRS.setGdsPnr(pnr);
        return lowFareRS;
    }

    public String getCancellationFee(IssuanceRequest issuanceRequest){
        PNRResponse pnrResponse = new PNRResponse();
        TravellerMasterInfo travellerMasterInfo = new TravellerMasterInfo();
        travellerMasterInfo.setTravellersList(issuanceRequest.getTravellerList());
        travellerMasterInfo.setSeamen(issuanceRequest.isSeamen());
        travellerMasterInfo.setItinerary(issuanceRequest.getFlightItinerary());


        StringBuilder cancelFeeText = new StringBuilder();
        AirPriceRsp priceRsp = null;
        try {
            AirItinerary airItinerary = AirRequestClient.buildAirItinerary(travellerMasterInfo.getItinerary(), travellerMasterInfo.isSeamen());
            TypeCabinClass typeCabinClass = TypeCabinClass.valueOf(issuanceRequest.getCabinClass().upperValue());
            priceRsp = AirRequestClient.priceItinerary(airItinerary, "INR", typeCabinClass, travellerMasterInfo.getTravellersList(), travellerMasterInfo.isSeamen());
            for(AirPriceResult airPriceResult : priceRsp.getAirPriceResult()){
                FareRuleKey fareRuleKey = airPriceResult.getAirPricingSolution().get(0).getAirPricingInfo().get(0).getFareInfo().get(0).getFareRuleKey();
                FareRulesClient fareRulesClient =  new FareRulesClient();
                AirFareRulesRsp airFareRulesRsp = fareRulesClient.getFareRules(fareRuleKey.getFareInfoRef(),fareRuleKey.getValue());

                for(FareRule fareRule :airFareRulesRsp.getFareRule()){
                    for(FareRuleLong fareRuleLong :fareRule.getFareRuleLong()){
                        cancelFeeText.append(fareRuleLong.getValue());
                    }
                }
            }
        } catch (AirFaultMessage airFaultMessage) {
            airFaultMessage.printStackTrace();
            logger.error("Error in getting cancel Fee text for travelport "+ airFaultMessage);
        }

        return cancelFeeText.toString();
    }
}