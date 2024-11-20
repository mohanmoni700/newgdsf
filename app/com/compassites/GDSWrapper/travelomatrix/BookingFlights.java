package com.compassites.GDSWrapper.travelomatrix;

import com.compassites.model.BaggageDetails;
import com.compassites.model.IssuanceRequest;
import com.compassites.model.MealDetails;
import com.compassites.model.MealDetailsMap;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.compassites.model.travelomatrix.CommitBookingRequest;
import com.compassites.model.travelomatrix.FareRuleRequest;
import com.compassites.model.travelomatrix.Passenger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import configs.WsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import play.Play;
import play.libs.ws.WSRequestHolder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BookingFlights {
    static Logger logger = LoggerFactory.getLogger("gds");

    static Logger travelomatrixLogger = LoggerFactory.getLogger("travelomatrix");

    public WsConfig wsconf = new WsConfig();

    FareRuleRequest fareRuleRequest = new FareRuleRequest();

    @Autowired
    public WSRequestHolder wsrholder;

    public JsonNode getUpdatedFares(String resultToken) {
        JsonNode jsonRequest = getJsonFromResultToken(resultToken);
        JsonNode response = null;
        try {
            int timeout = Play.application().configuration().getInt("travelomatrix.timeout");
            wsrholder= wsconf.getRequestHolder("/UpdateFareQuote");
            travelomatrixLogger.debug("TraveloMatrixFlightSearch : Request to TM : "+ jsonRequest.toString());
            travelomatrixLogger.debug("TraveloMatrixFlightSearch : Call to Travelomatrix Backend : "+ System.currentTimeMillis());
            response = wsrholder.post(jsonRequest).get(timeout).asJson();
            travelomatrixLogger.debug("TraveloMatrixFlightSearch : Recieved Response from Travelomatrix Backend : "+ System.currentTimeMillis());
            travelomatrixLogger.debug("TraveloMatrix Response:"+response.toString());
        }catch(Exception e){
            travelomatrixLogger.error(e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    public JsonNode getJsonFromResultToken(String resultToken){
        JsonNode node = null;
        try{
            fareRuleRequest.setResultToken(resultToken);
            ObjectMapper mapper = new ObjectMapper();
            node= mapper.valueToTree(fareRuleRequest);

        }catch(Exception e){
            travelomatrixLogger.error("Exception Occured:"+ e.getMessage());
            e.printStackTrace();
        }
        return node;
    }

    public JsonNode commitBooking(IssuanceRequest issuanceRequest,Boolean roundtrip){
        JsonNode jsonRequest = getJsonFromCommitBookingRequest(issuanceRequest,roundtrip);
        JsonNode response = null;
        try {
            int timeout = Play.application().configuration().getInt("travelomatrix.timeout");
            wsrholder= wsconf.getRequestHolder("/CommitBooking");
            travelomatrixLogger.debug("TraveloMatrixFlightSearch : Request to TM : "+ jsonRequest.toString());
            travelomatrixLogger.debug("TraveloMatrixFlightSearch : Call to Travelomatrix Backend : "+ System.currentTimeMillis());
            response = wsrholder.post(jsonRequest).get(timeout).asJson();
            travelomatrixLogger.debug("TraveloMatrixFlightSearch : Recieved Response from Travelomatrix Backend : "+ System.currentTimeMillis());
            travelomatrixLogger.debug("TraveloMatrix Response:"+response.toString());
        }catch(Exception e){
            travelomatrixLogger.error(e.getMessage());
            e.printStackTrace();
        }
        return response;

    }

    public JsonNode getJsonFromCommitBookingRequest(IssuanceRequest issuanceRequest,Boolean returnJourney){
        JsonNode node = null;
        CommitBookingRequest commitBookingRequest = new CommitBookingRequest();
       List<BaggageDetails> baggageDetailsList = issuanceRequest.getBaggageDetails();
        if(returnJourney){
            commitBookingRequest.setAppReference(issuanceRequest.getReAppRef());
            commitBookingRequest.setSequenceNumber("1");
            commitBookingRequest.setResultToken(issuanceRequest.getReResultToken());
        }else {
            commitBookingRequest.setAppReference(issuanceRequest.getAppRef());
            commitBookingRequest.setSequenceNumber("0");
            commitBookingRequest.setResultToken(issuanceRequest.getResultToken());
        }
        List<Passenger>  passengerList = new ArrayList<>();

        //ADT
        for( Traveller traveller : issuanceRequest.getTravellerList()) {
            Passenger passenger = new Passenger();
            passenger.setEmail(traveller.getPersonalDetails().getEmail());
            passenger.setCountryCode(traveller.getPersonalDetails().getCountryCode());
            if(traveller.getPersonalDetails().getGender().equalsIgnoreCase("female"))
                passenger.setGender(new Long(2));
            else
                passenger.setGender(new Long(1));

            String dateStr = traveller.getPassportDetails().getDateOfBirth().toString();
            SimpleDateFormat inputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = null;
                try {
                    date = inputFormat.parse(dateStr);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                String formattedDate = outputFormat.format(date);
            passenger.setDateOfBirth(formattedDate);
            passenger.setFirstName(traveller.getPersonalDetails().getFirstName());
            passenger.setLastName(traveller.getPersonalDetails().getLastName());
            passenger.setTitle(traveller.getPersonalDetails().getSalutation());
            passenger.setAddressLine1(traveller.getPersonalDetails().getAddressLine());
            passenger.setCity(traveller.getPassportDetails().getNationality().getNationality());
            passenger.setCountryCode(traveller.getPassportDetails().getNationality().getThreeLetterCode());
            passenger.setCountryName(traveller.getPassportDetails().getNationality().getNationality());
            passenger.setContactNo(traveller.getPersonalDetails().getMobileNumber());
            if(traveller.getPassportDetails().getPassportNumber() != null)
            passenger.setPassportNumber(traveller.getPassportDetails().getPassportNumber());
            if(traveller.getPersonalDetails().getPincode() != null)
            passenger.setPinCode(traveller.getPersonalDetails().getPincode());
            else
                passenger.setPinCode("567812");
            Long paxType = getPaxType(formattedDate);
            passenger.setPaxType(paxType);
            if(paxType == 1)
                passenger.setIsLeadPax("1");
            else
                passenger.setIsLeadPax("");

            List<BaggageDetails> baggageDetailsList1 =  traveller.getBaggageDetails();
            List<MealDetails> mealDetailsList = traveller.getMealDetails();

            if(baggageDetailsList1 != null){
                List<String> baggageIds = new ArrayList<>();
                for (BaggageDetails baggageDetails : baggageDetailsList1) {
                    if (returnJourney) {
                        if (baggageDetails.getReturnDetails().equals(Boolean.TRUE)) {
                            baggageIds.add(baggageDetails.getBaggageId());
                        }
                    } else {
                        if (baggageDetails.getReturnDetails() == null || baggageDetails.getReturnDetails().equals(Boolean.FALSE)) {
                            baggageIds.add(baggageDetails.getBaggageId());
                        }
                    }
                }
                passenger.setBaggageId(baggageIds);
            }

            if(mealDetailsList != null ) {
                List<String> mealIds = new ArrayList<>();
                for (MealDetails mealDetails : mealDetailsList) {
                   if(returnJourney){
                       if(mealDetails.getReturnDetails().equals(Boolean.TRUE)){
                           mealIds.add(mealDetails.getMealId());
                       }
                   }else{
                       if(mealDetails.getReturnDetails() == null || mealDetails.getReturnDetails().equals(Boolean.FALSE)) {
                           mealIds.add(mealDetails.getMealId());
                       }
                   }
                }

                passenger.setMealId(mealIds);
            }
            passengerList.add(passenger);
        }

        commitBookingRequest.setPassengers(passengerList);

      try{
          ObjectMapper mapper = new ObjectMapper();
          node= mapper.valueToTree(commitBookingRequest);
          travelomatrixLogger.debug("commitBooking request: "+ node);
      }catch(Exception e){
          travelomatrixLogger.error("Exception Occured:"+ e.getMessage());
          e.printStackTrace();
      }
        return node;
      }

      private Long getPaxType(String dob){
          DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
          LocalDate birthdate = LocalDate.parse(dob, dateFormatter);
          LocalDate currentDate = LocalDate.now();
          Period age = Period.between(birthdate, currentDate);
          //ADT
          if(age.getYears() >12){
              return new Long(1);
          }else if(age.getYears() > 2 && age.getYears() <= 12){
              //CHD
              return new Long(2);
          }

          return new Long(3);
      }

}
