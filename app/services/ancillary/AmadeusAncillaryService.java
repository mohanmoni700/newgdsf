package services.ancillary;

import com.compassites.model.AncillaryServicesResponse;
import dto.ancillary.AncillaryBookingRequest;
import dto.ancillary.AncillaryBookingResponse;
import models.AncillaryServiceRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface AmadeusAncillaryService {

    AncillaryServicesResponse additionalBaggageInformationStandalone(AncillaryServiceRequest ancillaryServiceRequest);

    AncillaryServicesResponse additionalMealsInformationStandalone ( AncillaryServiceRequest ancillaryServiceRequest);

    Map<String, List<AncillaryBookingResponse>> getpaymentConfirmAncillaryServices (AncillaryBookingRequest ancillaryBookingRequest);

}
