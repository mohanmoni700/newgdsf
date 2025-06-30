package utils;

import com.compassites.model.*;
import com.compassites.model.splitticket.PossibleRoutes;
import models.SplitTicketTransitAirports;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.beans.BeanUtils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SplitTicketHelper {

    private static final long connectionTime = play.Play.application().configuration().getLong("split.transitpoint.connectionTime");

    public List<SearchParameters> createSearchParameters(Map<String, PossibleRoutes> possibleRoutesMap, SearchParameters searchParameters, List<SplitTicketTransitAirports> splitTicketTransitAirports) {
        List<SearchParameters> searchParametersList = new ArrayList<>();
        for (Map.Entry<String, PossibleRoutes> possibleRoutesEntry : possibleRoutesMap.entrySet()) {
            SearchParameters searchParameters1 = SerializationUtils.clone(searchParameters);
            searchParameters1.setSequence(possibleRoutesEntry.getValue().getSequence());
            List<SearchJourney> journeyList = createJourneys(possibleRoutesEntry.getValue(), searchParameters1);
            searchParameters1.setJourneyList(journeyList);
            searchParametersList.add(searchParameters1);
        }
        return searchParametersList;
    }

    public List<SearchParameters> createSplitSearchParameters(Map<String, PossibleRoutes> possibleRoutesMap, SearchParameters searchParameters, List<SplitTicketTransitAirports> splitTicketTransitAirports) {
        List<SearchParameters> searchParametersList = new ArrayList<>();
        for (Map.Entry<String, PossibleRoutes> possibleRoutesEntry : possibleRoutesMap.entrySet()) {
            SearchParameters searchParameters1 = SerializationUtils.clone(searchParameters);
            searchParameters1.setSequence(possibleRoutesEntry.getValue().getSequence());
            List<SearchJourney> journeyList = createTransitJourneys(possibleRoutesEntry.getValue(), searchParameters1);
            searchParameters1.setJourneyList(journeyList);
            searchParametersList.add(searchParameters1);
        }
        return searchParametersList;
    }

    private SearchParameters createFromSearchParameters(SplitTicketTransitAirports splitTicketTransitAirports, SearchParameters searchParameters,Map<String, PossibleRoutes> possibleRoutesMap, String fromLocation) {
        SearchParameters searchParameters1 = SerializationUtils.clone(searchParameters);
        List<SearchJourney> journeyList = new ArrayList<>();
        for (SearchJourney searchJourneyItem: searchParameters.getJourneyList()) {
            SearchJourney searchJourney = SerializationUtils.clone(searchJourneyItem);
            PossibleRoutes possibleRoutes = possibleRoutesMap.get(searchJourneyItem.getOrigin()+searchJourneyItem.getDestination());
            searchJourney.setOrigin(possibleRoutes.getToLocation());
            searchJourney.setDestination(searchParameters.getJourneyList().get(0).getDestination());
            searchJourney.setTravelDate(possibleRoutes.getArrivalDate());

            ZonedDateTime zonedDateTime = ZonedDateTime.parse(possibleRoutes.getArrivalTime());
            ZonedDateTime midnightTime = zonedDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedTime = midnightTime.format(formatter);

            searchJourney.setTravelDateStr(formattedTime);
            journeyList.add(searchJourney);
        }
        searchParameters1.setSequence(1);
        searchParameters1.setJourneyList(journeyList);
        return searchParameters1;
    }

    public List<SearchJourney> createTransitJourneys(SplitTicketTransitAirports splitTicketTransitAirports, SearchParameters searchParameters,Map<String, PossibleRoutes> possibleRoutesMap, String fromLocation) {
        List<SearchJourney> journeyList = new ArrayList<>();
        for (SearchJourney searchJourneyItem: searchParameters.getJourneyList()) {
            SearchJourney searchJourney1 = SerializationUtils.clone(searchJourneyItem);
            PossibleRoutes possibleRoutes = possibleRoutesMap.get(searchJourneyItem.getOrigin()+searchJourneyItem.getDestination());
            searchJourneyItem.setOrigin(possibleRoutes.getToLocation());
            searchJourneyItem.setDestination(searchParameters.getJourneyList().get(0).getDestination());
            searchJourneyItem.setTravelDate(possibleRoutes.getArrivalDate());

            ZonedDateTime zonedDateTime = ZonedDateTime.parse(possibleRoutes.getArrivalTime());
            ZonedDateTime midnightTime1 = zonedDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0);
            DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedTime1 = midnightTime1.format(formatter1);

            searchJourney1.setTravelDateStr(formattedTime1);
            journeyList.add(searchJourney1);
        }
        return journeyList;
    }

    private Map<String, PossibleRoutes> possibleToRoutesMap(Map<String, PossibleRoutes> possibleRoutesMap, boolean isFirst) {
        Map<String, PossibleRoutes> mapWithFromLocation = new HashMap<>();
        for (Map.Entry<String, PossibleRoutes> possibleRoutesEntry : possibleRoutesMap.entrySet()) {
            if (isFirst) {
                mapWithFromLocation.put(possibleRoutesEntry.getValue().getFromLocation(), possibleRoutesEntry.getValue());
            } else {
                if(!mapWithFromLocation.containsKey(possibleRoutesEntry.getValue().getToLocation())) {
                    mapWithFromLocation.put(possibleRoutesEntry.getValue().getToLocation(), possibleRoutesEntry.getValue());
                }
            }
        }
        return mapWithFromLocation;
    }

    public List<SearchJourney> createTransitJourneys(PossibleRoutes possibleRoutes, SearchParameters searchParameters) {
        List<SearchJourney> journeyList = new ArrayList<>();
        for (SearchJourney searchJourneyItem: searchParameters.getJourneyList()) {
            SearchJourney searchJourney = SerializationUtils.clone(searchJourneyItem);
            searchJourney.setOrigin(possibleRoutes.getToLocation());
            searchJourney.setDestination(searchParameters.getJourneyList().get(0).getDestination());
            searchJourney.setTravelDate(possibleRoutes.getArrivalDate());

            ZonedDateTime zonedDateTime = ZonedDateTime.parse(possibleRoutes.getArrivalTime());
            ZonedDateTime midnightTime = zonedDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0).plusHours(connectionTime);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedTime = midnightTime.format(formatter);

            searchJourney.setTravelDateStr(formattedTime);
            journeyList.add(searchJourney);
        }
        return journeyList;
    }

    public List<SearchJourney> createJourneys(PossibleRoutes possibleRoutes, SearchParameters searchParameters) {
        List<SearchJourney> journeyList = new ArrayList<>();
        for (SearchJourney searchJourneyItem: searchParameters.getJourneyList()) {
            SearchJourney searchJourney = SerializationUtils.clone(searchJourneyItem);
            searchJourney.setOrigin(possibleRoutes.getToLocation());
            searchJourney.setDestination(searchParameters.getJourneyList().get(0).getDestination());
            searchJourney.setTravelDate(possibleRoutes.getArrivalDate());

            ZonedDateTime zonedDateTime = ZonedDateTime.parse(possibleRoutes.getArrivalTime());
            ZonedDateTime midnightTime = zonedDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0).plusHours(4);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedTime = midnightTime.format(formatter);

            searchJourney.setTravelDateStr(formattedTime);
            journeyList.add(searchJourney);
        }
        return journeyList;
    }

    public  Map<String, PossibleRoutes> createPossibleRoutes(SearchResponse searchResponse) {
        Map<String, PossibleRoutes> possibleRoutesMap = new LinkedHashMap<>();
        ConcurrentHashMap<Integer, FlightItinerary> nonSeamenHashMap = searchResponse.getAirSolution().getNonSeamenHashMap();
        for (Map.Entry<Integer, FlightItinerary> flightItineraryEntry: nonSeamenHashMap.entrySet()) {
            FlightItinerary flightItinerary = flightItineraryEntry.getValue();
            if(flightItinerary != null) {
                List<Journey> journeyList = flightItinerary.getJourneyList();
                for (Journey journey: journeyList) {
                    int sequence=1;
                    for (AirSegmentInformation airSegmentInformation: journey.getAirSegmentList()) {
                        StringBuilder mapKey = new StringBuilder();
                        PossibleRoutes possibleRoutes = new PossibleRoutes();
                        mapKey.append(airSegmentInformation.getFromLocation());
                        mapKey.append(airSegmentInformation.getToLocation());
                        if(!nonSeamenHashMap.containsKey(mapKey.toString())) {
                            possibleRoutes.setFromLocation(airSegmentInformation.getFromLocation());
                            possibleRoutes.setToLocation(airSegmentInformation.getToLocation());
                            possibleRoutes.setSequence(sequence);
                            possibleRoutes.setDepartureDate(airSegmentInformation.getDepartureDate());
                            possibleRoutes.setDepartureTime(airSegmentInformation.getDepartureTime());
                            possibleRoutes.setArrivalDate(airSegmentInformation.getArrivalDate());
                            possibleRoutes.setArrivalTime(airSegmentInformation.getArrivalTime());
                            possibleRoutes.setKey(flightItineraryEntry.getKey());
                            possibleRoutesMap.put(mapKey.toString(), possibleRoutes);
                            sequence++;
                        }
                    }
                }
            }
        }
        return  possibleRoutesMap;
    }
}
