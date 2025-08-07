package services.indigo;

import com.compassites.model.*;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import play.Play;
import play.libs.Json;

@Service
public class IndigoFlightServiceImpl implements IndigoFlightService{

    static Logger logger = LoggerFactory.getLogger("gds");

    static Logger indigoLogger = LoggerFactory.getLogger("indigo");

    private static final OkHttpClient client = new OkHttpClient();
    private static final String endPoint = Play.application().configuration().getString("indigo.service.endPoint");

    @Override
    public PNRResponse checkFareChangeAndAvailability(TravellerMasterInfo travellerMasterInfo) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(travellerMasterInfo);
            RequestBody requestBody = RequestBody.create(jsonString, MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder().url(endPoint+"checkFare").post(requestBody).build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    indigoLogger.debug("Indigo Fare Change Response: " + responseBody);
                    return objectMapper.readValue(responseBody, PNRResponse.class);
                } else {
                    logger.error("Failed to fetch data from Indigo API: " + response.message() +
                        " for traveller info: " + Json.toJson(travellerMasterInfo));
                    throw new Exception("Failed to fetch data from Indigo API: " + response.message());
                }
            }
        } catch (Exception e) {
            logger.error("Error during Indigo fare change check: " + e.getMessage() +
                " for traveller info: " + Json.toJson(travellerMasterInfo), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public PNRResponse generatePNR(TravellerMasterInfo travellerMasterInfo) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(travellerMasterInfo);
            RequestBody requestBody = RequestBody.create(jsonString, MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder().url(endPoint+"generatePNR").post(requestBody).build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    indigoLogger.debug("Indigo PNR Generation Response: " + responseBody);
                    return objectMapper.readValue(responseBody, PNRResponse.class);
                } else {
                    logger.error("Failed to fetch data from Indigo API: " + response.message() +
                        " for traveller info: " + Json.toJson(travellerMasterInfo));
                    throw new Exception("Failed to fetch data from Indigo API: " + response.message());
                }
            }
        } catch (Exception e) {
            logger.error("Error during Indigo fare change check: " + e.getMessage() +
                    " for traveller info: " + Json.toJson(travellerMasterInfo), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public IssuanceResponse priceBookedPNR(IssuanceRequest issuanceRequest) {
        try {
            logger.info("Indigo price booked PNR request: " + Json.toJson(issuanceRequest));
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(issuanceRequest);
            RequestBody requestBody = RequestBody.create(jsonString, MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder().url(endPoint+"priceBookedPNR").post(requestBody).build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    indigoLogger.debug("Indigo Price Booked PNR Response: " + responseBody);
                    return objectMapper.readValue(responseBody, IssuanceResponse.class);
                } else {
                    logger.error("Failed to fetch data from Indigo API: " + response.message() +
                            " for traveller info: " + Json.toJson(issuanceRequest));
                    throw new Exception("Failed to fetch data from Indigo API: " + response.message());
                }
            }
        } catch (Exception e) {
            logger.error("Error during Indigo price booked PNR: " + e.getMessage() +
                    " for issuance request: " + Json.toJson(issuanceRequest), e);
        }
        return null;
    }

    @Override
    public IssuanceResponse issueTicket(IssuanceRequest issuanceRequest) {
        logger.debug("Indigo issue ticket request: " + Json.toJson(issuanceRequest));
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(issuanceRequest);
            RequestBody requestBody = RequestBody.create(jsonString, MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder().url(endPoint+"issueTicket").post(requestBody).build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    indigoLogger.debug("Indigo Issue Ticket Response: " + responseBody);
                    return objectMapper.readValue(responseBody, IssuanceResponse.class);
                } else {
                    logger.error("Failed to fetch data from Indigo API: " + response.message() +
                            " for issuance request: " + Json.toJson(issuanceRequest));
                    throw new Exception("Failed to fetch data from Indigo API: " + response.message());
                }
            }
        } catch (Exception e) {
            logger.error("Error during Indigo issue ticket: " + e.getMessage() +
                    " for issuance request: " + Json.toJson(issuanceRequest), e);
        }
        return null;
    }

    @Override
    public AncillaryServicesResponse getAvailableAncillaryServices(TravellerMasterInfo travellerMasterInfo) {
        logger.info("Indigo get available ancillary services request: " + Json.toJson(travellerMasterInfo));
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(travellerMasterInfo);
            RequestBody requestBody = RequestBody.create(jsonString, MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder().url(endPoint+"showAdditionalBaggageInfoStandalone").post(requestBody).build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    indigoLogger.debug("Indigo Issue Ticket Response: " + responseBody);
                    return objectMapper.readValue(responseBody, AncillaryServicesResponse.class);
                } else {
                    logger.error("Failed to fetch data from Indigo API: " + response.message() +
                            " for issuance request: " + Json.toJson(travellerMasterInfo));
                    throw new Exception("Failed to fetch data from Indigo API: " + response.message());
                }
            }
        } catch (Exception e) {
            logger.error("Error during Indigo get available ancillary services: " + e.getMessage() +
                    " for traveller info: " + Json.toJson(travellerMasterInfo), e);
        }
        return null;
    }
}
