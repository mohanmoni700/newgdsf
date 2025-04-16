package models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import play.db.ebean.Model.Finder;

import javax.persistence.*;

@Entity
@Table(name = "seaman_preloaded_fare_rules")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreloadedSeamanFareRules {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "airline_code")
    private String airlineCode;

    @Column(name = "cancellation_fee_text")
    private String cancellationFeeText;

    @Column(name = "changes_fee_text")
    private String changesFeeText;

    @Column(name = "noshow_fee_text")
    private String noShowFeeText;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAirlineCode() {
        return airlineCode;
    }

    public void setAirlineCode(String airlineCode) {
        this.airlineCode = airlineCode;
    }

    public String getCancellationFeeText() {
        return cancellationFeeText;
    }

    public void setCancellationFeeText(String cancellationFeeText) {
        this.cancellationFeeText = cancellationFeeText;
    }

    public String getChangesFeeText() {
        return changesFeeText;
    }

    public void setChangesFeeText(String changesFeeText) {
        this.changesFeeText = changesFeeText;
    }

    public String getNoShowFeeText() {
        return noShowFeeText;
    }

    public void setNoShowFeeText(String noShowFeeText) {
        this.noShowFeeText = noShowFeeText;
    }


    private static final Finder<Long, PreloadedSeamanFareRules> find = new Finder<>(Long.class, PreloadedSeamanFareRules.class);

    public static int findRowCount() {
        return find.findRowCount();
    }

    public static PreloadedSeamanFareRules findSeamanFareRuleByAirlineCode(String airlineCode) {

        return find.where().eq("airline_code", airlineCode).findUnique();

    }

    public static boolean doesAirlineCodeExist(String airlineCode) {

        boolean doesAirlineFareRuleExist = false;
        PreloadedSeamanFareRules preloadedSeamanFareRules = find.query().where().eq("airline_code", airlineCode).findUnique();

        if (preloadedSeamanFareRules != null) {
            doesAirlineFareRuleExist = true;
        }

        return doesAirlineFareRuleExist;
    }


}