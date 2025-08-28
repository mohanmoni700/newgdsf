package utils;

import com.compassites.constants.TraveloMatrixConstants;
import com.compassites.model.*;
import models.FlightSearchOffice;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import play.libs.Json;


import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SplitTicketMerger {


    static Logger logger = LoggerFactory.getLogger("gds");

    private static final String officeId = "BOMVS34C3";

    private static final int connectionTime = play.Play.application().configuration().getInt("split.minConnectionTime");

    public void splitMergeResults(ConcurrentHashMap<Integer, FlightItinerary> allFightItineraries, SearchResponse searchResponse) {
        try{
            AirSolution airSolution = searchResponse.getAirSolution();
            String provider = searchResponse.getProvider();
            if(provider.equals(TraveloMatrixConstants.tmofficeId)){
                System.out.println("travelomatrix merge");
            }
            FlightSearchOffice office = searchResponse.getFlightSearchOffice();
            if(allFightItineraries.isEmpty()) {
                mergeSeamenAndNonSeamenResults(allFightItineraries, airSolution);
            } else {
                ConcurrentHashMap<Integer, FlightItinerary> seamenFareHash = airSolution.getSeamenHashMap();
                ConcurrentHashMap<Integer, FlightItinerary> nonSeamenFareHash = airSolution.getNonSeamenHashMap();

                for (Integer hashKey : allFightItineraries.keySet()) {
                    if (seamenFareHash != null && seamenFareHash.containsKey(hashKey)) {
                        FlightItinerary mainFlightItinerary = allFightItineraries.get(hashKey);
                        FlightItinerary seamenItinerary = seamenFareHash.get(hashKey);
                        if (mainFlightItinerary.getSeamanPricingInformation() == null
                                || mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue() == null
                                || (seamenItinerary.getPricingInformation() != null && seamenItinerary.getPricingInformation().getTotalPriceValue() != null
                                && seamenItinerary.getPricingInformation().getTotalPriceValue().longValue() < mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue().longValue())
                                || (seamenItinerary.getPricingInformation().getPricingOfficeId().equalsIgnoreCase(officeId)
                                && seamenItinerary.getPricingInformation() != null && seamenItinerary.getPricingInformation().getTotalPriceValue() != null
                                && seamenItinerary.getPricingInformation().getTotalPriceValue().longValue() <= mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue().longValue())) {
                            mainFlightItinerary.setSeamanPricingInformation(seamenItinerary.getPricingInformation());
                            mainFlightItinerary.setJourneyList(seamenItinerary.getJourneyList());
                        }
                        allFightItineraries.put(hashKey, mainFlightItinerary);
                        seamenFareHash.remove(hashKey);
                    }
                    if (nonSeamenFareHash != null && nonSeamenFareHash.containsKey(hashKey)) {
                        FlightItinerary mainFlightItinerary = allFightItineraries.get(hashKey);
                        FlightItinerary nonSeamenItinerary = nonSeamenFareHash.get(hashKey);
                        if (mainFlightItinerary.getPricingInformation() == null
                                || mainFlightItinerary.getPricingInformation().getTotalPriceValue() == null
                                || (nonSeamenItinerary.getPricingInformation() != null && nonSeamenItinerary.getPricingInformation().getTotalPriceValue() != null
                                && nonSeamenItinerary.getPricingInformation().getTotalPriceValue().longValue() < mainFlightItinerary.getPricingInformation().getTotalPriceValue().longValue())
                                || (nonSeamenItinerary.getPricingInformation().getPricingOfficeId().equalsIgnoreCase(officeId)
                                && nonSeamenItinerary.getPricingInformation() != null && nonSeamenItinerary.getPricingInformation().getTotalPriceValue() != null
                                && nonSeamenItinerary.getPricingInformation().getTotalPriceValue().longValue() <= mainFlightItinerary.getPricingInformation().getTotalPriceValue().longValue())) {
                            mainFlightItinerary.setPricingInformation(nonSeamenItinerary.getPricingInformation());
                            mainFlightItinerary.setNonSeamenJourneyList(nonSeamenItinerary.getJourneyList());
                            //for Travelomatrix
                            if(nonSeamenItinerary.getResultToken() != null){
                                mainFlightItinerary.setResultToken(nonSeamenItinerary.getResultToken());
                                mainFlightItinerary.setIsLCC(nonSeamenItinerary.getLCC());
                            }
                        }
                        allFightItineraries.put(hashKey, mainFlightItinerary);
                        nonSeamenFareHash.remove(hashKey);
                    }
                }
                ConcurrentHashMap<Integer, FlightItinerary> list = mergeSeamenAndNonSeamenResults(new ConcurrentHashMap<Integer, FlightItinerary>(), airSolution);
                allFightItineraries.putAll(list);
            }
        }catch (Exception e){
            logger.error("MergeResults:: ex:"+ e.getMessage());
        }
    }

    public ConcurrentHashMap<Integer, FlightItinerary> mergeSeamenAndNonSeamenResults(ConcurrentHashMap<Integer, FlightItinerary> allFightItineraries, AirSolution airSolution) {
        if (airSolution.getNonSeamenHashMap() != null && !airSolution.getNonSeamenHashMap().isEmpty()) {
            ConcurrentHashMap<Integer, FlightItinerary> nonSeamenFareHash = airSolution.getNonSeamenHashMap();
            allFightItineraries.putAll(nonSeamenFareHash);
        }
        if (airSolution.getSeamenHashMap() != null && !airSolution.getSeamenHashMap().isEmpty()) {
            ConcurrentHashMap<Integer, FlightItinerary> seamenFareHash = airSolution.getSeamenHashMap();
            for (Integer hashKey : seamenFareHash.keySet()) {
                FlightItinerary seamenItinerary = seamenFareHash.get(hashKey);
                if (allFightItineraries.containsKey(hashKey)) {
                    FlightItinerary itinerary = allFightItineraries.get(hashKey);
                    itinerary.setPriceOnlyPTC(true);
                    itinerary.setSeamanPricingInformation(seamenFareHash.get(hashKey).getPricingInformation());
                    itinerary.setJourneyList(seamenFareHash.get(hashKey).getJourneyList());
                    itinerary.setNonSeamenJourneyList(allFightItineraries.get(hashKey).getJourneyList());
                    allFightItineraries.put(hashKey, itinerary);
                } else {
                    seamenItinerary.setPriceOnlyPTC(true);
                    seamenItinerary.setSeamanPricingInformation(seamenItinerary.getPricingInformation());
                    allFightItineraries.put(hashKey, seamenItinerary);
                }
            }
        }
        return allFightItineraries;


    }

    public ConcurrentHashMap<Integer, FlightItinerary> mergeSplitSearchResults(ConcurrentHashMap<Integer, FlightItinerary> allFightItineraries, List<SearchResponse> searchResponses) {
        for (SearchResponse searchResponse: searchResponses) {
            ConcurrentHashMap<Integer, FlightItinerary> seamenHashMap = searchResponse.getAirSolution().getSeamenHashMap();
            createSeamenMap(seamenHashMap);
        }
        return allFightItineraries;
    }

    private ConcurrentHashMap<String, Journey> createSeamenMap(ConcurrentHashMap<Integer, FlightItinerary> seamenHashMap) {
        List<AirSegmentInformation> mergedAirsegmentList = new ArrayList<>();
        ConcurrentHashMap<String, Journey> journeyConcurrentHashMap = new ConcurrentHashMap<>();
        for (Map.Entry<Integer, FlightItinerary> map: seamenHashMap.entrySet()) {
            FlightItinerary flightItinerary = map.getValue();
            for (Journey journey: flightItinerary.getJourneyList()) {
                String toLocation = journey.getAirSegmentList().get(journey.getAirSegmentList().size()-1).getToLocation();
                if(toLocation.contains(toLocation)) {
                    //second segments
                    createAirSegment(journey.getAirSegmentList(), mergedAirsegmentList, journeyConcurrentHashMap);
                } else {
                    journeyConcurrentHashMap.put(toLocation, journey);
                }
            }
        }
        return journeyConcurrentHashMap;
    }

    private void createAirSegment(List<AirSegmentInformation> airSegmentInformations, List<AirSegmentInformation> mergedAirsegmentList, ConcurrentHashMap<String, Journey> journeyConcurrentHashMap) {
        String toLocation =  airSegmentInformations.get(0).getFromLocation();
        if(journeyConcurrentHashMap.containsKey(toLocation)) {
            journeyConcurrentHashMap.get(toLocation).getAirSegmentList().addAll(airSegmentInformations);
        }
    }

    public SearchResponse mergeSearchResponse(List<SearchResponse> responses) {
        SearchResponse searchResponse = new SearchResponse();
        if(responses.size() > 0 && responses.get(0).getErrorMessageList().size() == 0) {
            BeanUtils.copyProperties(responses.get(0), searchResponse);
        } else {
            return null;
        }
        ConcurrentHashMap<Integer, FlightItinerary> semenMap = searchResponse.getAirSolution().getSeamenHashMap();
        int i=1;
        Map<Integer, FlightItinerary> addedMap = new HashMap<>();
        for (Map.Entry<Integer, FlightItinerary> flightItineraryMap: semenMap.entrySet()) {
           for (int nextIndex=1; nextIndex < responses.size(); nextIndex++) {
               flightItineraryMap.getValue().getJourneyList().addAll(getJoruney(responses.get(nextIndex),addedMap, nextIndex));
           }
           i++;
        }
        return searchResponse;
    }

    private List<Journey> getJoruney(SearchResponse searchResponse, Map<Integer, FlightItinerary> addedMap, int index) {
        List<Integer> allKeys = new ArrayList<>(searchResponse.getAirSolution().getSeamenHashMap().keySet());
        Integer mapKey = allKeys.get(0);
        return searchResponse.getAirSolution().getSeamenHashMap().get(mapKey).getJourneyList();
    }

    public List<FlightItinerary> mergingSplitTicket(String fromLocation,String toLocation, ConcurrentHashMap<String,List<FlightItinerary>> concurrentHashMap, boolean isSourceAirportDomestic, boolean isDestinationAirportDomestic) {
        List<FlightItinerary> flightItineraries = concurrentHashMap.get(fromLocation);
        List<FlightItinerary> mergedResult = new ArrayList<>();
        System.out.println("mergingSplitTicket fromLocation "+fromLocation+" size "+flightItineraries.size()+" toLocation "+toLocation+" isSourceAirportDomestic "+isSourceAirportDomestic+" isDestinationAirportDomestic "+isDestinationAirportDomestic+ " concurrentHashMap size "+concurrentHashMap.size()+" keySet "+concurrentHashMap.keySet().toString());
        for (String key: concurrentHashMap.keySet()) {
            System.out.println("key "+key+" size "+concurrentHashMap.get(key).size());
        }
        if(flightItineraries!=null) {
            int k=0;
            for (FlightItinerary flightItinerary : flightItineraries) {
                List<PricingInformation> spiltPrices = new ArrayList<>();
                FlightItinerary flightItinerary1 = SerializationUtils.clone(flightItinerary);
                flightItinerary1.setSplitTicket(true);
                spiltPrices.add(flightItinerary.getPricingInformation());
                if(isSourceAirportDomestic) {
                    List<FlightItinerary> newFlightItineraries = createItinerariesJourney(flightItinerary.getToLocation(), toLocation, concurrentHashMap, flightItinerary1, k, spiltPrices);
                    if (newFlightItineraries.size() > 0) {
                        for (FlightItinerary flightItinerary2 : newFlightItineraries) {
                            List<PricingInformation> newSplitPrice = new ArrayList<>();
                            FlightItinerary newFlightItinerary1 = SerializationUtils.clone(flightItinerary1);
                            newSplitPrice.add(newFlightItinerary1.getPricingInformation());
                            newSplitPrice.add(flightItinerary2.getPricingInformation());
                            newFlightItinerary1.getJourneyList().addAll(flightItinerary2.getJourneyList());
                            createTotalPricing(newSplitPrice, newFlightItinerary1);
                            newFlightItinerary1.setSplitPricingInformationList(newSplitPrice);
                            String toLoc = newFlightItinerary1.getJourneyList().get(newFlightItinerary1.getJourneyList().size() - 1).getToLocation();

                            System.out.println("fromLocation " + flightItinerary.getToLocation() + " toLoc" + toLoc);
                            logger.debug("fromLocation " + flightItinerary.getToLocation() + " toLoc" + toLoc);
                            if (toLoc != null && isAllSegmentMerged(toLocation, newFlightItinerary1) && toLoc.equalsIgnoreCase(toLocation)) {
                                mergedResult.add(newFlightItinerary1);
                            }
                        }
                    }
                } else {
                    System.out.println("isDestinationAirportDomestic"+ isDestinationAirportDomestic);
                    toLocation = "VXE";
                    creatingNewJourney(flightItinerary.getToLocation(), toLocation, concurrentHashMap, flightItinerary1, k, spiltPrices);
                    String toLoc = flightItinerary1.getJourneyList().get(flightItinerary1.getJourneyList().size() - 1).getToLocation();
                    System.out.println("fromLocation else " + flightItinerary.getToLocation() + " toLoc" + toLoc);
                    logger.debug("fromLocation else " + flightItinerary.getToLocation() + " toLoc" + toLoc);
                    if (toLoc != null && isAllSegmentMerged(toLocation, flightItinerary1) && toLoc.equalsIgnoreCase(toLocation)) {
                        createTotalPricing(spiltPrices, flightItinerary1);
                        flightItinerary1.setSplitPricingInformationList(spiltPrices);
                        mergedResult.add(flightItinerary1);
                    }
                }
                k++;
            }
        }
        return mergedResult;
    }

    /*
    public List<FlightItinerary> mergingSplitTicket(String fromLocation,String toLocation, ConcurrentHashMap<String,List<FlightItinerary>> concurrentHashMap, boolean isSourceAirportDomestic) {
        List<FlightItinerary> flightItineraries = concurrentHashMap.get(fromLocation);
        List<FlightItinerary> mergedResult = new ArrayList<>();
        if(flightItineraries!=null) {
            int k=0;
            for (FlightItinerary flightItinerary : flightItineraries) {
                System.out.println("K "+k);
                System.out.println("From Loc "+flightItinerary.getJourneyList().get(0).getAirSegmentList().get(0).getFromLocation()+"  size "+flightItinerary.getJourneyList().get(0).getAirSegmentList().size());
                List<PricingInformation> spiltPrices = new ArrayList<>();
                FlightItinerary flightItinerary1 = SerializationUtils.clone(flightItinerary);
                flightItinerary1.setSplitTicket(true);
                spiltPrices.add(flightItinerary.getPricingInformation());
                if(isSourceAirportDomestic) {
                    List<FlightItinerary> newFlightItineraries = createItinerariesJourney(flightItinerary.getToLocation(), toLocation, concurrentHashMap, flightItinerary1, k, spiltPrices);
                    if (newFlightItineraries.size() > 0) {
                        for (FlightItinerary flightItinerary2 : newFlightItineraries) {
                            List<PricingInformation> newSplitPrice = new ArrayList<>();
                            FlightItinerary newFlightItinerary1 = SerializationUtils.clone(flightItinerary1);
                            newSplitPrice.add(newFlightItinerary1.getPricingInformation());
                            newSplitPrice.add(flightItinerary2.getPricingInformation());
                            newFlightItinerary1.getJourneyList().addAll(flightItinerary2.getJourneyList());
                            createTotalPricing(newSplitPrice, newFlightItinerary1);
                            newFlightItinerary1.setSplitPricingInformationList(newSplitPrice);
                            String toLoc = newFlightItinerary1.getJourneyList().get(newFlightItinerary1.getJourneyList().size() - 1).getToLocation();

                            System.out.println("fromLocation " + flightItinerary.getToLocation() + " toLoc" + toLoc);
                            logger.debug("fromLocation " + flightItinerary.getToLocation() + " toLoc" + toLoc);
                            if (toLoc != null && isAllSegmentMerged(toLocation, newFlightItinerary1) && toLoc.equalsIgnoreCase(toLocation)) {
                                mergedResult.add(newFlightItinerary1);
                            }
                        }
                    }
                } else {
                    creatingNewJourney(flightItinerary.getToLocation(), toLocation, concurrentHashMap, flightItinerary1, k, spiltPrices);
                    String toLoc = flightItinerary1.getJourneyList().get(flightItinerary1.getJourneyList().size() - 1).getToLocation();
                    System.out.println("fromLocation else " + flightItinerary.getToLocation() + " toLoc" + toLoc);
                    logger.debug("fromLocation else " + flightItinerary.getToLocation() + " toLoc" + toLoc);
                    if (toLoc != null && isAllSegmentMerged(toLocation, flightItinerary1) && toLoc.equalsIgnoreCase(toLocation)) {
                        createTotalPricing(spiltPrices, flightItinerary1);
                        flightItinerary1.setSplitPricingInformationList(spiltPrices);
                        mergedResult.add(flightItinerary1);
                    }

                    /*List<FlightItinerary> newFlightItineraries = creatingNonSeamenJourney(flightItinerary.getToLocation(), toLocation, concurrentHashMap, flightItinerary1, k, spiltPrices);

                    System.out.println("newFlightItineraries size "+newFlightItineraries.size());
                    for (FlightItinerary flightItinerary2: newFlightItineraries) {
                        String toLoc = flightItinerary2.getJourneyList().get(flightItinerary2.getJourneyList().size() - 1).getToLocation();
                        System.out.println("fromLocation else " + flightItinerary.getToLocation() + " toLoc" + toLoc);
                        logger.debug("fromLocation else " + flightItinerary.getToLocation() + " toLoc" + toLoc);
                        if (toLoc != null && isAllSegmentMerged(toLocation, flightItinerary2) && toLoc.equalsIgnoreCase(toLocation)) {
                            createTotalPricing(spiltPrices, flightItinerary2);
                            flightItinerary2.setSplitPricingInformationList(spiltPrices);
                            mergedResult.add(flightItinerary2);
                        }
                    }
                }
                /*creatingNewJourney(flightItinerary.getToLocation(), toLocation, concurrentHashMap, flightItinerary1, k, spiltPrices);
                String toLoc = flightItinerary1.getJourneyList().get(flightItinerary1.getJourneyList().size() - 1).getToLocation();
                System.out.println("fromLocation " + flightItinerary.getToLocation() + " toLoc" + toLoc);
                logger.debug("fromLocation " + flightItinerary.getToLocation() + " toLoc" + toLoc);
                if (toLoc != null && isAllSegmentMerged(toLocation, flightItinerary1) && toLoc.equalsIgnoreCase(toLocation)) {
                    createTotalPricing(spiltPrices, flightItinerary1);
                    flightItinerary1.setSplitPricingInformationList(spiltPrices);
                    mergedResult.add(flightItinerary1);
                }*/
                /*List<FlightItinerary> newFlightItineraries = creatingJourneyWithNonStop(flightItinerary.getToLocation(), toLocation, concurrentHashMap, flightItinerary1, k, spiltPrices);
                if (newFlightItineraries.size()>0) {
                    for (FlightItinerary flightItinerary2 : newFlightItineraries) {
                        List<PricingInformation> spiltPricesNew = new ArrayList<>();
                        spiltPricesNew.add(flightItinerary.getPricingInformation());
                        spiltPricesNew.add(flightItinerary2.getPricingInformation());
                        String toLoc = flightItinerary2.getJourneyList().get(flightItinerary2.getJourneyList().size() - 1).getToLocation();
                        System.out.println("fromLocation " + flightItinerary.getToLocation() + " toLoc" + toLoc);
                        logger.debug("fromLocation " + flightItinerary.getToLocation() + " toLoc" + toLoc);
                        if (toLoc != null && isAllSegmentMerged(toLocation, flightItinerary2) && toLoc.equalsIgnoreCase(toLocation)) {
                            createTotalPricing(spiltPricesNew, flightItinerary2);
                            flightItinerary2.setSplitPricingInformationList(spiltPricesNew);
                            mergedResult.add(flightItinerary2);
                        }
                    }
                }
                k++;
                k++;
                        }
                        }
                        return mergedResult;
                        }
    */

    /**
     * Merges split tickets with time constraint between 3-8 hours for connection time.
     * This method ensures that the connection time between first segment arrival and second segment departure
     * is greater than 3 hours and less than 8 hours.
     * If no suitable connection is found, it tries to find next day flights in the second segment.
     *
     * @param fromLocation Starting location (e.g., VXE)
     * @param toLocation Final destination (e.g., BOM)
     * @param concurrentHashMap Map containing flight itineraries by location
     * @param isSourceAirportDomestic Whether source airport is domestic
     * @param isDestinationAirportDomestic Whether destination airport is domestic
     * @return List of merged flight itineraries meeting the time constraint
     */
    public List<FlightItinerary> mergingSplitTicketWithTimeConstraint(String fromLocation, String toLocation,
            ConcurrentHashMap<String, List<FlightItinerary>> concurrentHashMap,
            boolean isSourceAirportDomestic, boolean isDestinationAirportDomestic) {

        List<FlightItinerary> flightItineraries = concurrentHashMap.get(fromLocation);
        List<FlightItinerary> mergedResult = new ArrayList<>();

        if (flightItineraries == null || flightItineraries.isEmpty()) {
            return mergedResult;
        }

        // Take the first segment's first result as requested
        FlightItinerary firstSegment = flightItineraries.get(0);

        System.out.println("mergingSplitTicketWithTimeConstraint fromLocation " + fromLocation + " size " +
            flightItineraries.size() + " toLocation " + toLocation + " isSourceAirportDomestic " +
            isSourceAirportDomestic + " isDestinationAirportDomestic " + isDestinationAirportDomestic);

        // First try to find intermediate connections (e.g., VXE -> CMN -> BOM)
        List<FlightItinerary> intermediateConnections = findIntermediateConnections(fromLocation, toLocation,
            concurrentHashMap, firstSegment, isSourceAirportDomestic, isDestinationAirportDomestic);
        if (!intermediateConnections.isEmpty()) {
            mergedResult.addAll(intermediateConnections);
        }

        // If no intermediate connections found, try direct connections with time constraint
        if (mergedResult.isEmpty()) {
            List<FlightItinerary> directConnections = createItinerariesJourneyWithTimeConstraint(fromLocation,
                toLocation, concurrentHashMap, firstSegment, isSourceAirportDomestic, isDestinationAirportDomestic);
            if (!directConnections.isEmpty()) {
                mergedResult.addAll(directConnections);
            }
        }

        // If still no results, try next day flights
        if (mergedResult.isEmpty()) {
            List<FlightItinerary> nextDayConnections = createItinerariesJourneyWithNextDayConstraint(fromLocation,
                toLocation, concurrentHashMap, firstSegment, isSourceAirportDomestic, isDestinationAirportDomestic);
            if (!nextDayConnections.isEmpty()) {
                mergedResult.addAll(nextDayConnections);
            }
        }

        return mergedResult;
    }

    /**
     * Finds intermediate connections by looking for routes through other locations.
     * This will find routes like VXE-CMN-BOM where CMN is an intermediate location.
     */
    private List<FlightItinerary> findIntermediateConnections(String fromLocation, String toLocation,
                                                               ConcurrentHashMap<String, List<FlightItinerary>> concurrentHashMap, FlightItinerary firstSegment,
                                                               boolean isSourceAirportDomestic, boolean isDestinationAirportDomestic) {

        List<FlightItinerary> intermediateConnections = new ArrayList<>();

        if (fromLocation != null && concurrentHashMap != null && concurrentHashMap.size() != 0) {

            // Look through all available locations to find intermediate connections
            for (String intermediateLocation : concurrentHashMap.keySet()) {
                // Skip if it's the same as fromLocation or toLocation
                if (intermediateLocation.equalsIgnoreCase(fromLocation) ||
                        intermediateLocation.equalsIgnoreCase(toLocation)) {
                    continue;
                }

                System.out.println("Checking intermediate location: " + intermediateLocation);

                // First check if there are flights from BOM to this intermediate location
                List<FlightItinerary> flightsFromBOM = concurrentHashMap.get(fromLocation);
                if (flightsFromBOM == null || flightsFromBOM.isEmpty()) {
                    System.out.println("No flights found from " + fromLocation + " to " + intermediateLocation);
                    continue;
                }

                // Find flights from BOM that go to this intermediate location
                List<FlightItinerary> bomToIntermediate = new ArrayList<>();
                for (FlightItinerary flight : flightsFromBOM) {
                    if (flight.getToLocation().equalsIgnoreCase(intermediateLocation)) {
                        bomToIntermediate.add(flight);
                    }
                }

                if (bomToIntermediate.isEmpty()) {
                    System.out.println("No flights found from " + fromLocation + " to " + intermediateLocation);
                    continue;
                }

                System.out.println("Found " + bomToIntermediate.size() + " flights from " + fromLocation + " to " + intermediateLocation);

                // Now check if there are flights from this intermediate location to VXE
                List<FlightItinerary> flightsFromIntermediate = concurrentHashMap.get(intermediateLocation);
                if (flightsFromIntermediate == null || flightsFromIntermediate.isEmpty()) {
                    System.out.println("No flights found from " + intermediateLocation + " to " + toLocation);
                    continue;
                }

                // Find flights from intermediate location that go to VXE
                List<FlightItinerary> intermediateToVXE = new ArrayList<>();
                for (FlightItinerary flight : flightsFromIntermediate) {
                    if (flight.getToLocation().equalsIgnoreCase(toLocation)) {
                        intermediateToVXE.add(flight);
                    }
                }

                if (intermediateToVXE.isEmpty()) {
                    System.out.println("No flights found from " + intermediateLocation + " to " + toLocation);
                    continue;
                }

                System.out.println("Found " + intermediateToVXE.size() + " flights from " + intermediateLocation + " to " + toLocation);

                // Now create merged itineraries for each valid combination
                for (FlightItinerary bomFlight : bomToIntermediate) {
                    for (FlightItinerary intermediateFlight : intermediateToVXE) {
                        
                        // Check time constraint between BOM flight arrival and intermediate flight departure
                        String arrivalTime = bomFlight.getJourneyList().get(
                                bomFlight.getJourneyList().size() - 1).getAirSegmentList().get(
                                bomFlight.getJourneyList().get(bomFlight.getJourneyList().size() - 1)
                                        .getAirSegmentList().size() - 1).getArrivalTime();
                        String departureTime = intermediateFlight.getJourneyList().get(0).getAirSegmentList().get(0).getDepartureTime();

                        long connectionTimeMinutes = calculateArrivalDeparture(arrivalTime, departureTime);

                        // Check if connection time is between 3-8 hours (180-480 minutes)
                        if (connectionTimeMinutes >= 180 && connectionTimeMinutes <= 900) {
                            System.out.println("Found intermediate connection: " + fromLocation + " -> " +
                                    intermediateLocation + " -> " + toLocation + " (connection time: " +
                                    connectionTimeMinutes + " minutes)");

                            // Create a merged itinerary for this intermediate connection
                            FlightItinerary mergedItinerary = SerializationUtils.clone(bomFlight);
                            mergedItinerary.setSplitTicket(true);
                            List<PricingInformation> newSplitPrices = new ArrayList<>();
                            newSplitPrices.add(bomFlight.getPricingInformation());
                            newSplitPrices.add(intermediateFlight.getPricingInformation());

                            // Add the intermediate flight's journey
                            mergedItinerary.getJourneyList().addAll(intermediateFlight.getJourneyList());

                            // Create total pricing
                            createTotalPricing(newSplitPrices, mergedItinerary);
                            mergedItinerary.setSplitPricingInformationList(newSplitPrices);

                            intermediateConnections.add(mergedItinerary);

                            // Limit the number of intermediate connections to prevent memory issues
                            if (intermediateConnections.size() >= 30) {
                                System.out.println("Reached maximum intermediate connections limit (30), stopping search");
                                return intermediateConnections;
                            }
                        } else {
                            System.out.println("Connection time " + connectionTimeMinutes + " minutes not in valid range (180-900) for " + 
                                    fromLocation + " -> " + intermediateLocation + " -> " + toLocation);
                        }
                    }
                }
            }
        }

        System.out.println("Total intermediate connections found: " + intermediateConnections.size());
        return intermediateConnections;
    }

    /**
     * Creates itineraries journey with time constraint between 3-8 hours.
     * Only adds itineraries where the connection time is between 3-8 hours.
     */
    private List<FlightItinerary> createItinerariesJourneyWithTimeConstraint(String fromLocation, String toLocation,
                                                                             ConcurrentHashMap<String, List<FlightItinerary>> concurrentHashMap, FlightItinerary firstSegment,
                                                                             boolean isSourceAirportDomestic, boolean isDestinationAirportDomestic) {

        List<FlightItinerary> newFlightItineraries = new ArrayList<>();

        if (fromLocation != null && concurrentHashMap != null && concurrentHashMap.size() != 0 &&
                concurrentHashMap.containsKey(fromLocation)) {

            List<FlightItinerary> flightItineraries = concurrentHashMap.get(fromLocation);

            for (FlightItinerary flightItinerary1 : flightItineraries) {
                if (!firstSegment.getToLocation().equalsIgnoreCase(toLocation)) {
                    String arrivalTime = firstSegment.getJourneyList().get(
                            firstSegment.getJourneyList().size() - 1).getAirSegmentList().get(
                            firstSegment.getJourneyList().get(firstSegment.getJourneyList().size() - 1)
                                    .getAirSegmentList().size() - 1).getArrivalTime();
                    String departureTime = flightItinerary1.getJourneyList().get(0).getAirSegmentList().get(0).getDepartureTime();

                    long connectionTimeMinutes = calculateArrivalDeparture(arrivalTime, departureTime);

                    // Check if connection time is between 3-8 hours (180-480 minutes)
                    if (connectionTimeMinutes >= 180 && connectionTimeMinutes <= 900) {
                        if (firstSegment.getJourneyList().get(0).getAirSegmentList().size() > 1) {
                            newFlightItineraries.add(flightItinerary1);
                            break;
                        }
                        newFlightItineraries.add(flightItinerary1);
                    }
                }
            }
        }
        return newFlightItineraries;
    }

    /**
     * Creates itineraries journey with next day constraint.
     * Looks for flights on the next day when no same-day connection is available.
     */
    private List<FlightItinerary> createItinerariesJourneyWithNextDayConstraint(String fromLocation, String toLocation,
                                                                                ConcurrentHashMap<String, List<FlightItinerary>> concurrentHashMap, FlightItinerary firstSegment,
                                                                                boolean isSourceAirportDomestic, boolean isDestinationAirportDomestic) {

        List<FlightItinerary> newFlightItineraries = new ArrayList<>();

        if (fromLocation != null && concurrentHashMap != null && concurrentHashMap.size() != 0 &&
                concurrentHashMap.containsKey(fromLocation)) {

            List<FlightItinerary> flightItineraries = concurrentHashMap.get(fromLocation);

            for (FlightItinerary flightItinerary1 : flightItineraries) {
                if (!firstSegment.getToLocation().equalsIgnoreCase(toLocation)) {
                    String arrivalTime = firstSegment.getJourneyList().get(
                            firstSegment.getJourneyList().size() - 1).getAirSegmentList().get(
                            firstSegment.getJourneyList().get(firstSegment.getJourneyList().size() - 1)
                                    .getAirSegmentList().size() - 1).getArrivalTime();
                    String departureTime = flightItinerary1.getJourneyList().get(0).getAirSegmentList().get(0).getDepartureTime();

                    long connectionTimeMinutes = calculateArrivalDeparture(arrivalTime, departureTime);

                    // For next day flights, we want connection time > 24 hours (1440 minutes)
                    // but still reasonable (let's say less than 48 hours - 2880 minutes)
                    if (connectionTimeMinutes >= 600 && connectionTimeMinutes <= 2880) {
                        if (firstSegment.getJourneyList().get(0).getAirSegmentList().size() > 1) {
                            newFlightItineraries.add(flightItinerary1);
                            break;
                        }
                        newFlightItineraries.add(flightItinerary1);
                    }
                }
            }
        }
        return newFlightItineraries;
    }

    public List<FlightItinerary> connectingSegments(String fromLocation, String toLocation, ConcurrentHashMap<String, List<FlightItinerary>> concurrentHashMap, boolean isSourceAirportDomestic) {
        List<FlightItinerary> flightItineraries = concurrentHashMap.get(fromLocation);
        List<FlightItinerary> mergedResult = new ArrayList<>();
        if (flightItineraries != null) {
            for (FlightItinerary flightItinerary : flightItineraries) {
                List<PricingInformation> splitPrices = new ArrayList<>();
                FlightItinerary clonedItinerary = SerializationUtils.clone(flightItinerary);
                clonedItinerary.setSplitTicket(true);
                splitPrices.add(flightItinerary.getPricingInformation());

                if (flightItinerary.getJourneyList().get(0).getNoOfStops() == 1 || flightItinerary.getJourneyList().get(0).getNoOfStops() == 0) {
                    for (FlightItinerary otherItinerary : flightItineraries) {
                        if (!flightItinerary.equals(otherItinerary)) {
                            FlightItinerary mergedItinerary = SerializationUtils.clone(clonedItinerary);
                            mergedItinerary.getJourneyList().addAll(otherItinerary.getJourneyList());
                            splitPrices.add(otherItinerary.getPricingInformation());
                            createTotalPricing(splitPrices, mergedItinerary);
                            mergedItinerary.setSplitPricingInformationList(splitPrices);
                            mergedResult.add(mergedItinerary);

                            // Limit the size of mergedResult to prevent memory overflow
                            if (mergedResult.size() >= 80) {
                                return mergedResult;
                            }
                        }
                    }
                } else {
                    if (isSourceAirportDomestic) {
                        List<FlightItinerary> newItineraries = createItinerariesJourney(flightItinerary.getToLocation(), toLocation, concurrentHashMap, clonedItinerary, 0, splitPrices);
                        for (FlightItinerary newItinerary : newItineraries) {
                            List<PricingInformation> newSplitPrices = new ArrayList<>(splitPrices);
                            newSplitPrices.add(newItinerary.getPricingInformation());
                            clonedItinerary.getJourneyList().addAll(newItinerary.getJourneyList());
                            createTotalPricing(newSplitPrices, clonedItinerary);
                            clonedItinerary.setSplitPricingInformationList(newSplitPrices);
                            if (isAllSegmentMerged(toLocation, clonedItinerary)) {
                                mergedResult.add(clonedItinerary);

                                // Limit the size of mergedResult to prevent memory overflow
                                if (mergedResult.size() >= 80) {
                                    return mergedResult;
                                }
                            }
                        }
                    } else {
                        creatingNewJourney(flightItinerary.getToLocation(), toLocation, concurrentHashMap, clonedItinerary, 0, splitPrices);
                        if (isAllSegmentMerged(toLocation, clonedItinerary)) {
                            createTotalPricing(splitPrices, clonedItinerary);
                            clonedItinerary.setSplitPricingInformationList(splitPrices);
                            mergedResult.add(clonedItinerary);

                            // Limit the size of mergedResult to prevent memory overflow
                            if (mergedResult.size() >= 80) {
                                return mergedResult;
                            }
                        }
                    }
                }
            }
        }
        return mergedResult;
    }

    /*
    public List<FlightItinerary> connectingSegments(String fromLocation, String toLocation, ConcurrentHashMap<String, List<FlightItinerary>> concurrentHashMap, boolean isSourceAirportDomestic) {
        List<FlightItinerary> flightItineraries = concurrentHashMap.get(fromLocation);
        List<FlightItinerary> mergedResult = new ArrayList<>();
        if (flightItineraries != null) {
            for (FlightItinerary flightItinerary : flightItineraries) {
                List<PricingInformation> splitPrices = new ArrayList<>();
                FlightItinerary clonedItinerary = SerializationUtils.clone(flightItinerary);
                clonedItinerary.setSplitTicket(true);
                splitPrices.add(flightItinerary.getPricingInformation());

                if (flightItinerary.getJourneyList().size() == 1) { // Single leg check
                    List<FlightItinerary> secondLegItineraries = concurrentHashMap.get(flightItinerary.getToLocation());
                    if (secondLegItineraries != null) {
                        for (FlightItinerary secondLeg : secondLegItineraries) {
                            if (!flightItinerary.equals(secondLeg)) {
                                FlightItinerary mergedItinerary = SerializationUtils.clone(clonedItinerary);
                                mergedItinerary.getJourneyList().addAll(secondLeg.getJourneyList());
                                splitPrices.add(secondLeg.getPricingInformation());
                                createTotalPricing(splitPrices, mergedItinerary);
                                mergedItinerary.setSplitPricingInformationList(splitPrices);
                                mergedResult.add(mergedItinerary);
                            }
                        }
                    }
                } else {
                    if (isSourceAirportDomestic) {
                        List<FlightItinerary> newItineraries = createItinerariesJourney(flightItinerary.getToLocation(), toLocation, concurrentHashMap, clonedItinerary, 0, splitPrices);
                        for (FlightItinerary newItinerary : newItineraries) {
                            List<PricingInformation> newSplitPrices = new ArrayList<>(splitPrices);
                            newSplitPrices.add(newItinerary.getPricingInformation());
                            clonedItinerary.getJourneyList().addAll(newItinerary.getJourneyList());
                            createTotalPricing(newSplitPrices, clonedItinerary);
                            clonedItinerary.setSplitPricingInformationList(newSplitPrices);
                            if (isAllSegmentMerged(toLocation, clonedItinerary)) {
                                mergedResult.add(clonedItinerary);
                            }
                        }
                    } else {
                        creatingNewJourney(flightItinerary.getToLocation(), toLocation, concurrentHashMap, clonedItinerary, 0, splitPrices);
                        if (isAllSegmentMerged(toLocation, clonedItinerary)) {
                            createTotalPricing(splitPrices, clonedItinerary);
                            clonedItinerary.setSplitPricingInformationList(splitPrices);
                            mergedResult.add(clonedItinerary);
                        }
                    }
                }
            }
        }

        // Remove null elements and handle null values in the comparator
        mergedResult.removeIf(Objects::isNull);
        mergedResult.sort(Comparator.comparing(itinerary -> {
            PricingInformation pricingInfo = itinerary.getPricingInformation();
            return pricingInfo != null && pricingInfo.getTotalPriceValue() != null ? pricingInfo.getTotalPriceValue() : BigDecimal.ZERO;
        }));

        // Limit the result to 150 entries
        if (mergedResult.size() > 150) {
            mergedResult = mergedResult.subList(0, 150);
        }

        return mergedResult;
    }

    */


    private List<FlightItinerary> createItinerariesJourney(String fromLocation, String toLocation, ConcurrentHashMap<String,List<FlightItinerary>> concurrentHashMap, FlightItinerary flightItinerary, int count, List<PricingInformation> spiltPrices) {
        List<FlightItinerary> newFlightItineraries = new ArrayList<>();
        if (fromLocation != null && concurrentHashMap != null && concurrentHashMap.size() != 0 && concurrentHashMap.containsKey(fromLocation)) {
            List<FlightItinerary> flightItineraries = null;
            flightItineraries = concurrentHashMap.get(fromLocation);
            for (FlightItinerary flightItinerary1: flightItineraries) {
                if (!flightItinerary.getToLocation().equalsIgnoreCase(toLocation)) {
                    String arrivalTime = flightItinerary.getJourneyList().get(flightItinerary.getJourneyList().size() - 1).getAirSegmentList().get(flightItinerary.getJourneyList().get(flightItinerary.getJourneyList().size() - 1).getAirSegmentList().size() - 1).getArrivalTime();
                    String departureTime = flightItinerary1.getJourneyList().get(0).getAirSegmentList().get(0).getDepartureTime();
                    if (calculateArrivalDeparture(arrivalTime, departureTime) > connectionTime) {
                        if(flightItinerary.getJourneyList().get(0).getAirSegmentList().size() > 1) {
                            newFlightItineraries.add(flightItinerary1);
                            break;
                        }
                        newFlightItineraries.add(flightItinerary1);
                    }
                }
            }
        }
        return newFlightItineraries;
    }

    private boolean isAllSegmentMerged(String toLocation, FlightItinerary flightItinerary) {
        boolean isMergedAll = true;
        List<Journey> journeyList = flightItinerary.getJourneyList();
        for (int i=0;i<journeyList.size()-1;i++) {
            if(!journeyList.get(i).getToLocation().equalsIgnoreCase(journeyList.get(i+1).getFromLocation())) {
                return false;
            }
        }
        return isMergedAll;
    }

    private void createTotalPricing(List<PricingInformation> pricingInformations, FlightItinerary flightItinerary) {
        if(pricingInformations.size()>0) {
            PricingInformation pricingInformation = SerializationUtils.clone(pricingInformations.get(0));
            for (int i=1; i<pricingInformations.size(); i++) {
                pricingInformation.setBasePrice(pricingInformation.getBasePrice().add(pricingInformations.get(i).getBasePrice()));
                pricingInformation.setAdtBasePrice(pricingInformation.getAdtBasePrice().add(pricingInformations.get(i).getAdtBasePrice()));
                if (pricingInformation.getChdTotalPrice()!=null) {
                    pricingInformation.setChdBasePrice(pricingInformation.getChdBasePrice().add(pricingInformations.get(i).getChdBasePrice()));
                    pricingInformation.setChdTotalPrice(pricingInformation.getChdTotalPrice().add(pricingInformations.get(i).getChdTotalPrice()));
                }

                if(pricingInformation.getInfTotalPrice()!=null) {
                    pricingInformation.setInfBasePrice(pricingInformation.getInfBasePrice().add(pricingInformations.get(i).getInfBasePrice()));
                    pricingInformation.setInfTotalPrice(pricingInformation.getInfTotalPrice().add(pricingInformations.get(i).getInfTotalPrice()));
                }
                pricingInformation.setAdtTotalPrice(pricingInformation.getAdtTotalPrice().add(pricingInformations.get(i).getAdtTotalPrice()));
                pricingInformation.getPaxFareDetailsList().get(0).getFareJourneyList().addAll(pricingInformations.get(i).getPaxFareDetailsList().get(0).getFareJourneyList());
                pricingInformation.setTax(pricingInformation.getTax().add(pricingInformations.get(i).getTax()));
                pricingInformation.setTotalPrice(pricingInformation.getTotalPrice().add(pricingInformations.get(i).getTotalPrice()));
                pricingInformation.setTotalPriceValue(pricingInformation.getTotalPriceValue().add(pricingInformations.get(i).getTotalPriceValue()));
                pricingInformation.getPassengerTaxes().addAll(pricingInformations.get(i).getPassengerTaxes());
            }
            flightItinerary.setSeamanPricingInformation(pricingInformation);
            flightItinerary.setPricingInformation(new PricingInformation());
        }
    }

    public  List<FlightItinerary> creatingJourneyWithNonStop(String fromLocation, String toLocation, ConcurrentHashMap<String,List<FlightItinerary>> concurrentHashMap, FlightItinerary flightItinerary, int count, List<PricingInformation> spiltPrices) {
        List<FlightItinerary> newFlightItineraries = new ArrayList<>();
        if(fromLocation !=null && concurrentHashMap!=null && concurrentHashMap.size() !=0 && concurrentHashMap.containsKey(fromLocation)) {
            System.out.println("toLocation "+toLocation);
            System.out.println("fromLocation "+fromLocation);
            List<FlightItinerary> flightItineraries = null;
            flightItineraries = concurrentHashMap.get(fromLocation);
            if (flightItineraries != null && flightItineraries.size() > 0) {
                boolean isOriginDomestic = false;
                if(flightItinerary.getJourneyList().get(0).getAirSegmentList().size() == 1) {
                    List<FlightItinerary> flightItineraryList = concurrentHashMap.get(fromLocation);
                    CopyOnWriteArrayList<FlightItinerary> copyOnWriteArrayList = new CopyOnWriteArrayList<>(flightItineraryList);
                    flightItineraries = SerializationUtils.clone(copyOnWriteArrayList);
                    isOriginDomestic=true;
                } else {
                    flightItineraries = concurrentHashMap.get(fromLocation);
                }
                int k=0;
                int cou=0;
                boolean isClonedAlready = false;
                FlightItinerary flightItinerary2 = null;
                for (FlightItinerary flightItinerary1: flightItineraries) {
                    if (!flightItinerary.getToLocation().equalsIgnoreCase(toLocation)) {
                        String arrivalTime = flightItinerary.getJourneyList().get(flightItinerary.getJourneyList().size() - 1).getAirSegmentList().get(flightItinerary.getJourneyList().get(flightItinerary.getJourneyList().size() - 1).getAirSegmentList().size() - 1).getArrivalTime();
                        String departureTime = flightItinerary1.getJourneyList().get(0).getAirSegmentList().get(0).getDepartureTime();
                        if (calculateArrivalDeparture(arrivalTime, departureTime) > connectionTime) {
                            k++;
                            System.out.println("flightItinerary "+k);
                            if (isOriginDomestic) {
                                if(!isClonedAlready) {
                                    flightItinerary2 = SerializationUtils.clone(flightItinerary);
                                    isClonedAlready = true;
                                }
                                spiltPrices.add(flightItinerary1.getPricingInformation());
                                flightItinerary.getJourneyList().addAll(flightItinerary1.getJourneyList());
                                newFlightItineraries.add(flightItinerary);
                                flightItineraries.remove(flightItinerary1);
                                flightItinerary = SerializationUtils.clone(flightItinerary2);;
                            } else {
                                spiltPrices.add(flightItinerary1.getPricingInformation());
                                flightItinerary.getJourneyList().addAll(flightItinerary1.getJourneyList());
                                newFlightItineraries.add(flightItinerary);
                                break;
                            }
                        }
                    } else {
                        System.out.println("Merged");
                    }
                    cou++;
                }
            }
        }
        return newFlightItineraries;
    }

    public void creatingNewJourney(String fromLocation, String toLocation, ConcurrentHashMap<String,List<FlightItinerary>> concurrentHashMap, FlightItinerary flightItinerary, int count, List<PricingInformation> spiltPrices) {
        if(fromLocation !=null && concurrentHashMap!=null && concurrentHashMap.size() !=0 && concurrentHashMap.containsKey(fromLocation)) {
            System.out.println("toLocation merge "+toLocation);
            System.out.println("fromLocation merge "+fromLocation);
            List<FlightItinerary> flightItineraries = concurrentHashMap.get(fromLocation);
            System.out.println("flightItineraries merge size "+flightItineraries.size());
            if (flightItineraries != null && flightItineraries.size() > 0) {
                for (FlightItinerary flightItinerary1: flightItineraries) {
                    /*FlightItinerary flightItinerary1 = null;
                    if (flightItineraries.size() - 1 > count) {
                        flightItinerary1 = flightItineraries.get(count);
                    } else {
                        flightItinerary1 = flightItineraries.get(0);
                    }*/
                    if (!flightItinerary.getToLocation().equalsIgnoreCase(toLocation)) {
                        String arrivalTime = flightItinerary.getJourneyList().get(flightItinerary.getJourneyList().size() - 1).getAirSegmentList().get(flightItinerary.getJourneyList().get(flightItinerary.getJourneyList().size() - 1).getAirSegmentList().size() - 1).getArrivalTime();
                        String departureTime = flightItinerary1.getJourneyList().get(0).getAirSegmentList().get(0).getDepartureTime();
                        if (calculateArrivalDeparture(arrivalTime, departureTime) > connectionTime) {
                            spiltPrices.add(flightItinerary1.getPricingInformation());
                            System.out.println("is seamen "+flightItinerary1.getJourneyList().get(0).isSeamen());
                            flightItinerary.getJourneyList().addAll(flightItinerary1.getJourneyList());
                            flightItineraries.remove(flightItinerary1);
                            break;
                        }
                    } else {
                        System.out.println("Merged");
                    }
                }
            }
        }
    }

    public List<FlightItinerary> creatingNonSeamenJourney(String fromLocation, String toLocation, ConcurrentHashMap<String,List<FlightItinerary>> concurrentHashMap, FlightItinerary flightItinerary, int count, List<PricingInformation> spiltPrices) {
        List<FlightItinerary> flightItineraryList = new ArrayList<>();
        if(fromLocation !=null && concurrentHashMap!=null && concurrentHashMap.size() !=0 && concurrentHashMap.containsKey(fromLocation)) {
            System.out.println(" creatingNonSeamenJourney toLocation merge "+toLocation);
            System.out.println(" creatingNonSeamenJourney fromLocation merge "+fromLocation);
            List<FlightItinerary> flightItineraries = concurrentHashMap.get(fromLocation);
            System.out.println(" creatingNonSeamenJourney flightItineraries merge size "+flightItineraries.size());
            if (flightItineraries != null && flightItineraries.size() > 0) {
                int i=0;
                for (FlightItinerary flightItinerary1: flightItineraries) {
                    FlightItinerary orignalIterary = SerializationUtils.clone(flightItinerary);
                    if (!flightItinerary.getToLocation().equalsIgnoreCase(toLocation)) {
                        String arrivalTime = flightItinerary.getJourneyList().get(flightItinerary.getJourneyList().size() - 1).getAirSegmentList().get(flightItinerary.getJourneyList().get(flightItinerary.getJourneyList().size() - 1).getAirSegmentList().size() - 1).getArrivalTime();
                        String departureTime = flightItinerary1.getJourneyList().get(0).getAirSegmentList().get(0).getDepartureTime();
                        if (calculateArrivalDeparture(arrivalTime, departureTime) > connectionTime) {
                            spiltPrices.add(flightItinerary1.getPricingInformation());
                            orignalIterary.getJourneyList().addAll(flightItinerary1.getJourneyList());
                            flightItineraryList.add(orignalIterary);
                        }
                    } else {
                        System.out.println("Merged");
                    }
                    i++;
                }
            }
        }
        return flightItineraryList;
    }

    public void creatingJourney(String fromLocation, String toLocation, ConcurrentHashMap<String,List<FlightItinerary>> concurrentHashMap, FlightItinerary flightItinerary, int count, List<PricingInformation> spiltPrices) {
        if(fromLocation !=null && concurrentHashMap!=null && concurrentHashMap.size() !=0 && concurrentHashMap.containsKey(fromLocation)) {
            List<FlightItinerary> flightItineraries = concurrentHashMap.get(fromLocation);
            if (flightItineraries != null && flightItineraries.size() > 0) {
                FlightItinerary flightItinerary1 = null;
                if (flightItineraries.size() - 1 > count) {
                    flightItinerary1 = flightItineraries.get(count);
                } else {
                    flightItinerary1 = flightItineraries.get(0);
                }
                if (!flightItinerary.getToLocation().equalsIgnoreCase(toLocation)) {
                    spiltPrices.add(flightItinerary1.getPricingInformation());
                    System.out.println("Pre Loc " + flightItinerary.getJourneyList().get(flightItinerary.getJourneyList().size() - 1).getAirSegmentList().get(flightItinerary.getJourneyList().get(flightItinerary.getJourneyList().size() - 1).getAirSegmentList().size() - 1).getToLocation());
                    System.out.println("Pre Time " + flightItinerary.getJourneyList().get(flightItinerary.getJourneyList().size() - 1).getAirSegmentList().get(flightItinerary.getJourneyList().get(flightItinerary.getJourneyList().size() - 1).getAirSegmentList().size() - 1).getArrivalTime());
                    System.out.println("Pre Date " + flightItinerary.getJourneyList().get(flightItinerary.getJourneyList().size() - 1).getAirSegmentList().get(flightItinerary.getJourneyList().get(flightItinerary.getJourneyList().size() - 1).getAirSegmentList().size() - 1).getArrivalDate());

                    System.out.println("Curr Loc " + flightItinerary1.getJourneyList().get(0).getAirSegmentList().get(0).getToLocation());
                    System.out.println("Curr Time " + flightItinerary1.getJourneyList().get(0).getAirSegmentList().get(0).getDepartureTime());
                    System.out.println("Curr Date " + flightItinerary1.getJourneyList().get(0).getAirSegmentList().get(0).getDepartureDate());
                    String arrivalTime  = flightItinerary.getJourneyList().get(flightItinerary.getJourneyList().size() - 1).getAirSegmentList().get(flightItinerary.getJourneyList().get(flightItinerary.getJourneyList().size() - 1).getAirSegmentList().size() - 1).getArrivalTime();
                    String departureTime = flightItinerary1.getJourneyList().get(0).getAirSegmentList().get(0).getDepartureTime();
                    if(calculateArrivalDeparture(arrivalTime,departureTime) > connectionTime) {
                        flightItinerary.getJourneyList().addAll(flightItinerary1.getJourneyList());
                    } else {
                        creatingJourney(flightItinerary1.getToLocation(), toLocation, concurrentHashMap, flightItinerary, count+1, spiltPrices);
                    }
                    creatingJourney(flightItinerary1.getToLocation(), toLocation, concurrentHashMap, flightItinerary, count, spiltPrices);
                    flightItinerary.setToLocation(flightItinerary1.getToLocation());
                } else {
                    System.out.println("Merged");
                }
            }
        }
    }

    private long calculateArrivalDeparture(String arrivalTime, String departureDate) {
        long layOverTime = 0l;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        LocalDateTime arrivalDateTime = LocalDateTime.parse(arrivalTime, formatter);
        LocalDateTime departureDateTime = LocalDateTime.parse(departureDate, formatter);
        Duration duration = Duration.between(arrivalDateTime,departureDateTime);
        layOverTime = duration.toMinutes();
        System.out.println("layOverTime "+layOverTime);
        return layOverTime;
    }
}
