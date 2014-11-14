package models;

import static com.avaje.ebean.Expr.like;
import static com.avaje.ebean.Expr.or;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import play.db.ebean.Model.Finder;
import play.libs.Json;
import redis.clients.jedis.Jedis;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;
import com.compassites.constants.CacheConstants;

/**
 * Created by Renu on 6/24/14.
 */
@Entity
@Table(name = "airline_code")
public class AirlineCode {
	@Id
	@Column(name = "id")
	public Long id;
	
	/*@Column(name = "icao_code")
	public String ICAO_code;*/
	
	@Column(name = "iata_code")
	public String IATA_code;
	
	@Column(name = "airline")
	public String airline;
	
	/*@Column(name = "call_sign")
	public String call_sign;*/
	
	@Column(name = "country_name")
	public String country_name;
	
	/*@Column(name = "comments")
	public String comments;*/
	
	@Lob
	@Column(name = "logo")
	public byte[] logo;
	
	@Column(name = "commission")
	private long commission;
	
	public long getCommission() {
		return commission;
	}

	public void setCommission(long commission) {
		this.commission = commission;
	}

	private static Finder<Long, AirlineCode> find = new Finder<Long, AirlineCode>(
			Long.class, AirlineCode.class);

	public AirlineCode() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/*public String getICAO_code() {
		return ICAO_code;
	}

	public void setICAO_code(String iCAO_code) {
		ICAO_code = iCAO_code;
	}*/

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

	/*public String getCall_sign() {
		return call_sign;
	}

	public void setCall_sign(String call_sign) {
		this.call_sign = call_sign;
	}*/

	public String getCountry_name() {
		return country_name;
	}

	public void setCountry_name(String country_name) {
		this.country_name = country_name;
	}

	/*public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}*/

	public byte[] getLogo() {
		return logo;
	}

	public void setLogo(byte[] logo) {
		this.logo = logo;
	}


	public static List<SqlRow> findAirlineList() {

		String sqlQuery = "SELECT id,iata_code,airline,logo FROM airline_code order by airline";

		SqlQuery sqlQuery1 = Ebean.createSqlQuery(sqlQuery);

		return sqlQuery1.findList();
	}

	public static AirlineCode findAirlineById(Long id) {
		return Ebean.find(AirlineCode.class, id);
	}

	public List<AirlineCode> findAirlinesByQuery(String query) {
		return find
				.where()
				.or(or(like("ICAO_code", "%" + query + "%"),
						like("IATA_code", "%" + query + "%")),
						or(like("airline", "%" + query + "%"),
								like("country_name", "%" + query + "%")))
				.findList();
	}

	public static int findRowCount() {
		return find.findRowCount();
	}

	
	
	

	public static AirlineCode getAirlineByCode(String airlineCode) {
		String cacheKey = "Airline#" + airlineCode;
		Jedis j = new Jedis("localhost", 6379);
		j.connect();
		String airlineJson = j.get(cacheKey);
		AirlineCode airline = null;
		if (airlineJson != null) {
			airline = Json.fromJson(Json.parse(airlineJson), AirlineCode.class);

		} else {
			List<AirlineCode> airlines = find.where()
					.eq("iata_code", airlineCode).findList();
			if (airlines.size() > 0)
				airline = find.where().eq("iata_code", airlineCode).findList()
						.get(0);
			else
				airline = new AirlineCode();
			j.setex(cacheKey, CacheConstants.CACHE_TIMEOUT_IN_SECS, Json
					.toJson(airline).toString());

		}
		j.disconnect();
		return airline;
	}

}
