package services;

import com.compassites.model.FlightItinerary;
import com.compassites.model.SearchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import okhttp3.*;
import play.Play;
import play.libs.Json;

@Service
public class IndigoFlightInfoServiceImpl implements IndigoFlightInfoService{

    static Logger logger = LoggerFactory.getLogger("gds");

    private static final OkHttpClient client = new OkHttpClient();
    static Logger indigoLogger = LoggerFactory.getLogger("indigo");
    //private static final String endPoint = "http://localhost:8086/indigo/baggage";
    private static final String endPoint = Play.application().configuration().getString("indigo.service.endPoint");
    @Override
    public FlightItinerary getFlightInfo(FlightItinerary flightItinerary) {
        // Implement the logic to fetch flight information for Indigo flights
        // This is a placeholder implementation
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(flightItinerary);
            RequestBody requestBody = RequestBody.create(jsonString, MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder().url(endPoint+"baggage").post(requestBody).build();
            try (Response response = client.newCall(request).execute()) {
                //System.out.println("Indigo "+response.body().string());
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    FlightItinerary flightItinerary1 = objectMapper.readValue(responseBody, FlightItinerary.class);
                    indigoLogger.debug("Indigo Flight Info Response: " + responseBody);
                    return flightItinerary1;
                } else {
                    logger.error("Failed to fetch data from Indigo API: " + response.message() +
                        " for flight itinerary: " + Json.toJson(flightItinerary));
                    throw new Exception("Failed to fetch data from Indigo API: " + response.message());
                }
            }
        } catch (Exception e) {
            logger.error("Error during Indigo flight info retrieval: " + e.getMessage(), e);
            e.printStackTrace();
            return null; // or handle the error as needed
        }
    }

    @Override
    public String getCancellationFee(FlightItinerary flightItinerary) {
        try {
            logger.info("Fetching cancellation fee for Indigo flight itinerary: " + Json.toJson(flightItinerary));
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(flightItinerary);
            RequestBody requestBody = RequestBody.create(jsonString, MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder().url(endPoint+"farerule").post(requestBody).build();
            try (Response response = client.newCall(request).execute()) {
                //System.out.println("Indigo "+response.body().string());
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    indigoLogger.debug("Indigo Flight Info Response: " + responseBody);
                    return responseBody;
                } else {
                    logger.error("Failed to fetch data from Indigo API: " + response.message() +
                            " for flight itinerary: " + Json.toJson(flightItinerary));
                    throw new Exception("Failed to fetch data from Indigo API: " + response.message());
                }
            }
        } catch (Exception e) {
            logger.error("Error during Indigo flight info retrieval: " + e.getMessage(), e);
            e.printStackTrace();
            return null; // or handle the error as needed
        }
    }
}
