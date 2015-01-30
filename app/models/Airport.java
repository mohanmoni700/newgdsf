package models;

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
public class Airport extends Model implements Serializable {
	public Airport() {

	}

	@Column(name = "id")
	@Id
	private Integer id;
	// @Column(name = "ident")
	// private String ident;
	@Column(name = "type")
	private String type;
	@Column(name = "airport_name")
	private String airportName;
	@Column(name = "latitude")
	private String latitude;
	@Column(name = "longitude")
	private String longitude;
	// @Column(name = "elevation_ft")
	// private String elevation_ft;
	// @Column(name = "continent")
	// private String continent;

	@Column(name = "iso_country")
	private String iso_country;
	// @Column(name = "iso_region")
	// private String iso_region;
	// @Column(name = "scheduled_service")
	// private String scheduled_service;
	@Column(name = "city_name")
	private String cityName;
	@Column(name = "iata_code")
	private String iata_code;
	/*
	 * @Column(name = "gps_code") private String gps_code;
	 */
	@Column(name = "local_code")
	private String local_code;
	@Column(name = "time_zone")
	private String time_zone;
	@Column(name = "dst")
	private String dst;
	@Column(name = "country")
	private String country;
	@Column(name = "gmtOffset")
	private String gmtOffset;

	@Transient
	private String distance;

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

	public static Airport getAiport(String iataCode) {
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

}
