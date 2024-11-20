package services.ancillary;

import com.compassites.model.AncillaryServicesResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AncillaryServiceWrapper implements AncillaryService{


    @Autowired
    AmadeusAncillaryService amadeusAncillaryService;
  
    @Autowired
    TravelomatixExtraService travelomatixExtraService;

    @Override
    public AncillaryServicesResponse getAdditionalBaggageInfo(String gdsPnr, String provider){

        AncillaryServicesResponse  ancillaryServicesResponse= null;

        if(provider.equalsIgnoreCase("Amadeus")){
            ancillaryServicesResponse = amadeusAncillaryService.additionalBaggageInformation(gdsPnr);
        }

        return ancillaryServicesResponse;
    }


    @Override
    public AncillaryServicesResponse getTmxExtraServices(String resultToken,String reResulttoken,String journeyType,Boolean isLCC){
        AncillaryServicesResponse  ancillaryServicesResponse= null;
        ancillaryServicesResponse = travelomatixExtraService.getExtraServicesfromTmx(resultToken,reResulttoken,journeyType,isLCC);
        return ancillaryServicesResponse;
    }


}

