package services.ancillary;

import com.compassites.model.AncillaryServicesResponse;

public interface TravelomatixExtraService {
    public AncillaryServicesResponse getExtraServicesfromTmx(String resultToken,String reResulttoken,String journeyType,Boolean isLCC);
}
