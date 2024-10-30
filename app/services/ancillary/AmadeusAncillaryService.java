package services.ancillary;

import com.compassites.model.AncillaryServicesResponse;
import org.springframework.stereotype.Service;

@Service
public interface AmadeusAncillaryService {

    AncillaryServicesResponse additionalBaggageInformation(String gdsPnr);

}
