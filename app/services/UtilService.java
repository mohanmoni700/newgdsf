package services;

import com.compassites.constants.CacheConstants;
import models.Airline;
import models.Airport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import play.libs.Json;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by yaseen on 23-05-2016.
 */

@Service
public class UtilService {

    @Autowired
    private RedisTemplate redisTemplate;

    public Airport getAirport(String iataCode){

        String airportJson = (String) redisTemplate.opsForValue().get(iataCode);
        Airport airport = new Airport();
        if (airportJson != null) {
            airport = Json.fromJson(Json.parse(airportJson), Airport.class);

        } else {
            List<Airport> airportList = Airport.find.where().eq("iata_code", iataCode).findList();
            if(airportList != null && airportList.size() > 0){
                airport = airportList.get(0);
                redisTemplate.opsForValue().set(iataCode, Json.toJson(airport).toString());
            }


        }
        return airport;
    }

    public Airline getAirlineByCode(String airlineCode) {
        String cacheKey = "Airline#" + airlineCode;

        String airlineJson =  (String) redisTemplate.opsForValue().get(cacheKey);
        Airline airline = null;
        if (airlineJson != null) {
            airline = Json.fromJson(Json.parse(airlineJson), Airline.class);
        } else {
            List<Airline> airlines = Airline.find.where().eq("iata_code", airlineCode).findList();
            if (airlines.size() > 0) {
                airline = Airline.find.where().eq("iata_code", airlineCode).findList().get(0);
            } else {
                airline = new Airline();
                airline.setIataCode(airlineCode);
            }
            redisTemplate.opsForValue().set(cacheKey,Json.toJson(airline).toString());
            redisTemplate.expire(cacheKey,CacheConstants.CACHE_TIMEOUT_IN_SECS, TimeUnit.SECONDS);

        }
        return airline;
    }
}
