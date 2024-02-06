package models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class Airport extends Model implements Serializable {

	@Column(name = "id")
	@Id
	private Long id;

	@Column(name = "airport_name")
	private String airportName;

	@Column(name = "latitude")
	private String latitude;

	@Column(name = "longitude")
	private String longitude;

	@Column(name = "iso_country")
	private String iso_country;

	@Column(name = "city_name")
	private String cityName;

	@Column(name = "iata_code")
	private String iata_code;

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

	@Transient
	private String distance;

	@Column(name = "city_code")
	private String cityCode;

	@Column(name = "alt")
	private Long alt;

	@Column(name = "icao_code")
	private String icaoCode;

	@Column(name = "state")
	private String state;


	@Column(name = "weather_zone")
	private String weatherZone;

	@Column(name = "fs")
	private String fs;

	@Column(name = "state_code")
	private String stateCode;

	@Column(name = "region_name")
	private String regionName;

	@Column(name = "weather_url")
	private String weatherUrl;

	@Column(name = "delay_index_url")
	private String delayIndexUrl;

	public static final String IATA_CODE = "iata_code";

	@Column(name = "classification")
	private String classification;

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

	public static Finder<Integer, Airport> find = new Finder<Integer, Airport>(
			Integer.class, Airport.class);

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public static int findRowCount() {
		return find.findRowCount();
	}
	public Long getAlt() {
		return alt;
	}

	public void setAlt(Long alt) {
		this.alt = alt;
	}


	public String getIcaoCode() {
		return icaoCode;
	}

	public void setIcaoCode(String icaoCode) {
		this.icaoCode = icaoCode;
	}



	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getWeatherZone() {
		return weatherZone;
	}

	public void setWeatherZone(String weatherZone) {
		this.weatherZone = weatherZone;
	}

	public String getFs() {
		return fs;
	}

	public void setFs(String fs) {
		this.fs = fs;
	}

	public String getStateCode() {
		return stateCode;
	}

	public void setStateCode(String stateCode) {
		this.stateCode = stateCode;
	}

	public String getRegionName() {
		return regionName;
	}

	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}

	public String getWeatherUrl() {
		return weatherUrl;
	}

	public void setWeatherUrl(String weatherUrl) {
		this.weatherUrl = weatherUrl;
	}

	public String getDelayIndexUrl() {
		return delayIndexUrl;
	}

	public void setDelayIndexUrl(String delayIndexUrl) {
		this.delayIndexUrl = delayIndexUrl;
	}

	public String getClassification() {
		return classification;
	}

	public void setClassification(String classification) {
		this.classification = classification;
	}

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
