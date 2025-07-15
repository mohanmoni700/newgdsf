package dto;

import com.compassites.model.BaggageDetails;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AncillaryConfirmPaymentRQ implements Serializable {

    Map<String, BaggageDetails> confirmBaggageDetails;

    public Map<String, BaggageDetails> getConfirmBaggageDetails() {
        return confirmBaggageDetails;
    }

    public void setConfirmBaggageDetails(Map<String, BaggageDetails> confirmBaggageDetails) {
        this.confirmBaggageDetails = confirmBaggageDetails;
    }

}
