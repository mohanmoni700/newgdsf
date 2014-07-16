package models;

import play.db.ebean.Model;
import play.libs.Json;
import redis.clients.jedis.Jedis;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Created by Renu on 6/24/14.
 */
@Entity
public class AirlineCode {
    @Id
    public Long id;

    public String ICAO_code;
    public String IATA_code;
    public String airline;
    public String call_sign;
    public String country_name;
    public String comments;

    public static Model.Finder<Long, AirlineCode> find = new Model.Finder<Long, AirlineCode>(Long.class, AirlineCode.class);

    public AirlineCode(){}

    public AirlineCode(String airline, String call_sign, String comments, String country_name, String IATA_code, String ICAO_code) {
        this.comments = comments;
        this.IATA_code = IATA_code;
        this.ICAO_code = ICAO_code;
        this.airline = airline;
        this.call_sign = call_sign;
        this.country_name = country_name;
    }

    public static AirlineCode getAirlineByCode(String airlineCode){
        String cacheKey = "Airline#"+airlineCode;
        Jedis j = new Jedis("localhost", 6379);
        j.connect();
        String airlineJson = j.get(cacheKey);
        AirlineCode airline = null;
        if (airlineJson != null) {
            airline  = Json.fromJson(Json.parse(airlineJson), AirlineCode.class);

        } else {
            airline = find.where().eq("iata_code",airlineCode).findList().get(0);
            j.set(cacheKey,Json.toJson(airline).toString());

        }
        j.disconnect();
        return airline;
    }

}
