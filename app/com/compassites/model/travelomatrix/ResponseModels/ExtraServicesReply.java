
package com.compassites.model.travelomatrix.ResponseModels;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;


@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtraServicesReply {

    @JsonProperty("ExtraServices")
    private ExtraServices extraServices;
    @JsonProperty("Message")
    private String message;
    @JsonProperty("Status")
    private Long status;

    public ExtraServices getExtraServices() {
        return extraServices;
    }

    public void setExtraServices(ExtraServices extraServices) {
        this.extraServices = extraServices;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getStatus() {
        return status;
    }

    public void setStatus(Long status) {
        this.status = status;
    }

}
