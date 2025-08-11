package services.indigo;

import com.compassites.constants.IndigoConstants;
import com.compassites.constants.TraveloMatrixConstants;
import com.compassites.exceptions.RetryException;
import com.compassites.model.SearchParameters;
import com.compassites.model.SearchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.FlightSearchOffice;
import okhttp3.*;
import org.apache.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import play.Play;
import play.libs.Json;
import services.FlightSearch;

import java.util.ArrayList;
import java.util.List;
import java.net.URI;

import services.RetryOnFailure;

import java.util.concurrent.*;
@Service
public class IndigoFlightSearch implements FlightSearch {

    private static final OkHttpClient client = new OkHttpClient();
    private static final String endPoint = Play.application().configuration().getString("indigo.service.endPoint");

    static Logger logger = LoggerFactory.getLogger("gds");

    static Logger indigoLogger = LoggerFactory.getLogger("indigo");
    @RetryOnFailure(attempts = 2, delay = 2000, exception = RetryException.class)
    public SearchResponse search(SearchParameters searchParameters, FlightSearchOffice office) throws Exception {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(searchParameters);
            RequestBody requestBody = RequestBody.create(jsonString, MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder().url(endPoint+"flightSearch").post(requestBody).build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    SearchResponse searchResponse = objectMapper.readValue(responseBody, SearchResponse.class);
                    indigoLogger.debug("Indigo Flight Search Response: " + responseBody);
                    searchResponse.setFlightSearchOffice(office);
                    searchResponse.setProvider("Indigo");
                    return searchResponse;
                } else {
                    logger.error("Failed to fetch data from Indigo API: " + response.message() +
                        " for search parameters: " + Json.toJson(searchParameters));
                    SearchResponse searchResponse = new SearchResponse();
                    searchResponse.setFlightSearchOffice(office);
                    searchResponse.setProvider("Indigo");
                    return searchResponse;
                    //throw new Exception("Failed to fetch data from Indigo API: " + response.message());
                }
            }
        } catch (Exception e) {
            logger.error("Error during Indigo flight search: " + e.getMessage() +
                " for search parameters: " + Json.toJson(searchParameters), e);
            //e.printStackTrace();
            SearchResponse searchResponse = new SearchResponse();
            searchResponse.setFlightSearchOffice(office);
            searchResponse.setProvider("Indigo");
            return searchResponse;
        }
    }

    @Override
    public String provider() {
        return null;
    }

    @Override
    public List<FlightSearchOffice> getOfficeList() {
        FlightSearchOffice fs = new FlightSearchOffice();
        fs.setOfficeId(IndigoConstants.officeId);
        List<FlightSearchOffice> lfs = new ArrayList<>();
        lfs.add(fs);
        return lfs;
    }
}
