
package com.compassites.model.travelomatrix.ResponseModels;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;


public class TravelomatrixSearchReply {

    @JsonProperty("Message")
    private String message;
    @JsonProperty("Search")
    private Search Search;
    @JsonProperty("Status")
    private Boolean status;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public com.compassites.model.travelomatrix.ResponseModels.Search getSearch() {
        return Search;
    }

    public void setSearch(com.compassites.model.travelomatrix.ResponseModels.Search search) {
        Search = search;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "TravelomatrixSearchReply{" +
                "message='" + message + '\'' +
                ", Search=" + Search.toString() +
                ", status=" + status +
                '}';
    }
}
