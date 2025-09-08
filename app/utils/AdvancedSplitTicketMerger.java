package utils;

import com.compassites.model.*;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import play.libs.Json;
import services.ConfigurationMasterService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AdvancedSplitTicketMerger {

    static Logger logger = LoggerFactory.getLogger("gds");

    public List<FlightItinerary> mergeAllSplitTicketCombinations(String fromLocation, String toLocation,
            ConcurrentHashMap<String, List<FlightItinerary>> concurrentHashMap,
            boolean isSourceAirportDomestic, boolean isDestinationAirportDomestic) {
        
        List<FlightItinerary> allMergedResults = new ArrayList<>();
        final int MAX_RESULTS = 100; // Limit to prevent system crashes
        
        if (fromLocation == null || toLocation == null || concurrentHashMap == null || concurrentHashMap.isEmpty()) {
            logger.warn("Invalid parameters provided for mergeAllSplitTicketCombinations");
            logger.warn("fromLocation: " + fromLocation + ", toLocation: " + toLocation + 
                       ", concurrentHashMap: " + (concurrentHashMap == null ? "null" : "empty"));
            return allMergedResults;
        }
        //logger.info("all concurrentHashMap details: "+ Json.toJson(concurrentHashMap));
        logger.info("=== STARTING SPLIT TICKET MERGE ANALYSIS ===");
        logger.info("From: " + fromLocation + " To: " + toLocation);
        logger.info("Available locations in map: " + concurrentHashMap.keySet());
        logger.info("Total locations available: " + concurrentHashMap.size());
        logger.info("Maximum results allowed: " + MAX_RESULTS);
        
        // Get all flights from the starting location
        List<FlightItinerary> firstSegmentFlights = concurrentHashMap.get(fromLocation);
        if (firstSegmentFlights == null || firstSegmentFlights.isEmpty()) {
            logger.error("NO FLIGHTS FOUND from " + fromLocation + " - This is why no results!");
            logger.error("Available locations: " + concurrentHashMap.keySet());
            logger.error("Check if " + fromLocation + " exists in the flight data");
            logger.error("=== NO RESULTS POSSIBLE - No flights from source location ===");
            return allMergedResults;
        }
        
        logger.info("First segment flights found: " + firstSegmentFlights.size() + " from " + fromLocation);
        
        // Sort first segment flights by priority (0 stops first, then 1 stop, etc.)
        firstSegmentFlights.sort((f1, f2) -> {
            int stops1 = getTotalStops(f1);
            int stops2 = getTotalStops(f2);
            return Integer.compare(stops1, stops2); // Lower stops get higher priority
        });
        
        logger.info("First segment flights sorted by priority (0 stops first)");
        
        // Try to find direct connections first
        List<FlightItinerary> directConnections = findDirectConnections(fromLocation, toLocation, 
            concurrentHashMap, firstSegmentFlights, isSourceAirportDomestic, isDestinationAirportDomestic, MAX_RESULTS);
        allMergedResults.addAll(directConnections);
        
        logger.info("Direct connections found: " + directConnections.size());
        
        // Check if we've reached the limit
        if (allMergedResults.size() >= MAX_RESULTS) {
            logger.info("Reached maximum results limit (" + MAX_RESULTS + ") with direct connections only");
            logger.info("Skipping intermediate connections to respect limit");
        } else {
            // Then find intermediate connections
            int remainingSlots = MAX_RESULTS - allMergedResults.size();
            List<FlightItinerary> intermediateConnections = findAllIntermediateConnections(fromLocation, toLocation, 
                concurrentHashMap, firstSegmentFlights, isSourceAirportDomestic, isDestinationAirportDomestic, remainingSlots);
            allMergedResults.addAll(intermediateConnections);
            
            logger.info("Intermediate connections found: " + intermediateConnections.size());
        }
        
        // Sort final results by total stops (0 stops first, then 1 stop, etc.)
        allMergedResults.sort((f1, f2) -> {
            int totalStops1 = getTotalStops(f1);
            int totalStops2 = getTotalStops(f2);
            return Integer.compare(totalStops1, totalStops2); // Lower total stops get higher priority
        });
        
        // Final analysis and logging
        if (allMergedResults.isEmpty()) {
            logger.warn("=== NO RESULTS FOUND - DETAILED ANALYSIS ===");
            logger.warn("Route: " + fromLocation + " -> " + toLocation);
            logger.warn("First segment flights available: " + firstSegmentFlights.size());
            logger.warn("Direct connections attempted: " + directConnections.size());
            int firstLegCount = concurrentHashMap.get(fromLocation) != null ? concurrentHashMap.get(fromLocation).size() : 0;
            int secondLegCount = concurrentHashMap.get(toLocation) != null ? concurrentHashMap.get(toLocation).size() : 0;
            logger.warn("First-leg options from " + fromLocation + ": " + firstLegCount + ", second-leg options from " + toLocation + ": " + secondLegCount);
            logger.warn("Intermediates considered: " + (Math.max(concurrentHashMap.keySet().size() - 2, 0)) + " -> " + concurrentHashMap.keySet());
            logger.warn("Possible reasons for no results:");
            logger.warn("1. No flights from " + fromLocation + " to any intermediate location");
            logger.warn("2. No flights from intermediate locations to " + toLocation);
            logger.warn("3. Connection times not between 3-8 hours");
            logger.warn("4. Route combination not possible");
            logger.warn("5. Data mismatch between segments");
            logger.warn("=== NO RESULTS POSSIBLE FOR THIS ROUTE ===");
        } else {
            logger.info("Total merged results found: " + allMergedResults.size() + " (limited to " + MAX_RESULTS + ")");
            logger.info("Results sorted by priority (0 stops first)");
            
            if (allMergedResults.size() >= MAX_RESULTS) {
                logger.warn("WARNING: Results were limited to " + MAX_RESULTS + " to prevent system crashes");
                logger.warn("There may be more valid combinations available");
            }
        }
        
        return allMergedResults;
    }
    

    private List<FlightItinerary> findDirectConnections(String fromLocation, String toLocation,
            ConcurrentHashMap<String, List<FlightItinerary>> concurrentHashMap,
            List<FlightItinerary> firstSegmentFlights,
            boolean isSourceAirportDomestic, boolean isDestinationAirportDomestic, int maxResults) {
        
        List<FlightItinerary> directConnections = new ArrayList<>();
        logger.info("=== ANALYZING DIRECT CONNECTIONS ===");
        logger.info("Looking for: " + fromLocation + " -> [Intermediate] -> " + toLocation);
        logger.info("Maximum results allowed: " + maxResults);
        
        // Collect all possible second-segment flights (from any origin)
        List<FlightItinerary> secondSegmentFlights = new ArrayList<>();
        for (List<FlightItinerary> list : concurrentHashMap.values()) {
            if (list != null && !list.isEmpty()) {
                secondSegmentFlights.addAll(list);
            }
        }
        if (secondSegmentFlights.isEmpty()) {
            logger.error("NO SECOND SEGMENT FLIGHTS available in provided data");
            logger.error("Available locations: " + concurrentHashMap.keySet());
            return directConnections;
        }
        
        logger.info("Second segment flights found (all origins): " + secondSegmentFlights.size());
        
        // Sort second segment flights by priority (0 stops first, then 1 stop, etc.)
        secondSegmentFlights.sort((f1, f2) -> {
            int stops1 = getTotalStops(f1);
            int stops2 = getTotalStops(f2);
            return Integer.compare(stops1, stops2); // Lower stops get higher priority
        });
        
        logger.info("Second segment flights sorted by priority (0 stops first)");
        
        int totalCombinationsChecked = 0;
        int validRouteMatches = 0;
        int validTimeConnections = 0;
        int successfulMerges = 0;
        //System.out.println("findDirectConnections "+firstSegmentFlights.size()+" "+secondSegmentFlights.size());
        // Check combinations of first and second segment flights until limit is reached
        for (FlightItinerary firstFlight : firstSegmentFlights) {
            // Early termination if we've reached the limit
            if (directConnections.size() >= maxResults) {
                logger.info("Reached maximum results limit (" + maxResults + ") for direct connections");
                break;
            }
            // Skip if the first segment passes through the final destination en-route
            if (itineraryPassesThroughButNotEndAt(firstFlight, toLocation)) {
                logger.info("Skipping first segment that passes through final destination en-route: " + toLocation);
                continue;
            }
            //System.out.println("First loop "+firstFlight.getPricingInformation().getProvider());
            
            for (FlightItinerary secondFlight : secondSegmentFlights) {
                // Early termination if we've reached the limit
                if (directConnections.size() >= maxResults) {
                    break;
                }
                //System.out.println("Second loop "+firstFlight.getPricingInformation().getProvider());
                totalCombinationsChecked++;
                
                // Skip if it's the same flight
                if (firstFlight.equals(secondFlight)) {
                    continue;
                }
                
                // Check if the first flight's destination matches the second flight's origin
                String firstFlightDestination = getLastDestination(firstFlight);
                String secondFlightOrigin = getFirstOrigin(secondFlight);
                
                if (firstFlightDestination == null || secondFlightOrigin == null || 
                    !firstFlightDestination.equalsIgnoreCase(secondFlightOrigin)) {
                    continue;
                }
                
                validRouteMatches++;
                logger.info("Route match found: " + fromLocation + " -> " + firstFlightDestination + " -> " + toLocation);
                
                // Calculate connection time
                long connectionTime = calculateConnectionTime(firstFlight, secondFlight);
                logger.info("Connection time calculated: " + connectionTime + " minutes");
                
                // Check if connection time is between 3-8 hours (180-480 minutes)
                if (connectionTime >= 120 && connectionTime <= 480) {
                    validTimeConnections++;
                    int firstFlightStops = getTotalStops(firstFlight);
                    int secondFlightStops = getTotalStops(secondFlight);
                    int totalStops = firstFlightStops + secondFlightStops;
                    
                    logger.info("Valid connection time found: " + fromLocation + " -> " + 
                            firstFlightDestination + " -> " + toLocation + " (connection time: " + 
                            connectionTime + " minutes, total stops: " + totalStops + 
                            " [first: " + firstFlightStops + ", second: " + secondFlightStops + "])");
                    
                    // Create merged itinerary
                    FlightItinerary mergedItinerary = createMergedItinerary(firstFlight, secondFlight);
                    if (mergedItinerary != null) {
                        successfulMerges++;
                        directConnections.add(mergedItinerary);
                        logger.info("Successfully merged itinerary #" + successfulMerges + " (Total: " + directConnections.size() + "/" + maxResults + ")");
                        
                        // Early termination if we've reached the limit
                        if (directConnections.size() >= maxResults) {
                            logger.info("Reached maximum results limit (" + maxResults + ") for direct connections");
                            break;
                        }
                    } else {
                        logger.warn("Failed to merge itinerary for route: " + fromLocation + " -> " + firstFlightDestination + " -> " + toLocation);
                    }
                } else {
                    logger.info("Connection time " + connectionTime + " minutes is outside valid range (3-8 hours)");
                }
            }
        }
        
        logger.info("=== DIRECT CONNECTIONS ANALYSIS SUMMARY ===");
        logger.info("Total combinations checked: " + totalCombinationsChecked);
        logger.info("Valid route matches: " + validRouteMatches);
        logger.info("Valid connection times (3-8 hours): " + validTimeConnections);
        logger.info("Successful merges: " + successfulMerges);
        logger.info("Final direct connections: " + directConnections.size() + " (limited to " + maxResults + ")");
        
        if (directConnections.isEmpty()) {
            logger.error("NO DIRECT CONNECTIONS FOUND - Analysis:");
            if (validRouteMatches == 0) {
                logger.error("- No routes connect " + fromLocation + " to " + toLocation + " through intermediate locations");
            }
            if (validTimeConnections == 0 && validRouteMatches > 0) {
                logger.error("- Routes exist but connection times are not between 3-8 hours");
            }
            if (successfulMerges == 0 && validTimeConnections > 0) {
                logger.error("- Valid routes and times exist but merge operation failed");
            }
        } else if (directConnections.size() >= maxResults) {
            logger.warn("WARNING: Direct connections limited to " + maxResults + " to prevent system crashes");
        }
        
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
            boolean isSourceAirportDomestic, boolean isDestinationAirportDomestic, int maxResults) {
        
        List<FlightItinerary> allIntermediateConnections = new ArrayList<>();
        System.out.println("findAllIntermediateConnections ");
        logger.info("=== ANALYZING INTERMEDIATE CONNECTIONS ===");
        logger.info("Looking for: " + fromLocation + " -> [City1] -> [City2] -> " + toLocation);
        logger.info("Maximum results allowed: " + maxResults);
        
        int totalIntermediateLocationsChecked = 0;
        int locationsWithFlights = 0;
        int totalValidConnections = 0;
        
        // Check each location as a potential intermediate stop
        for (String intermediateLocation : concurrentHashMap.keySet()) {
            totalIntermediateLocationsChecked++;
            
            // Skip if it's the same as fromLocation or toLocation
            if (intermediateLocation.equalsIgnoreCase(fromLocation) || 
                intermediateLocation.equalsIgnoreCase(toLocation)) {
                logger.info("Skipping " + intermediateLocation + " (same as source or destination)");
                continue;
            }
            
            logger.info("Checking intermediate location " + totalIntermediateLocationsChecked + ": " + intermediateLocation);
            
            // Get flights from intermediate location to final destination
            List<FlightItinerary> intermediateToDestination = concurrentHashMap.get(intermediateLocation);
            if (intermediateToDestination == null || intermediateToDestination.isEmpty()) {
                logger.info("No flights found from intermediate location " + intermediateLocation + " to " + toLocation);
                continue;
            }
            
            locationsWithFlights++;
            logger.info("Found " + intermediateToDestination.size() + " flights from " + intermediateLocation + " to " + toLocation);
            
            // Sort intermediate flights by priority (0 stops first, then 1 stop, etc.)
            intermediateToDestination.sort((f1, f2) -> {
                int stops1 = getTotalStops(f1);
                int stops2 = getTotalStops(f2);
                return Integer.compare(stops1, stops2); // Lower stops get higher priority
            });
            
            int intermediateValidConnections = 0;
            int routeMatchesForThisLocation = 0;
            
            // Check combinations of first segment flights until limit is reached
            for (FlightItinerary firstFlight : firstSegmentFlights) {
                // Early termination if we've reached the limit
                if (allIntermediateConnections.size() >= maxResults) {
                    logger.info("Reached maximum results limit (" + maxResults + ") for intermediate connections");
                    break;
                }
                // Skip if the first segment passes through the final destination en-route
                if (itineraryPassesThroughButNotEndAt(firstFlight, toLocation)) {
                    logger.info("Skipping first segment that passes through final destination en-route: " + toLocation);
                    continue;
                }
                //System.out.println("firstFlight loop "+firstFlight.getPricingInformation().getProvider()+" intermediateLocation "+intermediateLocation);
                String firstFlightDestination = getLastDestination(firstFlight);
                
                // Check if first flight goes to this intermediate location
                if (firstFlightDestination == null || !firstFlightDestination.equalsIgnoreCase(intermediateLocation)) {
                    continue;
                }
                
                routeMatchesForThisLocation++;
                logger.info("Route match found: " + fromLocation + " -> " + intermediateLocation);
                
                // Check combinations of intermediate flights until limit is reached
                for (FlightItinerary intermediateFlight : intermediateToDestination) {
                    //System.out.println("intermediateFlight loop "+intermediateFlight.getPricingInformation().getProvider()+" intermediateLocation "+intermediateLocation);
                    // Early termination if we've reached the limit
                    if (allIntermediateConnections.size() >= maxResults) {
                        break;
                    }
                    String intermediateFlightDestination = getLastDestination(intermediateFlight);
                    
                    // Check if intermediate flight goes to final destination
                    if (intermediateFlightDestination == null || !intermediateFlightDestination.equalsIgnoreCase(toLocation)) {
                        continue;
                    }
                    
                    logger.info("Route match found: " + intermediateLocation + " -> " + toLocation);
                    
                    // Calculate connection time
                    long connectionTime = calculateConnectionTime(firstFlight, intermediateFlight);
                    logger.info("Connection time calculated: " + connectionTime + " minutes");
                    //System.out.println("intermediateFlight loop "+intermediateFlight.getPricingInformation().getProvider()+" connectionTime "+connectionTime);
                    // Check if connection time is between 3-8 hours (180-480 minutes)
                    if (connectionTime >= 180 && connectionTime <= 480) {
                        intermediateValidConnections++;
                        totalValidConnections++;
                        int firstFlightStops = getTotalStops(firstFlight);
                        int intermediateFlightStops = getTotalStops(intermediateFlight);
                        int totalStops = firstFlightStops + intermediateFlightStops;
                        
                        logger.info("Valid intermediate connection: " + fromLocation + " -> " + 
                                intermediateLocation + " -> " + toLocation + " (connection time: " + 
                                connectionTime + " minutes, total stops: " + totalStops + 
                                " [first: " + firstFlightStops + ", intermediate: " + intermediateFlightStops + "])");
                        
                        // Create merged itinerary
                        FlightItinerary mergedItinerary = createMergedItinerary(firstFlight, intermediateFlight);
                        if (mergedItinerary != null) {
                            allIntermediateConnections.add(mergedItinerary);
                            logger.info("Successfully merged intermediate itinerary #" + allIntermediateConnections.size() + " (Total: " + allIntermediateConnections.size() + "/" + maxResults + ")");
                            
                            // Early termination if we've reached the limit
                            if (allIntermediateConnections.size() >= maxResults) {
                                logger.info("Reached maximum results limit (" + maxResults + ") for intermediate connections");
                                break;
                            }
                        } else {
                            logger.warn("Failed to merge intermediate itinerary for route: " + fromLocation + " -> " + intermediateLocation + " -> " + toLocation);
                        }
                    } else {
                        logger.info("Connection time " + connectionTime + " minutes is outside valid range (3-8 hours)");
                    }
                }
            }
            
            logger.info("Intermediate location " + intermediateLocation + " summary:");
            logger.info("- Route matches: " + routeMatchesForThisLocation);
            logger.info("- Valid connections: " + intermediateValidConnections);
            
            // Early termination if we've reached the limit
            if (allIntermediateConnections.size() >= maxResults) {
                logger.info("Reached maximum results limit (" + maxResults + ") for intermediate connections");
                break;
            }
        }
        
        logger.info("=== INTERMEDIATE CONNECTIONS ANALYSIS SUMMARY ===");
        logger.info("Total intermediate locations checked: " + totalIntermediateLocationsChecked);
        logger.info("Locations with flights to destination: " + locationsWithFlights);
        logger.info("Total valid intermediate connections: " + totalValidConnections);
        logger.info("Final intermediate connections: " + allIntermediateConnections.size() + " (limited to " + maxResults + ")");
        
        if (allIntermediateConnections.isEmpty()) {
            logger.error("NO INTERMEDIATE CONNECTIONS FOUND - Analysis:");
            if (locationsWithFlights == 0) {
                logger.error("- No intermediate locations have flights to " + toLocation);
            }
            if (totalValidConnections == 0 && locationsWithFlights > 0) {
                logger.error("- Intermediate locations exist but no valid connection times (3-8 hours)");
            }
        } else if (allIntermediateConnections.size() >= maxResults) {
            logger.warn("WARNING: Intermediate connections limited to " + maxResults + " to prevent system crashes");
        }
        
        return allIntermediateConnections;
    }
    
    /**
     * Gets the destination of the last air segment in the last journey.
     */
    private String getLastDestination(FlightItinerary flightItinerary) {
        if (flightItinerary == null || flightItinerary.getJourneyList() == null || flightItinerary.getJourneyList().isEmpty()) {
            return null;
        }
        
        Journey lastJourney = flightItinerary.getJourneyList().get(flightItinerary.getJourneyList().size() - 1);
        if (lastJourney == null || lastJourney.getAirSegmentList() == null || lastJourney.getAirSegmentList().isEmpty()) {
            return null;
        }
        
        AirSegmentInformation lastSegment = lastJourney.getAirSegmentList().get(lastJourney.getAirSegmentList().size() - 1);
        return lastSegment != null ? lastSegment.getToLocation() : null;
    }
    
    /**
     * Gets the origin of the first air segment in the first journey.
     */
    private String getFirstOrigin(FlightItinerary flightItinerary) {
        if (flightItinerary == null || flightItinerary.getJourneyList() == null || flightItinerary.getJourneyList().isEmpty()) {
            return null;
        }
        
        Journey firstJourney = flightItinerary.getJourneyList().get(0);
        if (firstJourney == null || firstJourney.getAirSegmentList() == null || firstJourney.getAirSegmentList().isEmpty()) {
            return null;
        }
        
        AirSegmentInformation firstSegment = firstJourney.getAirSegmentList().get(0);
        return firstSegment != null ? firstSegment.getFromLocation() : null;
    }

    private boolean itineraryPassesThroughButNotEndAt(FlightItinerary itinerary, String targetLocation) {
        if (itinerary == null || itinerary.getJourneyList() == null || itinerary.getJourneyList().isEmpty() || targetLocation == null) {
            return false;
        }
        // Determine final destination of the itinerary
        String finalDest = getLastDestination(itinerary);
        if (finalDest != null && finalDest.equalsIgnoreCase(targetLocation)) {
            // Ends at final destination - allowed
            return false;
        }
        // Scan all segments; if any segment's toLocation equals targetLocation, it's a through-touch
        for (Journey journey : itinerary.getJourneyList()) {
            if (journey == null || journey.getAirSegmentList() == null) continue;
            List<AirSegmentInformation> segs = journey.getAirSegmentList();
            for (int i = 0; i < segs.size(); i++) {
                AirSegmentInformation seg = segs.get(i);
                if (seg != null && seg.getToLocation() != null && seg.getToLocation().equalsIgnoreCase(targetLocation)) {
                    return true;
                }
            }
        }
        return false;
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
            
            // Use airport time zones; never fall back to system default
            java.time.ZoneId arrivalZone = getArrivalZone(firstFlight);
            java.time.ZoneId departureZone = getDepartureZone(secondFlight);

            // Try multiple date formats; attach derived airport zone when no zone present in the string
            java.time.ZonedDateTime arrivalDateTime = parseDateTime(arrivalTime, arrivalZone);
            java.time.ZonedDateTime departureDateTime = parseDateTime(departureTime, departureZone);
            
            if (arrivalDateTime == null || departureDateTime == null) {
                logger.error("Failed to parse date times - arrival: " + arrivalTime + ", departure: " + departureTime);
                return -1;
            }
            
            java.time.Duration duration = java.time.Duration.between(arrivalDateTime, departureDateTime);
            
            return duration.toMinutes();
        } catch (Exception e) {
            logger.error("Error calculating connection time: " + e.getMessage());
            return -1;
        }
    }
    

    private java.time.ZonedDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }

        String[] patterns = {
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss"
        };
        
        for (String pattern : patterns) {
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(pattern);
                if (pattern.contains("XXX") || pattern.contains("Z")) {
                    return java.time.ZonedDateTime.parse(dateTimeStr, formatter);
                } else {
                    java.time.LocalDateTime localDateTime = java.time.LocalDateTime.parse(dateTimeStr, formatter);
                    return localDateTime.atZone(java.time.ZoneId.systemDefault());
                }
            } catch (Exception e) {
                continue;
            }
        }
        
        logger.warn("Could not parse date-time string with any known pattern: " + dateTimeStr);
        return null;
    }

    // Overload with explicit fallback zone (no system default usage)
    private java.time.ZonedDateTime parseDateTime(String dateTimeStr, java.time.ZoneId fallbackZone) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        String[] patterns = {
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss"
        };
        for (String pattern : patterns) {
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(pattern);
                if (pattern.contains("XXX") || pattern.contains("Z")) {
                    return java.time.ZonedDateTime.parse(dateTimeStr, formatter);
                } else {
                    java.time.LocalDateTime localDateTime = java.time.LocalDateTime.parse(dateTimeStr, formatter);
                    // Use provided airport zone; if unavailable, use UTC (not system default)
                    java.time.ZoneId zone = (fallbackZone != null) ? fallbackZone : java.time.ZoneOffset.UTC;
                    return localDateTime.atZone(zone);
                }
            } catch (Exception ignored) {}
        }
        logger.warn("Could not parse date-time string with any known pattern: " + dateTimeStr);
        return null;
    }

    // Helpers to derive airport time zones for last arrival and first departure
    private java.time.ZoneId getArrivalZone(FlightItinerary itinerary) {
        try {
            if (itinerary == null || itinerary.getJourneyList() == null || itinerary.getJourneyList().isEmpty()) return null;
            Journey lastJourney = itinerary.getJourneyList().get(itinerary.getJourneyList().size()-1);
            if (lastJourney == null || lastJourney.getAirSegmentList() == null || lastJourney.getAirSegmentList().isEmpty()) return null;
            AirSegmentInformation lastSeg = lastJourney.getAirSegmentList().get(lastJourney.getAirSegmentList().size()-1);
            if (lastSeg != null && lastSeg.getToAirport() != null && lastSeg.getToAirport().getTime_zone() != null) {
                return java.time.ZoneId.of(lastSeg.getToAirport().getTime_zone());
            }
        } catch (Exception ignored) {}
        return null;
    }

    private java.time.ZoneId getDepartureZone(FlightItinerary itinerary) {
        try {
            if (itinerary == null || itinerary.getJourneyList() == null || itinerary.getJourneyList().isEmpty()) return null;
            Journey firstJourney = itinerary.getJourneyList().get(0);
            if (firstJourney == null || firstJourney.getAirSegmentList() == null || firstJourney.getAirSegmentList().isEmpty()) return null;
            AirSegmentInformation firstSeg = firstJourney.getAirSegmentList().get(0);
            if (firstSeg != null && firstSeg.getFromAirport() != null && firstSeg.getFromAirport().getTime_zone() != null) {
                return java.time.ZoneId.of(firstSeg.getFromAirport().getTime_zone());
            }
        } catch (Exception ignored) {}
        return null;
    }
    
    /**
     * Gets the arrival time of the last air segment in the last journey.
     */
    private String getLastArrivalTime(FlightItinerary flightItinerary) {
        if (flightItinerary == null || flightItinerary.getJourneyList() == null || flightItinerary.getJourneyList().isEmpty()) {
            return null;
        }
        
        Journey lastJourney = flightItinerary.getJourneyList().get(flightItinerary.getJourneyList().size() - 1);
        if (lastJourney == null || lastJourney.getAirSegmentList() == null || lastJourney.getAirSegmentList().isEmpty()) {
            return null;
        }
        
        AirSegmentInformation lastSegment = lastJourney.getAirSegmentList().get(lastJourney.getAirSegmentList().size() - 1);
        return lastSegment != null ? lastSegment.getArrivalTime() : null;
    }
    
    /**
     * Gets the departure time of the first air segment in the first journey.
     */
    private String getFirstDepartureTime(FlightItinerary flightItinerary) {
        if (flightItinerary == null || flightItinerary.getJourneyList() == null || flightItinerary.getJourneyList().isEmpty()) {
            return null;
        }
        
        Journey firstJourney = flightItinerary.getJourneyList().get(0);
        if (firstJourney == null || firstJourney.getAirSegmentList() == null || firstJourney.getAirSegmentList().isEmpty()) {
            return null;
        }
        
        AirSegmentInformation firstSegment = firstJourney.getAirSegmentList().get(0);
        return firstSegment != null ? firstSegment.getDepartureTime() : null;
    }
    
    /**
     * Creates a merged itinerary from two separate flights.
     */
    private FlightItinerary createMergedItinerary(FlightItinerary firstFlight, FlightItinerary secondFlight) {
        try {
            //System.out.println("firstFlight "+firstFlight.getPricingInformation().getProvider()+"  secondFlight "+secondFlight.getPricingInformation().getProvider());
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
