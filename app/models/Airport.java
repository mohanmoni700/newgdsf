package models;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.redis.core.RedisTemplate;
import play.db.ebean.Model;
import play.libs.Json;
import redis.clients.jedis.Jedis;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

/**
 * Created by mahendra-singh on 25/6/14.
 */
@Entity
@Table(name = "airport")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Airport extends Model implements Serializable {
	public Airport() {

	}

	@Column(name = "id")
	@Id
	private Integer id;

	/*@Column(name = "ident")
	private String ident;*/

	@Column(name = "type")
	private String type;

	@Column(name = "airport_name")
	private String airportName;

	@Column(name = "latitude")
	private String latitude;

	@Column(name = "longitude")
	private String longitude;

	/*@Column(name = "elevation_ft")
	private String elevation_ft;*/

	/*@Column(name = "continent")
	private String continent;*/

	@Column(name = "iso_country")
	private String iso_country;

/*	@Column(name = "iso_region")
	private String iso_region;

	@Column(name = "scheduled_service")
	private String scheduled_service;*/

	@Column(name = "city_name")
	private String cityName;

	@Column(name = "iata_code")
	private String iata_code;

	/*@Column(name = "gps_code")
	private String gps_code;*/

	@Column(name = "local_code")
	private String local_code;

	@Column(name = "time_zone")
	private String time_zone;

	@Column(name = "dst")
	private String dst;

	@Column(name = "country")
	private String country;

	@Column(name = "gmt_offset")
	private String gmtOffset;


	@Column(name = "alt")
	private Long alt;

	/*@Column(name = "alternatenames")
	private String alternateNames;

	@Column(name = "connections")
	private Long connections;

	@Column(name = "departures")
	private Long departures;

	@Column(name = "facebook")
	private String facebook;*/

	@Column(name = "icao_code")
	private String icaoCode;

	/*@Column(name = "instagram")
	private String instagram;*/

	@Column(name = "is_international")
	private Long isInternational;

	@Column(name = "is_major")
	private boolean isMajor;

	@Column(name = "phone")
	private String phone;

	@Column(name = "phone_formatted")
	private String phoneFormatted;

	@Column(name = "popularity")
	private Long popularity;

	/*@Column(name = "postal_code")
	private String postalCode;

	@Column(name = "runways")
	private Long runways;

	@Column(name = "slug")
	private String slug;*/

	@Column(name = "state")
	private String state;

	@Column(name = "time_now")
	private String timeNow;

	/*@Column(name = "twitter")
	private String twitter;

	@Column(name = "un_locode")
	private String unLocode;

	@Column(name = "weather_zone")
	private String weatherZone;

	@Column(name = "website")
	private String website;*/

	@Transient
	private String distance;

    @Column(name = "city_code")
    private String cityCode;

	public static Finder<Integer, Airport> find = new Finder<Integer, Airport>(
			Integer.class, Airport.class);

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getAirportName() {
		return airportName;
	}

	public void setAirportName(String airportName) {
		this.airportName = airportName;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public String getIso_country() {
		return iso_country;
	}

	public void setIso_country(String iso_country) {
		this.iso_country = iso_country;
	}

	public String getCityName() {
		return cityName;
	}

	public void setCityName(String cityName) {
		this.cityName = cityName;
	}

	public String getIata_code() {
		return iata_code;
	}

	public void setIata_code(String iata_code) {
		this.iata_code = iata_code;
	}

	/*
	 * public String getGps_code() { return gps_code; }
	 * 
	 * public void setGps_code(String gps_code) { this.gps_code = gps_code; }
	 */

	public String getLocal_code() {
		return local_code;
	}

	public void setLocal_code(String local_code) {
		this.local_code = local_code;
	}

	public String getTime_zone() {
		return time_zone;
	}

	public void setTime_zone(String time_zone) {
		this.time_zone = time_zone;
	}

	public String getDst() {
		return dst;
	}

	public void setDst(String dst) {
		this.dst = dst;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getGmtOffset() {
		return gmtOffset;
	}

	public void setGmtOffset(String gmtOffset) {
		this.gmtOffset = gmtOffset;
	}

	public String getDistance() {
		return distance;
	}

	public void setDistance(String distance) {
		this.distance = distance;
	}

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

	public Long getAlt() {
		return alt;
	}

	public void setAlt(Long alt) {
		this.alt = alt;
	}

	/*public String getAlternateNames() {
		return alternateNames;
	}

	public void setAlternateNames(String alternateNames) {
		this.alternateNames = alternateNames;
	}

	public Long getConnections() {
		return connections;
	}

	public void setConnections(Long connections) {
		this.connections = connections;
	}

	public Long getDepartures() {
		return departures;
	}

	public void setDepartures(Long departures) {
		this.departures = departures;
	}

	public String getFacebook() {
		return facebook;
	}

	public void setFacebook(String facebook) {
		this.facebook = facebook;
	}*/

	public String getIcaoCode() {
		return icaoCode;
	}

	public void setIcaoCode(String icaoCode) {
		this.icaoCode = icaoCode;
	}

	/*public String getInstagram() {
		return instagram;
	}

	public void setInstagram(String instagram) {
		this.instagram = instagram;
	}*/

	public Long getIsInternational() {
		return isInternational;
	}

	public void setIsInternational(Long isInternational) {
		this.isInternational = isInternational;
	}

	public boolean isMajor() {
		return isMajor;
	}

	public void setMajor(boolean major) {
		isMajor = major;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getPhoneFormatted() {
		return phoneFormatted;
	}

	public void setPhoneFormatted(String phoneFormatted) {
		this.phoneFormatted = phoneFormatted;
	}

	public Long getPopularity() {
		return popularity;
	}

	public void setPopularity(Long popularity) {
		this.popularity = popularity;
	}

	/*public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public Long getRunways() {
		return runways;
	}

	public void setRunways(Long runways) {
		this.runways = runways;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}*/

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getTimeNow() {
		return timeNow;
	}

	public void setTimeNow(String timeNow) {
		this.timeNow = timeNow;
	}

	/*public String getTwitter() {
		return twitter;
	}

	public void setTwitter(String twitter) {
		this.twitter = twitter;
	}

	public String getUnLocode() {
		return unLocode;
	}

	public void setUnLocode(String unLocode) {
		this.unLocode = unLocode;
	}

	public String getWeatherZone() {
		return weatherZone;
	}

	public void setWeatherZone(String weatherZone) {
		this.weatherZone = weatherZone;
	}

	public String getWebsite() {
		return website;
	}

	public void setWebsite(String website) {
		this.website = website;
	}*/

	public static Finder<Integer, Airport> getFind() {
		return find;
	}

	public static void setFind(Finder<Integer, Airport> find) {
		Airport.find = find;
	}

	private static Airport getAiport(String iataCode) {
		Jedis j = new Jedis("localhost", 6379);
		j.connect();
		String airportJson = j.get(iataCode);
		Airport airport = new Airport();
		if (airportJson != null) {
			airport = Json.fromJson(Json.parse(airportJson), Airport.class);

		} else {
			List<Airport> airportList = find.where().eq("iata_code", iataCode).findList();
			if(airportList != null && airportList.size() > 0){
				airport = airportList.get(0);
				j.set(iataCode, Json.toJson(airport).toString());
			}


		}
		j.disconnect();
		return airport;
	}

	public static Airport getAirport(String iataCode, RedisTemplate redisTemplate){

		String airportJson = null;

		Airport airport = new Airport();
		/*if (redisTemplate.opsForValue().get(iataCode) != null) {
			airportJson = (String) redisTemplate.opsForValue().get(iataCode);
			airport = Json.fromJson(Json.parse(airportJson), Airport.class);

		} else {*/
			List<Airport> airportList = Airport.find.where().eq("iata_code", iataCode).findList();
			if(airportList != null && airportList.size() > 0){
				airport = airportList.get(0);
				//redisTemplate.opsForValue().set(iataCode, Json.toJson(airport).toString());
			}


		//}
		return airport;
	}

	public static boolean checkCountry(String country, List<String> codes){
		int count = find.where().in("iata_code",codes).eq("country", country).findRowCount();
		if(count == codes.size()){
			return true;
		}
		return false;
	}
}
