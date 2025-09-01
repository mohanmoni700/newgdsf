package services.ancillary;

import com.compassites.model.AncillaryServicesResponse;
import dto.ancillary.AncillaryBookingRequest;
import dto.ancillary.AncillaryBookingResponse;
import com.compassites.model.traveller.TravellerMasterInfo;
import models.AncillaryServiceRequest;

import java.util.List;
import java.util.Map;

public interface AncillaryService {


    AncillaryServicesResponse getTmxExtraServices(String resultToken, String reResulttoken, String journeyType, Boolean isLCC);

    AncillaryServicesResponse getAdditionalBaggageInfoStandalone( AncillaryServiceRequest ancillaryServiceRequest);

    AncillaryServicesResponse getMealsInfoStandalone( AncillaryServiceRequest ancillaryServiceRequest);
    AncillaryServicesResponse getAvailableAncillaryServices(TravellerMasterInfo travellerMasterInfo);

    Map<String, List<AncillaryBookingResponse>> getAncillaryBaggageConfirm(AncillaryBookingRequest ancillaryBookingRequest);

}

