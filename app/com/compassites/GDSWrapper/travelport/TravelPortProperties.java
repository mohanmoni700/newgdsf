package com.compassites.GDSWrapper.travelport;

/**
 * Created by Renu on 5/28/14.
 */
public class TravelPortProperties {
    private static TravelPortProperties ourInstance = new TravelPortProperties();

    public static TravelPortProperties getInstance() {
        return ourInstance;
    }

    private TravelPortProperties() {
    }
}
