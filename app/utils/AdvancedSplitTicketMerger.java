package utils;

import com.compassites.model.*;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AdvancedSplitTicketMerger {

    static Logger logger = LoggerFactory.getLogger("gds");

    /**
     * Advanced split ticket merger that finds ALL combinations of flights
     * where the connection time is between 3-8 hours.
     * Prioritizes flights with 0 stops for better quality connections.
     * 
     * @param fromLocation Starting location (e.g., VXE)
     * @param toLocation Final destination (e.g., BOM)
     * @param concurrentHashMap Map containing flight itineraries by location
     * @param isSourceAirportDomestic Whether source airport is domestic
     * @param isDestinationAirportDomestic Whether destination airport is domestic
     * @return List of all valid merged flight itineraries meeting the time constraint, prioritized by stops
     */
    public List<FlightItinerary> mergeAllSplitTicketCombinations(String fromLocation, String toLocation,
            ConcurrentHashMap<String, List<FlightItinerary>> concurrentHashMap,
            boolean isSourceAirportDomestic, boolean isDestinationAirportDomestic) {
        
        List<FlightItinerary> allMergedResults = new ArrayList<>();
        
        if (fromLocation == null || toLocation == null || concurrentHashMap == null || concurrentHashMap.isEmpty()) {
            logger.warn("Invalid parameters provided for mergeAllSplitTicketCombinations");
            return allMergedResults;
        }
        
        // Get all flights from the starting location
        List<FlightItinerary> firstSegmentFlights = concurrentHashMap.get(fromLocation);
        if (firstSegmentFlights == null || firstSegmentFlights.isEmpty()) {
            logger.info("No flights found from " + fromLocation);
            return allMergedResults;
        }
        
        logger.info("Starting advanced split ticket merge from " + fromLocation + " to " + toLocation);
        logger.info("Found " + firstSegmentFlights.size() + " first segment flights");
        
        // Sort first segment flights by priority (0 stops first, then 1 stop, etc.)
        firstSegmentFlights.sort((f1, f2) -> {
            int stops1 = getTotalStops(f1);
            int stops2 = getTotalStops(f2);
            return Integer.compare(stops1, stops2); // Lower stops get higher priority
        });
        
        logger.info("First segment flights sorted by priority (0 stops first)");
        
        // Try to find direct connections first
        List<FlightItinerary> directConnections = findDirectConnections(fromLocation, toLocation, 
            concurrentHashMap, firstSegmentFlights, isSourceAirportDomestic, isDestinationAirportDomestic);
        allMergedResults.addAll(directConnections);
        
        // Then find intermediate connections
        List<FlightItinerary> intermediateConnections = findAllIntermediateConnections(fromLocation, toLocation, 
            concurrentHashMap, firstSegmentFlights, isSourceAirportDomestic, isDestinationAirportDomestic);
        allMergedResults.addAll(intermediateConnections);
        
        // Sort final results by total stops (0 stops first, then 1 stop, etc.)
        allMergedResults.sort((f1, f2) -> {
            int totalStops1 = getTotalStops(f1);
            int totalStops2 = getTotalStops(f2);
            return Integer.compare(totalStops1, totalStops2); // Lower total stops get higher priority
        });
        
        logger.info("Total merged results found: " + allMergedResults.size());
        logger.info("Results sorted by priority (0 stops first)");
        return allMergedResults;
    }
    
    /**
     * Finds direct connections from fromLocation to toLocation with 3-8 hour connection time.
     * Checks ALL combinations of first and second segment flights.
     * Prioritizes flights with 0 stops.
     */
    private List<FlightItinerary> findDirectConnections(String fromLocation, String toLocation,
            ConcurrentHashMap<String, List<FlightItinerary>> concurrentHashMap,
            List<FlightItinerary> firstSegmentFlights,
            boolean isSourceAirportDomestic, boolean isDestinationAirportDomestic) {
        
        List<FlightItinerary> directConnections = new ArrayList<>();
        
        // Get all flights from the destination location (for second segment)
        List<FlightItinerary> secondSegmentFlights = concurrentHashMap.get(toLocation);
        if (secondSegmentFlights == null || secondSegmentFlights.isEmpty()) {
            logger.info("No second segment flights found for " + toLocation);
            return directConnections;
        }
        
        // Sort second segment flights by priority (0 stops first, then 1 stop, etc.)
        secondSegmentFlights.sort((f1, f2) -> {
            int stops1 = getTotalStops(f1);
            int stops2 = getTotalStops(f2);
            return Integer.compare(stops1, stops2); // Lower stops get higher priority
        });
        
        logger.info("Found " + secondSegmentFlights.size() + " second segment flights (sorted by priority)");
        
        // Check ALL combinations of first and second segment flights
        for (FlightItinerary firstFlight : firstSegmentFlights) {
            for (FlightItinerary secondFlight : secondSegmentFlights) {
                
                // Skip if it's the same flight
                if (firstFlight.equals(secondFlight)) {
                    continue;
                }
                
                // Check if the first flight's destination matches the second flight's origin
                String firstFlightDestination = getLastDestination(firstFlight);
                String secondFlightOrigin = getFirstOrigin(secondFlight);
                
                if (!firstFlightDestination.equalsIgnoreCase(secondFlightOrigin)) {
                    continue;
                }
                
                // Calculate connection time
                long connectionTime = calculateConnectionTime(firstFlight, secondFlight);
                
                // Check if connection time is between 3-8 hours (180-480 minutes)
                if (connectionTime >= 180 && connectionTime <= 480) {
                    int firstFlightStops = getTotalStops(firstFlight);
                    int secondFlightStops = getTotalStops(secondFlight);
                    int totalStops = firstFlightStops + secondFlightStops;
                    
                    logger.info("Found valid direct connection: " + fromLocation + " -> " + 
                            firstFlightDestination + " -> " + toLocation + " (connection time: " + 
                            connectionTime + " minutes, total stops: " + totalStops + 
                            " [first: " + firstFlightStops + ", second: " + secondFlightStops + "])");
                    
                    // Create merged itinerary
                    FlightItinerary mergedItinerary = createMergedItinerary(firstFlight, secondFlight);
                    if (mergedItinerary != null) {
                        directConnections.add(mergedItinerary);
                    }
                }
            }
        }
        
        logger.info("Found " + directConnections.size() + " direct connections");
        return directConnections;
    }
    
    /**
     * Finds ALL intermediate connections through other locations.
     * Checks ALL combinations of flights through each intermediate location.
     * Prioritizes flights with 0 stops.
     */
    private List<FlightItinerary> findAllIntermediateConnections(String fromLocation, String toLocation,
            ConcurrentHashMap<String, List<FlightItinerary>> concurrentHashMap,
            List<FlightItinerary> firstSegmentFlights,
            boolean isSourceAirportDomestic, boolean isDestinationAirportDomestic) {
        
        List<FlightItinerary> allIntermediateConnections = new ArrayList<>();
        
        // Check each location as a potential intermediate stop
        for (String intermediateLocation : concurrentHashMap.keySet()) {
            // Skip if it's the same as fromLocation or toLocation
            if (intermediateLocation.equalsIgnoreCase(fromLocation) || 
                intermediateLocation.equalsIgnoreCase(toLocation)) {
                continue;
            }
            
            logger.info("Checking intermediate location: " + intermediateLocation);
            
            // Get flights from intermediate location to final destination
            List<FlightItinerary> intermediateToDestination = concurrentHashMap.get(intermediateLocation);
            if (intermediateToDestination == null || intermediateToDestination.isEmpty()) {
                continue;
            }
            
            // Sort intermediate flights by priority (0 stops first, then 1 stop, etc.)
            intermediateToDestination.sort((f1, f2) -> {
                int stops1 = getTotalStops(f1);
                int stops2 = getTotalStops(f2);
                return Integer.compare(stops1, stops2); // Lower stops get higher priority
            });
            
            // Check ALL combinations of first segment flights
            for (FlightItinerary firstFlight : firstSegmentFlights) {
                String firstFlightDestination = getLastDestination(firstFlight);
                
                // Check if first flight goes to this intermediate location
                if (!firstFlightDestination.equalsIgnoreCase(intermediateLocation)) {
                    continue;
                }
                
                // Check ALL combinations of intermediate flights
                for (FlightItinerary intermediateFlight : intermediateToDestination) {
                    String intermediateFlightDestination = getLastDestination(intermediateFlight);
                    
                    // Check if intermediate flight goes to final destination
                    if (!intermediateFlightDestination.equalsIgnoreCase(toLocation)) {
                        continue;
                    }
                    
                    // Calculate connection time
                    long connectionTime = calculateConnectionTime(firstFlight, intermediateFlight);
                    
                    // Check if connection time is between 3-8 hours (180-480 minutes)
                    if (connectionTime >= 180 && connectionTime <= 480) {
                        int firstFlightStops = getTotalStops(firstFlight);
                        int intermediateFlightStops = getTotalStops(intermediateFlight);
                        int totalStops = firstFlightStops + intermediateFlightStops;
                        
                        logger.info("Found valid intermediate connection: " + fromLocation + " -> " + 
                                intermediateLocation + " -> " + toLocation + " (connection time: " + 
                                connectionTime + " minutes, total stops: " + totalStops + 
                                " [first: " + firstFlightStops + ", intermediate: " + intermediateFlightStops + "])");
                        
                        // Create merged itinerary
                        FlightItinerary mergedItinerary = createMergedItinerary(firstFlight, intermediateFlight);
                        if (mergedItinerary != null) {
                            allIntermediateConnections.add(mergedItinerary);
                        }
                        
                        // Limit to prevent memory issues
                        if (allIntermediateConnections.size() >= 100) {
                            logger.info("Reached maximum intermediate connections limit (100)");
                            return allIntermediateConnections;
                        }
                    }
                }
            }
        }
        
        logger.info("Found " + allIntermediateConnections.size() + " intermediate connections");
        return allIntermediateConnections;
    }
    
    /**
     * Gets the destination of the last air segment in the last journey.
     */
    private String getLastDestination(FlightItinerary flightItinerary) {
        if (flightItinerary.getJourneyList() == null || flightItinerary.getJourneyList().isEmpty()) {
            return null;
        }
        
        Journey lastJourney = flightItinerary.getJourneyList().get(flightItinerary.getJourneyList().size() - 1);
        if (lastJourney.getAirSegmentList() == null || lastJourney.getAirSegmentList().isEmpty()) {
            return null;
        }
        
        AirSegmentInformation lastSegment = lastJourney.getAirSegmentList().get(lastJourney.getAirSegmentList().size() - 1);
        return lastSegment.getToLocation();
    }
    
    /**
     * Gets the origin of the first air segment in the first journey.
     */
    private String getFirstOrigin(FlightItinerary flightItinerary) {
        if (flightItinerary.getJourneyList() == null || flightItinerary.getJourneyList().isEmpty()) {
            return null;
        }
        
        Journey firstJourney = flightItinerary.getJourneyList().get(0);
        if (firstJourney.getAirSegmentList() == null || firstJourney.getAirSegmentList().isEmpty()) {
            return null;
        }
        
        AirSegmentInformation firstSegment = firstJourney.getAirSegmentList().get(0);
        return firstSegment.getFromLocation();
    }
    
    /**
     * Calculates the connection time between two flights in minutes.
     */
    private long calculateConnectionTime(FlightItinerary firstFlight, FlightItinerary secondFlight) {
        try {
            String arrivalTime = getLastArrivalTime(firstFlight);
            String departureTime = getFirstDepartureTime(secondFlight);
            
            if (arrivalTime == null || departureTime == null) {
                return -1;
            }
            
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            java.time.LocalDateTime arrivalDateTime = java.time.LocalDateTime.parse(arrivalTime, formatter);
            java.time.LocalDateTime departureDateTime = java.time.LocalDateTime.parse(departureTime, formatter);
            java.time.Duration duration = java.time.Duration.between(arrivalDateTime, departureDateTime);
            
            return duration.toMinutes();
        } catch (Exception e) {
            logger.error("Error calculating connection time: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Gets the arrival time of the last air segment in the last journey.
     */
    private String getLastArrivalTime(FlightItinerary flightItinerary) {
        if (flightItinerary.getJourneyList() == null || flightItinerary.getJourneyList().isEmpty()) {
            return null;
        }
        
        Journey lastJourney = flightItinerary.getJourneyList().get(flightItinerary.getJourneyList().size() - 1);
        if (lastJourney.getAirSegmentList() == null || lastJourney.getAirSegmentList().isEmpty()) {
            return null;
        }
        
        AirSegmentInformation lastSegment = lastJourney.getAirSegmentList().get(lastJourney.getAirSegmentList().size() - 1);
        return lastSegment.getArrivalTime();
    }
    
    /**
     * Gets the departure time of the first air segment in the first journey.
     */
    private String getFirstDepartureTime(FlightItinerary flightItinerary) {
        if (flightItinerary.getJourneyList() == null || flightItinerary.getJourneyList().isEmpty()) {
            return null;
        }
        
        Journey firstJourney = flightItinerary.getJourneyList().get(0);
        if (firstJourney.getAirSegmentList() == null || firstJourney.getAirSegmentList().isEmpty()) {
            return null;
        }
        
        AirSegmentInformation firstSegment = firstJourney.getAirSegmentList().get(0);
        return firstSegment.getDepartureTime();
    }
    
    /**
     * Creates a merged itinerary from two separate flights.
     */
    private FlightItinerary createMergedItinerary(FlightItinerary firstFlight, FlightItinerary secondFlight) {
        try {
            // Clone the first flight
            FlightItinerary mergedItinerary = SerializationUtils.clone(firstFlight);
            mergedItinerary.setSplitTicket(true);
            
            // Create split pricing information
            List<PricingInformation> splitPricing = new ArrayList<>();
            splitPricing.add(firstFlight.getPricingInformation());
            splitPricing.add(secondFlight.getPricingInformation());
            mergedItinerary.setSplitPricingInformationList(splitPricing);
            
            // Add the second flight's journey
            mergedItinerary.getJourneyList().addAll(secondFlight.getJourneyList());
            
            // Create total pricing
            createTotalPricing(splitPricing, mergedItinerary);
            
            return mergedItinerary;
        } catch (Exception e) {
            logger.error("Error creating merged itinerary: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Creates total pricing from split pricing information.
     */
    private void createTotalPricing(List<PricingInformation> splitPricing, FlightItinerary mergedItinerary) {
        if (splitPricing == null || splitPricing.isEmpty()) {
            return;
        }
        
        try {
            PricingInformation totalPricing = SerializationUtils.clone(splitPricing.get(0));
            
            // Add all pricing information
            for (int i = 1; i < splitPricing.size(); i++) {
                PricingInformation currentPricing = splitPricing.get(i);
                
                if (totalPricing.getBasePrice() != null && currentPricing.getBasePrice() != null) {
                    totalPricing.setBasePrice(totalPricing.getBasePrice().add(currentPricing.getBasePrice()));
                }
                
                if (totalPricing.getAdtBasePrice() != null && currentPricing.getAdtBasePrice() != null) {
                    totalPricing.setAdtBasePrice(totalPricing.getAdtBasePrice().add(currentPricing.getAdtBasePrice()));
                }
                
                if (totalPricing.getAdtTotalPrice() != null && currentPricing.getAdtTotalPrice() != null) {
                    totalPricing.setAdtTotalPrice(totalPricing.getAdtTotalPrice().add(currentPricing.getAdtTotalPrice()));
                }
                
                if (totalPricing.getTotalPrice() != null && currentPricing.getTotalPrice() != null) {
                    totalPricing.setTotalPrice(totalPricing.getTotalPrice().add(currentPricing.getTotalPrice()));
                }
                
                if (totalPricing.getTotalPriceValue() != null && currentPricing.getTotalPriceValue() != null) {
                    totalPricing.setTotalPriceValue(totalPricing.getTotalPriceValue().add(currentPricing.getTotalPriceValue()));
                }
                
                if (totalPricing.getTax() != null && currentPricing.getTax() != null) {
                    totalPricing.setTax(totalPricing.getTax().add(currentPricing.getTax()));
                }
            }
            
            mergedItinerary.setPricingInformation(totalPricing);
            
        } catch (Exception e) {
            logger.error("Error creating total pricing: " + e.getMessage());
        }
    }

    /**
     * Gets the total number of stops for a flight itinerary.
     * Returns the sum of stops across all journeys.
     */
    private int getTotalStops(FlightItinerary flightItinerary) {
        if (flightItinerary.getJourneyList() == null || flightItinerary.getJourneyList().isEmpty()) {
            return 0;
        }
        
        int totalStops = 0;
        for (Journey journey : flightItinerary.getJourneyList()) {
            if (journey.getNoOfStops() != null) {
                totalStops += journey.getNoOfStops();
            }
        }
        
        return totalStops;
    }
}
