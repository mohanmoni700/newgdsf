package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;
import com.compassites.constants.CacheConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class Airline {



	@Id
	private long id;

	@Column(name = "airline_name", unique =true )
	private String airlineName;

	@Column(name = "iata_code" )
	private String iataCode;

	@Column(name = "icao_code")
	private String icaoCode;

//	@Column(name = "seamen_commission")
//	private String seamenCommission;
//
//	@Column(name = "non_seamen_commission")
//	private String nonSeamenCommission;
//
//	@Column(name = "commission_basis")
//	private String commissionBasis;

	@Column(name = "logo_url")
	private String logoUrl;

	@Lob
	@Column(name = "airline_logo")
	public byte[] airlineLogo;

	@Column(name = "fs")
	private String fs;

	@Column(name = "phone")
	private String phone;


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


//	public String getSeamenCommission() {
//		return seamenCommission;
//	}
//
//	public void setSeamenCommission(String seamenCommission) {
//		this.seamenCommission = seamenCommission;
//	}
//
//	public String getNonSeamenCommission() {
//		return nonSeamenCommission;
//	}
//
//	public void setNonSeamenCommission(String nonSeamenCommission) {
//		this.nonSeamenCommission = nonSeamenCommission;
//	}
//
//
//	public String getCommissionBasis() {
//		return commissionBasis;
//	}
//
//	public void setCommissionBasis(String commissionBasis) {
//		this.commissionBasis = commissionBasis;
//	}


	public byte[] getAirlineLogo() {
		return airlineLogo;
	}

	public void setAirlineLogo(byte[] airlineLogo) {
		this.airlineLogo = airlineLogo;
	}


	public String getIcaoCode() {
		return icaoCode;
	}

	public void setIcaoCode(String icaoCode) {
		this.icaoCode = icaoCode;
	}


	public String getLogoUrl() {
		return logoUrl;
	}

	public void setLogoUrl(String logoUrl) {
		this.logoUrl = logoUrl;
	}

	public static Finder<Long, Airline> find = new Finder<Long, Airline>(
			Long.class, Airline.class);
	


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
