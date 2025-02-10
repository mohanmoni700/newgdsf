package models;

import com.avaje.ebean.annotation.ConcurrencyMode;
import com.avaje.ebean.annotation.EntityConcurrencyMode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.db.ebean.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.List;

@Entity
@Table(name = "split_transit_airports")
@EntityConcurrencyMode(ConcurrencyMode.NONE)
public class SplitTicketTransitAirports extends Model {

    static Logger logger = LoggerFactory.getLogger("joc");
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "airport")
    private String airport;

    @Column(name = "transit_airport")
    private String transitAirport;

    @Column(name = "airline")
    private String airline;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAirport() {
        return airport;
    }

    public void setAirport(String airport) {
        this.airport = airport;
    }

    public String getTransitAirport() {
        return transitAirport;
    }

    public void setTransitAirport(String transitAirport) {
        this.transitAirport = transitAirport;
    }

    public String getAirline() {
        return airline;
    }

    public void setAirline(String airline) {
        this.airline = airline;
    }

    public static Model.Finder<Long, SplitTicketTransitAirports> find = new Model.Finder<Long, SplitTicketTransitAirports>(Long.class, SplitTicketTransitAirports.class);

    @JsonIgnore
    public static List<SplitTicketTransitAirports> getAllTransitPoints() {
        return SplitTicketTransitAirports.find.all();
    }

    public static List<SplitTicketTransitAirports> getAllTransitByIata(String iataCode) {
        List<SplitTicketTransitAirports> splitTicketTransitAirports = SplitTicketTransitAirports.find.where().eq("airport", iataCode).findList();
        return splitTicketTransitAirports;
    }
}

