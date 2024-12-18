package services.ancillary;

import com.compassites.model.AncillaryServicesResponse;
import models.AncillaryServiceRequest;
import org.springframework.stereotype.Service;

@Service
public interface AncillaryService {


    AncillaryServicesResponse getAdditionalBaggageInfo(String gdsPnr, String provider);

    public AncillaryServicesResponse getTmxExtraServices(String resultToken,String reResulttoken,String journeyType,Boolean isLCC);

    AncillaryServicesResponse getAdditionalBaggageInfoStandalone( AncillaryServiceRequest ancillaryServiceRequest);

}

