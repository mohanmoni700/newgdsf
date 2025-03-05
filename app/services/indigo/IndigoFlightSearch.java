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
import org.springframework.stereotype.Service;
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
    private static final String endPoint = "http://localhost:8086/indigo/search";

    //@RetryOnFailure(attempts = 2, delay = 2000, exception = RetryException.class)
    public SearchResponse search(SearchParameters searchParameters, FlightSearchOffice office) throws Exception {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(searchParameters);
            RequestBody requestBody = RequestBody.create(jsonString, MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder().url(endPoint).post(requestBody).build();
            try (Response response = client.newCall(request).execute()) {
                System.out.println("Indigo "+response.body().string());
            }
            return new SearchResponse();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new SearchResponse();
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
