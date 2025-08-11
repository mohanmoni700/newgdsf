package services;

import com.amadeus.xml.fcuqcr_08_1_1a.FareConvertCurrencyReply;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.constants.CacheConstants;
import dto.AmadeusConvertCurrencyRQ;
import dto.AmadeusConvertCurrencyRS;
import models.Airline;
import models.Airport;
import models.AmadeusSessionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import play.libs.Json;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by yaseen on 23-05-2016.
 */

@Service
public class UtilService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ServiceHandler serviceHandler;


    static Logger logger = LoggerFactory.getLogger("gds");

    public Airport getAirport(String iataCode) {

        String airportJson = (String) redisTemplate.opsForValue().get(iataCode);
        Airport airport = new Airport();
        if (airportJson != null) {
            airport = Json.fromJson(Json.parse(airportJson), Airport.class);

        } else {
            List<Airport> airportList = Airport.find.where().eq("iata_code", iataCode).findList();
            if (airportList != null && airportList.size() > 0) {
                airport = airportList.get(0);
                redisTemplate.opsForValue().set(iataCode, Json.toJson(airport).toString());
            }


        }
        return airport;
    }

    public Airline getAirlineByCode(String airlineCode) {
        String cacheKey = "Airline#" + airlineCode;

        String airlineJson = (String) redisTemplate.opsForValue().get(cacheKey);
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
            redisTemplate.opsForValue().set(cacheKey, Json.toJson(airline).toString());
            redisTemplate.expire(cacheKey, CacheConstants.CACHE_TIMEOUT_IN_SECS, TimeUnit.SECONDS);

        }
        return airline;
    }

    //This method is used to get the ROE map from amadeus (BSR && ICH)
    public Map<String, AmadeusConvertCurrencyRS> getAmadeusExchangeInfo(AmadeusConvertCurrencyRQ amadeusConvertCurrencyRQ) {

        Map<String, AmadeusConvertCurrencyRS> amadeusExchangeRatesMap = new HashMap<>();
        try {

            //Login
            AmadeusSessionWrapper amadeusSessionWrapper = serviceHandler.logIn(false);

            //Getting convert currency response here
            FareConvertCurrencyReply fareConvertCurrencyReply = serviceHandler.getFareConvertCurrencyReply(amadeusSessionWrapper, amadeusConvertCurrencyRQ);

            List<FareConvertCurrencyReply.ConversionDetails> conversionDetailsList = fareConvertCurrencyReply.getConversionDetails();

            for (FareConvertCurrencyReply.ConversionDetails conversionDetails : conversionDetailsList) {

                AmadeusConvertCurrencyRS amadeusConvertCurrencyRS = new AmadeusConvertCurrencyRS();

                amadeusConvertCurrencyRS.setFromCurrency(amadeusConvertCurrencyRS.getFromCurrency());
                amadeusConvertCurrencyRS.setToCurrency(amadeusConvertCurrencyRS.getToCurrency());

                FareConvertCurrencyReply.ConversionDetails.ConversionRate conversionRate = conversionDetails.getConversionRate();
                FareConvertCurrencyReply.ConversionDetails.ConversionRate.ConversionRateDetails conversionRateDetails = conversionRate.getConversionRateDetails();

                BigDecimal rate = conversionRateDetails.getRate();
                String rateType = conversionRateDetails.getRateType();

                amadeusConvertCurrencyRS.setConversionRate(rate);

                amadeusExchangeRatesMap.put(rateType, amadeusConvertCurrencyRS);
            }

            return amadeusExchangeRatesMap;

        } catch (Exception e) {
            logger.debug("Error while fetching Amadeus exchange info ");
            return amadeusExchangeRatesMap;
        }
    }


}
