package services.ancillary;

import com.compassites.model.AncillaryServicesResponse;
import dto.AncillaryConfirmPaymentRQ;
import dto.AncillaryConfirmPaymentRS;
import models.AncillaryServiceRequest;
import org.springframework.stereotype.Service;

public interface AncillaryService {


    AncillaryServicesResponse getTmxExtraServices(String resultToken, String reResulttoken, String journeyType, Boolean isLCC);

    AncillaryServicesResponse getAdditionalBaggageInfoStandalone( AncillaryServiceRequest ancillaryServiceRequest);

    AncillaryServicesResponse getMealsInfoStandalone( AncillaryServiceRequest ancillaryServiceRequest);

    AncillaryConfirmPaymentRS getAncillaryBaggageConfirm(AncillaryConfirmPaymentRQ ancillaryConfirmPaymentRQ);

}

