package services.ancillary;

import com.compassites.model.AncillaryServicesResponse;
import models.AncillaryServiceRequest;
import org.springframework.stereotype.Service;

@Service
public interface AmadeusAncillaryService {

    AncillaryServicesResponse additionalBaggageInformation(String gdsPnr);
    AncillaryServicesResponse additionalBaggageInformationStandalone ( AncillaryServiceRequest ancillaryServiceRequest);
}
