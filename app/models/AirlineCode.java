package models;

import com.compassites.constants.CacheConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import play.db.ebean.Model;
import play.libs.Json;
import redis.clients.jedis.Jedis;

import javax.persistence.*;
import java.util.List;

/**
 * Created by Renu on 6/24/14.
 */
@Entity
@Table(name="airline_code")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AirlineCode {
    @Id
    @Column(name="id")
    public Long id;
    @Column(name="icao_code")
    public String ICAO_code;
    @Column(name="iata_code")
    public String IATA_code;
    @Column(name="airline")
    public String airline;
    @Column(name="call_sign")
    public String call_sign;
    @Column(name="country_name")
    public String country_name;
    @Column(name="comments")
    public String comments;
    @Lob
    @Column(name="logo")
    public byte[] logo;
    @Column(name="logo_name")
    public String logo_name;





    public AirlineCode(Long id, String iCAO_code, String iATA_code,
                       String airline, String call_sign, String country_name,
                       String comments, byte[] logo, String logo_name) {

        this.id = id;
        ICAO_code = iCAO_code;
        IATA_code = iATA_code;
        this.airline = airline;
        this.call_sign = call_sign;
        this.country_name = country_name;
        this.comments = comments;
        this.logo = logo;
        this.logo_name = logo_name;
    }

    public String getLogo_name() {
        return logo_name;
    }

    public void setLogo_name(String logo_name) {
        this.logo_name = logo_name;
    }


    public static Model.Finder<Long, AirlineCode> find = new Model.Finder<Long, AirlineCode>(Long.class, AirlineCode.class);

    public AirlineCode(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getICAO_code() {
        return ICAO_code;
    }

    public void setICAO_code(String iCAO_code) {
        ICAO_code = iCAO_code;
    }

    public String getIATA_code() {
        return IATA_code;
    }

    public void setIATA_code(String iATA_code) {
        IATA_code = iATA_code;
    }

    public String getAirline() {
        return airline;
    }

    public void setAirline(String airline) {
        this.airline = airline;
    }

    public String getCall_sign() {
        return call_sign;
    }

    public void setCall_sign(String call_sign) {
        this.call_sign = call_sign;
    }

    public String getCountry_name() {
        return country_name;
    }

    public void setCountry_name(String country_name) {
        this.country_name = country_name;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public byte[] getLogo() {
        return logo;
    }

    public void setLogo(byte[] logo) {
        this.logo = logo;
    }

    public static Model.Finder<Long, AirlineCode> getFind() {
        return find;
    }

    public static void setFind(Model.Finder<Long, AirlineCode> find) {
        AirlineCode.find = find;
    }


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
            List<AirlineCode> airlines = find.where().eq("iata_code",airlineCode).findList();
            if (airlines.size() > 0)
                airline = find.where().eq("iata_code",airlineCode).findList().get(0);
            else
                airline = new AirlineCode( airlineCode, "","", "", airlineCode,""  );
            j.setex(cacheKey, CacheConstants.CACHE_TIMEOUT_IN_SECS, Json.toJson(airline).toString());

        }
        j.disconnect();
        return airline;
    }

}
