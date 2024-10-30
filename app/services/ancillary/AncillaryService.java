package services.ancillary;

import com.compassites.model.AncillaryServicesResponse;
import org.springframework.stereotype.Service;

@Service
public interface AncillaryService {

    AncillaryServicesResponse getAdditionalBaggageInfo(String gdsPnr, String provider);

}
