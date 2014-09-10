package com.compassites.GDSWrapper.travelport;

import com.travelport.schema.air_v26_0.AirSegmentRef;
import com.travelport.schema.air_v26_0.FlightDetails;
import com.travelport.schema.air_v26_0.FlightDetailsRef;
import com.travelport.schema.air_v26_0.TypeBaseAirSegment;
import com.travelport.schema.common_v26_0.ProviderReservationInfoRef;
import com.travelport.schema.universal_v26_0.ProviderReservationInfo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class Helper {
    /**
     * Utility class for building a map that knows about all the segments in the
     * response.
     */
    public static class AirSegmentMap extends HashMap<String, TypeBaseAirSegment> {
        public void add(TypeBaseAirSegment segment) {
            put(segment.getKey(), segment);
        }

        @Override
        public TypeBaseAirSegment get(Object wontWork) {
            throw new RuntimeException("This is disallowed because it was a " + "common mistake to pass a AirSegmentRef here instead "
                    + "of the string contained in the AirSegmentRef");
        }

        public TypeBaseAirSegment getByRef(AirSegmentRef ref) {
            return super.get(ref.getKey());
        }
    }

    /**
     * Utility class for building a map that knows all the flight details
     * objects and can look them up by their key.
     */
    public static class FlightDetailsMap extends HashMap<String, FlightDetails> {
        public void add(FlightDetails detail) {
            put(detail.getKey(), detail);
        }

        @Override
        public FlightDetails get(Object wontWork) {
            throw new RuntimeException("This is disallowed because it was a " + "common mistake to pass a FlightSegmentRef here instead "
                    + "of the string contained in the FlightSegmentRef");
        }

        public FlightDetails getByRef(FlightDetailsRef ref) {
            return super.get(ref.getKey());
        }

    }


    /**
     * Utility class for building a map that knows about all the segments in the
     * response.
     */
    public static class ReservationInfoMap extends HashMap<String, ProviderReservationInfo> {
        public void add(ProviderReservationInfo providerReservationInfo) {
            put(providerReservationInfo.getKey(), providerReservationInfo);
        }

        @Override
        public ProviderReservationInfo get(Object wontWork) {
            throw new RuntimeException("This is disallowed because it was a " + "common mistake to pass a ProviderReservationInfoRef here instead "
                    + "of the string contained in the ProviderReservationInfoRef");
        }

        public ProviderReservationInfo getByRef(ProviderReservationInfoRef ref) {
            return super.get(ref.getKey());
        }
    }

    // this is the format we SEND to travelport
    public static SimpleDateFormat searchFormat = new SimpleDateFormat("yyyy-MM-dd");

    // return a date that is n days in future
    public static String daysInFuture(int n) {
        Date now = new Date(), future;
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.DATE, n);
        future = calendar.getTime();
        return searchFormat.format(future);
    }

    /**
     * Build the map from references to flight details to to real flight
     * details.
     */
    public static FlightDetailsMap createFlightDetailsMap(List<FlightDetails> details) {
        FlightDetailsMap result = new FlightDetailsMap();
        for (Iterator<FlightDetails> iterator = details.iterator(); iterator.hasNext();) {
            FlightDetails deet = (FlightDetails) iterator.next();
            result.add(deet);
        }
        return result;
    }

    /**
     * Take a air segment list and construct a map of all the segments into a
     * segment map. This makes other parts of the work easier.
     */
    public static AirSegmentMap createAirSegmentMap(List<TypeBaseAirSegment> segments) {
        // construct a map with all the segments and their keys
        AirSegmentMap segmentMap = new AirSegmentMap();

        for (Iterator<TypeBaseAirSegment> iterator = segments.iterator(); iterator.hasNext();) {
            TypeBaseAirSegment airSegment = (TypeBaseAirSegment) iterator.next();
            segmentMap.add(airSegment);
        }

        return segmentMap;
    }

    /**
     * Take a ProviderReservationInfo list and construct a map of all the reservationInfo into a
     * info map. This makes other parts of the work easier.
     */
    public static  ReservationInfoMap createReservationInfoMap(List<ProviderReservationInfo> reservationInfos){
        ReservationInfoMap reservationInfoMap = new ReservationInfoMap();

        for(ProviderReservationInfo reservationInfo : reservationInfos){
            reservationInfoMap.add(reservationInfo);
        }

        return  reservationInfoMap;

    }
    /**
     * Parse a number from something with a currency code on the front.  Aborts
     * if the number cannot be understood.
     */
    public static double parseNumberWithCurrency(String numberWithCurrency) {
        // first 3 chars are currency code
        String price = numberWithCurrency.substring(3);
        return Double.parseDouble(price);
    }


    public static void writeXML(Object reply) {
        File file = new File(reply.getClass().toString() + ".xml");
        try {
            if (!file.exists()) {
                file.createNewFile();
                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(reply.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
