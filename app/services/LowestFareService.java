package services;

import com.compassites.model.IssuanceRequest;
import com.compassites.model.LowFareResponse;

/**
 * Created by yaseen on 24-09-2016.
 */
public interface LowestFareService {

    public LowFareResponse getLowestFare(IssuanceRequest issuanceRequest);
}
