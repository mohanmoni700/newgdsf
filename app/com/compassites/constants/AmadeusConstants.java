package com.compassites.constants;

import java.util.EnumSet;

/**
 * Created by Yaseen on 09-05-2015.
 */
public class AmadeusConstants {

    public static String CANCEL_PNR_ITINERARY_TYPE = "I";

    public static String PRICE_BASE_FARE = "B";

    public static String PRICE_BASE_EQUIVALENT_FARE = "E";

    public static int SESSION_POOL_SIZE = 10;

    public static String AMADEUS_SESSION_LIST = "amadeusSessionList";

    public static String AMADEUS_ACTIVE_SESSION_LIST = "amadeusActiveSessionList";

    public static int INACTIVITY_TIMEOUT = 12;

    public static String SIMULTANEOUS_PNR_CHANGE = "SIMULTANEOUS CHANGES TO PNR";

    public static String SEGMENT_HOLDING_CONFIRMED = "HK";

    public static String TOTAL_FARE_IDENTIFIER = "712";

    public static String DOB_QUALIFIER = "706";

    public static final String SERVICE_TAX_COUNTRY = "India";

    public static final String AMADEUS_FLIGHT_AVAILIBILITY_CODE = "OK";

    public static final String ISSUANCE_OK_STATUS = "O";

    public static final String CAPPING_LIMIT_STRING = "CT RJT";

    public static final String NO_ITINERARY_ERROR_CODE = "931,977";

    public static final String PASSENGER_REFERENCE_STRING = "PT";


    public static enum SEGMENT_STATUS {
        CONFIRMAT_WAITLIST("KL"), SCHEDULE_CHANGE("TK"), EXPIRED_TIME_LIMIT("HX");

        public final String segmentStatus;

        SEGMENT_STATUS(String segmentStatus) {
            this.segmentStatus = segmentStatus;
        }

        public String getSegmentStatus(){
            return segmentStatus;
        }
        @Override
        public String toString() {
            return segmentStatus;
        }
    }

    public enum CONFIRMATION_SEGMENT_STATUS {

        UN("UN"),UC("UC"),KK("KK"),UU("UU"),US("US"),NO("NO");
        public final String segmentStatus;

        CONFIRMATION_SEGMENT_STATUS(String segmentStatus){
            this.segmentStatus = segmentStatus;
        }

        public static boolean contains(String segmentStatus) {

            for (CONFIRMATION_SEGMENT_STATUS s : CONFIRMATION_SEGMENT_STATUS.values()) {
                if (s.name().equals(segmentStatus)) {
                    return true;
                }
            }

            return false;
        }

        public String getConfirmationSegmentStatus(){
            return segmentStatus;
        }
        @Override
        public String toString() {
            return segmentStatus;
        }
    }

    public enum QUEUE_TYPE{

        SCHEDULE_CHANGE("SCHDULE CHANGE"), EXPIRE_TIME_LIMIT("EXPIRE TIME LIMIT"),
        CONFIRMATION("CONFIRMATION"), SEGMENT_CONFIRMATION("SEGMENT CONFIRMATION");

        private final String queueType;

        QUEUE_TYPE(String queueType){
            this.queueType = queueType;
        }

        public String getQueueType(){
            return queueType;
        }

        @Override
        public String toString() {
            return queueType;
        }

    }

    public enum SEAT_TYPE{

        WIDOW("W"),AISLE("A");

        private final String seatType;

        SEAT_TYPE(String seatType){
            this.seatType =  seatType;
        }

        public String getSeatType(){
            return seatType;
        }
    }
}
