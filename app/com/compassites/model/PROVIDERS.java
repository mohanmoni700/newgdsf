package com.compassites.model;

/**
 * Created by Yaseen on 11-05-2015.
 */
public enum PROVIDERS {

    AMADEUS("Amadeus"), TRAVELPORT("Travelport"), MYSTIFLY("Mystifly"),TRAVELOMATRIX("TraveloMatrix");
    String provider;
    private PROVIDERS(String provider){
        this.provider = provider;
    }
    public String getProvider() {
        return provider;
    }

    public String toString() {
        return provider;
    }
};
