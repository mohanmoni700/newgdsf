package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;
import com.compassites.constants.CacheConstants;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.redis.core.RedisTemplate;
import play.db.ebean.Model.Finder;
import play.libs.Json;
import redis.clients.jedis.Jedis;

import javax.persistence.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.avaje.ebean.Expr.like;

/**
 * Created by Renu on 6/24/14.
 */
@Entity
@Table(name = "airline")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Airline {
	@Id
	private long id;

	/*@Column(name = "instagram")
	private String instagram;

	@Column(name = "linkedin")
	private String linkedin;*/

	@Column(name = "is_passenger")
	private Long isPassenger;

	/*@Column(name = "twitter")
	private String twitter;*/

	@Column(name = "iosa_registered")
	private Long iosaRegistered;

	@Column(name = "three_letter_code")
	private String threeLetterCode;

	@Column(name = "airline_name", unique =true )
	private String airlineName;

	@Column(name = "iata_code" )
	private String iataCode;

	/*@Column(name = "accidents_last_5y")
	private Long accidentsLast5y;

	@Column(name = "total_aircrafts")
	private Long totalAircrafts;

	@Column(name = "callsign")
	private String callsign;*/

	@Column(name = "is_scheduled")
	private Long isScheduled;

	/*@Column(name = "slug")
	private String slug;

	@Column(name = "email")
	private String email;*/

	@Column(name = "is_cargo")
	private Long isCargo;

	@Column(name = "website")
	private String website;

	@Column(name = "average_fleet_age")
	private Long averageFleetAge;

	@Column(name = "kind")
	private String kind;

	/*@Column(name = "facebook")
	private String facebook;*/

	@Column(name = "icao_code")
	private String icaoCode;

	@Column(name = "country_code")
	private String countryCode;

	/*@Column(name = "crashes_last_5y")
	private Long crashesLast5y;*/

	@Column(name = "phone")
	private String phone;

	@Column(name = "iata_prefix")
	private Long iataPrefix;

	@Column(name = "is_international")
	private Long isInternational;

	/*@Column(name = "updated")
	private String updated;

	@Column(name = "seamen_commission")
	private String seamenCommission;

	@Column(name = "non_seamen_commission")
	private String nonSeamenCommission;

	@Column(name = "commission_basis")
	private String commissionBasis;

	@Column(name = "logo_url")
	private String logoUrl;*/

	@Lob
	@Column(name = "airline_logo")
	public byte[] airlineLogo;

	public static Finder<Long, Airline> find = new Finder<Long, Airline>(
			Long.class, Airline.class);
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getAirlineName() {
		return airlineName;
	}

	public void setAirlineName(String airlineName) {
		this.airlineName = airlineName;
	}

	public String getIataCode() {
		return iataCode;
	}

	public void setIataCode(String iataCode) {
		this.iataCode = iataCode;
	}

	public String getThreeLetterCode() {
		return threeLetterCode;
	}

	public void setThreeLetterCode(String threeLetterCode) {
		this.threeLetterCode = threeLetterCode;
	}

	/*public String getSeamenCommission() {
		return seamenCommission;
	}

	public void setSeamenCommission(String seamenCommission) {
		this.seamenCommission = seamenCommission;
	}

	public String getNonSeamenCommission() {
		return nonSeamenCommission;
	}

	public void setNonSeamenCommission(String nonSeamenCommission) {
		this.nonSeamenCommission = nonSeamenCommission;
	}

	
	public String getCommissionBasis() {
		return commissionBasis;
	}

	public void setCommissionBasis(String commissionBasis) {
		this.commissionBasis = commissionBasis;
	}*/

	
	public byte[] getAirlineLogo() {
		return airlineLogo;
	}

	public void setAirlineLogo(byte[] airlineLogo) {
		this.airlineLogo = airlineLogo;
	}

	public static int findRowCount() {
		return find.findRowCount();
	}
	
	public static List<Airline> findAirlineList()
	{
		return find.all();
	}
	
	public static Airline findAirlineById(Long id)
	{
		return find.byId(id);
	}

   /* public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

	public String getInstagram() {
		return instagram;
	}

	public void setInstagram(String instagram) {
		this.instagram = instagram;
	}

	public String getLinkedin() {
		return linkedin;
	}

	public void setLinkedin(String linkedin) {
		this.linkedin = linkedin;
	}*/

	public Long getIsPassenger() {
		return isPassenger;
	}

	public void setIsPassenger(Long isPassenger) {
		this.isPassenger = isPassenger;
	}

	/*public String getTwitter() {
		return twitter;
	}

	public void setTwitter(String twitter) {
		this.twitter = twitter;
	}*/

	public Long getIosaRegistered() {
		return iosaRegistered;
	}

	public void setIosaRegistered(Long iosaRegistered) {
		this.iosaRegistered = iosaRegistered;
	}

	/*public Long getAccidentsLast5y() {
		return accidentsLast5y;
	}

	public void setAccidentsLast5y(Long accidentsLast5y) {
		this.accidentsLast5y = accidentsLast5y;
	}

	public Long getTotalAircrafts() {
		return totalAircrafts;
	}

	public void setTotalAircrafts(Long totalAircrafts) {
		this.totalAircrafts = totalAircrafts;
	}

	public String getCallsign() {
		return callsign;
	}

	public void setCallsign(String callsign) {
		this.callsign = callsign;
	}*/

	public Long getIsScheduled() {
		return isScheduled;
	}

	public void setIsScheduled(Long isScheduled) {
		this.isScheduled = isScheduled;
	}

	/*public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}*/

	public Long getIsCargo() {
		return isCargo;
	}

	public void setIsCargo(Long isCargo) {
		this.isCargo = isCargo;
	}

	public String getWebsite() {
		return website;
	}

	public void setWebsite(String website) {
		this.website = website;
	}

	public Long getAverageFleetAge() {
		return averageFleetAge;
	}

	public void setAverageFleetAge(Long averageFleetAge) {
		this.averageFleetAge = averageFleetAge;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	/*public String getFacebook() {
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

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	/*public Long getCrashesLast5y() {
		return crashesLast5y;
	}

	public void setCrashesLast5y(Long crashesLast5y) {
		this.crashesLast5y = crashesLast5y;
	}*/

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public Long getIataPrefix() {
		return iataPrefix;
	}

	public void setIataPrefix(Long iataPrefix) {
		this.iataPrefix = iataPrefix;
	}

	public Long getIsInternational() {
		return isInternational;
	}

	public void setIsInternational(Long isInternational) {
		this.isInternational = isInternational;
	}

	/*public String getUpdated() {
		return updated;
	}

	public void setUpdated(String updated) {
		this.updated = updated;
	}*/

	public static List<SqlRow> findCommissionbasicList()
	{
		String sqlQuery = "SELECT distinct commission_basis FROM airline WHERE  commission_basis != '' ";

		SqlQuery sqlQuery1 = Ebean.createSqlQuery(sqlQuery);

		return sqlQuery1.findList();
	}
	
	public static List<Airline> findAirLinesByCodes(List<String> airlines) {
		return find.where().in("iata_code", airlines).findList();
	}
	
	public static List<Airline> findAirlinesByQuery(String query) {
		return find.where().or(like("iata_code", "%" + query + "%"),like("airline_name", "%" + query + "%")).findList();
	}
	
	public static Airline findAirLineNameByCode(String iataCode) {
		// TODO: use findUnique when we have unique iata codes
		// AirlineCode airlineCode = AirlineCode.find.where().eq("iata_code", IATA_code).findUnique();
		return find.where().eq("iata_code", iataCode).findList().get(0);
	}
	

	private static Airline getAirlineByCode(String airlineCode) {
		String cacheKey = "Airline#" + airlineCode;
		Jedis j = new Jedis("localhost", 6379);
		j.connect();
		String airlineJson = j.get(cacheKey);
		Airline airline = null;
		if (airlineJson != null) {
			airline = Json.fromJson(Json.parse(airlineJson), Airline.class);
		} else {
			List<Airline> airlines = find.where().eq("iata_code", airlineCode).findList();
			if (airlines.size() > 0) {
				airline = find.where().eq("iata_code", airlineCode).findList().get(0);
			} else {
				airline = new Airline();
				airline.setIataCode(airlineCode);
			}
			j.setex(cacheKey, CacheConstants.CACHE_TIMEOUT_IN_SECS, Json.toJson(airline).toString());
		}
		j.disconnect();
		return airline;
	}

	public static Airline getAirlineByCode(String airlineCode, RedisTemplate redisTemplate) {
		String cacheKey = "Airline#" + airlineCode;
		String airlineJson =  null;

		Airline airline = null;
		if (redisTemplate.opsForValue().get(cacheKey) != null) {
			airlineJson =  (String) redisTemplate.opsForValue().get(cacheKey);
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
