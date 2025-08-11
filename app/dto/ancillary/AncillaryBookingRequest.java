package dto.ancillary;

import com.compassites.model.BaggageDetails;
import com.compassites.model.MealDetails;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AncillaryBookingRequest implements Serializable {

    private List<BaggageDetails> baggageDetailsList;

    private List<MealDetails> mealDetailsList;

    private String provider;

    private String gdsPnr;

    public List<BaggageDetails> getBaggageDetailsList() {
        return baggageDetailsList;
    }

    public void setBaggageDetailsList(List<BaggageDetails> baggageDetailsList) {
        this.baggageDetailsList = baggageDetailsList;
    }

    public List<MealDetails> getMealDetailsList() {
        return mealDetailsList;
    }

    public void setMealDetailsList(List<MealDetails> mealDetailsList) {
        this.mealDetailsList = mealDetailsList;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getGdsPnr() {
        return gdsPnr;
    }

    public void setGdsPnr(String gdsPnr) {
        this.gdsPnr = gdsPnr;
    }

}
