package services;

import com.compassites.model.travelomatrix.ResponseModels.TraveloMatrixFaruleReply;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.List;

public interface TraveloMatrixFlightInfoService {
    public List<HashMap> flightFareRules(String resultToken);
}
