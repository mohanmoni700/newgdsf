package services.ancillary;

import com.compassites.GDSWrapper.travelomatrix.BookingFlights;
import com.compassites.GDSWrapper.travelomatrix.ExtraServicesTMX;
import com.compassites.constants.StaticConstatnts;
import com.compassites.exceptions.RetryException;
import com.compassites.model.*;
import com.compassites.model.travelomatrix.ResponseModels.Baggage;
import com.compassites.model.travelomatrix.ResponseModels.ExtraServicesReply;
import com.compassites.model.travelomatrix.ResponseModels.Meal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import services.RetryOnFailure;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TravelomatixExtraServiceImpl implements TravelomatixExtraService {
    static Logger logger = LoggerFactory.getLogger("gds");

    static Logger travelomatrixLogger = LoggerFactory.getLogger("travelomatrix");

    public ExtraServicesTMX extraServicesTMX = new ExtraServicesTMX();
    public BookingFlights bookingFlights = new BookingFlights();
    @Override
    @RetryOnFailure(attempts = 2, delay =30000, exception = RetryException.class)
    public AncillaryServicesResponse getExtraServicesfromTmx(String resultToken,String reResulttoken,String journeyType,Boolean isLCC) {
        travelomatrixLogger.debug("Request  for ExtraServices: ResultToken:"+ resultToken );
        List<BaggageDetails> baggageList = new ArrayList<>();
        if(!isLCC){
            JsonNode jsonResponse = bookingFlights.getUpdatedFares(resultToken);
        }
        AncillaryServicesResponse ancillaryServicesResponse = new AncillaryServicesResponse();
        travelomatrixLogger.debug("Request for ExtraServices: ResultToken:"+ resultToken);
        JsonNode extraServicesResponse = extraServicesTMX.getExtraServices(resultToken);
        JsonNode returnextraServicesResponse = null;
        if(reResulttoken != null && !reResulttoken.equalsIgnoreCase(""))
            returnextraServicesResponse = extraServicesTMX.getExtraServices(reResulttoken);
        try {
            ExtraServicesReply extraServicesReply = new ObjectMapper().treeToValue(extraServicesResponse, ExtraServicesReply.class);
            ExtraServicesReply extraServicesReply1 = null;
            if(returnextraServicesResponse != null)
                extraServicesReply1  = new ObjectMapper().treeToValue(returnextraServicesResponse,ExtraServicesReply.class);
            HashMap<String,List<BaggageDetails>> baggageMap = new HashMap<>();
            if( (extraServicesReply != null && extraServicesReply.getStatus() != 0) ||
                    (extraServicesReply1 != null && extraServicesReply1.getStatus() != 0) ) {
                logger.debug("Get Baggage Details");
                Boolean status = Boolean.FALSE;

                if(JourneyType.ONE_WAY.toString().equalsIgnoreCase(journeyType)) {
                    //oneWay jouney
                    baggageList = getBaggageDetails(extraServicesReply);
                    if (baggageList != null && baggageList.size() > 0) {
                        ancillaryServicesResponse.setBaggageList(baggageList);
                        status = Boolean.TRUE;
                    }
                }else{
                    //Segment based mapping for round_trip and multicity
                   baggageMap = getBaggageDetailsMap(extraServicesReply,Boolean.FALSE);
                    if(extraServicesReply1 != null && extraServicesReply1.getStatus() != 0) {
                        HashMap<String,List<BaggageDetails>> returnBaggageMap =   getBaggageDetailsMap(extraServicesReply1,Boolean.TRUE);
                        for(Map.Entry<String,List<BaggageDetails>> entry: returnBaggageMap.entrySet()){
                            baggageMap.put(entry.getKey(),entry.getValue());
                        }

                    }
                    if(baggageMap != null){
                        ancillaryServicesResponse.setBaggageMap(baggageMap);
                        status = Boolean.TRUE;
                    }
                }

                HashMap<String,List<MealDetails>> mealsMap = getMealDetails(extraServicesReply,Boolean.FALSE);
                if(extraServicesReply1 != null && extraServicesReply1.getStatus() != 0) {
                    HashMap<String,List<MealDetails>> returnMealsMap = getMealDetails(extraServicesReply1,Boolean.TRUE);
                    for(Map.Entry<String,List<MealDetails>> entry: returnMealsMap.entrySet()){
                        mealsMap.put(entry.getKey(),entry.getValue());
                    }
                }

                if(mealsMap != null){
                    ancillaryServicesResponse.setMealDetailsMap(mealsMap);
                    status = Boolean.TRUE;
                }

                if(!status){
                    ancillaryServicesResponse.setSuccess(Boolean.FALSE);
                    ancillaryServicesResponse.setProvider(StaticConstatnts.TRAVELOMATRIX);
                    ErrorMessage errorMessage = new ErrorMessage();
                    errorMessage.setProvider(StaticConstatnts.TRAVELOMATRIX);
                    errorMessage.setMessage("An Unexpected Error occured");
                }else{
                    ancillaryServicesResponse.setSuccess(Boolean.TRUE);
                    ancillaryServicesResponse.setProvider(StaticConstatnts.TRAVELOMATRIX);
                }
            }else{
                ancillaryServicesResponse.setSuccess(Boolean.FALSE);
                ancillaryServicesResponse.setProvider(StaticConstatnts.TRAVELOMATRIX);
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setProvider(StaticConstatnts.TRAVELOMATRIX);
                errorMessage.setMessage(extraServicesReply.getMessage());
            }
        } catch (JsonProcessingException e) {
            logger.debug("An exception has occured while parsing the Json :" + e.getMessage());
            e.printStackTrace();
        }

        return ancillaryServicesResponse;
    }

    public List<BaggageDetails> getBaggageDetails(ExtraServicesReply extraServicesReply){
        List<BaggageDetails> baggageList = null;
        try {
            baggageList = new ArrayList<>();
            if(extraServicesReply.getExtraServices().getExtraServiceDetails().getBaggage() != null){
                List<Baggage> baggages = extraServicesReply.getExtraServices().getExtraServiceDetails().getBaggage().get(0);
                if(baggages != null) {
                    for (Baggage baggage : baggages) {
                        BaggageDetails baggageDetails = new BaggageDetails();
                        baggageDetails.setBaggageId(baggage.getBaggageId());
                        baggageDetails.setCode(baggage.getCode());
                        baggageDetails.setOrigin(baggage.getOrigin());
                        baggageDetails.setDestination(baggage.getDestination());
                        baggageDetails.setWeight(baggage.getWeight());
                        baggageDetails.setPrice(baggage.getPrice());
                        baggageDetails.setReturnDetails(Boolean.FALSE);
                        baggageList.add(baggageDetails);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("An exception occured while traversing the BaggageDetails"+ e.getMessage());
            e.printStackTrace();
        }
        return baggageList;
    }

    public HashMap<String,List<MealDetails>>  getMealDetails(ExtraServicesReply extraServicesReply, Boolean returnDetails){
        HashMap<String,List<MealDetails>> mealsMap = new HashMap<>();
        List<MealDetails> mealDetailsList = new ArrayList<>();
        try {

            if(extraServicesReply.getStatus() != 0){
                List<List<Meal>> mealList = extraServicesReply.getExtraServices().getExtraServiceDetails().getMeals();
                if(mealList == null || mealList.size() == 0){
                    mealList=  extraServicesReply.getExtraServices().getExtraServiceDetails().getMealPreference();
                }
                for(List<Meal> mealList1 :mealList ){
                    for(Meal meal: mealList1 ){
                        String segment = meal.getOrigin()+"-"+meal.getDestination();
                        MealDetails mealDetails = new MealDetails();
                        mealDetails.setMealId(meal.getMealId());
                        mealDetails.setSegment(segment);
                        mealDetails.setMealType(meal.getMealTypes());
                        mealDetails.setMealCode(meal.getCode());
                        mealDetails.setOrigin(meal.getOrigin());
                        mealDetails.setDestination(meal.getDestination());
                        mealDetails.setReturnDetails(returnDetails);
                        if(meal.getPrice() != null)
                            mealDetails.setMealPrice(new BigDecimal(meal.getPrice()));
                        else
                            mealDetails.setMealPrice(new BigDecimal(0));
                        mealDetails.setMealDesc(meal.getDescription());
                        mealDetailsList.add(mealDetails);
                    }
                }
            }
            if(mealDetailsList != null){
                mealsMap = mealDetailsList.stream()
                        .collect(Collectors.groupingBy(MealDetails::getSegment, HashMap::new, Collectors.toList()));

            }
        } catch (Exception e) {
            logger.debug("An exception occured while traversing the BaggageDetails"+ e.getMessage());
            e.printStackTrace();
        }
        return mealsMap;
    }


    public HashMap<String,List<BaggageDetails>>  getBaggageDetailsMap(ExtraServicesReply extraServicesReply, Boolean returnDetails){
        HashMap<String,List<BaggageDetails>> baggageMap = new HashMap<>();
        List<BaggageDetails> baggageDetailsList = new ArrayList<>();
        try {

            if(extraServicesReply.getStatus() != 0 && extraServicesReply.getExtraServices().getExtraServiceDetails().getBaggage()!= null){
                List<List<Baggage>> baggageList = extraServicesReply.getExtraServices().getExtraServiceDetails().getBaggage();
                for(List<Baggage> baggages :baggageList ){
                    for(Baggage baggage: baggages ){
                        String segment = baggage.getOrigin()+"-"+baggage.getDestination();
                        BaggageDetails baggageDetails = new BaggageDetails();
                        baggageDetails.setSegment(segment);
                        baggageDetails.setWeight(baggage.getWeight());
                        baggageDetails.setBaggageId(baggage.getBaggageId());
                        baggageDetails.setPrice(baggage.getPrice());
                        baggageDetails.setOrigin(baggage.getOrigin());
                        baggageDetails.setDestination(baggage.getDestination());
                        baggageDetails.setCode(baggage.getCode());
                        baggageDetails.setReturnDetails(returnDetails);
                        baggageDetailsList.add(baggageDetails);
                    }
                }
            }
            if(baggageDetailsList != null && baggageDetailsList.size() > 0){
                baggageMap = baggageDetailsList.stream()
                        .collect(Collectors.groupingBy(BaggageDetails::getSegment, HashMap::new, Collectors.toList()));

            }
        } catch (Exception e) {
            logger.debug("An exception occured while traversing the BaggageDetails"+ e.getMessage());
            e.printStackTrace();
        }
        return baggageMap;
    }

}
