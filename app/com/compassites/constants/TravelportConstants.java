package com.compassites.constants;

/**
 * Created by yaseen on 05-02-2016.
 */
public class TravelportConstants {

    public static final String NO_ITINERARY_ERROR_CODE = "3037";

    public static final String SEGMENT_CANCELLED = "HX";

    public static final String FORM_OF_PAYMENT = "";

    public static final String UNCONFIRMED_SEGMENT = "UC";

    public enum SEAT_TYPE{

        WIDOW("NSSW"),AISLE("NSSA");

        private final String seatType;

        SEAT_TYPE(String seatType){
            this.seatType =  seatType;
        }

        public String getSeatType(){
            return seatType;
        }
    }
}
