package com.compassites.GDSWrapper.travelomatrix;

import com.compassites.model.BaggageDetails;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.compassites.model.travelomatrix.HoldTicketRequest;
import com.compassites.model.travelomatrix.Passenger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import configs.WsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import play.libs.ws.WSRequestHolder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HoldTicketTMX {

    static Logger logger = LoggerFactory.getLogger("gds");

    static Logger travelomatrixLogger = LoggerFactory.getLogger("travelomatrix");

    public WsConfig wsconf = new WsConfig();

    @Autowired
    public WSRequestHolder wsrholder;

    public JsonNode HoldBooking(TravellerMasterInfo travellerMasterInfo,Long seqno){
        JsonNode jsonRequest = getJsonforHoldBookingRequest(travellerMasterInfo,seqno);
        JsonNode response = null;
        try {
            wsrholder= wsconf.getRequestHolder("/HoldTicket");
            travelomatrixLogger.debug("TraveloMatrixFlightSearch : Request to TM : "+ jsonRequest.toString());
            travelomatrixLogger.debug("TraveloMatrixFlightSearch : Call to Travelomatrix Backend : "+ System.currentTimeMillis());
            response = wsrholder.post(jsonRequest).get(40000).asJson();
            travelomatrixLogger.debug("TraveloMatrixFlightSearch : Recieved Response from Travelomatrix Backend : "+ System.currentTimeMillis());
            travelomatrixLogger.debug("TraveloMatrix Response:"+response.toString());
        }catch(Exception e){
            travelomatrixLogger.error(e.getMessage());
            e.printStackTrace();
        }
        return response;

    }

    public JsonNode getJsonforHoldBookingRequest(TravellerMasterInfo travellerMasterInfo,Long seqno){
        JsonNode jsonNode = null;
        try{
            HoldTicketRequest holdTicketRequest = new HoldTicketRequest();

            holdTicketRequest.setSequenceNumber(seqno);
            if(seqno == 0) {
                holdTicketRequest.setAppReference(travellerMasterInfo.getAppReference());
                holdTicketRequest.setResultToken(travellerMasterInfo.getItinerary().getResultToken());
            }else{
                holdTicketRequest.setAppReference(travellerMasterInfo.getAppReference());
                holdTicketRequest.setResultToken(travellerMasterInfo.getItinerary().getReturnResultToken());
            }

            List<Passenger> passengerList = new ArrayList<>();
            //ADT
            for( Traveller traveller : travellerMasterInfo.getTravellersList()) {
                Passenger passenger = new Passenger();
                passenger.setEmail(traveller.getPersonalDetails().getEmail());
                passenger.setCountryCode(traveller.getPersonalDetails().getCountryCode());
                if(traveller.getPersonalDetails().getGender().equalsIgnoreCase("female"))
                    passenger.setGender(new Long(2));
                else
                    passenger.setGender(new Long(1));

                String dateStr = null;
                String formattedDate = null;
                if(traveller.getPassportDetails().getDateOfBirth() != null) {
                    dateStr = traveller.getPassportDetails().getDateOfBirth().toString();
                    SimpleDateFormat inputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
                    SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
                    Date date = null;
                    try {
                        date = inputFormat.parse(dateStr);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                    formattedDate = outputFormat.format(date);
                    passenger.setDateOfBirth(formattedDate);
                }
                passenger.setFirstName(traveller.getPersonalDetails().getFirstName());
                passenger.setLastName(traveller.getPersonalDetails().getLastName());
                passenger.setTitle(traveller.getPersonalDetails().getSalutation());
                passenger.setAddressLine1(traveller.getPersonalDetails().getAddressLine());
                passenger.setCity(traveller.getPassportDetails().getNationality().getNationality());
                passenger.setCountryCode(traveller.getPassportDetails().getNationality().getThreeLetterCode());
                passenger.setContactNo(traveller.getPersonalDetails().getMobileNumber());
                passenger.setCountryName(traveller.getPassportDetails().getNationality().getNationality());
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

                List<String> baggage = new ArrayList<>();
                if(traveller.getBaggageDetails() != null) {
                    baggage.add(traveller.getBaggageDetails().getBaggageId());
                    passenger.setBaggageId(baggage);
                }
                passengerList.add(passenger);
            }

            holdTicketRequest.setPassengers(passengerList);
            ObjectMapper mapper = new ObjectMapper();
            jsonNode= mapper.valueToTree(holdTicketRequest);

        }catch(Exception e){
            travelomatrixLogger.error("Exception Occured:"+ e.getMessage());
            e.printStackTrace();
        }

        return jsonNode;
    }

    private Long getPaxType(String dob){
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if(dob != null) {
            LocalDate birthdate = LocalDate.parse(dob, dateFormatter);
            LocalDate currentDate = LocalDate.now();
            Period age = Period.between(birthdate, currentDate);
            //ADT
            if (age.getYears() > 12) {
                return new Long(1);
            } else if (age.getYears() > 2 && age.getYears() <= 12) {
                //CHD
                return new Long(2);
            }
            return new Long(3);
        }else{ //ADT
            return new Long(1);
        }
    }

}
