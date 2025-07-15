package services.ancillary;

import com.compassites.model.AncillaryServicesResponse;
import dto.AncillaryConfirmPaymentRQ;
import dto.AncillaryConfirmPaymentRS;
import models.AncillaryServiceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AncillaryServiceWrapper implements AncillaryService {


    @Autowired
    AmadeusAncillaryService amadeusAncillaryService;

    @Autowired
    TravelomatixExtraService travelomatixExtraService;

    @Override
    public AncillaryServicesResponse getAdditionalBaggageInfoStandalone(AncillaryServiceRequest ancillaryServiceRequest) {

        AncillaryServicesResponse ancillaryServicesResponse = null;

        if (ancillaryServiceRequest.getProvider().equalsIgnoreCase("Amadeus")) {
            ancillaryServicesResponse = amadeusAncillaryService.additionalBaggageInformationStandalone(ancillaryServiceRequest);
        }

        return ancillaryServicesResponse;
    }

    @Override
    public AncillaryServicesResponse getMealsInfoStandalone( AncillaryServiceRequest ancillaryServiceRequest) {

        AncillaryServicesResponse ancillaryServicesResponse = null;

        if (ancillaryServiceRequest.getProvider().equalsIgnoreCase("Amadeus")) {
            ancillaryServicesResponse = amadeusAncillaryService.additionalMealsInformationStandalone(ancillaryServiceRequest);
        }

        return ancillaryServicesResponse;
    }

    @Override
    public AncillaryConfirmPaymentRS getAncillaryBaggageConfirm(AncillaryConfirmPaymentRQ ancillaryConfirmPaymentRQ) {

        AncillaryConfirmPaymentRS ancillaryConfirmPaymentRS = amadeusAncillaryService.getpaymentConfirmAncillaryServices(ancillaryConfirmPaymentRQ);

        return ancillaryConfirmPaymentRS;
    }


    @Override
    public AncillaryServicesResponse getTmxExtraServices(String resultToken, String reResulttoken, String journeyType, Boolean isLCC) {
        AncillaryServicesResponse ancillaryServicesResponse = null;
        ancillaryServicesResponse = travelomatixExtraService.getExtraServicesfromTmx(resultToken, reResulttoken, journeyType, isLCC);
        return ancillaryServicesResponse;
    }

}

