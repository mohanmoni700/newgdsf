package services.ancillary;

import com.compassites.model.AncillaryServicesResponse;
import dto.AncillaryConfirmPaymentRQ;
import dto.AncillaryConfirmPaymentRS;
import models.AncillaryServiceRequest;
import org.springframework.stereotype.Service;

@Service
public interface AmadeusAncillaryService {

    AncillaryServicesResponse additionalBaggageInformationStandalone(AncillaryServiceRequest ancillaryServiceRequest);

    AncillaryServicesResponse additionalMealsInformationStandalone ( AncillaryServiceRequest ancillaryServiceRequest);

    AncillaryConfirmPaymentRS getpaymentConfirmAncillaryServices (AncillaryConfirmPaymentRQ ancillaryConfirmPaymentRQ);

}
