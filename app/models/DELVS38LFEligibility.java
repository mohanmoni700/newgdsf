package models;

import play.db.ebean.Model;

import javax.persistence.*;

@Entity
@Table(name = "DELVS38LF_eligibility")
public class DELVS38LFEligibility extends Model{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (name = "airline_code")
    private String airlineCode;

    @Column (name = "is_refund", columnDefinition = "TINYINT(1) default '0'")
    private boolean isRefund;

    @Column (name = "is_reissue", columnDefinition = "TINYINT(1) default '0'")
    private boolean isReissue;

    private static final Model.Finder<Long, DELVS38LFEligibility> find = new Model.Finder<>(Long.class, DELVS38LFEligibility.class);

    public static int findRowCount() {
        return find.findRowCount();
    }

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

    public boolean isRefund() {
        return isRefund;
    }

    public void setRefund(boolean refund) {
        isRefund = refund;
    }

    public boolean isReissue() {
        return isReissue;
    }

    public void setReissue(boolean reissue) {
        isReissue = reissue;
    }


    public static DELVS38LFEligibility getEligibleAirlineCodeByValidatingCarrier (String validatingCarrierCode) {

        return  find.where().eq("airlineCode", validatingCarrierCode).findUnique();

    }


}
