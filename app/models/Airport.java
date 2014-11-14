package models;

import play.db.ebean.Model;
import play.db.ebean.Model.Finder;
import play.libs.Json;
import redis.clients.jedis.Jedis;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

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
	private String elevation_ft;
	
	@Column(name = "continent")
	private String continent;*/

	@Column(name = "iso_country")
	private String iso_country;
	
	/*@Column(name = "iso_region")
	private String iso_region;
	
	@Column(name = "scheduled_service")
	private String scheduled_service;
	*/
	@Column(name = "city_name")
	private String cityName;
	
	@Column(name = "iata_code")
	private String iata_code;
	
	@Column(name = "gps_code")
	private String gps_code;
	
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

	public static final String IATA_CODE = "iata_code";

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

	private static Finder<Integer, Airport> find = new Finder<Integer, Airport>(
			Integer.class, Airport.class);

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	/*public String getIdent() {
		return ident;
	}

	public void setIdent(String ident) {
		this.ident = ident;
	}*/

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

	/*public String getElevation_ft() {
		return elevation_ft;
	}

	public void setElevation_ft(String elevation_ft) {
		this.elevation_ft = elevation_ft;
	}

	public String getContinent() {
		return continent;
	}

	public void setContinent(String continent) {
		this.continent = continent;
	}*/

	public String getIso_country() {
		return iso_country;
	}

	public void setIso_country(String iso_country) {
		this.iso_country = iso_country;
	}

	/*public String getIso_region() {
		return iso_region;
	}

	public void setIso_region(String iso_region) {
		this.iso_region = iso_region;
	}*/

	/*public String getScheduled_service() {
		return scheduled_service;
	}

	public void setScheduled_service(String scheduled_service) {
		this.scheduled_service = scheduled_service;
	}*/

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

	public String getGps_code() {
		return gps_code;
	}

	public void setGps_code(String gps_code) {
		this.gps_code = gps_code;
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

	public static int findRowCount() {
		return find.findRowCount();
	}

	public static Airport getAirport(String query) {
		return find.where().like("iata_code", "%" + query + "%").findList()
				.get(0);
	}

	public static List<Airport> getAIrport() {
		return find.where().order("airportName").findList();

	}

	public static Airport findAirportById(int id) {
		return find.byId(id);
	}

	public static Airport findAirportByIataCode(String IATA_code) {
		return find.where().eq("iata_code", IATA_code).findUnique();
	}

	public static List<Airport> findAirports(Set<String> airportCodes) {
		return find.where().in(Airport.IATA_CODE, airportCodes).findList();
	}

	public static Airport getAiport(String iataCode) {
		Jedis j = new Jedis("localhost", 6379);
		j.connect();
		String airportJson = j.get(iataCode);
		Airport airport = null;
		if (airportJson != null) {
			airport = Json.fromJson(Json.parse(airportJson), Airport.class);

		} else {
			airport = find.where().eq("iata_code", iataCode).findList().get(0);
			j.set(iataCode, Json.toJson(airport).toString());

		}
		j.disconnect();
		return airport;
	}
}
